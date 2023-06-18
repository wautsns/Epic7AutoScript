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

import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicInteger;

import static java.lang.String.format;

/**
 * Task result.
 *
 * @author wautsns
 * @since {{{SINCE_PLACEHOLDER}}}
 */
@Accessors(fluent = true)
public final class TaskResult<T> {

    private static final ExecutorService SERV = Executors.newCachedThreadPool(new ThreadFactory() {
        private final AtomicInteger nextId = new AtomicInteger(1);

        @Override
        public Thread newThread(Runnable runnable) {
            Thread thread = new Thread(runnable);
            thread.setName(format("Task%04d", nextId.getAndIncrement()));
            thread.setDaemon(true);
            return thread;
        }
    });

    // *****************************************************************************************
    // *****************************************************************************************

    private final @Getter Task<T> task;

    private final Future<T> future;

    // *****************************************************************************************
    // Methods, getting state
    // *****************************************************************************************

    public boolean cancelled() {
        return future.isCancelled();
    }

    public boolean completed() {
        return future.isDone();
    }

    // *****************************************************************************************
    // Methods, manipulating task
    // *****************************************************************************************

    public boolean cancel(boolean force) {
        return future.cancel(force);
    }

    public T waitUntilComplete(int timeout) {
        try {
            if (timeout < 0) {
                return future.get();
            } else {
                return future.get(timeout, TimeUnit.MILLISECONDS);
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new InvocationException(e)
                    .with("task_name", task.name);
        } catch (TimeoutException e) {
            throw new InvocationException(e)
                    .with("task_name", task.name)
                    .with("timeout", timeout);
        } catch (ExecutionException e) {
            throw new InvocationException(e)
                    .with("task_name", task.name);
        }
    }

    // *****************************************************************************************
    // PackageConstructors, used by `Task`
    // *****************************************************************************************

    TaskResult(Task<T> task) {
        this.task = task;
        this.future = SERV.submit(task);
    }

}
