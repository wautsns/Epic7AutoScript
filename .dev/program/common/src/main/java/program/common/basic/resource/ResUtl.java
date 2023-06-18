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
package program.common.basic.resource;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import program.common.basic.exception.InvocationException;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.io.InputStreamReader;

import static java.lang.String.format;

/**
 * Resource utility.
 *
 * @author wautsns
 * @since {{{SINCE_PLACEHOLDER}}}
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class ResUtl {

    private static final String HOME_PATH = System.getProperty("autoscript.home", "./");

    // *****************************************************************************************
    // StaticMethods, generating path
    // *****************************************************************************************

    public static String home(String path) {
        return new File(HOME_PATH, path).getPath();
    }

    public static String home(String pathFmt, Object... args) {
        return home(format(pathFmt, args));
    }

    // *****************************************************************************************
    // StaticMethods, loading resource
    // *****************************************************************************************

    public static InputStream loadAsStream(String path) {
        try {
            return new FileInputStream(path);
        } catch (FileNotFoundException e) {
            String message = "Resource not found";
            throw new InvocationException(message)
                    .with("resource_path", path);
        }
    }

    public static InputStreamReader loadAsReader(String path) {
        return new InputStreamReader(loadAsStream(path));
    }

    public static InputStream tryLoadAsStream(String path) {
        try {
            return new FileInputStream(path);
        } catch (FileNotFoundException e) {
            return null;
        }
    }

    public static InputStreamReader tryLoadAsReader(String path) {
        InputStream stream = tryLoadAsStream(path);
        return (stream == null) ? null : new InputStreamReader(stream);
    }

}
