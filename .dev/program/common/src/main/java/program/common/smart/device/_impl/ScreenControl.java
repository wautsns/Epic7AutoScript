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
package program.common.smart.device._impl;

import program.common.basic.resource.SilentCloseable;

/**
 * Screen control.
 *
 * @author wautsns
 * @since {{{SINCE_PLACEHOLDER}}}
 */
public interface ScreenControl extends SilentCloseable {

    // *****************************************************************************************
    // Methods, tapping on screen
    // *****************************************************************************************

    void tap(int x, int y);

    // *****************************************************************************************
    // Methods, pressing on screen
    // *****************************************************************************************

    void press(int x, int y, int duration);

    // *****************************************************************************************
    // Methods, swiping on screen
    // *****************************************************************************************

    void swipe(int x1, int y1, int x2, int y2, int duration);

}
