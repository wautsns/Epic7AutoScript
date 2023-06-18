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

import java.util.Iterator;
import java.util.LinkedList;

/**
 * OCR char box list.
 *
 * @author wautsns
 * @since {{{SINCE_PLACEHOLDER}}}
 */
public final class OCRCharBoxList extends LinkedList<OCRCharBox> {

    // *****************************************************************************************
    // Methods, comparing text
    // *****************************************************************************************

    public boolean equalsText(String text, double accuracy) {
        Iterator<OCRCharBox> iterator = iterator();
        for (int i = 0, l = text.length(); i < l; i++) {
            if (!iterator.hasNext()) {return false;}
            if (text.charAt(i) != iterator.next().character()) {return false;}
        }
        return true;
    }

    // *****************************************************************************************
    // Methods, getting inline string
    // *****************************************************************************************

    // @param [accuracy] range: [0.0, 1.0]
    public String inline(double accuracy) {
        StringBuilder bu = new StringBuilder(size());
        for (OCRCharBox charBox : this) {
            if (charBox.accuracy() >= accuracy) {
                bu.append(charBox.character());
            }
        }
        return bu.toString();
    }

    // *****************************************************************************************
    // OverrideMethods, Object
    // *****************************************************************************************

    @Override
    public String toString() {
        return inline(0);
    }

}
