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

import program.common.basic.exception.InvocationException;
import program.common.basic.logger.Logger;
import program.common.basic.resource.SilentCloseable;
import program.common.basic.task.Task;

import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * ADB socket. package class, used by `ADB` & `ADBStream`
 *
 * @author wautsns
 * @since {{{SINCE_PLACEHOLDER}}}
 */
final class ADBSocket implements SilentCloseable {

    private static final Logger logger = new Logger(ADBSocket.class);

    // *****************************************************************************************
    // *****************************************************************************************

    final InetSocketAddress address;

    final Socket delegate;
    private final InputStream input;
    private final OutputStream output;

    private final Thread receivingThread;

    private final AtomicInteger nextLocalId = new AtomicInteger(1);
    final Map<Integer, ADBStream> streamMap = new ConcurrentHashMap<>();

    final String lineSepInShell;

    // *****************************************************************************************
    // Methods, opening stream
    // *****************************************************************************************

    ADBStream open(String destination) {
        int localId = nextLocalId.getAndIncrement();
        ADBStream stream = new ADBStream(this, localId);
        streamMap.put(localId, stream);
        send(ADBPackage.initOPEN(localId, destination));
        return stream;
    }

    InputStream fixLineSepInShellIfNeeded(ADBStream.Input input) {
        if ("\r\n".equals(lineSepInShell)) {
            return new FixWindowsLineSepInputStream(input);
        } else {
            return input;
        }
    }

    // *****************************************************************************************
    // Methods, sending package
    // *****************************************************************************************

    void send(ADBPackage pakkage) {
        logger.debug("adbd{%s} <= package%s", address, pakkage);
        synchronized (output) {
            try {
                writeIntLE(pakkage.command);
                writeIntLE(pakkage.arg0);
                writeIntLE(pakkage.arg1);
                writeIntLE(pakkage.length);
                writeIntLE(pakkage.crc32);
                writeIntLE(pakkage.magic);
                output.write(pakkage.buffer, 0, pakkage.length);
                output.flush();
            } catch (IOException e) {
                throw new InvocationException(e)
                        .with("socket_address", address);
            }
        }
    }

    // *****************************************************************************************
    // OverrideMethods, SilentCloseable
    // *****************************************************************************************

    @Override
    public synchronized boolean closed() {
        return delegate.isClosed();
    }

    @Override
    public synchronized void close() {
        if (delegate.isClosed()) {return;}
        Logger.title(3, "[adb] close");
        Logger.attribute("adbd.address", address);
        Logger.info("closing adb...");
        streamMap.values().forEach(ADBStream::close);
        try {
            delegate.close();
        } catch (IOException e) {
            throw new InvocationException(e)
                    .with("socket_address", address);
        }
        Task.sleep(50);
        if (receivingThread.isAlive()) {
            boolean alive = true;
            for (int i = 0; i < 50 && alive; i++) {
                Task.sleep(100);
                alive = receivingThread.isAlive();
            }
            if (alive) {
                receivingThread.interrupt();
                Logger.warn("receiving thread was forced to interrupt due to abnormal state");
            }
        }
        Logger.info("adb close okay");
        Logger.emptyLine();
    }

    // *****************************************************************************************
    // Constructors
    // *****************************************************************************************

    ADBSocket(InetSocketAddress address) {
        Logger.title(3, "[adb] init");
        Logger.attribute("adbd.address", address);
        this.address = address;
        this.delegate = new Socket();
        Logger.info("connecting adbd...");
        try {
            delegate.connect(address);
            this.input = delegate.getInputStream();
            this.output = delegate.getOutputStream();
        } catch (IOException e) {
            throw new InvocationException(e)
                    .with("socket_address", address);
        }
        send(ADBPackage.initCNXN("host::\0"));
        // noinspection StatementWithEmptyBody
        while (receive().command != ADBPackage.A_CNXN) {}
        Logger.info("starting receiving thread...");
        this.receivingThread = new Thread(() -> {
            while (true) {
                ADBPackage pakkage;
                try {
                    pakkage = receive();
                } catch (InvocationException e) {
                    Throwable cause = e.getCause();
                    if ((cause instanceof SocketException)
                            && "Socket closed".equals(cause.getMessage())) {
                        Logger.info("receiving thread stopped");
                        break;
                    }
                    throw e;
                }
                switch (pakkage.command) {
                    case ADBPackage.A_AUTH -> {
                        // TODO resolve ADBPackage.A_AUTH
                    }
                    case ADBPackage.A_OKAY -> {
                        ADBStream stream = streamMap.get(pakkage.arg1);
                        if (stream == null) {continue;}
                        stream.handleOnOKAYReceived(pakkage.arg0);
                    }
                    case ADBPackage.A_WRTE -> {
                        ADBStream stream = streamMap.get(pakkage.arg1);
                        if (stream == null) {continue;}
                        stream.handleOnWRTEReceived(pakkage.buffer);
                    }
                    case ADBPackage.A_CLSE -> {
                        // noinspection resource
                        ADBStream stream = streamMap.remove(pakkage.arg1);
                        if (stream == null) {continue;}
                        stream.handleOnCLSEReceived();
                    }
                }
            }
        }, "ADBSocket#receiving");
        receivingThread.setDaemon(true);
        receivingThread.start();
        // noinspection resource (adb stream will be auto closed on CLSE received)
        this.lineSepInShell = new String(open("shell:echo\0").input().readAllBytes());
        Logger.info("adb init okay");
        Logger.emptyLine();
    }

    // *****************************************************************************************
    // InternalMethods
    // *****************************************************************************************

    private ADBPackage receive() {
        ADBPackage pakkage;
        synchronized (input) {
            try {
                int command = readIntLE();
                int arg0 = readIntLE();
                int arg1 = readIntLE();
                int length = readIntLE();
                int crc32 = readIntLE();
                int magic = readIntLE();
                byte[] payload = readBytes(length);
                pakkage = ADBPackage.init(command, arg0, arg1, crc32, magic, payload);
            } catch (IOException e) {
                throw new InvocationException(e)
                        .with("socket_address", address);
            }
        }
        logger.debug("adbd{%s} => package%s", address, pakkage);
        return pakkage;
    }

    private byte[] readBytes(int n) throws IOException {
        byte[] bytes = new byte[n];
        for (int i = 0, r; i < n; ) {
            r = input.read(bytes, i, n - i);
            if (r == -1) {throw new EOFException();}
            i += r;
        }
        return bytes;
    }

    private int readIntLE() throws IOException {
        int b1, b2, b3, b4;
        b1 = input.read();
        b2 = input.read();
        b3 = input.read();
        b4 = input.read();
        if ((b1 | b2 | b3 | b4) < 0) {throw new EOFException();}
        return b1 | (b2 << 8) | (b3 << 16) | (b4 << 24);
    }

    private void writeIntLE(int value) throws IOException {
        output.write(value);
        output.write(value >>> 8);
        output.write(value >>> 16);
        output.write(value >>> 24);
    }

    // *****************************************************************************************
    // InternalStaticClasses
    // *****************************************************************************************

    private static class FixWindowsLineSepInputStream extends InputStream {

        private final ADBStream.Input input;

        @Override
        public int read() {
            int b = input.read();
            if (b == -1) {
                return -1;
            } else if ((b == '\r') && (input.peek() == '\n')) {
                return input.read();
            } else {
                return b;
            }
        }

        FixWindowsLineSepInputStream(ADBStream.Input input) {
            this.input = input;
        }

    }

}
