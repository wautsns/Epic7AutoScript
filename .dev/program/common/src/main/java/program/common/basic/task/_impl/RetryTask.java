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
package program.common.basic.task._impl;

import program.common.basic.exception.InvocationException;
import program.common.basic.task.Task;
import program.common.basic.task.TaskRetryArgs;

import java.util.concurrent.ThreadLocalRandom;

/**
 * Special {@link Task} supported for retry.
 *
 * @author wautsns
 * @since {{{SINCE_PLACEHOLDER}}}
 */
public final class RetryTask<T> extends Task<T> {

    private final Task<T> delegate;
    private final TaskRetryArgs args;

    // *****************************************************************************************
    // OverrideMethods, Task
    // *****************************************************************************************

    @Override
    public T call() {
        for (int retried = 0; ; retried++) {
            try {
                return delegate.call();
            } catch (RuntimeException e) {
                if (retried >= args.retries()) {
                    String message = "Task has been retried too many times";
                    throw new InvocationException(message, e)
                            .with("task_name", delegate.name())
                            .with("retried", retried);
                }
                String reason = args.fallback().resolve(e, retried);
                if (reason != null) {
                    String message = "Retry of task was interrupted";
                    throw new InvocationException(message, e)
                            .with("task_name", delegate.name())
                            .with("reason_for_interruption", reason)
                            .with("retried", retried);
                }
                int delay = (int) (args.baseDelay() * Math.pow(args.multiplier(), retried));
                if (args.randDelay() > 0) {
                    delay += ThreadLocalRandom.current().nextInt(args.randDelay());
                }
                delay = Math.min(delay, args.maxDelay());
                Task.sleep(delay);
            }
        }
    }

    // *****************************************************************************************
    // Constructors
    // *****************************************************************************************

    public RetryTask(Task<T> delegate, TaskRetryArgs args) {
        super(delegate.name() + "/retry");
        this.delegate = delegate;
        this.args = args;
    }

}
