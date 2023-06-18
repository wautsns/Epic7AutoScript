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
package program.common.basic.exception;

import java.io.IOException;
import java.io.PrintStream;
import java.io.PrintWriter;
import java.util.AbstractMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Special {@link RuntimeException} for invocation.
 *
 * @author wautsns
 * @since {{{SINCE_PLACEHOLDER}}}
 */
public final class InvocationException extends RuntimeException {

    private final List<Map.Entry<String, Object>> context = new LinkedList<>();

    // *****************************************************************************************
    // Methods, associating context value with key
    // *****************************************************************************************

    public InvocationException with(String key, Object value) {
        context.add(new AbstractMap.SimpleImmutableEntry<>(key, value));
        return this;
    }

    // *****************************************************************************************
    // OverrideMethods, RuntimeException
    // *****************************************************************************************

    @Override
    public void printStackTrace(PrintStream stream) {
        try {
            internalPrintStackTrace(stream);
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
    }

    @Override
    public void printStackTrace(PrintWriter writer) {
        try {
            internalPrintStackTrace(writer);
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
    }

    // *****************************************************************************************
    // Constructors
    // *****************************************************************************************

    public InvocationException(Throwable cause) {
        super(null /* Failed to invoke `CLASS#METHOD(...)` */, cause);
    }

    public InvocationException(String message) {
        super(message);
    }

    public InvocationException(String message, Throwable cause) {
        super(message, cause);
    }

    // *****************************************************************************************
    // InternalMethods
    // *****************************************************************************************

    private void internalPrintStackTrace(Appendable writer) throws IOException {
        String lineSep = System.lineSeparator();
        Set<StackTraceElement> printedTraceSet = new HashSet<>();
        for (Throwable thrown = this; thrown != null; thrown = thrown.getCause()) {
            StackTraceElement[] trace = thrown.getStackTrace();
            if (!printedTraceSet.isEmpty()) {
                writer.append(lineSep);
                writer.append("Caused by: ");
            }
            writer.append(thrown.getClass().getName());
            writer.append(": ");
            String message = thrown.getMessage();
            if (message != null) {
                writer.append(message);
            } else {
                StackTraceElement element = trace[0];
                writer.append("Failed to invoke `");
                writer.append(element.getClassName());
                writer.append("#");
                writer.append(element.getMethodName());
                writer.append("(...)`");
            }
            if (thrown instanceof InvocationException e) {
                for (Map.Entry<String, Object> entry : e.context) {
                    writer.append(lineSep);
                    writer.append("\t\twith <");
                    writer.append(entry.getKey());
                    writer.append("> = ");
                    writer.append(String.valueOf(entry.getValue()));
                }
            }
            for (int i = 0, l = trace.length; i < l; i++) {
                StackTraceElement element = trace[i];
                if (printedTraceSet.add(element)) {
                    writer.append(lineSep);
                    writer.append("\tat ");
                    writer.append(element.toString());
                } else {
                    writer.append(lineSep);
                    writer.append("\t... ");
                    writer.append(Integer.toString(l - i));
                    writer.append(" more");
                    break;
                }
            }
        }
        writer.append(lineSep);
    }

}
