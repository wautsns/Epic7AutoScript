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
import program.common.basic.resource.ResUtl;
import program.common.basic.resource.data.Data;
import program.common.basic.resource.data.DataMap;
import program.common.basic.utility.ObV;
import program.common.basic.utility.WeakSet;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.util.LinkedHashMap;
import java.util.Properties;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.stream.Stream;

/**
 * Config.
 *
 * @author wautsns
 * @since {{{SINCE_PLACEHOLDER}}}
 */
@Accessors(fluent = true)
public final class Config {

    // *****************************************************************************************
    // StaticMethods, initializing instance
    // *****************************************************************************************

    private static final WeakSet<Config> REFERENCES = new WeakSet<>();

    public static Config of(ConfigTemplate template, String customer) {
        return of(template, new File(customer));
    }

    public static Config of(ConfigTemplate template, File customer) {
        String absolute = customer.getAbsolutePath();
        return REFERENCES.add(
                ref -> ref.file.getAbsolutePath().equals(absolute),
                () -> new Config(template, absolute)
        );
    }

    // *****************************************************************************************
    // *****************************************************************************************

    private final @Getter ConfigTemplate template;

    private final ReentrantReadWriteLock lock = new ReentrantReadWriteLock();

    private final @Getter File file;
    private final LinkedHashMap<String, Property> properties;

    private final WeakSet<Runnable> callbacksAfterSaving = new WeakSet<>();

    // *****************************************************************************************
    // Methods, getting property
    // *****************************************************************************************

    public Property get(String name) {
        Property property = properties.get(name);
        if (property == null) {
            String message = "Property not found for the specified name in config";
            throw new InvocationException(message)
                    .with("property_name", name)
                    .with("config_path", file.getAbsolutePath());
        }
        return property;
    }

    public Stream<Property> propertyStream() {
        return properties.values().stream();
    }

    public String getValue(String name) {
        return get(name).value().get();
    }

