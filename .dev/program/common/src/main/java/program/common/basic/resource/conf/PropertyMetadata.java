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

import com.alibaba.fastjson2.JSONObject;
import lombok.Getter;
import lombok.experimental.Accessors;
import program.common.basic.exception.InvocationException;
import program.common.basic.resource.i18n.I18N;
import program.common.basic.resource.i18n.I18NText;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

/**
 * Property metadata.
 *
 * @author wautsns
 * @since {{{SINCE_PLACEHOLDER}}}
 */
@Accessors(fluent = true)
public final class PropertyMetadata {

    private final @Getter String name;
    private final @Getter long since;
    private final @Getter I18NText description;
    private final @Getter Options options;
    private final @Getter String defaults;

    // *********************************************************************************
    // PackageConstructors, used by `ConfigMetadata`
    // *********************************************************************************

    PropertyMetadata(I18N i18n, JSONObject node) {
        this.name = node.getString("name");
        this.since = node.getLongValue("since");
        this.description = i18n.text(name);
        this.options = new Options(i18n, node);
        this.defaults = node.getString("defaults");
    }

    // *********************************************************************************
    // Classes
    // *********************************************************************************

    @Accessors(fluent = true)
    public final class Options {

        private final @Getter boolean customized;
        private final Map<String, I18NText> descriptionMap;

        // *********************************************************************************
        // Methods, getting value
        // *********************************************************************************

        public Set<String> values() {
            return descriptionMap.keySet();
        }

        public I18NText description(String value) {
            I18NText description = descriptionMap.get(value);
            if (description != null) {
                return description;
            } else if (customized) {
                return PropertyMetadata.this.description;
            } else {
                String message = "Description not found for the specified value";
                throw new InvocationException(message)
                        .with("property_name", name)
                        .with("option_value", value);
            }
        }

        // *********************************************************************************
        // InternalConstructors
        // *********************************************************************************

        private Options(I18N i18n, JSONObject node) {
            String[] values = node.getObject("options", String[].class);
            if (values == null) {
                this.customized = true;
                this.descriptionMap = Collections.emptyMap();
            } else {
                int size = values.length;
                this.customized = (size > 0) && "...".equals(values[size - 1]);
                size -= customized ? 1 : 0;
                Map<String, I18NText> descriptionMap = new LinkedHashMap<>(size);
                for (int i = 0; i < size; i++) {
                    String value = values[i];
                    descriptionMap.put(value, i18n.text(name, "#options[", value, "]"));
                }
                this.descriptionMap = Collections.unmodifiableMap(descriptionMap);
            }
        }

    }

}
