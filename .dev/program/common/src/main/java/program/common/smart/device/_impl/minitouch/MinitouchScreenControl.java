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
package program.common.smart.device._impl.minitouch;

import lombok.experimental.Accessors;
import program.common.basic.resource.conf.Config;
import program.common.basic.task.Task;
import program.common.smart.device._impl.ScreenControl;
import program.common.smart.device._impl.adb.impl.ADB;
import program.common.smart.device._impl.minitouch.impl.Minitouch;

/**
 * {@link ScreenControl} implementation based on minitouch.
 *
 * @author wautsns
 * @since {{{SINCE_PLACEHOLDER}}}
 */
@Accessors(fluent = true)
public final class MinitouchScreenControl implements ScreenControl {

    private final Minitouch minitouch;

    // *****************************************************************************************
    // OverrideMethods, ScreenControl
    // *****************************************************************************************

    @Override
    public synchronized void tap(int x, int y) {
        minitouch.d(0, x, y).c().u(0).c().send();
    }

    @Override
    public synchronized void press(int x, int y, int duration) {
        minitouch.d(0, x, y).c().w(duration).u(0).c().send();
        Task.sleep(duration);
    }

    @Override
    public synchronized void swipe(int x1, int y1, int x2, int y2, int duration) {
        // TODO swipe by minitouch commands
        minitouch.adb().swipe(x1, y1, x2, y2, duration);
    }

    // *****************************************************************************************
    // OverrideMethods, SilentCloseable
    // *****************************************************************************************

    @Override
    public synchronized boolean closed() {
        return minitouch.closed();
    }

    @Override
    public synchronized void close() {
        minitouch.close();
    }

    // *****************************************************************************************
    // Constructors
    // *****************************************************************************************

    public MinitouchScreenControl(Config config) {
        String adbdAddress = config.getValue("adbd.address");
        int adbdConnectionTimeout = config.getValueAsInt("adbd.connection-timeout");
        int minitouchConnectionTimeout = config.getValueAsInt("minitouch.connection-timeout");

        ADB adb = ADB.of(this, adbdAddress, adbdConnectionTimeout);
        minitouch = Minitouch.of(adb, this, minitouchConnectionTimeout);
    }

}
