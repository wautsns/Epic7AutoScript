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
package program.common.smart.ocr;

import lombok.RequiredArgsConstructor;
import lombok.experimental.Accessors;
import program.common.basic.logger.Logger;
import program.common.basic.resource.ResUtl;
import program.common.basic.resource.conf.Config;
import program.common.basic.resource.conf.ConfigTemplate;
import program.common.basic.resource.conf.Configurable;
import program.common.basic.utility.WeakSet;
import program.common.basic.vision.Image;
import program.common.smart.ocr._impl.OCR;
import program.common.smart.ocr._impl.tesseract.TesseractOCR;
import program.common.smart.ocr.model.OCRCharBoxList;

import java.io.File;
import java.util.function.Function;

/**
 * Smart ocr.
 *
 * @author wautsns
 * @since {{{SINCE_PLACEHOLDER}}}
 */
@Accessors(fluent = true)
public final class SmartOCR extends Configurable {

    // *****************************************************************************************
    // StaticMethods, initializing instance
    // *****************************************************************************************

    private static final ConfigTemplate TEMPLATE = ConfigTemplate.of(ResUtl.home(
            "/.dev/resources/common/smart/ocr/#SmartOCR"
    ));
    private static final WeakSet<SmartOCR> REFERENCES = new WeakSet<>();

    public static synchronized SmartOCR of(String customer) {
        return of(new File(customer));
    }

    public static synchronized SmartOCR of(File customer) {
        Config config = Config.of(TEMPLATE, customer);
        return REFERENCES.add(ref -> ref.config == config, () -> new SmartOCR(config));
    }

    // *****************************************************************************************
    // *****************************************************************************************

    private OCR ocr;

    // *****************************************************************************************
    // Methods, recognizing image
    // *****************************************************************************************

    public synchronized OCRCharBoxList textarea(Image image) {
        acquireNotClosed();
        long start = System.nanoTime();
        ocr.setImage(image);
        OCRCharBoxList result = ocr.textarea(0, 0, image.width(), image.height());
        long cost = (System.nanoTime() - start) / 1_000_000;
        Logger.info("ocr(%dms) @ %s => %s", cost, image.name(), result.inline(0));
        return result;
    }

    // *****************************************************************************************
    // OverrideMethods, Configurable
    // *****************************************************************************************

    @Override
    protected void release() {
        if (ocr != null) {
            ocr.close();
            ocr = null;
        }
    }

    protected void reinitialize() {
        OCRImpl ocrImpl = config.getValueAsEnum("ocr.impl", OCRImpl.class);

        ocr = ocrImpl.constructor.apply(config);
    }

    // *****************************************************************************************
    // InternalConstructors
    // *****************************************************************************************

    private SmartOCR(Config config) {
        super(config);
    }

    // *****************************************************************************************
    // InternalEnums
    // *****************************************************************************************

    @SuppressWarnings("unused")
    @RequiredArgsConstructor
    private enum OCRImpl {

        tesseract(TesseractOCR::new),
        ;

        final Function<Config, OCR> constructor;

    }

}
