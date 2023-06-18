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
import lombok.Builder;
import lombok.Getter;
import lombok.experimental.Accessors;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.IntUnaryOperator;

@Accessors(fluent = true)
public final class ImageOps {

    private static final IntUnaryOperator RGB_RAW = rgb -> rgb;
    private static final Map<Integer, IntUnaryOperator> RGB_THRESHOLD_CACHE =
            new ConcurrentHashMap<>();
    private static final Map<Integer, IntUnaryOperator> RGB_THRESHOLD_INVERSE_CACHE =
            new ConcurrentHashMap<>();
    private static final IntUnaryOperator RGB_INVERSE = rgb -> 0xFFFFFF - rgb;

    // *****************************************************************************************
    // StaticMethods, parsing json data
    // *****************************************************************************************

    public static ImageOps parse(JSONObject json) {
        int threshold = json.getIntValue("threshold", 0);
        boolean inverse = json.getBooleanValue("inverse", false);
        int[] cropArr = json.getObject("crop", int[].class);
        if (cropArr == null) {
            return new ImageOps(null, threshold, inverse);
        } else {
            String id = json.getString("id");
            Area crop = new Area(id, cropArr[0], cropArr[1], cropArr[2], cropArr[3]);
            return new ImageOps(crop, threshold, inverse);
        }
    }

    // *********************************************************************************
    // *********************************************************************************

    private final @Getter Area crop;

    private final @Getter int threshold;
    private final @Getter boolean inverse;

    // package field, used by `Image`
    final IntUnaryOperator converter;

    // *********************************************************************************
    // InternalConstructors
    // *********************************************************************************

    @Builder
    private ImageOps(Area crop, int threshold, boolean inverse) {
        this.crop = crop;
        this.threshold = threshold;
        this.inverse = inverse;
        if (threshold == 0) {
            this.converter = inverse ? RGB_INVERSE : RGB_RAW;
        } else if (inverse) {
            this.converter = RGB_THRESHOLD_INVERSE_CACHE
                    .computeIfAbsent(threshold, ImageOps::initRGBThresholdInverse);
        } else {
            this.converter = RGB_THRESHOLD_CACHE
                    .computeIfAbsent(threshold, ImageOps::initRGBThreshold);
        }
    }

    // *****************************************************************************************
    // InternalStaticMethods
    // *****************************************************************************************

    private static int gray(int rgb) {
        int r = rgb >> 16, g = (rgb >> 8) & 0xFF, b = rgb & 0xFF;
        return (r * 77 + g * 150 + b * 29 + 128) >> 8;
    }

    private static IntUnaryOperator initRGBThreshold(int threshold) {
        return rgb -> (gray(rgb) >= threshold) ? 0xFFFFFF : 0x000000;
    }

    private static IntUnaryOperator initRGBThresholdInverse(int threshold) {
        return rgb -> (gray(rgb) >= threshold) ? 0x000000 : 0xFFFFFF;
    }

}
