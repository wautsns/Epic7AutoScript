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
import program.common.basic.utility.StrUtl;

/**
 * ADB package (ADB_SERVER_VERSION 31). package class, used by `ADBSocket` & `ADBStream`
 *
 * @author wautsns
 * @since {{{SINCE_PLACEHOLDER}}}
 */
@SuppressWarnings("SpellCheckingInspection")
@RequiredArgsConstructor(access = AccessLevel.PRIVATE)
final class ADBPackage {

    // --- protocol overview and basics ---------------------------------------
    //
    // The transport layer deals in "messages", which consist of a 24 byte
    // header followed (optionally) by a payload.  The header consists of 6
    // 32 bit words which are sent across the wire in little endian format.
    //
    // struct message {
    //     unsigned command;       /* command identifier constant      */
    //     unsigned arg0;          /* first argument                   */
    //     unsigned arg1;          /* second argument                  */
    //     unsigned data_length;   /* length of payload (0 is allowed) */
    //     unsigned data_crc32;    /* crc32 of data payload            */
    //     unsigned magic;         /* command ^ 0xffffffff             */
    // };
    //
    // Receipt of an invalid message header, corrupt message payload, or an
    // unrecognized command MUST result in the closing of the remote
    // connection.  The protocol depends on shared state and any break in the
    // message stream will result in state getting out of sync.
    //
    // The following sections describe the six defined message types in
    // detail.  Their format is COMMAND(arg0, arg1, payload) where the payload
    // is represented by a quoted string or an empty string if none should be
    // sent.
    //
    // The identifiers "local-id" and "remote-id" are always relative to the
    // *sender* of the message, so for a receiver, the meanings are effectively
    // reversed.

    static final int MAX_PAYLOAD = 4096;

    static final int A_VERSION = 0x01000000;

    static final int A_CNXN = 0x4e584e43;
    static final int A_AUTH = 0x48545541;
    static final int A_OPEN = 0x4e45504f;
    static final int A_OKAY = 0x59414b4f;
    static final int A_WRTE = 0x45545257;
    static final int A_CLSE = 0x45534c43;
    static final int A_SYNC = 0x434e5953;

    private static final byte[] EMPTY_PAYLOAD = new byte[0];

    // *****************************************************************************************
    // StaticMethods, initializing instance
    // *****************************************************************************************

    static ADBPackage init(int command, int arg0, int arg1, byte[] payload) {
        return init(command, arg0, arg1, payload, payload.length);
    }

    static ADBPackage init(int command, int arg0, int arg1, byte[] buffer, int length) {
        int crc32 = 0;
        for (int i = 0; i < length; i++) {
            crc32 += (buffer[i] & 0xFF);
        }
        return new ADBPackage(command, arg0, arg1, length, crc32, ~command, buffer);
    }

    static ADBPackage init(int command, int arg0, int arg1, int crc32, int magic, byte[] payload) {
        return new ADBPackage(command, arg0, arg1, payload.length, crc32, magic, payload);
    }

    // --- CONNECT(version, maxdata, "system-identity-string") ----------------
    //
    // The CONNECT message establishes the presence of a remote system.
    // The version is used to ensure protocol compatibility and maxdata
    // declares the maximum message body size that the remote system
    // is willing to accept.
    //
    // Currently, version=0x01000000 and maxdata=4096
    //
    // Both sides send a CONNECT message when the connection between them is
    // established.  Until a CONNECT message is received no other messages may
    // be sent.  Any messages received before a CONNECT message MUST be ignored.
    //
    // If a CONNECT message is received with an unknown version or insufficiently
    // large maxdata value, the connection with the other side must be closed.
    //
    // The system identity string should be "<systemtype>:<serialno>:<banner>"
    // where systemtype is "bootloader", "device", or "host", serialno is some
    // kind of unique ID (or empty), and banner is a human-readable version
    // or identifier string.  The banner is used to transmit useful properties.
    static ADBPackage initCNXN(String systemId) {
        return init(A_CNXN, A_VERSION, MAX_PAYLOAD, systemId.getBytes());
    }

    // --- OPEN(local-id, 0, "destination") -----------------------------------
    //
    // The OPEN message informs the recipient that the sender has a stream
    // identified by local-id that it wishes to connect to the named
    // destination in the message payload.  The local-id may not be zero.
    //
    // The OPEN message MUST result in either a READY message indicating that
    // the connection has been established (and identifying the other end) or
    // a CLOSE message, indicating failure.  An OPEN message also implies
    // a READY message sent at the same time.
    //
    // Common destination naming conventions include:
    //
    // * "tcp:<host>:<port>" - host may be omitted to indicate localhost
    // * "udp:<host>:<port>" - host may be omitted to indicate localhost
    // * "local-dgram:<identifier>"
    // * "local-stream:<identifier>"
    // * "shell" - local shell service
    // * "upload" - service for pushing files across (like aproto's /sync)
    // * "fs-bridge" - FUSE protocol filesystem bridge
    static ADBPackage initOPEN(int localId, String destination) {
        return init(A_OPEN, localId, 0, destination.getBytes());
    }

    // --- READY(local-id, remote-id, "") -------------------------------------
    //
    // The READY message informs the recipient that the sender's stream
    // identified by local-id is ready for write messages and that it is
    // connected to the recipient's stream identified by remote-id.
    //
    // Neither the local-id nor the remote-id may be zero.
    //
    // A READY message containing a remote-id which does not map to an open
    // stream on the recipient's side is ignored.  The stream may have been
    // closed while this message was in-flight.
    //
    // The local-id is ignored on all but the first READY message (where it
    // is used to establish the connection).  Nonetheless, the local-id MUST
    // not change on later READY messages sent to the same stream.
    static ADBPackage initOKAY(int localId, int remoteId) {
        return init(A_OKAY, localId, remoteId, EMPTY_PAYLOAD);
    }