    public int getValueAsInt(String name) {
        String value = getValue(name);
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException e) {
            String message = "Cannot convert property value to int value";
            throw new InvocationException(message, e)
                    .with("config_path", file.getAbsolutePath())
                    .with("property_name", name)
                    .with("property_value", value);
        }
    }

    public <E extends Enum<E>> E getValueAsEnum(String name, Class<E> type) {
        String value = getValue(name);
        try {
            return Enum.valueOf(type, value);
        } catch (NullPointerException | IllegalArgumentException e) {
            String message = "Cannot convert property value to Enum value";
            throw new InvocationException(message, e)
                    .with("config_path", file.getAbsolutePath())
                    .with("property_name", name)
                    .with("property_value", value)
                    .with("enum_type", type);
        }
    }

    public <T> T getValueAsDataPayload(String name, DataMap<T> map) {
        String value = getValue(name);
        try {
            return map.get(value).payload();
        } catch (InvocationException e) {
            String message = "Cannot convert property value to Data value";
            throw new InvocationException(message, e)
                    .with("config_path", file.getAbsolutePath())
                    .with("property_name", name)
                    .with("property_value", value)
                    .with("data_map_directory", map.directory().getAbsolutePath());
        }
    }

    // *****************************************************************************************
    // Methods, setting value
    // *****************************************************************************************

    public String setValue(String name, String value) {
        return get(name).value().set(value);
    }

    // *****************************************************************************************
    // Methods, saving config
    // *****************************************************************************************

    public void save() {
        lock.readLock().lock();
        try {
            try {
                // noinspection ResultOfMethodCallIgnored
                file.getParentFile().mkdirs();
                // noinspection ResultOfMethodCallIgnored
                file.createNewFile();
                try (PrintWriter writer = new PrintWriter(new FileOutputStream(file))) {
                    writer.printf(
                            "## @metadata: {\"program\":\"%s\",\"version\":%d}%n%n",
                            template.program().getName(), template.version()
                    );
                    for (Object element : template.content) {
                        if (element instanceof String raw) {
                            writer.print(raw);
                        } else if (element instanceof PropertyMetadata propertyMetadata) {
                            writer.printf("## %s%n", propertyMetadata.description());
                            writer.println("##");
                            PropertyMetadata.Options options = propertyMetadata.options();
                            if (!options.values().isEmpty()) {
                                writer.println("## @options:");
                                for (String value : options.values()) {
                                    writer.print("##   ");
                                    writer.print(value);
                                    String description = options.description(value).get();
                                    if (!description.isBlank()) {
                                        writer.print(" : ");
                                        writer.print(description);
                                    }
                                    writer.println();
                                }
                                if (options.customized()) {
                                    writer.println("#   ...");
                                }
                            }
                            writer.print("## @defaults:");
                            String defaults = propertyMetadata.defaults();
                            if (!defaults.isEmpty()) {
                                writer.print(' ');
                                writer.print(defaults);
                            }
                            writer.println();
                            String value = properties.get(propertyMetadata.name()).value().get();
                            writer.print(propertyMetadata.name());
                            writer.print(" =");
                            if (!value.isEmpty()) {
                                writer.print(' ');
                                writer.print(value);
                            }
                            writer.println();
                        }
                    }
                }
            } catch (IOException e) {
                throw new InvocationException(e)
                        .with("config_path", file.getAbsolutePath());
            }
            callbacksAfterSaving.forEach(Runnable::run);
        } finally {
            lock.readLock().unlock();
        }
    }

    // *****************************************************************************************
    // Methods, adding callback
    // *****************************************************************************************

    // WARNING: The given callback needs to be strongly referenced by the caller of this method
    // because this instance only holds its weak reference.
    //
    // @return the give callback
    public Object callbackAfterSaving(Runnable callback) {
        return callbacksAfterSaving.add(callback);
    }

    // *****************************************************************************************
    // InternalConstructors
    // *****************************************************************************************

    private Config(ConfigTemplate template, String customer) {
        this.template = template;
        this.file = new File(customer).getAbsoluteFile();
        this.properties = new LinkedHashMap<>();
        InputStreamReader inputStreamReader = ResUtl.tryLoadAsReader(customer);
        if (inputStreamReader == null) {
            initDefaultProperties();
        } else {
            try (BufferedReader reader = new BufferedReader(inputStreamReader)) {
                JSONObject metadataNode = readMetadata(reader);
                validateProgram(metadataNode);
                long version = ConfigTemplate.readVersion(metadataNode);
                readProperties(reader, version);
            } catch (InvocationException | IOException e) {
                throw new InvocationException(e)
                        .with("config_template_path", template)
                        .with("customer_config_path", customer);
            }
        }
    }

    // *****************************************************************************************
    // InternalMethods
    // *****************************************************************************************

    private void initDefaultProperties() {
        template.propertyMetadataMap.forEach((name, propertyMetadata) -> {
            ObV<String> observableValue = new ObV<>(lock, propertyMetadata.defaults());
            this.properties.put(name, new Property(propertyMetadata, observableValue));
        });
    }

    private JSONObject readMetadata(BufferedReader reader) throws IOException {
        // the first line in customer config: ## @metadata: {"program":"...","version":...}
        try {
            String line = reader.readLine();
            return JSONObject.parseObject(line.substring(line.indexOf('{')));
        } catch (RuntimeException e) {
            String message = "Missing metadata line in customer config";
            throw new InvocationException(message);
        }
    }

    private void validateProgram(JSONObject node) {
        Class<?> program = ConfigTemplate.readProgram(node);
        if (program != template.program()) {
            String message = "Illegal metadata: `program` in customer config";
            throw new InvocationException(message)
                    .with("expected_program", template.program())
                    .with("provided_program", program);
        }
    }

    private void readProperties(BufferedReader reader, long version) throws IOException {
        Properties properties = new Properties();
        properties.load(reader);
        template.propertyMetadataMap.forEach((name, propertyMetadata) -> {
            String value;
            if (version < propertyMetadata.since()) {
                value = propertyMetadata.defaults();
            } else {
                value = properties.getProperty(name, propertyMetadata.defaults());
            }
            ObV<String> observableValue = new ObV<>(lock, value);
            this.properties.put(name, new Property(propertyMetadata, observableValue));
        });
    }

}
