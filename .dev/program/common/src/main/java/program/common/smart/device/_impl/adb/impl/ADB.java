/*
 *  Copyright (C) 2023 the original author or authors
 *
 *  This program is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */
package program.common.smart.device._impl.adb.impl;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import program.common.basic.exception.InvocationException;
import program.common.basic.resource.ResUtl;
import program.common.basic.resource.SilentCloseable;
import program.common.basic.task.Task;
import program.common.basic.utility.StrUtl;
import program.common.basic.utility.WeakSet;
import program.common.basic.vision.Area;
import program.common.basic.vision.Image;
import program.common.smart.device._impl.ScreenCapture;

import java.io.IOException;
import java.io.InputStream;
import java.net.InetSocketAddress;

import static java.lang.String.format;

/**
 * ADB.
 *
 * @author wautsns
 * @since {{{SINCE_PLACEHOLDER}}}
 */
@RequiredArgsConstructor(access = AccessLevel.PRIVATE)
public final class ADB implements SilentCloseable {

    // *****************************************************************************************
    // StaticMethods, initializing instance
    // *****************************************************************************************

    private static final WeakSet<ADB> REFERENCES = new WeakSet<>();

    // Note: The holder identifies which objects hold the adb; When the adb is released, the close
    // method is called if and only if there is no holder.
    //
    // WARNING: The given holder needs to be strongly referenced by the caller of this method
    // because the ADB only holds its weak reference.
    public static ADB of(Object holder, String address, int timeout) {
        String taskName = format("ADB#of(<holder>,%s,%d)", address, timeout);
        return Task.basic(taskName, () -> {
            ADB adb;
            String[] hostAndPort = address.split(":", 2);
            String host = hostAndPort[0];
            int port = (hostAndPort.length == 1) ? 5555 : Integer.parseInt(hostAndPort[1]);
            InetSocketAddress socketAddress = new InetSocketAddress(host, port);
            synchronized (REFERENCES) {
                adb = REFERENCES.get(ref -> ref.socket.address.equals(socketAddress));
                if (adb != null) {
                    if (adb.socket.delegate.isClosed()) {
                        REFERENCES.remove(adb);
                    } else {
                        adb.holders.add(holder);
                        return adb;
                    }
                }
                adb = REFERENCES.add(new ADB(new ADBSocket(socketAddress)));
            }
            adb.holders.add(holder);
            return adb;
        }).submit().waitUntilComplete(timeout);
    }

    // *****************************************************************************************
    // *****************************************************************************************

    private final ADBSocket socket;

    private final WeakSet<Object> holders = new WeakSet<>();
    private final WeakSet<Runnable> callbacksBeforeClosing = new WeakSet<>();

    // *****************************************************************************************
    // Methods, getting metadata
    // *****************************************************************************************

    public InetSocketAddress socketAddress() {
        return socket.address;
    }

    public String lineSepInShell() {
        return socket.lineSepInShell;
    }

    public InputStream fixLineSepInShellIfNeeded(ADBStream.Input input) {
        return socket.fixLineSepInShellIfNeeded(input);
    }

    // *****************************************************************************************
    // Methods, opening stream
    // *****************************************************************************************

    public ADBStream open(String destination) {
        return socket.open(destination);
    }

    public byte[] openAndReadAllBytes(String destination) {
        // noinspection resource (adb stream will be auto closed on CLSE received)
        return open(destination).input().readAllBytes();
    }

    public String openAndReadAllString(String destination) {
        // noinspection resource (adb stream will be auto closed on CLSE received)
        return open(destination).input().readAllString();
    }

    public void openAndSkipAll(String destination) {
        // noinspection resource (adb stream will be auto closed on CLSE received)
        open(destination).input().skipAll();
    }

    // *****************************************************************************************
    // Methods, opening stream - shell
    // *****************************************************************************************

    public String shell(String command) {
        // shell:<command>\0
        int n = 7 + command.length();
        StringBuilder bu = new StringBuilder(n);
        bu.append("shell:").append(command).append('\0');
        return openAndReadAllString(bu.toString());
    }

    // *****************************************************************************************
    // Methods, opening stream - shell:<getprop>
    // *****************************************************************************************

    public String getprop(String options) {
        // shell:getprop <options>\0
        int n = 15 + options.length();
        StringBuilder bu = new StringBuilder(n);
        bu.append("shell:getprop ").append(options).append('\0');
        return openAndReadAllString(bu.toString());
    }

    public String getpropCpuAbi() {
        String destination = "shell:getprop ro.product.cpu.abi\0";
        return openAndReadAllString(destination);
    }

    public int getpropSdkVersion() {
        String destination = "shell:getprop ro.build.version.sdk\0";
        String string = openAndReadAllString(destination);
        return Integer.parseInt(string);
    }

    // *****************************************************************************************
    // Methods, opening stream - shell:<am>
    // *****************************************************************************************

    public boolean isAppRunning(String packageName) {
        int n = 16 + packageName.length();
        StringBuilder bu = new StringBuilder(n);
        bu.append("shell:pidof -s ").append(packageName).append('\0');
        return !openAndReadAllString(bu.toString()).isBlank();
    }

