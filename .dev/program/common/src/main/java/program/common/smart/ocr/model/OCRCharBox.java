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
package program.common.smart.ocr.model;

import lombok.Getter;
import lombok.experimental.Accessors;

/**
 * OCR char box.
 *
 * @author wautsns
 * @since {{{SINCE_PLACEHOLDER}}}
 */
@Accessors(fluent = true)
public final class OCRCharBox {

    // Coordinate System:
    // Integer coordinates are at the cracks between the pixels.
    // The top-left corner of the top-left pixel in the image is at (0,0). The bottom-right corner
    // of the bottom-right pixel in the image is at (width, height).

    private final @Getter int x;
    private final @Getter int y;
    private final @Getter int width;
    private final @Getter int height;
    private final @Getter char character;
    // range: [0.0, 1.0]
    private final @Getter float accuracy;

    // *****************************************************************************************
    // Constructors
    // *****************************************************************************************

    public OCRCharBox(int x, int y, int width, int height, char character, float accuracy) {
        this.x = x;
        this.y = y;
        this.width = width;
        this.height = height;
        this.character = character;
        this.accuracy = accuracy;
    }

}
