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

import lombok.Getter;
import lombok.experimental.Accessors;
import program.common.basic.exception.InvocationException;
import program.common.basic.task._impl.BasicTask;
import program.common.basic.task._impl.RetryTask;

import java.util.concurrent.Callable;

/**
 * Task.
 *
 * @author wautsns
 * @since {{{SINCE_PLACEHOLDER}}}
 */
@Accessors(fluent = true)
public abstract class Task<T> implements Callable<T> {

    // *****************************************************************************************
    // StaticMethods, initializing instance
    // *****************************************************************************************

    public static <T> Task<T> basic(String name, Callable<T> delegate) {
        return new BasicTask<>(name, delegate);
    }

    // *****************************************************************************************
    // StaticMethods, causing thread sleep
    // *****************************************************************************************

    public static void sleep(int duration) {
        if (duration < 1) {return;}
        try {
            Thread.sleep(duration);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new InvocationException(e);
        }
    }

    // *****************************************************************************************
    // *****************************************************************************************

    protected final @Getter String name;

    // *****************************************************************************************
    // Methods, executing function
    // *****************************************************************************************

    @Override
    public abstract T call();

    public final TaskResult<T> submit() {
        return new TaskResult<>(this);
    }

    // *****************************************************************************************
    // Methods, enhancing function
    // *****************************************************************************************

    public final Task<T> retry(TaskRetryArgs args) {
        return new RetryTask<>(this, args);
    }

    // *****************************************************************************************
    // InternalConstructors
    // *****************************************************************************************

    protected Task(String name) {
        this.name = name;
    }

}
