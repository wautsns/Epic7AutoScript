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

import program.common.basic.exception.InvocationException;
import program.common.basic.resource.SilentCloseable;

import static java.lang.String.format;

/**
 * Configurable.
 *
 * @author wautsns
 * @since {{{SINCE_PLACEHOLDER}}}
 */
public abstract class Configurable implements SilentCloseable {

    protected final Config config;
    @SuppressWarnings({"FieldCanBeLocal", "unused"}) // strongly referenced by this instance
    private final Object callbackAfterConfigSaving;

    private boolean closed;

    // *****************************************************************************************
    // Methods, getting metadata
    // *****************************************************************************************

    public final Config config() {
        return config;
    }

    // *****************************************************************************************
    // Methods, validating state
    // *****************************************************************************************

    public final synchronized void acquireNotClosed() {
        if (closed) {
            String message = format("Configurable [%s] closed", getClass().getSimpleName());
            throw new InvocationException(message)
                    .with("config_path", config.file().getAbsolutePath());
        }
    }

    // *****************************************************************************************
    // OverrideMethods, SilentCloseable
    // *****************************************************************************************

    @Override
    public final synchronized boolean closed() {
        return closed;
    }

    @Override
    public final synchronized void close() {
        if (closed) {
            return;
        }
        release();
        closed = true;
    }

    // *****************************************************************************************
    // InternalConstructors
    // *****************************************************************************************

    protected Configurable(Config config) {
        this.config = config;
        this.closed = true;
        this.callbackAfterConfigSaving = config.callbackAfterSaving(() -> {
            synchronized (this) {
                close();
                try {
                    reinitialize();
                } catch (Exception e) {
                    close();
                    String message = format(
                            "Failed to reinitialize configurable [%s]",
                            getClass().getSimpleName()
                    );
                    throw new InvocationException(message, e)
                            .with("config_path", this.config.file().getAbsolutePath());
                }
                this.closed = false;
            }
        });
    }

    // *****************************************************************************************
    // InternalMethods
    // *****************************************************************************************

    protected abstract void release();

    protected abstract void reinitialize();

}
