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
package program.common.basic.vision;

import com.alibaba.fastjson2.JSONObject;
import lombok.Getter;
import lombok.experimental.Accessors;

import java.util.Random;

import static java.lang.String.format;

/**
 * Area.
 *
 * @author wautsns
 * @since {{{SINCE_PLACEHOLDER}}}
 */
@Accessors(fluent = true)
public final class Area {

    // Coordinate System:
    // Integer coordinates are at the cracks between the pixels.
    // The top-left corner of the top-left pixel in the image is at (0,0). The bottom-right corner
    // of the bottom-right pixel in the image is at (width, height).

    // *****************************************************************************************
    // StaticMethods, parsing json data
    // *****************************************************************************************

    public static Area parse(JSONObject json) {
        String id = json.getString("id");
        int[] arr = json.getObject("area", int[].class);
        return new Area(id, arr[0], arr[1], arr[2], arr[3]);
    }

    // *****************************************************************************************
    // *****************************************************************************************

    private final @Getter String name;

    private final @Getter int x;
    private final @Getter int y;
    private final @Getter int width;
    private final @Getter int height;

    // *****************************************************************************************
    // Methods, getting random point
    // *****************************************************************************************

    public int x(Random random) {
        return x + random.nextInt(width);
    }

    public int y(Random random) {
        return y + random.nextInt(height);
    }

    // *****************************************************************************************
    // Constructors
    // *****************************************************************************************

    public Area(int x, int y, int width, int height) {
        this(format("(%d,%d,%d,%d)", x, y, x + width, y + height), x, y, width, height);
    }

    public Area(String name, int x, int y, int width, int height) {
        this.name = name;
        this.x = x;
        this.y = y;
        this.width = width;
        this.height = height;
    }

}
