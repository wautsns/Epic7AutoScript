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
package program.common.basic.resource.data;

import com.alibaba.fastjson2.JSONException;
import com.alibaba.fastjson2.JSONObject;
import com.alibaba.fastjson2.JSONReader;
import lombok.Getter;
import lombok.experimental.Accessors;
import program.common.basic.exception.InvocationException;
import program.common.basic.resource.ResUtl;
import program.common.basic.utility.WeakSet;

import java.io.File;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Stream;

/**
 * Data map.
 *
 * @author wautsns
 * @since {{{SINCE_PLACEHOLDER}}}
 */
@Accessors(fluent = true)
public final class DataMap<T> {

    // *****************************************************************************************
    // StaticMethods, initializing instance
    // *****************************************************************************************

    private static final WeakSet<DataMap<?>> REFERENCES = new WeakSet<>();

    public static <T> DataMap<T> of(String directory, Function<JSONObject, T> processor) {
        return of(new File(directory), processor);
    }

    public static <T> DataMap<T> of(File directory, Function<JSONObject, T> processor) {
        String absolute = directory.getAbsolutePath();
        // noinspection unchecked
        return (DataMap<T>) REFERENCES.add(
                ref -> ref.directory.getAbsolutePath().equals(absolute),
                () -> new DataMap<>(directory, processor)
        );
    }

    public static <T> DataMap<T> of(File directory, List<Data<T>> storage) {
        return new DataMap<>(directory, storage);
    }

    // *****************************************************************************************
    // *****************************************************************************************

    private final @Getter File directory;
    private final LinkedHashMap<String, List<Data<T>>> storage;

    // *****************************************************************************************
    // Methods, getting data
    // *****************************************************************************************

    // @return the latest version of the data for the given id
    public Data<T> get(String id) {
        return getAll(id).get(0);
    }

    // @return all versions of the data for the given id (sorted by version in reverse order)
    public List<Data<T>> getAll(String id) {
        List<Data<T>> history = storage.get(id);
        if (history == null) {
            String message = "Data not found for the specified id";
            throw new InvocationException(message)
                    .with("data_id", id);
        }
        return history;
    }

    // Note: Only the latest version of data is traversed.
    public Stream<Data<T>> stream() {
        return storage.values().stream().map(list -> list.get(0));
    }

    // *****************************************************************************************
    // InternalConstructors
    // *****************************************************************************************

    private DataMap(File directory, Function<JSONObject, T> processor) {
        this.directory = directory;
        this.storage = new LinkedHashMap<>();
        File main = new File(directory, "data.map.json");
        read(main, ResUtl.loadAsReader(main.getPath()), processor);
        File patch = new File(directory, "data.map.patch.json");
        if (patch.exists()) {
            read(patch, ResUtl.loadAsReader(patch.getPath()), processor);
        }

    }

    private DataMap(File directory, List<Data<T>> dataList) {
        this.directory = directory;
        this.storage = new LinkedHashMap<>(dataList.size());
        dataList.forEach(this::addData);
    }

    // *****************************************************************************************
    // InternalMethods
    // *****************************************************************************************

    private void addData(Data<T> data) {
        String id = data.id();
        List<Data<T>> history = storage.get(id);
        if (history == null) {
            history = List.of(data);
            storage.put(id, history);
        } else {
            List<Data<T>> temp = new ArrayList<>(history.size() + 1);
            temp.addAll(history);
            temp.removeIf(elem -> elem.ver() == data.ver());
            temp.add(data);
            Comparator<Data<T>> comparator = Comparator.comparingLong(Data::ver);
            temp.sort(comparator.reversed());
            storage.put(id, Collections.unmodifiableList(temp));
        }
    }

    private DataMap<T> read(
            File file, InputStreamReader streamReader, Function<JSONObject, T> processor) {
        try (JSONReader reader = JSONReader.of(streamReader)) {
            if (reader.isEnd()) {return this;}
            reader.startArray();
            do {
                JSONObject node = reader.read(JSONObject.class);
                try {
                    addData(new Data<>(node, processor));
                } catch (RuntimeException e) {
                    throw new InvocationException(e)
                            .with("data_node", node)
                            .with("data_map_path", file.getAbsolutePath());
                }
            } while (reader.isObject());
            reader.endArray();
            return this;
        } catch (JSONException e) {
            throw new InvocationException(e)
                    .with("data_map_path", file.getAbsolutePath());
        }
    }

}
