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
package program.common.basic.resource.conf;

import lombok.Getter;
import lombok.experimental.Accessors;
import program.common.basic.utility.ObV;

/**
 * Property.
 *
 * @author wautsns
 * @since {{{SINCE_PLACEHOLDER}}}
 */
@Accessors(fluent = true)
public final class Property {

    private final @Getter PropertyMetadata metadata;
    private final @Getter ObV<String> value;

    // *****************************************************************************************
    // PackageConstructors, used by `Config`
    // *****************************************************************************************

    Property(PropertyMetadata metadata, ObV<String> value) {
        this.metadata = metadata;
        this.value = value;
    }

}
