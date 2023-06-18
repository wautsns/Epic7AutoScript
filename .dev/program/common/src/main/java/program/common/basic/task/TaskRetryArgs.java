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
package program.common.basic.task;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.experimental.Accessors;

/**
 * Task retry arguments.
 *
 * @author wautsns
 * @since {{{SINCE_PLACEHOLDER}}}
 */
@Builder
@Accessors(fluent = true)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public final class TaskRetryArgs {

    private @Builder.Default @Getter int retries = 3;

    // delay = Math.min( baseDelay * multiplier + random(randDelay), maxDelay )

    private @Builder.Default @Getter int baseDelay = 0;
    private @Builder.Default @Getter double multiplier = 1;
    private @Builder.Default @Getter int randDelay = 0;
    private @Builder.Default @Getter int maxDelay = 7000;

    private @Builder.Default @Getter TaskRetryFallback fallback = (error, retried) -> null;

}
