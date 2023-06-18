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
package program.common.basic.resource.i18n;

import lombok.Getter;
import lombok.experimental.Accessors;
import program.common.basic.function.StringSupplier;

/**
 * I18N text.
 *
 * @author wautsns
 * @since {{{SINCE_PLACEHOLDER}}}
 */
@Accessors(fluent = true)
public final class I18NText implements StringSupplier {

    private final I18N i18n;

    private final @Getter String key;

    // *****************************************************************************************
    // OverrideMethods, StringSupplier
    // *****************************************************************************************

    @Override
    public String get() {
        return i18n.get(key);
    }

    public String get(String defaults) {
        return i18n.get(key, defaults);
    }

    // *****************************************************************************************
    // OverrideMethods, Object
    // *****************************************************************************************

    @Override
    public String toString() {
        return i18n.get(key);
    }

    // *****************************************************************************************
    // PackageConstructors, used by `I18N`
    // *****************************************************************************************

    I18NText(I18N i18n, String key) {
        this.i18n = i18n;
        this.key = key;
    }

}
