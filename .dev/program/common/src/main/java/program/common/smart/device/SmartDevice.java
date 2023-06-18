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
package program.common.smart.device;

import lombok.RequiredArgsConstructor;
import lombok.experimental.Accessors;
import program.common.basic.logger.Logger;
import program.common.basic.resource.ResUtl;
import program.common.basic.resource.conf.Config;
import program.common.basic.resource.conf.ConfigTemplate;
import program.common.basic.resource.conf.Configurable;
import program.common.basic.task.Task;
import program.common.basic.utility.WeakSet;
import program.common.basic.vision.Area;
import program.common.basic.vision.Image;
import program.common.basic.vision.ImageInArea;
import program.common.smart.device._impl.ScreenCapture;
import program.common.smart.device._impl.ScreenControl;
import program.common.smart.device._impl.adb.ADBScreenCapture;
import program.common.smart.device._impl.adb.ADBScreenControl;
import program.common.smart.device._impl.adb.impl.ADB;
import program.common.smart.device._impl.minitouch.MinitouchScreenControl;

import java.io.File;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.Function;

/**
 * Smart device.
 *
 * @author wautsns
 * @since {{{SINCE_PLACEHOLDER}}}
 */
@Accessors(fluent = true)
public final class SmartDevice extends Configurable {

    // *****************************************************************************************
    // StaticMethods, initializing instance
    // *****************************************************************************************

    private static final ConfigTemplate TEMPLATE = ConfigTemplate.of(ResUtl.home(
            "/.dev/resources/common/smart/device/#SmartDevice"
    ));
    private static final WeakSet<SmartDevice> REFERENCES = new WeakSet<>();

    public static synchronized SmartDevice of(String customer) {
        return of(new File(customer));
    }

    public static synchronized SmartDevice of(File customer) {
        Config config = Config.of(TEMPLATE, customer);
        return REFERENCES.add(ref -> ref.config == config, () -> new SmartDevice(config));
    }

    // *****************************************************************************************
    // *****************************************************************************************

    private ADB adb;
    private ScreenCapture capture;
    private ScreenControl control;

    // *****************************************************************************************
    // Methods, controlling application
    // *****************************************************************************************

    public boolean isAppRunning(String packageName) {
        acquireNotClosed();
        return adb.isAppRunning(packageName);
    }

    public void startApp(String packageName, String activityName) {
        acquireNotClosed();
        adb.startApp(packageName, activityName);
    }

    public void stopApp(String packageName) {
        acquireNotClosed();
        adb.forceStopApp(packageName);
    }

    // *****************************************************************************************
    // Methods, capturing screen
    // *****************************************************************************************

    public synchronized Image screenshot() {
        acquireNotClosed();
        return capture.screenshot();
    }

    public synchronized boolean isAppeared(ImageInArea target) {
        return screenshot().match(target.area(), target.image());
    }

    // *****************************************************************************************
    // Methods, tapping on screen
    // *****************************************************************************************

    public synchronized void tap(Area area) {
        acquireNotClosed();
        ThreadLocalRandom rand = ThreadLocalRandom.current();
        int x = area.x(rand), y = area.y(rand);
        Logger.info("tap (%d,%d) @ %s", x, y, area.name());
        control.tap(x, y);
    }

    public synchronized void tapUntilAppeared(Area area, ImageInArea target, int interval) {
        do {
            tap(area);
            Task.sleep(interval);
        } while (!isAppeared(target));
    }

    public synchronized void tapUntilDisappeared(Area area, ImageInArea target, int interval) {
        do {
            tap(area);
            Task.sleep(interval);
        } while (isAppeared(target));
    }

    // *****************************************************************************************
    // Methods, pressing on screen
    // *****************************************************************************************

    public synchronized void press(Area area, int duration) {
        acquireNotClosed();
        ThreadLocalRandom rand = ThreadLocalRandom.current();
        int x = area.x(rand), y = area.y(rand);
        Logger.info("press (%d,%d) @ %s, %dms", x, y, area.name(), duration);
        control.press(x, y, duration);
    }

    // *****************************************************************************************
    // Methods, swiping on screen
    // *****************************************************************************************

    public synchronized void swipe(Area area1, Area area2, int duration) {
        acquireNotClosed();
        ThreadLocalRandom rand = ThreadLocalRandom.current();
        int x1 = area1.x(rand), y1 = area1.y(rand), x2 = area2.x(rand), y2 = area2.y(rand);
        Logger.info(
                "swipe (%d,%d) @ %s -> (%d,%d) @ %s, %dms",
                x1, y1, area1.name(), x2, y2, area2.name(), duration
        );
        control.swipe(x1, y1, x2, y2, duration);
    }

    // *****************************************************************************************
    // OverrideMethods, Configurable
    // *****************************************************************************************

    @Override
    protected void release() {
        if (adb != null) {
            adb.release(this);
            adb = null;
        }
        if (capture != null) {
            capture.close();
            capture = null;
        }
        if (control != null) {
            control.close();
            control = null;
        }
    }

    protected void reinitialize() {
        CaptureImpl captureImpl = config.getValueAsEnum("capture.impl", CaptureImpl.class);
        ControlImpl controlImpl = config.getValueAsEnum("control.impl", ControlImpl.class);
        String adbdAddress = config.getValue("adbd.address");
        int adbdConnectionTimeout = config.getValueAsInt("adbd.connection-timeout");

        adb = ADB.of(this, adbdAddress, adbdConnectionTimeout);
        capture = captureImpl.constructor.apply(config);
        control = controlImpl.constructor.apply(config);
    }

    // *****************************************************************************************
    // InternalConstructors
    // *****************************************************************************************

    private SmartDevice(Config config) {
        super(config);
    }

    // *****************************************************************************************
    // InternalEnums
    // *****************************************************************************************

    @SuppressWarnings("unused")
    @RequiredArgsConstructor
    private enum CaptureImpl {

        adb(ADBScreenCapture::new),
        ;

        final Function<Config, ScreenCapture> constructor;

    }

    @SuppressWarnings("unused")
    @RequiredArgsConstructor
    private enum ControlImpl {

        adb(ADBScreenControl::new),
        minitouch(MinitouchScreenControl::new),
        ;

        final Function<Config, ScreenControl> constructor;

    }

}
