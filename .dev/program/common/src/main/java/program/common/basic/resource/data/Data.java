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

import com.alibaba.fastjson2.JSONObject;
import lombok.Getter;
import lombok.experimental.Accessors;

import java.util.function.Function;

/**
 * Data.
 *
 * @author wautsns
 * @since {{{SINCE_PLACEHOLDER}}}
 */
@Accessors(fluent = true)
public final class Data<T> {

    private final @Getter String id;
    private final @Getter long ver;
    private final @Getter T payload;

    // *********************************************************************************
    // Constructors
    // *********************************************************************************

    public Data(JSONObject json, Function<JSONObject, T> processor) {
        this.id = json.getString("id");
        this.ver = json.getLongValue("ver");
        this.payload = processor.apply(json);
    }

    public Data(String id, long ver, T payload) {
        this.id = id;
        this.ver = ver;
        this.payload = payload;
    }

}
