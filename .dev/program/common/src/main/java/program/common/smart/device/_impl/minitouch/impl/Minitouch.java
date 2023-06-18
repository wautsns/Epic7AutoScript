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
package program.common.smart.device._impl.minitouch.impl;

import lombok.Getter;
import lombok.experimental.Accessors;
import program.common.basic.logger.Logger;
import program.common.basic.resource.ResUtl;
import program.common.basic.resource.SilentCloseable;
import program.common.basic.task.Task;
import program.common.basic.utility.StrUtl;
import program.common.basic.utility.WeakSet;
import program.common.basic.vision.Area;
import program.common.smart.device._impl.ScreenCapture;
import program.common.smart.device._impl.adb.impl.ADB;
import program.common.smart.device._impl.adb.impl.ADBStream;

import java.io.ByteArrayOutputStream;

import static java.lang.String.format;

/**
 * Minitouch.
 *
 * @author wautsns
 * @since {{{SINCE_PLACEHOLDER}}}
 */
@Accessors(fluent = true)
public final class Minitouch implements SilentCloseable {

    // *****************************************************************************************
    // StaticMethods, initializing instance
    // *****************************************************************************************

    private static final WeakSet<Minitouch> REFERENCES = new WeakSet<>();

    public static Minitouch of(ADB adb, Object adbHolder, int timeout) {
        String taskName = format("Minitouch#of(%s,<adbHolder>,%d)", adb.socketAddress(), timeout);
        return Task.basic(taskName, () -> {
            return REFERENCES.add(
                    ref -> ref.adb == adb,
                    () -> new Minitouch(adb, adbHolder)
            );
        }).submit().waitUntilComplete(timeout);
    }

    // *****************************************************************************************
    // *****************************************************************************************

    private final @Getter ADB adb;
    private final Object adbHolder;
    @SuppressWarnings({"FieldCanBeLocal", "unused"}) // strongly referenced by this instance
    private final Object callbackBeforeADBClosing;

    private final ADBStream serverStream;
    private final ADBStream clientStream;

    private final double xScaling;
    private final double yScaling;
    private final String pressure;

    private final CommandsBuffer commands = new CommandsBuffer();
    private int waitDuration = 0;

    private boolean closed = false;

    // *****************************************************************************************
    // Methods, initializing command
    // *****************************************************************************************

    public synchronized Minitouch d(int contact, int x, int y) {
        String contactStr = StrUtl.decimal(contact);
        String xStr = StrUtl.decimal(x);
        String yStr = StrUtl.decimal(y);
        // d <contact> <x> <y> <pressure>\n
        int n = 6 + contactStr.length() + xStr.length() + yStr.length() + pressure.length();
        commands.ensureCapacity(n)
                .writeAscii("d ").writeAscii(contactStr).writeChar(' ').writeAscii(xStr)
                .writeChar(' ').writeAscii(yStr).writeChar(' ').writeAscii(pressure)
                .writeChar('\n');
        return this;
    }

    public synchronized Minitouch u(int contact) {
        String contactStr = StrUtl.decimal(contact);
        // u <contact>\n
        int n = 3 + contactStr.length();
        commands.ensureCapacity(n)
                .writeAscii("u ").writeAscii(contactStr).writeChar('\n');
        return this;
    }

    public synchronized Minitouch m(int contact, int x, int y) {
        String contactStr = StrUtl.decimal(contact);
        String xStr = StrUtl.decimal((int) (x * xScaling));
        String yStr = StrUtl.decimal((int) (y * yScaling));
        // m <contact> <x> <y> <pressure>\n
        int n = 6 + contactStr.length() + xStr.length() + yStr.length();
        commands.ensureCapacity(n)
                .writeAscii("m ").writeAscii(contactStr).writeChar(' ').writeAscii(xStr)
                .writeChar(' ').writeAscii(yStr).writeChar(' ').writeAscii(pressure)
                .writeChar('\n');
        return this;
    }

    public synchronized Minitouch c() {
        // c\n
        commands.ensureCapacity(2)
                .writeAscii("c\n");
        return this;
    }

    public synchronized Minitouch w(int duration) {
        String durationStr = StrUtl.decimal(duration);
        // w <duration>\n
        int n = 3 + durationStr.length();
        commands.ensureCapacity(n)
                .writeAscii("w ").writeAscii(durationStr).writeChar('\n');
        waitDuration += duration;
        return this;
    }

    // *****************************************************************************************
    // Methods, sending commands
    // *****************************************************************************************

