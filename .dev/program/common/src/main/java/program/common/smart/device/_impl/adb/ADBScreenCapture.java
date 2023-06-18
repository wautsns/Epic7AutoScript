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
package program.common.smart.device._impl.adb;

import lombok.experimental.Accessors;
import program.common.basic.resource.conf.Config;
import program.common.basic.vision.Image;
import program.common.smart.device._impl.ScreenCapture;
import program.common.smart.device._impl.adb.impl.ADB;

/**
 * {@link ScreenCapture} implementation based on adb.
 *
 * @author wautsns
 * @since {{{SINCE_PLACEHOLDER}}}
 */
@Accessors(fluent = true)
public final class ADBScreenCapture implements ScreenCapture {

    private final ADB adb;

    // *****************************************************************************************
    // OverrideMethods, ScreenCapture
    // *****************************************************************************************

    @Override
    public Image screenshot() {
        return adb.screencap();
    }

    // *****************************************************************************************
    // OverrideMethods, SilentCloseable
    // *****************************************************************************************

    @Override
    public synchronized void close() {
        adb.release(this);
    }

    @Override
    public synchronized boolean closed() {
        return adb.closed();
    }

    // *****************************************************************************************
    // Constructors
    // *****************************************************************************************

    public ADBScreenCapture(Config config) {
        String adbdAddress = config.getValue("adbd.address");
        int adbdConnectionTimeout = config.getValueAsInt("adbd.connection-timeout");

        adb = ADB.of(this, adbdAddress, adbdConnectionTimeout);
    }

}
