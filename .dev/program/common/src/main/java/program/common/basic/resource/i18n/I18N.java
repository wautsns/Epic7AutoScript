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
package program.common.basic.resource.i18n;

import lombok.Getter;
import lombok.experimental.Accessors;
import program.common.basic.exception.InvocationException;
import program.common.basic.resource.ResUtl;
import program.common.basic.utility.ObV;

import java.io.File;
import java.io.IOException;
import java.io.Reader;
import java.util.Properties;

/**
 * I18N.
 *
 * @author wautsns
 * @since {{{SINCE_PLACEHOLDER}}}
 */
@Accessors(fluent = true)
public final class I18N {

    private final @Getter String pathFormat;

    private final @Getter ObV<Language> language;
    @SuppressWarnings({"FieldCanBeLocal", "unused"}) // strongly referenced by this instance
    private final Object languageObserver;

    private final Properties properties;

    // *****************************************************************************************
    // Methods, getting l10n text
    // *****************************************************************************************

    public String get(String key) {
        return properties.getProperty(key, key);
    }

    public String get(String key, String defaults) {
        return properties.getProperty(key, defaults);
    }

    // *****************************************************************************************
    // Methods, initializing i18n text
    // *****************************************************************************************

    public I18NText text(String key) {
        return new I18NText(this, key);
    }

    public I18NText text(String keyPart1, String keyPart2) {
        int n = keyPart1.length() + keyPart2.length();
        StringBuilder bu = new StringBuilder(n);
        bu.append(keyPart1).append(keyPart2);
        return new I18NText(this, bu.toString());
    }

    public I18NText text(String keyPart1, String keyPart2, String keyPart3) {
        int n = keyPart1.length() + keyPart2.length() + keyPart3.length();
        StringBuilder bu = new StringBuilder(n);
        bu.append(keyPart1).append(keyPart2).append(keyPart3);
        return new I18NText(this, bu.toString());
    }

    public I18NText text(String... keyParts) {
        return text(String.join("", keyParts));
    }

    // *****************************************************************************************
    // Constructors
    // *****************************************************************************************

    public I18N(String directory, ObV<Language> language) {
        this.pathFormat = new File(directory, "/{{language}}.properties").getPath();
        this.language = language;
        this.properties = new Properties();
        this.languageObserver = language.observe((prev, curr) -> {
            properties.clear();
            if (curr == null) {return;}
            Reader reader = tryLoadAsReader(directory, prev, curr);
            if (reader == null) {return;}
            try (reader) {
                properties.load(reader);
            } catch (IOException e) {
                throw new InvocationException(e)
                        .with("path_format", directory)
                        .with("previous_language", prev)
                        .with("target_language", curr);
            }
        }, true);
    }

    public I18N(File directory, ObV<Language> language) {
        this(directory.getPath(), language);
    }

    // *****************************************************************************************
    // InternalStaticMethods
    // *****************************************************************************************

    private static Reader tryLoadAsReader(String pathFormat, Language prev, Language curr) {
        Reader reader = tryLoadAsReader(pathFormat, curr);
        if (reader != null) {return reader;}
        if ((prev != null) && prev.country().equals(curr.country())) {
            return tryLoadAsReader(pathFormat, null, prev);
        }
        for (Language language : Language.constants()) {
            if (!language.country().equals(curr.country())) {continue;}
            if (language == curr) {continue;}
            reader = tryLoadAsReader(pathFormat, language);
            if (reader != null) {return reader;}
        }
        return (curr != Language.en) ? tryLoadAsReader(pathFormat, Language.en) : null;
    }

    private static Reader tryLoadAsReader(String pathFormat, Language language) {
        return ResUtl.tryLoadAsReader(pathFormat.replace("{{language}}", language.name()));
    }

}
