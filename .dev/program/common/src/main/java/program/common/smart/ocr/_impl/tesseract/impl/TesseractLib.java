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

import com.sun.jna.Library;
import com.sun.jna.Native;
import com.sun.jna.Platform;
import com.sun.jna.Pointer;
import com.sun.jna.PointerType;
import com.sun.jna.Structure;
import program.common.basic.resource.ResUtl;

import java.io.File;
import java.nio.ByteBuffer;
import java.nio.IntBuffer;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.function.Supplier;

/**
 * Tesseract lib.
 *
 * @author wautsns
 * @since {{{SINCE_PLACEHOLDER}}}
 */
@SuppressWarnings({"SpellCheckingInspection", "GrazieInspection"})
interface TesseractLib extends Library {

    // https://github.com/tesseract-ocr/tesseract/blob/main/include/tesseract/capi.h

    TesseractLib INSTANCE = ((Supplier<TesseractLib>) () -> {
        System.setProperty("jna.encoding", StandardCharsets.UTF_8.name());
        String libPathKey = "jna.library.path";
        String libPath = ResUtl.home("/.bin/tesseract/jna/%s", Platform.RESOURCE_PREFIX);
        String currLibPath = System.getProperty(libPathKey);
        if ((currLibPath == null) || currLibPath.isBlank()) {
            currLibPath = libPath;
        } else if (!Arrays.asList(currLibPath.split(File.pathSeparator)).contains(libPath)) {
            currLibPath = currLibPath + File.pathSeparator + libPath;
        }
        System.setProperty(libPathKey, currLibPath);
        String libName = Platform.isWindows() ? "libtesseract530" : "tesseract";
        return Native.load(libName, TesseractLib.class);
    }).get();

    // *****************************************************************************************
    // Methods, capi.cpp
    // *****************************************************************************************

    TessBaseAPI TessBaseAPICreate();

    void TessBaseAPIDelete(TessBaseAPI handle);

    TessPageIterator TessResultIteratorGetPageIterator(TessResultIterator handle);

    void TessResultIteratorDelete(TessResultIterator handle);

    void TessDeleteText(Pointer text);

    // *****************************************************************************************
    // Methods, baseapi.cpp
    // *****************************************************************************************

    // Set the value of an internal "parameter."
    // Supply the name of the parameter and the value as a string, just as
    // you would in a config file.
    // Returns false if the name lookup failed.
    // Eg SetVariable("tessedit_char_blacklist", "xyz"); to ignore x, y and z.
    // Or SetVariable("classify_bln_numeric_mode", "1"); to set numeric-only mode.
    // SetVariable may be used before Init, but settings will revert to
    // defaults on End().
    //
    // Note: Must be called after Init(). Only works for non-init variables
    // (init variables should be passed to Init()).
    boolean TessBaseAPISetVariable(TessBaseAPI handle, String name, String value);

    // Instances are now mostly thread-safe and totally independent,
    // but some global parameters remain. Basically it is safe to use multiple
    // TessBaseAPIs in different threads in parallel, UNLESS:
    // you use SetVariable on some of the Params in classify and textord.
    // If you do, then the effect will be to change it for all your instances.
    //
    // Start tesseract. Returns zero on success and -1 on failure.
    // NOTE that the only members that may be called before Init are those
    // listed above here in the class definition.
    //
    // The datapath must be the name of the tessdata directory.
    // The language is (usually) an ISO 639-3 string or nullptr will default to
    // eng. It is entirely safe (and eventually will be efficient too) to call
    // Init multiple times on the same instance to change language, or just
    // to reset the classifier.
    // The language may be a string of the form [~]<lang>[+[~]<lang>]* indicating
    // that multiple languages are to be loaded. Eg hin+eng will load Hindi and
    // English. Languages may specify internally that they want to be loaded
    // with one or more other languages, so the ~ sign is available to override
    // that. Eg if hin were set to load eng by default, then hin+~eng would force
    // loading only hin. The number of loaded languages is limited only by
    // memory, with the caveat that loading additional languages will impact
    // both speed and accuracy, as there is more work to do to decide on the
    // applicable language, and there is more chance of hallucinating incorrect
    // words.
    // WARNING: On changing languages, all Tesseract parameters are reset
    // back to their default values. (Which may vary between languages.)
    // If you have a rare need to set a Variable that controls
    // initialization for a second call to Init you should explicitly
    // call End() and then use SetVariable before Init. This is only a very
    // rare use case, since there are very few uses that require any parameters
    // to be set before Init.
    //
    // If set_only_non_debug_params is true, only params that do not contain
    // "debug" in the name will be set.
    int TessBaseAPIInit3(TessBaseAPI handle, String datapath, String language);

    // Recognize a rectangle from an image and return the result as a string.
    // May be called many times for a single Init.
    // Currently has no error checking.
    // Greyscale of 8 and color of 24 or 32 bits per pixel may be given.
    // Palette color images will not work properly and must be converted to
    // 24 bit.
    // Binary images of 1 bit per pixel may also be given but they must be
    // byte packed with the MSB of the first byte being the first pixel, and a
    // 1 represents WHITE. For binary images set bytes_per_pixel=0.
    // The recognized text is returned as a char* which is coded
    // as UTF8 and must be freed with the delete [] operator.
    //
    // Note that TesseractRect is the simplified convenience interface.
    // For advanced uses, use SetImage, (optionally) SetRectangle, Recognize,
    // and one or more of the Get*Text functions below.
    @SuppressWarnings("unused")
    String TessBaseAPITesseractRect(
            TessBaseAPI handle, ByteBuffer imagedata, int bytes_per_pixel, int bytes_per_line,
            int left, int top, int width, int height);

