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
package program.common.basic.utility;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

import java.util.function.Supplier;

/**
 * String utility.
 *
 * @author wautsns
 * @since {{{SINCE_PLACEHOLDER}}}
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class StrUtl {

    // *****************************************************************************************
    // StaticMethods, getting decimal string
    // *****************************************************************************************

    private static final String[] CACHE_DECIMAL = ((Supplier<String[]>) () -> {
        String[] result = new String[1280];
        for (int i = 0, l = result.length; i < l; i++) {
            result[i] = Integer.toString(i).intern();
        }
        return result;
    }).get();

    public static String decimal(int value) {
        if ((value < CACHE_DECIMAL.length) && (value >= 0)) {
            return CACHE_DECIMAL[value];
        } else {
            return Integer.toString(value);
        }
    }

    // *****************************************************************************************
    // StaticMethods, getting inline chars
    // *****************************************************************************************

    public static CharSequence inline(CharSequence value) {
        int lineSepCount = 0;
        for (int i = 0, l = value.length(); i < l; i++) {
            char c = value.charAt(i);
            if ((c == '\r') || (c == '\n')) {
                lineSepCount++;
            }
        }
        if (lineSepCount == 0) {return value;}
        int n = value.length() + lineSepCount;
        StringBuilder bu = new StringBuilder(n);
        int prev = 0;
        for (int i = 0, l = value.length(); (lineSepCount > 0) && (i < l); i++) {
            char c;
            switch (value.charAt(i)) {
                case '\n' -> c = 'n';
                case '\r' -> c = 'r';
                default -> {continue;}
            }
            if (prev < i) {
                bu.append(value, prev, i);
            }
            bu.append('\\').append(c);
            lineSepCount--;
            prev = i + 1;
        }
        if (prev < value.length()) {
            bu.append(value, prev, value.length());
        }
        return bu;
    }

}