    public void startApp(String packageName, String activityName) {
        // shell:am start <packageName>/<activityName>\0
        int n = 17 + packageName.length() + packageName.length();
        StringBuilder bu = new StringBuilder(n);
        bu.append("shell:am start ").append(packageName).append('/').append(activityName)
                .append('\0');
        openAndSkipAll(bu.toString());
    }

    public void forceStopApp(String packageName) {
        // shell:am force-stop <packageName>\0
        int n = 21 + packageName.length();
        StringBuilder bu = new StringBuilder(n);
        bu.append("shell:am force-stop ").append(packageName).append('\0');
        openAndSkipAll(bu.toString());
    }

    // *****************************************************************************************
    // Methods, opening stream - shell:<input>
    // *****************************************************************************************

    public void tap(int x, int y) {
        String xStr = StrUtl.decimal(x);
        String yStr = StrUtl.decimal(y);
        // shell:input tap <x> <y>\0
        int n = 18 + xStr.length() + yStr.length();
        StringBuilder bu = new StringBuilder(n);
        bu.append("shell:input tap ").append(xStr).append(' ').append(yStr).append('\0');
        openAndSkipAll(bu.toString());
    }

    public void swipe(int x1, int y1, int x2, int y2, int duration) {
        String x1Str = StrUtl.decimal(x1);
        String y1Str = StrUtl.decimal(y1);
        String x2Str = StrUtl.decimal(x2);
        String y2Str = StrUtl.decimal(y2);
        String durationStr = StrUtl.decimal(duration);
        // shell:input swipe <x1> <y1> <x2> <y2> <duration>\0
        int n = 23 + x1Str.length() + y1Str.length() + x2Str.length() + y2Str.length()
                + durationStr.length();
        StringBuilder bu = new StringBuilder(n);
        bu.append("shell:input swipe ").append(x1Str).append(' ').append(y1Str).append(' ')
                .append(x2Str).append(' ').append(y2Str).append(' ').append(durationStr)
                .append('\0');
        openAndSkipAll(bu.toString());
    }

    // *****************************************************************************************
    // Methods, opening stream - shell:<screencap>
    // *****************************************************************************************

    public Image screencap() {
        Area area = ScreenCapture.SCREEN_AREA;
        String destination = "shell:screencap -p\0";
        // noinspection resource (adb stream will be auto closed on CLSE received)
        InputStream stream = fixLineSepInShellIfNeeded(open(destination).input());
        Image image = Image.read(area.name(), stream);
        if ((area.width() != image.width()) || (area.height() != image.height())) {
            String message = "Unexpected screen size";
            throw new InvocationException(message)
                    .with("expected_size", format("%dx%d", area.width(), area.height()))
                    .with("provided_size", format("%dx%d", image.width(), image.height()));
        }
        return image;
    }

    // *****************************************************************************************
    // Methods, opening stream - sync:<file>
    // *****************************************************************************************

    public void push(String local, String remote, String mode) {
        try (ADBStream stream = open("sync:\0")) {
            int capacity = Math.min(ADBPackage.MAX_PAYLOAD, 64 * 1024);
            ADBStream.Output output = stream.output();
            output.setBuffer(capacity);
            output.writeAscii("SEND")
                    .writeIntLE(remote.length() + 1 + mode.length())
                    .writeAscii(remote).writeAscii(",").writeAscii(mode);
            output.flush();
            try {
                try (InputStream resource = ResUtl.loadAsStream(local)) {
                    byte[] buffer = output.writeAscii("DATA").getBuffer();
                    for (int n; ; ) {
                        n = resource.read(buffer, 8, capacity - 8);
                        if (n <= 0) {break;}
                        output.setOffset(4).writeIntLE(n).setOffset(8 + n);
                        output.flush();
                    }
                }
            } catch (IOException e) {
                throw new InvocationException(e)
                        .with("local_file_path", local);
            }
            int lastUpdateTime = (int) (System.currentTimeMillis() / 1000);
            output.writeAscii("DONE").writeIntLE(lastUpdateTime);
            output.flush();
        }
    }

    // *****************************************************************************************
    // Methods, adding callback
    // *****************************************************************************************

    // WARNING: The given callback needs to be strongly referenced by the caller of this method
    // because this instance only holds its weak reference.
    //
    // @return the give callback
    public Object callbackBeforeClosing(Runnable callback) {
        callbacksBeforeClosing.add(callback);
        return callback;
    }

    // *****************************************************************************************
    // Methods, releasing adb
    // *****************************************************************************************

    public synchronized void release(Object holder) {
        if (holders.remove(holder) && holders.isEmpty()) {
            close();
        }
    }

    // *****************************************************************************************
    // OverrideMethods, SilentCloseable
    // *****************************************************************************************

    @Override
    public synchronized boolean closed() {
        return socket.closed();
    }

    @Override
    public synchronized void close() {
        if (socket.closed()) {return;}
        holders.clear();
        callbacksBeforeClosing.forEach(Runnable::run);
        socket.close();
        REFERENCES.remove(this);
    }

}
