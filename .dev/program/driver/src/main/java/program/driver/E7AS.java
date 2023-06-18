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
package program.driver;

import lombok.Getter;
import lombok.experimental.Accessors;
import program.common.basic.logger.Logger;
import program.common.basic.resource.ResUtl;
import program.common.basic.resource.conf.Config;
import program.common.basic.resource.conf.ConfigTemplate;
import program.common.basic.resource.conf.Configurable;
import program.common.basic.resource.i18n.Language;
import program.common.basic.utility.ObV;
import program.common.basic.utility.WeakSet;
import program.common.smart.device.SmartDevice;
import program.common.smart.ocr.SmartOCR;
import program.driver.basic.GameServer;

import java.io.File;

/**
 * Auto script.
 *
 * @author wautsns
 * @since {{{SINCE_PLACEHOLDER}}}
 */
@Accessors(fluent = true)
public final class E7AS extends Configurable {

    // *****************************************************************************************
    // StaticMethods, initializing instance
    // *****************************************************************************************

    private static final ConfigTemplate TEMPLATE = ConfigTemplate.of(ResUtl.home(
            "/.dev/resources/driver/#E7AS"
    ));
    private static final WeakSet<E7AS> REFERENCES = new WeakSet<>();

    public static synchronized E7AS of(String name) {
        String dir = new File(ResUtl.home("/configs"), name).getPath();
        Config config = Config.of(TEMPLATE, new File(dir, "/E7AS.config"));
        return REFERENCES.add(ref -> ref.config == config, () -> new E7AS(name, config));
    }

    // *****************************************************************************************
    // *****************************************************************************************

    private final @Getter String name;
    private final @Getter ObV<GameServer> server;
    private final @Getter ObV<Language> language;

    private final @Getter SmartDevice device;
    private final @Getter SmartOCR ocr;

    // *****************************************************************************************
    // OverrideMethods, Configurable
    // *****************************************************************************************

    // *****************************************************************************************
    // OverrideMethods, Configurable
    // *****************************************************************************************

    @Override
    protected void release() {
        server.set(null);
        language.set(null);
        device.close();
        ocr.close();
    }

    @Override
    protected void reinitialize() {
        Logger.title(0, "[E7AS] init");
        Logger.attribute("name", name);
        server.set(config.getValueAsDataPayload("server", GameServer.MAP));
        language.set(ocr.config().getValueAsEnum("text.language", Language.class));
        device.config().save();
        ocr.config().save();
        Logger.info("E7AS init okay");
        Logger.emptyLine();
    }

    // *****************************************************************************************
    // InternalConstructors
    // *****************************************************************************************

    private E7AS(String name, Config config) {
        super(config);
        this.name = name;
        this.server = new ObV<>(null);
        this.language = new ObV<>(null);
        String dir = config.file().getParent();
        this.device = SmartDevice.of(new File(dir, "/tools/device.properties"));
        this.ocr = SmartOCR.of(new File(dir, "/tools/ocr.properties"));
    }

}
