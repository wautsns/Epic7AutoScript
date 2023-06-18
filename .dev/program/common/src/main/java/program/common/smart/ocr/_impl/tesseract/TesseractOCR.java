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
package program.common.smart.ocr._impl.tesseract;

import com.alibaba.fastjson2.JSONObject;
import lombok.Getter;
import lombok.experimental.Accessors;
import program.common.basic.resource.ResUtl;
import program.common.basic.resource.conf.Config;
import program.common.basic.resource.data.DataMap;
import program.common.basic.vision.Image;
import program.common.smart.ocr._impl.OCR;
import program.common.smart.ocr._impl.tesseract.impl.Tesseract;
import program.common.smart.ocr.model.OCRCharBoxList;

import java.util.HashMap;
import java.util.Map;

/**
 * {@link OCR} implementation based on tesseract.
 *
 * @author wautsns
 * @since {{{SINCE_PLACEHOLDER}}}
 */
@Accessors(fluent = true)
public final class TesseractOCR implements OCR {

    private final Tesseract tesseract;

    // *****************************************************************************************
    // OverrideMethods, OCR
    // *****************************************************************************************

    @Override
    public synchronized void setImage(Image image) {
        tesseract.setImage(image);
    }

    public synchronized OCRCharBoxList textarea(int x, int y, int width, int height) {
        return tesseract.recognizeTextBlock(x, y, width, height);
    }

    // *****************************************************************************************
    // OverrideMethods, SilentCloseable
    // *****************************************************************************************

    @Override
    public synchronized void close() {
        tesseract.close();
    }

    @Override
    public synchronized boolean closed() {
        return tesseract.closed();
    }

    // *****************************************************************************************
    // Constructors
    // *****************************************************************************************

    public TesseractOCR(Config config) {
        TextLanguage textLanguage = config.getValueAsDataPayload("text.language", TextLanguage.MAP);

        Map<String, String> variables = new HashMap<>(4);
        variables.put("load_system_dawg", "F");
        variables.put("load_freq_dawg", "F");

        tesseract = new Tesseract(textLanguage.tessdata, variables);
    }

    // *****************************************************************************************
    // InternalStaticClasses
    // *****************************************************************************************

    @Accessors(fluent = true)
    private static final class TextLanguage {

        public static final DataMap<TextLanguage> MAP = DataMap.of(ResUtl.home(
                "/.dev/resources/common/smart/ocr/_impl/tesseract/#TesseractOCR.TextLanguage"
        ), TextLanguage::new);

        // *********************************************************************************
        // *********************************************************************************

        private final @Getter String tessdata;

        // *********************************************************************************
        // InternalConstructors
        // *********************************************************************************

        private TextLanguage(JSONObject json) {
            this.tessdata = json.getString("tessdata");
        }

    }

}
