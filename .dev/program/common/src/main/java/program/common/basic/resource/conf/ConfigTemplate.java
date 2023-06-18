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

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONException;
import com.alibaba.fastjson2.JSONObject;
import lombok.Getter;
import lombok.experimental.Accessors;
import program.common.basic.exception.InvocationException;
import program.common.basic.resource.ResUtl;
import program.common.basic.resource.i18n.I18N;
import program.common.basic.resource.i18n.Language;
import program.common.basic.utility.WeakSet;

import java.io.File;
import java.util.LinkedHashMap;
import java.util.LinkedList;

import static java.lang.String.format;

/**
 * Config template.
 *
 * @author wautsns
 * @since {{{SINCE_PLACEHOLDER}}}
 */
@Accessors(fluent = true)
public final class ConfigTemplate {

    // *********************************************************************************
    // StaticMethods, initializing instance
    // *********************************************************************************

    private static final WeakSet<ConfigTemplate> REFERENCES = new WeakSet<>();

    public static ConfigTemplate of(String directory) {
        return of(new File(directory));
    }

    public static ConfigTemplate of(File directory) {
        String absolute = directory.getAbsolutePath();
        return REFERENCES.add(
                ref -> ref.file.getAbsolutePath().equals(absolute),
                () -> new ConfigTemplate(absolute)
        );
    }

    // *********************************************************************************
    // *********************************************************************************

    private final File file;
    private final @Getter Class<? extends Configurable> program;
    private final @Getter long version;

    // package field, used by `Config`
    final LinkedList<Object> content;
    // package field, used by `Config`
    final LinkedHashMap<String, PropertyMetadata> propertyMetadataMap;

    // *********************************************************************************
    // InternalConstructors
    // *********************************************************************************

    private ConfigTemplate(String directory) {
        this.file = new File(directory, "config.metadata.json").getAbsoluteFile();
        this.content = new LinkedList<>();
        this.propertyMetadataMap = new LinkedHashMap<>();
        try {
            JSONObject node = JSON.parseObject(ResUtl.loadAsReader(file.getPath()));
            this.program = readProgram(node);
            this.version = readVersion(node);
            readContent(node);
        } catch (InvocationException | JSONException e) {
            throw new InvocationException(e)
                    .with("config_template_directory", directory);
        }
    }

    // *********************************************************************************
    // InternalMethods
    // *********************************************************************************

    private void readContent(JSONObject node) {
        String lineSep = System.lineSeparator();
        I18N i18n = new I18N(new File(file.getParent(), "/i18n"), Language.global());
        JSONArray contentNode = node.getJSONArray("content");
        for (int i = 0, l = contentNode.size(); i < l; i++) {
            JSONObject elementNode = contentNode.getJSONObject(i);
            String raw = elementNode.getString("raw");
            if (raw != null) {
                content.add(raw.replaceAll("\n", lineSep));
            } else {
                PropertyMetadata propertyMetadata = new PropertyMetadata(i18n, elementNode);
                content.add(propertyMetadata);
                propertyMetadataMap.put(propertyMetadata.name(), propertyMetadata);
            }
        }
    }

    // *********************************************************************************
    // PackageStaticMethods, used by `Config`
    // *********************************************************************************

    static Class<? extends Configurable> readProgram(JSONObject node) {
        String program = node.getString("program");
        if ((program == null) || program.isBlank()) {
            String message = "Missing metadata: `program` in config";
            throw new InvocationException(message);
        }
        Class<?> clazz;
        try {
            clazz = Class.forName(program);
        } catch (ClassNotFoundException e) {
            String message = "Illegal metadata: `program` in config";
            throw new InvocationException(message, e)
                    .with("program", program);
        }
        if (!Configurable.class.isAssignableFrom(clazz)) {
            String message = format(
                    "Metadata: `program` in config should implement `%s`",
                    Configurable.class.getName()
            );
            throw new InvocationException(message)
                    .with("program", clazz);
        }
        // noinspection unchecked
        return (Class<? extends Configurable>) clazz;
    }

    static long readVersion(JSONObject node) {
        String version = node.getString("version");
        if ((version == null) || version.isBlank()) {
            String message = "Missing metadata: `version` in config";
            throw new InvocationException(message);
        }
        try {
            return Long.parseLong(version);
        } catch (NumberFormatException e) {
            String message = "Illegal metadata: `version` in config";
            throw new InvocationException(message, e)
                    .with("version", version);
        }
    }

}
