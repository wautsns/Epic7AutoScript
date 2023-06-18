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

import java.lang.ref.WeakReference;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.function.Supplier;

/**
 * Weak set.
 *
 * @author wautsns
 * @since {{{SINCE_PLACEHOLDER}}}
 */
public final class WeakSet<T> {

    private final WeakNode<T> head = new WeakNode<>(null);

    // *****************************************************************************************
    // Methods, getting state
    // *****************************************************************************************

    public synchronized boolean isEmpty() {
        WeakNode<T> prev = head, curr = prev.next;
        while (curr != null) {
            T ref = curr.get();
            if (ref == null) {
                prev.next = curr.next;
            } else {
                return false;
            }
            curr = curr.next;
        }
        return true;
    }

    // *****************************************************************************************
    // Methods, getting referent
    // *****************************************************************************************

    public synchronized T get(Predicate<T> matcher) {
        WeakNode<T> prev = head, curr = prev.next;
        while (curr != null) {
            T ref = curr.get();
            if (ref == null) {
                prev.next = curr.next;
            } else if (matcher.test(ref)) {
                return ref;
            } else {
                prev = curr;
            }
            curr = curr.next;
        }
        return null;
    }

    public synchronized void forEach(Consumer<T> action) {
        WeakNode<T> prev = head, curr = prev.next;
        while (curr != null) {
            T ref = curr.get();
            if (ref == null) {
                prev.next = curr.next;
            } else {
                action.accept(ref);
                prev = curr;
            }
            curr = curr.next;
        }
    }

    // *****************************************************************************************
    // Methods, adding referent
    // *****************************************************************************************

    public synchronized T add(T referent) {
        boolean added = false;
        WeakNode<T> prev = head, curr = prev.next;
        while (curr != null) {
            T ref = curr.get();
            if (ref == null) {
                prev.next = curr.next;
            } else {
                if (ref.equals(referent)) {
                    added = true;
                }
                prev = curr;
            }
            curr = curr.next;
        }
        if (!added) {
            prev.next = new WeakNode<>(referent);
        }
        return referent;
    }

    // Note: If there are multiple referents that satisfy the given matcher, the first one will be
    // returned.
    //
    // @param [matcher] a function used to determine whether the reference is added
    // @param [supplier] if the given matcher returns false for all references in the set, the
    // referent this function returned will be added
    // @return the referent which matches the matcher or the referent returned from the supplier
    public synchronized T add(Predicate<T> matcher, Supplier<T> supplier) {
        T referent = null;
        WeakNode<T> prev = head, curr = prev.next;
        while (curr != null) {
            T ref = curr.get();
            if (ref == null) {
                prev.next = curr.next;
            } else {
                if ((referent == null) && matcher.test(ref)) {
                    referent = ref;
                }
                prev = curr;
            }
            curr = curr.next;
        }
        if (referent == null) {
            referent = supplier.get();
            prev.next = new WeakNode<>(referent);
        }
        return referent;
    }

    // *****************************************************************************************
    // Methods, removing referent
    // *****************************************************************************************

    public synchronized boolean remove(Object referent) {
        WeakNode<T> prev = head, curr = prev.next;
        while (curr != null) {
            T ref = curr.get();
            if (ref == null) {
                prev.next = curr.next;
            } else if (Objects.equals(ref, referent)) {
                prev.next = curr.next;
                return true;
            } else {
                prev = curr;
            }
            curr = curr.next;
        }
        return false;
    }

    // @return total number removed
    public synchronized int remove(Predicate<T> filter) {
        int removed = 0;
        WeakNode<T> prev = head, curr = prev.next;
        while (curr != null) {
            T ref = curr.get();
            if (ref == null) {
                prev.next = curr.next;
            } else if (filter.test(ref)) {
                prev.next = curr.next;
                removed++;
            } else {
                prev = curr;
            }
            curr = curr.next;
        }
        return removed;
    }

    public synchronized void clear() {
        head.next = null;
    }

    // *****************************************************************************************
    // InternalStaticClasses
    // *****************************************************************************************

    private static final class WeakNode<T> extends WeakReference<T> {

        WeakNode<T> next;

        WeakNode(T referent) {
            super(referent);
        }

    }

}
