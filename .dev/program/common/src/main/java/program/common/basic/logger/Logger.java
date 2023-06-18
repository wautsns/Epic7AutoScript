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
package program.common.basic.logger;

import program.common.basic.exception.InvocationException;

import java.io.IOException;
import java.io.InputStream;
import java.util.function.Supplier;
import java.util.logging.Level;
import java.util.logging.LogManager;
import java.util.logging.LogRecord;

import static java.lang.String.format;

/**
 * Logger.
 *
 * @author wautsns
 * @since {{{SINCE_PLACEHOLDER}}}
 */
public final class Logger {

    private static final Level ERROR = new Level("ERROR", 1000) {};
    private static final Level WARN = new Level("WARN", 900) {};
    private static final Level INFO = new Level("INFO", 800) {};
    private static final Level DEBUG = new Level("DEBUG", 500) {};

    private static final java.util.logging.Logger GLOBAL =
            ((Supplier<java.util.logging.Logger>) () -> {
                InputStream stream = Logger.class.getResourceAsStream("/logging.properties");
                if (stream != null) {
                    try (stream) {
                        LogManager.getLogManager().readConfiguration(stream);
                    } catch (IOException e) {
                        throw new InvocationException(e);
                    }
                }
                return java.util.logging.Logger.getGlobal();
            }).get();

    // *****************************************************************************************
    // StaticMethods, getting metadata
    // *****************************************************************************************

    public static String name() {
        return GLOBAL.getName();
    }

    // *****************************************************************************************
    // StaticMethods, logging info message
    // *****************************************************************************************

    public static void info(String msg) {
        log(INFO, msg);
    }

    public static void info(String fmt, Object... args) {
        log(INFO, fmt, args);
    }

    // *****************************************************************************************
    // StaticMethods, logging warn message
    // *****************************************************************************************

    public static void warn(String msg) {
        log(WARN, msg);
    }

    public static void warn(String fmt, Object... args) {
        log(WARN, fmt, args);
    }

    // *****************************************************************************************
    // StaticMethods, logging error message
    // *****************************************************************************************

    public static void error(String msg) {
        log(ERROR, msg);
    }

    public static void error(String fmt, Object... args) {
        log(ERROR, fmt, args);
    }

    // *****************************************************************************************
    // StaticMethods, logging special format message
    // *****************************************************************************************

    // example:
    //
    // ### character = ' ' ###
    // <empty_line>
    //
    // ### character = '*' ###
    // ************************************************
    //
    // ### character = '1' ###
    // 111111111111111111111111111111111111111111111111
    public static void splitLine(char character) {
        if (GLOBAL.isLoggable(INFO)) {
            LogRecord record = initRecord(INFO, null);
            record.setParameters(Formatter.initSplitLine(character));
            GLOBAL.log(record);
        }
    }

    // example:
    // <empty_line>
    public static void emptyLine() {
        splitLine(' ');
    }

    // example:
    //
    // ### level = 0 ###
    // +----------------------------------------------+
    // |                   <title>                    |
    // +----------------------------------------------+
    //
    // ### level = 1 ###
    // ################### <title> ####################
    //
    // ### level = 2 ###
    // =================== <title> ====================
    //
    // ### level = 3 ###
    // <<< <title> >>>
    //
    // ### other levels ###
    // <title>
    public static void title(int level, String text) {
        if (GLOBAL.isLoggable(INFO)) {
            LogRecord record = initRecord(INFO, null);
            record.setParameters(Formatter.initTitleParams(level, text));
            GLOBAL.log(record);
        }
    }

    // example:
    // [{name}] {value}
    public static void attribute(String name, Object value) {
        if (GLOBAL.isLoggable(INFO)) {
            LogRecord record = initRecord(INFO, null);
            record.setParameters(Formatter.initAttributeParams(name, value));
            GLOBAL.log(record);
        }
    }

    // *****************************************************************************************
    // *****************************************************************************************

    private final java.util.logging.Logger delegate;

    // *****************************************************************************************
    // Methods, logging debug message
    // *****************************************************************************************

    public void debug(String msg) {
        delegate.log(DEBUG, msg);
    }

    public void debug(String fmt, Object a) {
        if (delegate.isLoggable(DEBUG)) {
            delegate.log(initRecord(delegate, DEBUG, fmt, a));
        }
    }

    public void debug(String fmt, Object a, Object b) {
        if (delegate.isLoggable(DEBUG)) {
            delegate.log(initRecord(delegate, DEBUG, fmt, a, b));
        }
    }

    public void debug(String fmt, Object a, Object b, Object c) {
        if (delegate.isLoggable(DEBUG)) {
            delegate.log(initRecord(delegate, DEBUG, fmt, a, b, c));
        }
    }

    public void debug(String fmt, Object a, Object b, Object c, Object d) {
        if (delegate.isLoggable(DEBUG)) {
            delegate.log(initRecord(delegate, DEBUG, fmt, a, b, c, d));
        }
    }

    public void debug(String fmt, Object a, Object b, Object c, Object d, Object e) {
        if (delegate.isLoggable(DEBUG)) {
            delegate.log(initRecord(delegate, DEBUG, fmt, a, b, c, d, e));
        }
    }

    public void debug(String fmt, Object a, Object b, Object c, Object d, Object e, Object f) {
        if (delegate.isLoggable(DEBUG)) {
            delegate.log(initRecord(delegate, DEBUG, fmt, a, b, c, d, e, f));
        }
    }

    public void debug(String fmt, Object... args) {
        if (delegate.isLoggable(DEBUG)) {
            delegate.log(initRecord(delegate, DEBUG, fmt, args));
        }
    }

    // *****************************************************************************************
    // Constructors
    // *****************************************************************************************

    public Logger(Class<?> owner) {
        this.delegate = java.util.logging.Logger.getLogger(owner.getName());
    }

    // *****************************************************************************************
    // InternalStaticMethods
    // *****************************************************************************************

    private static LogRecord initRecord(Level level, String msg) {
        return new LogRecord(level, msg);
    }

    private static LogRecord initRecord(
            java.util.logging.Logger logger, Level level, String fmt, Object... args) {
        LogRecord record = new LogRecord(level, format(fmt, args));
        record.setLoggerName(logger.getName());
        if ((args.length > 0) && (args[args.length - 1] instanceof Throwable thrown)) {
            record.setThrown(thrown);
        }
        return record;
    }

    private static void log(Level level, String msg) {
        GLOBAL.log(level, msg);
    }

    private static void log(Level level, String fmt, Object... args) {
        if (GLOBAL.isLoggable(level)) {
            GLOBAL.log(initRecord(GLOBAL, level, fmt, args));
        }
    }

}
