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
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;
import program.common.basic.exception.InvocationException;
import program.common.basic.resource.SilentCloseable;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Spliterator;
import java.util.Spliterators;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

/**
 * ADB stream.
 *
 * @author wautsns
 * @since {{{SINCE_PLACEHOLDER}}}
 */
@Accessors(fluent = true)
public final class ADBStream implements SilentCloseable {

    private final ADBSocket socket;

    private final int localId;
    private int remoteId = 0;

    private final @Getter Input input = new Input();
    private final @Getter Output output = new Output();

    private boolean closed = false;

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
        if (socket.closed()) {return;}
        socket.send(ADBPackage.initCLSE(localId, remoteId));
    }

    // *****************************************************************************************
    // PackageMethods, used by `ADBSocket`
    // *****************************************************************************************

    void handleOnOKAYReceived(int remoteId) {
        this.remoteId = remoteId;
        synchronized (output) {
            output.sendable.set(true);
            output.notify();
        }
    }

    void handleOnWRTEReceived(byte[] payload) {
        synchronized (input) {
            if (payload.length > 0) {
                input.payloads.add(payload);
            }
            input.notify();
        }
        if (socket.closed()) {return;}
        socket.send(ADBPackage.initOKAY(localId, remoteId));
    }

    void handleOnCLSEReceived() {
        if (closed) {return;}
        socket.streamMap.remove(localId, this);
        closed = true;
        synchronized (input) {
            input.notifyAll();
        }
        synchronized (output) {
            output.notifyAll();
        }
    }

    // *****************************************************************************************
    // PackageConstructors, used by `ADBSocket`
    // *****************************************************************************************

    ADBStream(ADBSocket socket, int localId) {
        this.socket = socket;
        this.localId = localId;
    }

    // *****************************************************************************************
    // Classes
    // *****************************************************************************************

    @NoArgsConstructor(access = AccessLevel.PRIVATE)
    public class Input extends InputStream {

        private final LinkedList<byte[]> payloads = new LinkedList<>();

        private byte[] buffer;
        private int offset;

        private boolean skipLF;

        // *********************************************************************************
        // OverrideMethods, InputStream
        // *********************************************************************************

        @Override
        public int read() {
            if (!ensureBufferAlready()) {return -1;}
            byte b = buffer[offset++];
            if (skipLF) {
                skipLF = false;
                if (b == '\n') {
                    if (!ensureBufferAlready()) {return -1;}
                    b = buffer[offset++];
                }
            }
            return (b & 0xFF);
        }

        @Override
        public int read(byte[] buf) {
            return read(buf, 0, buf.length);
        }

        @Override
        public int read(byte[] buf, int off, int len) {
            if (len == 0) {return 0;}
            if (!ensureBufferAlready()) {return -1;}
            byte b = buffer[offset++];
            if (skipLF) {
                skipLF = false;
                if (b == '\n') {
                    if (!ensureBufferAlready()) {return -1;}
                    b = buffer[offset++];
                }
            }
            buf[off] = b;
            int n = 1;
            while (ensureBufferAlready()) {
                int limit = buffer.length - offset;
                if (len <= limit) {
                    System.arraycopy(buffer, offset, buf, off + n, len);
                    offset += len;
                    return n + len;
                } else {
                    System.arraycopy(buffer, offset, buf, off + n, limit);
                    n += limit;
                    len -= limit;
                    buffer = null;
                }
            }
            return n;
        }

        @Override
        public byte[] readAllBytes() {
            if (!ensureBufferAlready()) {return new byte[0];}
            if (skipLF) {
                skipLF = false;
                if (buffer[offset] == '\n') {
                    offset++;
                    if (!ensureBufferAlready()) {return new byte[0];}
                }
            }
            byte[] bytes;
            if (offset == 0) {
                bytes = buffer;
            } else {
                bytes = new byte[buffer.length - offset];
                System.arraycopy(buffer, offset, bytes, 0, bytes.length);
            }
            buffer = null;
            if (!ensureBufferAlready()) {return bytes;}
            LinkedList<byte[]> bytesList = new LinkedList<>();
            bytesList.add(bytes);
            int n = bytes.length;
            do {
                bytesList.add(buffer);
                n += buffer.length;
                buffer = null;
            } while (ensureBufferAlready());
            return concat(bytesList, n);
        }

        @Override
        public byte[] readNBytes(int len) {
            if (len == 0) {return new byte[0];}
            if (!ensureBufferAlready()) {return new byte[0];}
            if (skipLF) {
                skipLF = false;
                if (buffer[offset] == '\n') {
                    offset++;
                    if (!ensureBufferAlready()) {return new byte[0];}
                }
            }
            int limit = buffer.length - offset;
            if (len <= limit) {
                byte[] bytes = new byte[len];
                System.arraycopy(buffer, offset, bytes, 0, len);
                offset += len;
                return bytes;
            }
            byte[] bytes = new byte[limit];
            System.arraycopy(buffer, offset, bytes, 0, limit);
            buffer = null;
            if (!ensureBufferAlready()) {return bytes;}
            LinkedList<byte[]> bytesList = new LinkedList<>();
            bytesList.add(bytes);
            int n = bytes.length;
            len -= bytes.length;
            do {
                if (len <= buffer.length) {
                    bytes = new byte[len];
                    System.arraycopy(buffer, offset, bytes, 0, len);
                    offset += len;
                    bytesList.add(bytes);
                    n += len;
                    return concat(bytesList, n);
                }
                bytes = new byte[limit];
                System.arraycopy(buffer, offset, bytes, 0, limit);
                bytesList.add(buffer);
                n += buffer.length;
                len -= buffer.length;
                buffer = null;
            } while (ensureBufferAlready());
            return concat(bytesList, n);
        }

        @Override
        public int readNBytes(byte[] buf, int off, int len) {
            return read(buf, off, len);
        }

        @Override
        public long skip(long len) {
            if (len == 0) {return 0;}
            if (!ensureBufferAlready()) {return 0;}
            if (skipLF) {
                skipLF = false;
                if (buffer[offset] == '\n') {
                    offset++;
                    if (!ensureBufferAlready()) {return 0;}
                }
            }
            int limit = buffer.length - offset;
            if (len <= limit) {
                offset += len;
                return len;
            }
            buffer = null;
            if (!ensureBufferAlready()) {return limit;}
            int n = limit;
            len -= limit;
            do {
                if (len <= buffer.length) {
                    offset += len;
                    return n + len;
                }
                n += buffer.length;
                len -= buffer.length;
                buffer = null;
            } while (ensureBufferAlready());
            return n;
        }

        @Override
        public void skipNBytes(long len) {
            if (skip(len) < len) {
                String message = "ADB stream closed";
                throw new InvocationException(message)
                        .with("socket_address", socket.address)
                        .with("local_stream_id", localId)
                        .with("remote_stream_id", remoteId);
            }
        }

        @Override
        public int available() {
            return 0;
        }

        @Override
        public void close() {}

        @Override
        public long transferTo(OutputStream out) {
            if (!ensureBufferAlready()) {return 0;}
            if (skipLF) {
                skipLF = false;
                if (buffer[offset] == '\n') {
                    offset++;
                    if (!ensureBufferAlready()) {return 0;}
                }
            }
            int n = 0;
            try {
                if (offset != 0) {
                    int limit = buffer.length - offset;
                    out.write(buffer, offset, limit);
                    n += limit;
                    buffer = null;
                }
                while (ensureBufferAlready()) {
                    out.write(buffer);
                    buffer = null;
                }
            } catch (IOException e) {
                throw new InvocationException(e)
                        .with("socket_address", socket.address)
                        .with("local_stream_id", localId)
                        .with("remote_stream_id", remoteId);
            }
            return n;
        }

        // *********************************************************************************
        // Methods, additional functions
        // *********************************************************************************

        public int peek() {
            if (!ensureBufferAlready()) {return -1;}
            if (skipLF) {
                skipLF = false;
                if (buffer[offset] == '\n') {
                    offset++;
                    if (!ensureBufferAlready()) {return -1;}
                }
            }
            return buffer[offset];
        }

        public String readLine() {
            if (!ensureBufferAlready()) {return null;}
            if (skipLF) {
                skipLF = false;
                if (buffer[offset] == '\n') {
                    offset++;
                    if (!ensureBufferAlready()) {return null;}
                }
            }
            for (int i = offset, l = buffer.length; i < l; i++) {
                byte b = buffer[i];
                if (b == '\n') {
                    String line = new String(buffer, offset, i - offset);
                    offset = i + 1;
                    return line;
                } else if (b == '\r') {
                    String line = new String(buffer, offset, i - offset);
                    offset = i + 1;
                    skipLF = true;
                    return line;
                }
            }
            byte[] bytes;
            if (offset == 0) {
                bytes = buffer;
            } else {
                bytes = new byte[buffer.length - offset];
                System.arraycopy(buffer, offset, bytes, 0, bytes.length);
            }
            LinkedList<byte[]> bytesList = new LinkedList<>();
            bytesList.add(bytes);
            int n = bytes.length;
            buffer = null;
            while (ensureBufferAlready()) {
                for (int i = 0, l = buffer.length; i < l; i++) {
                    byte b = buffer[i];
                    if (b == '\n') {
                        offset = i + 1;
                        return new String(concat(bytesList, n + i));
                    } else if (b == '\r') {
                        offset = i + 1;
                        skipLF = true;
                        return new String(concat(bytesList, n + i));
                    }
                }
                bytesList.add(buffer);
                n += buffer.length;
                buffer = null;
            }
            return new String(concat(bytesList, n));
        }

        public String readAllString() {
            return lines().collect(Collectors.joining("\n"));
        }

        public void skipAll() {
            // noinspection ResultOfMethodCallIgnored
            skip(Long.MAX_VALUE);
        }

        public Stream<String> lines() {
            return StreamSupport.stream(Spliterators.spliteratorUnknownSize(new Iterator<>() {
                String nextLine = null;

                @Override
                public boolean hasNext() {
                    if (nextLine != null) {
                        return true;
                    } else {
                        nextLine = readLine();
                        return (nextLine != null);
                    }
                }

                @Override
                public String next() {
                    if (hasNext()) {
                        String line = nextLine;
                        nextLine = null;
                        return line;
                    } else {
                        throw new NoSuchElementException();
                    }
                }
            }, Spliterator.ORDERED | Spliterator.NONNULL), false);
        }

        // *********************************************************************************
        // InternalMethods
        // *********************************************************************************

        private boolean ensureBufferAlready() {
            if ((buffer == null) || (offset >= buffer.length)) {
                synchronized (this) {
                    while ((buffer = payloads.poll()) == null) {
                        if (closed) {return false;}
                        try {
                            wait();
                        } catch (InterruptedException e) {
                            Thread.currentThread().interrupt();
                            throw new InvocationException(e)
                                    .with("socket_address", socket.address)
                                    .with("local_stream_id", localId)
                                    .with("remote_stream_id", remoteId);
                        }
                    }
                }
                offset = 0;
            }
            return true;
        }

        // *********************************************************************************
        // InternalStaticMethods
        // *********************************************************************************

        private static byte[] concat(List<byte[]> bytesList, int length) {
            byte[] result = new byte[length];
            Iterator<byte[]> bytesItr = bytesList.iterator();
            for (int offset = 0; bytesItr.hasNext(); ) {
                byte[] bytes = bytesItr.next();
                System.arraycopy(bytes, 0, result, offset, bytes.length);
                offset += bytes.length;
            }
            return result;
        }

    }

    @NoArgsConstructor(access = AccessLevel.PRIVATE)
    public class Output extends OutputStream {

        private final AtomicBoolean sendable = new AtomicBoolean(false);
        private byte[] buffer;
        private int offset = 0;

        // *********************************************************************************
        // OverrideMethods, OutputStream
        // *********************************************************************************

        @Override
        public void write(int b) {
            buffer[offset++] = (byte) b;
        }

        @Override
        public void write(byte[] buf) {
            write(buf, 0, buf.length);
        }

        @Override
        public void write(byte[] buf, int off, int len) {
            System.arraycopy(buf, off, buffer, offset, len);
            offset += len;
        }

        @Override
        public void flush() {
            if (offset <= ADBPackage.MAX_PAYLOAD) {
                send(0, offset);
            } else {
                for (int i = 0; i < offset; ) {
                    int length = Math.min(ADBPackage.MAX_PAYLOAD, offset - i);
                    send(i, length);
                    i += length;
                }
            }
            offset = 0;
        }

        @Override
        public void close() {}

        // *********************************************************************************
        // Methods, additional functions
        // *********************************************************************************

        public byte[] getBuffer() {
            return buffer;
        }

        public Output setBuffer(int capacity) {
            return setBuffer(new byte[capacity]);
        }

        public Output setBuffer(byte[] buffer) {
            this.buffer = buffer;
            return this;
        }

        public Output setOffset(int offset) {
            this.offset = offset;
            return this;
        }

        public Output writeIntLE(int value) {
            buffer[offset++] = (byte) value;
            buffer[offset++] = (byte) (value >>> 8);
            buffer[offset++] = (byte) (value >>> 16);
            buffer[offset++] = (byte) (value >>> 24);
            return this;
        }

        public Output writeAscii(String value) {
            for (int i = 0, l = value.length(); i < l; i++) {
                buffer[offset++] = (byte) value.charAt(i);
            }
            return this;
        }

        // *********************************************************************************
        // InternalMethods
        // *********************************************************************************

        private void send(int start, int length) {
            while (!closed && !sendable.compareAndSet(true, false)) {
                synchronized (this) {
                    try {
                        wait();
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                        throw new InvocationException(e)
                                .with("socket_address", socket.address)
                                .with("local_stream_id", localId)
                                .with("remote_stream_id", remoteId);
                    }
                }
            }
            if (closed) {
                String message = "ADB stream closed";
                throw new InvocationException(message)
                        .with("socket_address", socket.address)
                        .with("local_stream_id", localId)
                        .with("remote_stream_id", remoteId);
            }
            byte[] payload;
            if (start == 0) {
                payload = buffer;
            } else {
                payload = new byte[length];
                System.arraycopy(buffer, start, payload, 0, length);
            }
            socket.send(ADBPackage.initWRTE(localId, remoteId, payload, length));
        }

    }

}