    public synchronized Minitouch send() {
        clientStream.output().setBuffer(commands.getBuffer()).setOffset(commands.size()).flush();
        Task.sleep(waitDuration);
        commands.reset();
        waitDuration = 0;
        return this;
    }

    // *****************************************************************************************
    // OverrideMethods, SilentCloseable
    // *****************************************************************************************

    @Override
    public synchronized boolean closed() {
        return closed;
    }

    @Override
    public synchronized void close() {
        if (closed) {return;}
        closeBeforeADBClosing();
        adb.release(adbHolder);
    }

    // *****************************************************************************************
    // InternalConstructors
    // *****************************************************************************************

    private Minitouch(ADB adb, Object adbHolder) {
        Logger.title(3, "[minitouch] init");
        Logger.attribute("adbd.address", adb.socketAddress());
        this.adb = adb;
        this.adbHolder = adbHolder;
        this.serverStream = startMinitouchServerIfNeeded();
        this.clientStream = connectMinitouchServer();
        Area screenArea = ScreenCapture.SCREEN_AREA;
        // line: ^ <max-contacts> <max-x> <max-y> <max-pressure>
        String[] args = clientStream.input().readLine().split(" ");
        this.xScaling = (Double.parseDouble(args[2]) + 1) / screenArea.width();
        this.yScaling = (Double.parseDouble(args[3]) + 1) / screenArea.height();
        this.pressure = StrUtl.decimal((int) Math.max(Double.parseDouble(args[4]) * 0.75, 1));
        clientStream.input().readLine(); // line: $ <pid>
        this.callbackBeforeADBClosing = adb.callbackBeforeClosing(this::closeBeforeADBClosing);
        Logger.info("minitouch init okay");
        Logger.emptyLine();
    }

    // *****************************************************************************************
    // InternalMethods
    // *****************************************************************************************

    private ADBStream startMinitouchServerIfNeeded() {
        if (!adb.shell("pidof -s minitouch").isBlank()) {
            Logger.info("minitouch server has started");
            return null;
        }
        if (adb.shell("find /data/local/tmp -name minitouch").isBlank()) {
            Logger.info("minitouch file not found under `/data/local/tmp`");
            String cpuAbi = adb.getpropCpuAbi();
            String suffix = adb.getpropSdkVersion() >= 16 ? "" : "-nopie";
            String local = ResUtl.home("/.bin/minitouch/%s/minitouch%s", cpuAbi, suffix);
            Logger.info("pushing %s ...", local);
            adb.push(local, "/data/local/tmp/minitouch", "0777");
        }
        Logger.info("starting minitouch server...");
        ADBStream stream = adb.open("shell:/data/local/tmp/minitouch\0");
        stream.input().peek();
        return stream;
    }

    private ADBStream connectMinitouchServer() {
        Logger.info("connecting minitouch server...");
        ADBStream stream;
        do {
            Task.sleep(200);
            stream = adb.open("localabstract:minitouch\0");
        } while (stream.input().peek() == -1);
        stream.input().readLine();  // line: v <version>
        stream.output().setBuffer(128);
        return stream;
    }

    private synchronized void closeBeforeADBClosing() {
        if (this.closed) {return;}
        Logger.title(3, "[minitouch] close");
        Logger.attribute("adbd.address", adb.socketAddress());
        Logger.info("closing minitouch...");
        this.clientStream.close();
        if (this.serverStream != null) {
            this.serverStream.close();
        }
        REFERENCES.remove(this);
        this.closed = true;
        Logger.info("minitouch close okay");
        Logger.emptyLine();
    }

    // *****************************************************************************************
    // InternalStaticClasses
    // *****************************************************************************************

    private static class CommandsBuffer extends ByteArrayOutputStream {

        CommandsBuffer ensureCapacity(int lengthToWrite) {
            int limit = buf.length - count;
            if (lengthToWrite > limit) {
                byte[] newBuf = new byte[count + lengthToWrite];
                System.arraycopy(buf, 0, newBuf, 0, count);
                buf = newBuf;
            }
            return this;
        }

        CommandsBuffer writeChar(char value) {
            buf[count++] = (byte) value;
            return this;
        }

        CommandsBuffer writeAscii(String value) {
            for (int i = 0, l = value.length(); i < l; i++) {
                buf[count++] = (byte) value.charAt(i);
            }
            return this;
        }

        byte[] getBuffer() {
            return buf;
        }

        CommandsBuffer() {
            super(128);
        }

    }

}
