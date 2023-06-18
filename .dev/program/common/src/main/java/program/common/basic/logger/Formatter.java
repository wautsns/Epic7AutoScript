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

import program.common.basic.utility.StrUtl;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.logging.LogRecord;

/**
 * Default logger formatter.
 *
 * @author wautsns
 * @since {{{SINCE_PLACEHOLDER}}}
 */
public class Formatter extends java.util.logging.Formatter {

    // *****************************************************************************************
    // StaticMethods, initializing parameters
    // *****************************************************************************************

    private static final Object PARAM0_SPLIT_LINE = new byte[0];
    private static final Object PARAM0_TITLE = new byte[0];
    private static final Object PARAM0_ATTRIBUTE = new byte[0];

    public static Object[] initSplitLine(char character) {
        return new Object[]{PARAM0_SPLIT_LINE, character};
    }

    public static Object[] initTitleParams(int level, String title) {
        return new Object[]{PARAM0_TITLE, level, title};
    }

    public static Object[] initAttributeParams(String name, Object value) {
        return new Object[]{PARAM0_ATTRIBUTE, name, value};
    }

    // *****************************************************************************************
    // OverrideMethods, Formatter
    // *****************************************************************************************

    @Override
    public final String format(LogRecord record) {
        Object[] params = record.getParameters();
        if ((params == null) || (params.length == 0)) {
            return defaultFormat(record);
        } else {
            Object param0 = params[0];
            if (param0 == PARAM0_SPLIT_LINE) {
                return formatSplitLine(record, (Character) params[1]);
            } else if (param0 == PARAM0_TITLE) {
                return formatTitle(record, (Integer) params[1], (String) params[2]);
            } else if (param0 == PARAM0_ATTRIBUTE) {
                return formatAttribute(record, (String) params[1], params[2]);
            } else {
                return defaultFormat(record);
            }
        }
    }

    // *****************************************************************************************
    // InternalMethods
    // *****************************************************************************************

    protected CharSequence getInstantChars(LogRecord record) {
        ZonedDateTime datetime = record.getInstant().atZone(ZoneId.systemDefault());
        // HH:mm:ss.SSS
        StringBuilder bu = new StringBuilder(12);
        int hour = datetime.getHour();
        int minute = datetime.getMinute();
        int second = datetime.getSecond();
        int millis = datetime.getNano() / 1_000_000;
        if (hour < 10) {
            bu.append('0');
        }
        bu.append(hour).append(':');
        if (minute < 10) {
            bu.append('0');
        }
        bu.append(minute).append(':');
        if (second < 10) {
            bu.append('0');
        }
        bu.append(second).append('.');
        if (millis < 10) {
            bu.append("00");
        } else if (millis < 100) {
            bu.append('0');
        }
        bu.append(millis);
        return bu;
    }

    protected String defaultFormat(LogRecord record) {
        String levelName = record.getLevel().getName();
        CharSequence instantChars = getInstantChars(record);
        CharSequence inlineMessage = StrUtl.inline(record.getMessage());
        String loggerName = record.getLoggerName();
        String lineSep = System.lineSeparator();
        boolean showLoggerName = !Logger.name().equals(loggerName);
        // <levelName:5> <instantChars> | <inlineMessage> // <loggerName><lineSep>
        int n = 9 + instantChars.length() + inlineMessage.length();
        if (showLoggerName) {
            n += 4 + loggerName.length();
        }
        n += lineSep.length();
        StringBuilder bu = new StringBuilder(n);
        bu.append(levelName);
        if (levelName.length() == 4) {
            bu.append(' ');
        } else {
            for (int i = levelName.length(); i < 5; i++) {
                bu.append(' ');
            }
        }
        bu.append(' ').append(instantChars).append(" | ").append(inlineMessage);
        if (showLoggerName) {
            bu.append(" // ").append(loggerName);
        }
        bu.append(lineSep);
        Throwable thrown = record.getThrown();
        if (thrown != null) {
            StringWriter trace = new StringWriter(512);
            thrown.printStackTrace(new PrintWriter(trace));
            bu.append(trace.getBuffer());
        }
        return bu.toString();
    }

    protected String formatSplitLine(LogRecord record, char character) {
        String lineSep = System.lineSeparator();
        if (character == ' ') {return lineSep;}
        int n = 48 + lineSep.length();
        StringBuilder bu = new StringBuilder(n);
        switch (character) {
            case '*' -> bu.append("************************************************");
            case '#' -> bu.append("################################################");
            case '=' -> bu.append("================================================");
            case '-' -> bu.append("------------------------------------------------");
            default -> {
                for (int i = 0; i < 48; i++) {
                    bu.append(character);
                }
            }
        }
        bu.append(lineSep);
        return bu.toString();
    }

    protected String formatTitle(LogRecord record, int level, String title) {
        String lineSep = System.lineSeparator();
        if (level == 1) {
            // ################### <title> ####################<lineSep>
            int n = 48 + lineSep.length();
            StringBuilder bu = new StringBuilder(n);
            bu.append("################################################");
            int i = 24 - ((title.length() + 3) >> 1);
            bu.setCharAt(i++, ' ');
            for (int j = 0; j < title.length(); j++) {
                bu.setCharAt(i++, title.charAt(j));
            }
            bu.setCharAt(i, ' ');
            bu.append(lineSep);
            return bu.toString();
        } else if (level == 2) {
            // =================== <title> ====================<lineSep>
            int n = 48 + lineSep.length();
            StringBuilder bu = new StringBuilder(n);
            bu.append("================================================");
            int i = 24 - ((title.length() + 3) >> 1);
            bu.setCharAt(i++, ' ');
            for (int j = 0; j < title.length(); j++) {
                bu.setCharAt(i++, title.charAt(j));
            }
            bu.setCharAt(i, ' ');
            bu.append(lineSep);
            return bu.toString();
        } else if (level == 3) {
            // <<< <title> >>><lineSep>
            int n = 8 + title.length() + lineSep.length();
            StringBuilder bu = new StringBuilder(n);
            bu.append("<<< ").append(title).append(" >>>").append(lineSep);
            return bu.toString();
        } else if (level == 0) {
            // |                    <title>                    |
            StringBuilder middle = new StringBuilder(48);
            middle.append("|                                              |");
            int i = 24 - ((title.length() + 3) >> 1) + 1;
            for (int j = 0; j < title.length(); j++) {
                middle.setCharAt(i++, title.charAt(j));
            }
            String border = "+----------------------------------------------+";
            // +----------------------------------------------+<lineSep>
            // |                   <title>                    |<lineSep>
            // +----------------------------------------------+<lineSep>
            int n = 48 * 3 + lineSep.length() * 3;
            StringBuilder bu = new StringBuilder(n);
            bu.append(border).append(lineSep).append(middle).append(lineSep).append(border)
                    .append(lineSep);
            return bu.toString();
        } else {
            // <title><lineSep>
            int n = title.length() + lineSep.length();
            StringBuilder bu = new StringBuilder(n);
            bu.append(title).append(lineSep);
            return bu.toString();
        }
    }

    protected String formatAttribute(LogRecord record, String name, Object value) {
        String lineSep = System.lineSeparator();
        CharSequence inlineValueChars = StrUtl.inline(String.valueOf(value));
        // [<name>] <inlineValueChars><lineSep>
        int n = 3 + name.length() + inlineValueChars.length() + lineSep.length();
        StringBuilder bu = new StringBuilder(n);
        bu.append('[').append(name).append("] ").append(inlineValueChars).append(lineSep);
        return bu.toString();
    }

}
