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
package program.driver.basic;

import lombok.Getter;
import lombok.experimental.Accessors;
import program.common.basic.resource.ResUtl;
import program.common.basic.resource.i18n.I18N;
import program.common.basic.resource.i18n.Language;
import program.common.basic.utility.ObV;

import static java.lang.String.format;

/**
 * Game i18n.
 *
 * @author wautsns
 * @since {{{SINCE_PLACEHOLDER}}}
 */
@Accessors(fluent = true)
public final class GameI18N {

    private final @Getter I18N items;

    // *********************************************************************************
    // Constructors
    // *********************************************************************************

    public GameI18N(ObV<Language> language) {
        String fmt = ResUtl.home("/.dev/resources/driver/basic/#GameI18N/%s");
        this.items = new I18N(format(fmt, "items"), language);
    }

}