    // --- WRITE(0, remote-id, "data") ----------------------------------------
    //
    // The WRITE message sends data to the recipient's stream identified by
    // remote-id.  The payload MUST be <= maxdata in length.
    //
    // A WRITE message containing a remote-id which does not map to an open
    // stream on the recipient's side is ignored.  The stream may have been
    // closed while this message was in-flight.
    //
    // A WRITE message may not be sent until a READY message is received.
    // Once a WRITE message is sent, an additional WRITE message may not be
    // sent until another READY message has been received.  Recipients of
    // a WRITE message that is in violation of this requirement will CLOSE
    // the connection.
    static ADBPackage initWRTE(int localId, int remoteId, byte[] buffer, int length) {
        return init(A_WRTE, localId, remoteId, buffer, length);
    }

    // --- CLOSE(local-id, remote-id, "") -------------------------------------
    //
    // The CLOSE message informs recipient that the connection between the
    // sender's stream (local-id) and the recipient's stream (remote-id) is
    // broken.  The remote-id MUST not be zero, but the local-id MAY be zero
    // if this CLOSE indicates a failed OPEN.
    //
    // A CLOSE message containing a remote-id which does not map to an open
    // stream on the recipient's side is ignored.  The stream may have
    // already been closed by the recipient while this message was in-flight.
    //
    // The recipient should not respond to a CLOSE message in any way.  The
    // recipient should cancel pending WRITEs or CLOSEs, but this is not a
    // requirement, since they will be ignored.
    static ADBPackage initCLSE(int localId, int remoteId) {
        return init(A_CLSE, localId, remoteId, EMPTY_PAYLOAD);
    }

    // *****************************************************************************************
    // *****************************************************************************************

    final int command;
    final int arg0;
    final int arg1;
    final int length;
    final int crc32;
    final int magic;

    final byte[] buffer;

    // *****************************************************************************************
    // OverrideMethods, Object
    // *****************************************************************************************

    @Override
    public String toString() {
        switch (command) {
            case A_SYNC -> {
                // {A_SYNC|online=<arg0:10>|sequence=<arg1:10>}
                StringBuilder bu = new StringBuilder(46);
                bu.append("{A_SYNC|online=").append(arg0).append("|sequence=").append(arg1)
                        .append('}');
                return bu.toString();
            }
            case A_CNXN -> {
                CharSequence inlinePayload = StrUtl.inline(new String(buffer, 0, length));
                // {A_CNXN|version=0x<arg0:10>|maxdata=<arg1:10>|payload=<inlinePayload>}
                int n = 55 + inlinePayload.length();
                StringBuilder bu = new StringBuilder(n);
                bu.append("{{A_CNXN|version=").append(arg0).append("|maxdata=").append(arg1)
                        .append("|payload=").append(inlinePayload).append('}');
                return bu.toString();
            }
            case A_OPEN -> {
                CharSequence inlinePayload = StrUtl.inline(new String(buffer, 0, length));
                // {A_OPEN|localId=<arg0:10>|payload=<inlinePayload>}
                int n = 36 + inlinePayload.length();
                StringBuilder bu = new StringBuilder(n);
                bu.append("{A_OPEN|localId=").append(arg0).append("|payload=").append(inlinePayload)
                        .append('}');
                return bu.toString();
            }
            case A_OKAY -> {
                // {A_OKAY|localId=<arg0:10>|remoteId=<arg1:10>}
                StringBuilder bu = new StringBuilder(45);
                bu.append("{A_OKAY|localId=").append(arg0).append("|remoteId=").append(arg1)
                        .append('}');
                return bu.toString();
            }
            case A_CLSE -> {
                // {A_CLSE|localId=<arg0:10>|remoteId=<arg1:10>}
                StringBuilder bu = new StringBuilder(45);
                bu.append("{A_CLSE|localId=").append(arg0).append("|remoteId=").append(arg1)
                        .append('}');
                return bu.toString();
            }
            case A_WRTE -> {
                if (length < 256) {
                    CharSequence inlinePayload = StrUtl.inline(new String(buffer, 0, length));
                    // {A_WRTE|remoteId=<arg1:10>|payload=<inlinePayload>}
                    int n = 37 + inlinePayload.length();
                    StringBuilder bu = new StringBuilder(n);
                    bu.append("{A_WRTE|remoteId=").append(arg1).append("|payload=")
                            .append(inlinePayload).append('}');
                    return bu.toString();
                } else {
                    // {A_WRTE|remoteId=<arg1:10>|payload.length=<payload.length:10>}
                    StringBuilder bu = new StringBuilder(54);
                    bu.append("{A_WRTE|remoteId=").append(arg1).append("|payload.length=")
                            .append(length).append('}');
                    return bu.toString();
                }
            }
            case A_AUTH -> {
                // {A_AUTH|type=<arg1:10>|payload.length=<payload.length:10>}
                StringBuilder bu = new StringBuilder(50);
                bu.append("{A_AUTH|type=").append(arg0).append("|payload.length=").append(length)
                        .append('}');
                return bu.toString();
            }
            default -> {
                // {<command:10>|arg0=<arg0:10>|arg1=<arg1:10>|payload.length=<payload.length:10>}
                StringBuilder bu = new StringBuilder(70);
                bu.append('{').append(command).append("|arg0=").append(arg0).append("|arg1=")
                        .append("|payload.length=").append(length).append('}');
                return bu.toString();
            }
        }
    }

}
