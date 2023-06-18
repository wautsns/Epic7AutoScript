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
import program.common.basic.resource.ResUtl;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.function.IntUnaryOperator;

/**
 * Image.
 *
 * @author wautsns
 * @since {{{SINCE_PLACEHOLDER}}}
 */
@Accessors(fluent = true)
@RequiredArgsConstructor(access = AccessLevel.PRIVATE)
public final class Image {

    // *****************************************************************************************
    // StaticMethods, initializing instance
    // *****************************************************************************************

    // Note: This method CLOSE the given stream after the read operation has completed.
    public static Image read(String name, InputStream stream) {
        BufferedImage image;
        try (stream) {
            image = ImageIO.read(stream);
        } catch (IOException e) {
            throw new InvocationException(e);
        }
        if (image == null) {
            String message = "No registered ImageReader claims to be able to read stream";
            throw new InvocationException(message);
        }
        return new Image(name, image);
    }

    public static Image load(String name, String path) {
        try {
            return read(name, ResUtl.loadAsStream(path));
        } catch (InvocationException e) {
            throw new InvocationException(e)
                    .with("image_path", path);
        }
    }

    // *****************************************************************************************
    // *****************************************************************************************

    private final @Getter String name;
    private final @Getter BufferedImage delegate;

    // *****************************************************************************************
    // Methods, getting metadata
    // *****************************************************************************************

    public int width() {
        return delegate.getWidth();
    }

    public int height() {
        return delegate.getHeight();
    }

    public int rgb(int x, int y) {
        return delegate.getRGB(x, y) & 0xFFFFFF;
    }

    // *****************************************************************************************
    // Methods, comparing image
    // *****************************************************************************************

    public boolean match(Area area, Image that) {
        int w = that.width(), h = that.height();
        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {
                if (rgb(area.x() + x, area.y() + y) != that.rgb(x, y)) {
                    return false;
                }
            }
        }
        return true;
    }

    public Area find(Image that) {
        return find(new Area(0, 0, width(), height()), that);
    }

    public Area find(Area area, Image that) {
        int x1l = area.x() + (area.width() - that.width());
        int y1l = area.y() + (area.height() - that.height());
        int x2l = that.width(), y2l = that.height();
        int first = that.rgb(0, 0);
        for (int y1 = area.y(); y1 < y1l; y1++) {
            nextComp:
            for (int x1 = area.x(); x1 < x1l; x1++) {
                if (rgb(x1, y1) != first) {continue;}
                for (int y2 = 0; y2 < y2l; y2++) {
                    for (int x2 = 0; x2 < x2l; x2++) {
                        if (rgb(x1 + x2, y1 + y2) != that.rgb(x2, y2)) {
                            continue nextComp;
                        }
                    }
                }
                return new Area(x1, y1, x2l, y2l);
            }
        }
        return null;
    }

    // *****************************************************************************************
    // Methods, manipulating image
    // *****************************************************************************************

    public Image mutate(ImageOps ops) {
        int x, y, w, h;
        String name;
        if (ops.crop() == null) {
            name = this.name;
            x = y = 0;
            w = width();
            h = height();
        } else {
            Area area = ops.crop();
            name = area.name();
            x = area.x();
            y = area.y();
            w = area.width();
            h = area.height();
        }
        BufferedImage result;
        IntUnaryOperator converter = ops.converter;
        if (ops.threshold() == 0) {
            result = new BufferedImage(w, h, BufferedImage.TYPE_3BYTE_BGR);
        } else {
            result = new BufferedImage(w, h, BufferedImage.TYPE_BYTE_BINARY);
        }
        for (int j = 0; j < h; j++) {
            for (int i = 0; i < w; i++) {
                int rgb = rgb(x + i, y + j) & 0xFFFFFF;
                result.setRGB(i, j, converter.applyAsInt(rgb));
            }
        }
        return new Image(name, result);
    }

    // *****************************************************************************************
    // Methods, saving image
    // *****************************************************************************************

    public void save(File directory, String filename) {
        String format;
        if (filename.endsWith(".png")) {
            format = "png";
        } else {
            int lastIndexOfDot = filename.lastIndexOf('.');
            format = ((lastIndexOfDot == -1)) ? "png" : filename.substring(lastIndexOfDot + 1);
        }
        File file = new File(directory, filename);
        boolean okay;
        try {
            okay = ImageIO.write(delegate, format, file);
        } catch (IOException e) {
            throw new InvocationException(e);
        }
        if (!okay) {
            String message = "No appropriate writer found for PNG";
            throw new InvocationException(message);
        }
    }

}
