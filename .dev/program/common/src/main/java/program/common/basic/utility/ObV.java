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

import lombok.Getter;
import program.common.basic.function.UnaryBiConsumer;

import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * Observable value.
 *
 * @author wautsns
 * @since {{{SINCE_PLACEHOLDER}}}
 */
public final class ObV<T> {

    private @Getter final ReentrantReadWriteLock lock;

    private volatile T value;
    private final WeakSet<UnaryBiConsumer<T>> observers = new WeakSet<>();

    // *****************************************************************************************
    // Methods, getting value
    // *****************************************************************************************

    public T get() {
        lock.readLock().lock();
        try {
            return value;
        } finally {
            lock.readLock().unlock();
        }
    }

    // *****************************************************************************************
    // Methods, setting value
    // *****************************************************************************************

    public T set(T value) {
        T prev;
        lock.writeLock().lock();
        try {
            prev = this.value;
            this.value = value;
            lock.readLock().lock();
        } finally {
            lock.writeLock().unlock();
        }
        try {
            observers.forEach(observer -> observer.accept(prev, value));
        } finally {
            lock.readLock().unlock();
        }
        return prev;
    }

    // *****************************************************************************************
    // Methods, adding observer
    // *****************************************************************************************

    // WARNING: The given observer needs to be strongly referenced by the caller of this method
    // because this instance only holds its weak reference.
    //
    // @param [observer] the arguments[0] is the previous value, or null if the observer is
    // triggered immediately; the arguments[1] is the current value
    // @return the given observer
    public Object observe(UnaryBiConsumer<T> observer, boolean immediately) {
        lock.writeLock().lock();
        try {
            observers.add(observer);
        } finally {
            lock.writeLock().unlock();
        }
        if (immediately) {
            lock.readLock().lock();
            try {
                observer.accept(null, value);
            } finally {
                lock.readLock().unlock();
            }
        }
        return observer;
    }

    // *****************************************************************************************
    // Constructors
    // *****************************************************************************************

    public ObV(T initialValue) {
        this(new ReentrantReadWriteLock(), initialValue);
    }

    public ObV(ReentrantReadWriteLock lock, T initialValue) {
        this.lock = lock;
        this.value = initialValue;
    }

}
