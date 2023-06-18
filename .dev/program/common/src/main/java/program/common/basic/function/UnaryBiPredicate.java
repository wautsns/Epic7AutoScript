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
package program.common.basic.function;

import java.util.function.BiPredicate;

/**
 * Special {@link BiPredicate} with the same parameters type.
 *
 * @author wautsns
 * @since {{{SINCE_PLACEHOLDER}}}
 */
@FunctionalInterface
public interface UnaryBiPredicate<T> extends BiPredicate<T, T> {}
