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

import lombok.AccessLevel;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.experimental.Accessors;
import program.common.basic.utility.ObV;

import java.util.List;
import java.util.Locale;
import java.util.function.Supplier;

/**
 * Language.
 *
 * @author wautsns
 * @since {{{SINCE_PLACEHOLDER}}}
 */
@Accessors(fluent = true)
@RequiredArgsConstructor(access = AccessLevel.PRIVATE)
public enum Language {

    en("english", Locale.ENGLISH),

    zhCN("简体中文", Locale.SIMPLIFIED_CHINESE),

    ;

    // *****************************************************************************************
    // *****************************************************************************************

    private final @Getter String displayName;

    private final @Getter Locale locale;

    // *****************************************************************************************
    // Methods, getting metadata
    // *****************************************************************************************

    public String country() {
        return locale.getCountry();
    }

    // *****************************************************************************************
    // OverrideMethods, Object
    // *****************************************************************************************

    @Override
    public String toString() {
        return displayName;
    }

    // *****************************************************************************************
    // *****************************************************************************************

    private static final @Getter List<Language> constants = List.of(values());

    private static final @Getter Language defaults = ((Supplier<Language>) () -> {
        Language result;
        Locale defaultLocale = Locale.getDefault();
        result = constants.stream()
                .filter(constant -> constant.locale.equals(defaultLocale))
                .findFirst()
                .orElse(null);
        if (result != null) {return result;}
        String defaultLanguage = defaultLocale.getLanguage();
        result = constants.stream()
                .filter(constant -> constant.locale.getLanguage().equals(defaultLanguage))
                .findFirst()
                .orElse(null);
        return (result != null) ? result : Language.en;
    }).get();

    private static final @Getter ObV<Language> global = new ObV<>(defaults);

}
