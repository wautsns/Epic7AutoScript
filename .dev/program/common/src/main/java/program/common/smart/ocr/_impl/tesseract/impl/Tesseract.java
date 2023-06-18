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
package program.common.smart.ocr._impl.tesseract.impl;

import com.sun.jna.Pointer;
import program.common.basic.exception.InvocationException;
import program.common.basic.logger.Logger;
import program.common.basic.resource.ResUtl;
import program.common.basic.resource.SilentCloseable;
import program.common.basic.vision.Image;
import program.common.smart.ocr.model.OCRCharBox;
import program.common.smart.ocr.model.OCRCharBoxList;

import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.awt.image.DataBufferByte;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.IntBuffer;
import java.util.Map;

/**
 * Tesseract.
 *
 * @author wautsns
 * @since {{{SINCE_PLACEHOLDER}}}
 */
public final class Tesseract implements SilentCloseable {

    private final String language;
    private final TesseractLib lib = TesseractLib.INSTANCE;
    private final TesseractLib.TessBaseAPI handle;

    private boolean closed = false;

    // *****************************************************************************************
    // Methods, setting image
    // *****************************************************************************************

    public synchronized void setImage(Image image) {
        int w = image.width(), h = image.height();
        BufferedImage bi = image.delegate();
        int bpp = bi.getColorModel().getPixelSize();
        if (!(bi.getData().getDataBuffer() instanceof DataBufferByte)) {
            BufferedImage temp = new BufferedImage(w, h, BufferedImage.TYPE_4BYTE_ABGR);
            Graphics2D g2d = temp.createGraphics();
            try {
                g2d.drawImage(bi, 0, 0, null);
            } finally {
                g2d.dispose();
            }
            bi = temp;
        } else if (bi.getRaster().getParent() != null) {
            BufferedImage temp = new BufferedImage(w, h, bi.getType());
            Graphics2D g2d = temp.createGraphics();
            try {
                g2d.drawImage(bi, 0, 0, null);
            } finally {
                g2d.dispose();
            }
            bi = temp;
        }
        DataBufferByte buf = (DataBufferByte) bi.getData().getDataBuffer();
        byte[] data = buf.getData();
        ByteBuffer imagedata = ByteBuffer.allocateDirect(data.length);
        imagedata.order(ByteOrder.nativeOrder());
        imagedata.put(data);
        imagedata.flip();
        int bytespp = bpp / 8, bytespl = (int) Math.ceil(w * bpp / 8.0);
        lib.TessBaseAPISetImage(handle, imagedata, w, h, bytespp, bytespl);
    }

    // *****************************************************************************************
    // Methods, recognizing image
    // *****************************************************************************************

    public synchronized OCRCharBoxList recognizeTextBlock(int x, int y, int width, int height) {
        OCRCharBoxList result = new OCRCharBoxList();
        int level = TesseractLib.TessPageIteratorLevel.SYMBOL.ordinal();
        lib.TessBaseAPISetRectangle(handle, x, y, width, height);
        if (lib.TessBaseAPIRecognize(handle, null) != 0) {
            String message = "Failed to recognize image by tesseract";
            throw new InvocationException(message);
        }
        TesseractLib.TessResultIterator resultItr = lib.TessBaseAPIGetIterator(handle);
        try {
            TesseractLib.TessPageIterator pageItr =
                    lib.TessResultIteratorGetPageIterator(resultItr);
            lib.TessPageIteratorBegin(pageItr);
            IntBuffer xb = IntBuffer.allocate(1), yb = IntBuffer.allocate(1);
            IntBuffer rb = IntBuffer.allocate(1), bb = IntBuffer.allocate(1);
            do {
                Pointer textPtr = lib.TessResultIteratorGetUTF8Text(resultItr, level);
                if (textPtr == null) {continue;}
                String text = textPtr.getString(0);
                lib.TessDeleteText(textPtr);
                float accuracy = lib.TessResultIteratorConfidence(resultItr, level) / 100.0f;
                lib.TessPageIteratorBoundingBox(pageItr, level, xb, yb, rb, bb);
                int cx = xb.get(), cy = yb.get(), cw = rb.get() - x, ch = bb.get() - y;
                xb.clear();
                yb.clear();
                rb.clear();
                bb.clear();
                result.add(new OCRCharBox(cx, cy, cw, ch, text.charAt(0), accuracy));
            } while (lib.TessPageIteratorNext(pageItr, level));
        } finally {
            lib.TessResultIteratorDelete(resultItr);
        }
        return result;
    }

    // *****************************************************************************************
    // OverrideMethods, SilentCloseable
    // *****************************************************************************************

    @Override
    public synchronized void close() {
        if (closed) {return;}
        Logger.title(3, "[tesseract] close");
        Logger.attribute("language", language);
        lib.TessBaseAPIDelete(handle);
        closed = true;
        Logger.info("tesseract close okay");
        Logger.emptyLine();
    }

    @Override
    public synchronized boolean closed() {
        return closed;
    }

    // *****************************************************************************************
    // Constructors
    // *****************************************************************************************

    public Tesseract(String language, Map<String, String> variables) {
        Logger.title(3, "[tesseract] init");
        Logger.attribute("language", language);
        this.language = language;
        String datapath = ResUtl.home("/.bin/tesseract/tessdata");
        Logger.info("initializing TessBaseAPI...");
        this.handle = lib.TessBaseAPICreate();
        if (lib.TessBaseAPIInit3(handle, datapath, language) != 0) {
            String message = "Failed to initialize TessBaseAPI";
            throw new InvocationException(message)
                    .with("datapath", datapath)
                    .with("language", language);
        }
        for (Map.Entry<String, String> variable : variables.entrySet()) {
            String name = variable.getKey();
            String value = variable.getValue();
            if (!lib.TessBaseAPISetVariable(handle, name, value)) {
                String message = "Failed to lookup variable name";
                throw new InvocationException(message)
                        .with("datapath", datapath)
                        .with("language", language)
                        .with("variable_name", name);
            }
        }
        Logger.info("tesseract init okay");
        Logger.emptyLine();
    }

}
