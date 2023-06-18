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
package program.driver.basic;

import com.alibaba.fastjson2.JSONObject;
import lombok.Getter;
import program.common.basic.resource.ResUtl;
import program.common.basic.resource.data.DataMap;

/**
 * Game server.
 *
 * @author wautsns
 * @since {{{SINCE_PLACEHOLDER}}}
 */
public final class GameServer {

    public static final DataMap<GameServer> MAP = DataMap.of(ResUtl.home(
            "/.dev/resources/driver/basic/#GameServer"
    ), GameServer::new);

    // *********************************************************************************
    // *********************************************************************************

    private final @Getter String region;
    private final @Getter String packageName;
    private final @Getter String activityName;

    // *********************************************************************************
    // InternalConstructors
    // *********************************************************************************

    private GameServer(JSONObject json) {
        this.region = json.getString("region").intern();
        this.packageName = json.getString("packageName");
        this.activityName = json.getString("activityName").intern();
    }

}
