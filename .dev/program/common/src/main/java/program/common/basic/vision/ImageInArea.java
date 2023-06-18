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
package program.common.basic.vision;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.experimental.Accessors;
import program.common.basic.exception.InvocationException;
import program.common.basic.resource.data.Data;
import program.common.basic.resource.data.DataMap;

import java.io.File;
import java.util.LinkedList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Image in area.
 *
 * @author wautsns
 * @since {{{SINCE_PLACEHOLDER}}}
 */
@Accessors(fluent = true)
@RequiredArgsConstructor(access = AccessLevel.PRIVATE)
public final class ImageInArea {

    // *****************************************************************************************
    // StaticMethods, initializing instance
    // *****************************************************************************************

    public static ImageInArea load(String path) {
        return new ImageInArea(new File(path));
    }

    public static DataMap<ImageInArea> loadAll(String directory) {
        File dir = new File(directory);
        File[] files = dir.listFiles();
        if (files == null) {return DataMap.of(dir, List.of());}
        List<Data<ImageInArea>> dataList = new LinkedList<>();
        for (File file : files) {
            ImageInArea payload = new ImageInArea(file);
            dataList.add(new Data<>(payload.image.name(), file.lastModified(), payload));
        }
        return DataMap.of(dir, dataList);
    }

    // *****************************************************************************************
    // *****************************************************************************************

    private final @Getter Area area;
    private final @Getter Image image;

    // *****************************************************************************************
    // InternalConstructors
    // *****************************************************************************************

    private ImageInArea(File file) {
        String filename = file.getName();
        String regexp = "(?<name>.*)\\.\\[(?<x>\\d+),(?<y>\\d+),(?<w>\\d+),(?<h>\\d+)].*";
        Matcher matcher = Pattern.compile(regexp).matcher(filename);
        if (!matcher.find()) {
            String message = "Missing metadata in path";
            throw new InvocationException(message)
                    .with("path", file);
        }
        String name = matcher.group("name");
        int x = Integer.parseInt(matcher.group("x"));
        int y = Integer.parseInt(matcher.group("y"));
        int w = Integer.parseInt(matcher.group("w"));
        int h = Integer.parseInt(matcher.group("h"));
        this.area = new Area(name, x, y, w, h);
        this.image = Image.load(name, file.getPath());
    }

}