    // Provide an image for Tesseract to recognize. Format is as
    // TesseractRect above. Copies the image buffer and converts to Pix.
    // SetImage clears all recognition results, and sets the rectangle to the
    // full image, so it may be followed immediately by a GetUTF8Text, and it
    // will automatically perform recognition.
    void TessBaseAPISetImage(
            TessBaseAPI handle, ByteBuffer imagedata, int width, int height, int bytes_per_pixel,
            int bytes_per_line);

    // Restrict recognition to a sub-rectangle of the image. Call after SetImage.
    // Each SetRectangle clears the recogntion results so multiple rectangles
    // can be recognized with the same image.
    void TessBaseAPISetRectangle(TessBaseAPI handle, int left, int top, int width, int height);

    // Recognize the image from SetAndThresholdImage, generating Tesseract
    // internal structures. Returns 0 on success.
    // Optional. The Get*Text functions below will call Recognize if needed.
    // After Recognize, the output is kept internally until the next SetImage.
    int TessBaseAPIRecognize(TessBaseAPI handle, Structure monitor);

    // Get a reading-order iterator to the results of LayoutAnalysis and/or
    // Recognize. The returned iterator must be deleted after use.
    // WARNING! This class points to data held within the TessBaseAPI class, and
    // therefore can only be used while the TessBaseAPI class still exists and
    // has not been subjected to a call of Init, SetImage, Recognize, Clear, End
    // DetectOS, or anything else that changes the internal PAGE_RES.
    TessResultIterator TessBaseAPIGetIterator(TessBaseAPI handle);

    // Moves the iterator to point to the start of the page to begin an
    // iteration.
    void TessPageIteratorBegin(TessPageIterator handle);

    // *****************************************************************************************
    // Methods, ltrresultiterator.cpp
    // *****************************************************************************************

    // Returns the null terminated UTF-8 encoded text string for the current
    // choice.
    // NOTE: Unlike LTRResultIterator::GetUTF8Text, the return points to an
    // internal structure and should NOT be delete[]ed to free after use.
    Pointer TessResultIteratorGetUTF8Text(TessResultIterator handle, int level);

    // Returns the mean confidence of the current object at the given level.
    // The number should be interpreted as a percent probability. (0.0f-100.0f)
    float TessResultIteratorConfidence(TessResultIterator handle, int level);

    // *****************************************************************************************
    // Methods, pageiterator.cpp
    // *****************************************************************************************

    // Coordinate system:
    // Integer coordinates are at the cracks between the pixels.
    // The top-left corner of the top-left pixel in the image is at (0,0).
    // The bottom-right corner of the bottom-right pixel in the image is at
    // (width, height).
    // Every bounding box goes from the top-left of the top-left contained
    // pixel to the bottom-right of the bottom-right contained pixel, so
    // the bounding box of the single top-left pixel in the image is:
    // (0,0)->(1,1).
    // If an image rectangle has been set in the API, then returned coordinates
    // relate to the original (full) image, rather than the rectangle.

    // Returns the bounding rectangle of the current object at the given level.
    // See comment on coordinate system above.
    // Returns false if there is no such object at the current position.
    // The returned bounding box is guaranteed to match the size and position
    // of the image returned by GetBinaryImage, but may clip foreground pixels
    // from a grey image. The padding argument to GetImage can be used to expand
    // the image to include more foreground pixels. See GetImage below.
    boolean TessPageIteratorBoundingBox(
            TessPageIterator handle, int level, IntBuffer left, IntBuffer top, IntBuffer right,
            IntBuffer bottom);

    // Moves to the start of the next object at the given level in the
    // page hierarchy, and returns false if the end of the page was reached.
    // NOTE that RIL_SYMBOL will skip non-text blocks, but all other
    // PageIteratorLevel level values will visit each non-text block once.
    // Think of non text blocks as containing a single para, with a single line,
    // with a single imaginary word.
    // Calls to Next with different levels may be freely intermixed.
    // This function iterates words in right-to-left scripts correctly, if
    // the appropriate language has been loaded into Tesseract.
    boolean TessPageIteratorNext(TessPageIterator handle, int level);

    // *****************************************************************************************
    // Enums
    // *****************************************************************************************

    @SuppressWarnings("unused")
    enum TessPageIteratorLevel {BLOCK, PARA, TEXTLINE, WORD, SYMBOL}

    // *****************************************************************************************
    // StaticClasses
    // *****************************************************************************************

    @SuppressWarnings("unused")
    class TessBaseAPI extends PointerType {

        public TessBaseAPI(Pointer address) {
            super(address);
        }

        public TessBaseAPI() {
            super();
        }

    }

    @SuppressWarnings("unused")
    class TessResultIterator extends PointerType {

        public TessResultIterator(Pointer address) {
            super(address);
        }

        public TessResultIterator() {
            super();
        }

    }

    @SuppressWarnings("unused")
    class TessPageIterator extends PointerType {

        public TessPageIterator(Pointer address) {
            super(address);
        }

        public TessPageIterator() {
            super();
        }

    }

}
