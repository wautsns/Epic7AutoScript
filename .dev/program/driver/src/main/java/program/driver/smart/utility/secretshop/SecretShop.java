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
package program.driver.smart.utility.secretshop;

import lombok.Getter;
import lombok.experimental.Accessors;
import program.common.basic.logger.Logger;
import program.common.basic.resource.ResUtl;
import program.common.basic.resource.data.DataMap;
import program.common.basic.task.Task;
import program.common.basic.vision.Area;
import program.common.basic.vision.Image;
import program.common.basic.vision.ImageInArea;
import program.common.basic.vision.ImageOps;
import program.driver.E7AS;

import java.io.File;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static java.lang.String.format;

/**
 * Smart utility: SecretShop.
 *
 * @author wautsns
 * @since {{{SINCE_PLACEHOLDER}}}
 */
@Accessors(fluent = true)
public final class SecretShop {

    private static final String ROOT = "/.dev/resources/driver/smart/utility/secretshop";

    private static final Area SWIPE01 = new Area(630, 610, 20, 20);
    private static final Area SWIPE02 = new Area(630, 90, 20, 20);

    // *********************************************************************************
    // *********************************************************************************

    private final E7AS e7as;

    private final DataMap<Area> areaMap;
    private final DataMap<ImageInArea> imageInAreaMap;
    private final DataMap<ImageOps> ocrImageOpsMap;
    private final DataMap<String> ocrTextMap;

    private final @Getter Stat stat;

    // *********************************************************************************
    // Methods, getting status
    // *********************************************************************************

    public boolean isHere(Image screenshot) {
        Image image = screenshot.mutate(ocrImageOpsMap.get("SECRET_SHOP").payload());
        String text = ocrTextMap.get("SECRET_SHOP").payload();
        return e7as.ocr().textarea(image).equalsText(text, 0);
    }

    public int getMinuteLeftUntilRefresh(Image screenshot) {
        Image image = screenshot.mutate(ocrImageOpsMap.get("NM_LEFT_UNTIL_REFRESH").payload());
        Pattern pattern = Pattern.compile(ocrTextMap.get("NM_LEFT_UNTIL_REFRESH").payload());
        Matcher matcher = pattern.matcher(e7as.ocr().textarea(image).inline(0));
        if (!matcher.find()) {
            throw new IllegalStateException();
        }
        String minute = matcher.group("minute");
        if (minute != null) {
            return Integer.parseInt(minute);
        } else if (matcher.group("second") != null) {
            return 1;
        } else if (matcher.group("hour") != null) {
            return 60;
        } else {
            throw new IllegalStateException();
        }
    }

    public void refresh() {
        Logger.title(1, "[SecretShop] refresh");
        stat.incrementRefreshTimes();
        e7as.device().tapUntilAppeared(
                areaMap.get("REFRESH").payload(),
                imageInAreaMap.get("REFRESH_ENSURE").payload(),
                1000
        );
        e7as.device().tapUntilDisappeared(
                areaMap.get("REFRESH_ENSURE").payload(),
                imageInAreaMap.get("REFRESH_ENSURE").payload(),
                2000
        );
    }

    public void detectAndPurchase() {
        Logger.title(1, "[SecretShop] detect and purchase");
        Image screenshot;
        for (int i = 0; ; i++) {
            screenshot = e7as.device().screenshot();
            boolean hasBlank = purchaseIfNeeded(screenshot, "ITEM1_TEXT", "ITEM1_PURCHASE") |
                    purchaseIfNeeded(screenshot, "ITEM2_TEXT", "ITEM2_PURCHASE") |
                    purchaseIfNeeded(screenshot, "ITEM3_TEXT", "ITEM3_PURCHASE") |
                    purchaseIfNeeded(screenshot, "ITEM4_TEXT", "ITEM4_PURCHASE");
            if (!hasBlank || isHere(screenshot)) {break;}
            if (i == 2) {
                screenshot.save(new File("d://png"), format("error_%s.png", System.currentTimeMillis()));
                throw new IllegalStateException();
            }
            Task.sleep(3000);
        }
        e7as.device().swipe(SWIPE01, SWIPE02, 300);
        Task.sleep(2000);
        screenshot = e7as.device().screenshot();
        purchaseIfNeeded(screenshot, "ITEM5_TEXT", "ITEM5_PURCHASE");
        purchaseIfNeeded(screenshot, "ITEM6_TEXT", "ITEM6_PURCHASE");
        Logger.info("SecretShop detect and purchase okay");
        Logger.attribute("refresh.stat", stat.refreshStatText());
        Logger.attribute("covenant_bookmarks.stat", stat.covenantBookmarksStatText());
        Logger.attribute("mystic_medals.stat", stat.mysticMedalsStatText());
        Logger.emptyLine();
    }

    // *********************************************************************************
    // Constructors
    // *********************************************************************************

    public SecretShop(E7AS e7as) {
        this.e7as = e7as;
        String language = e7as.language().get().name();
        this.areaMap = DataMap.of(ResUtl.home("%s/#areaMap", ROOT), Area::parse);
        this.imageInAreaMap = ImageInArea.loadAll(ResUtl.home("%s/#imageInAreaMap", ROOT));
        this.ocrImageOpsMap = DataMap.of(ResUtl.home(
                "%s/#ocrImageOpsMap/%s", ROOT, language
        ), ImageOps::parse);
        this.ocrTextMap = DataMap.of(ResUtl.home(
                "%s/#ocrTextMap/%s", ROOT, language
        ), json -> json.getString("text"));
        this.stat = new Stat();
    }

    // *********************************************************************************
    // InternalMethods
    // *********************************************************************************

    // @return whether the ocr result is blank
    private boolean purchaseIfNeeded(
            Image screenshot, String ocrImageOpsId, String purchaseAreaId) {
        Image image = screenshot.mutate(ocrImageOpsMap.get(ocrImageOpsId).payload());
        String result = e7as.ocr().textarea(image).inline(0);
        if (ocrTextMap.get("COVENANT_BOOKMARKS").payload().equals(result)) {
            stat.incrementCovenantBookmarksTimes();
        } else if (ocrTextMap.get("MYSTIC_MEDALS").payload().equals(result)) {
            stat.incrementMysticMedalsTimes();
        } else {
            return result.isBlank();
        }
        Logger.title(2, "important item detected");
        Logger.attribute("item", result);
        e7as.device().tapUntilAppeared(
                areaMap.get(purchaseAreaId).payload(),
                imageInAreaMap.get("PURCHASE_ENSURE").payload(),
                1000
        );
        e7as.device().tapUntilDisappeared(
                areaMap.get("PURCHASE_ENSURE").payload(),
                imageInAreaMap.get("PURCHASE_ENSURE").payload(),
                2000
        );
        Logger.info("item purchased okay");
        Logger.emptyLine();
        return false;
    }

    // *********************************************************************************
    // StaticClasses
    // *********************************************************************************

    @Accessors(fluent = true)
    public static final class Stat {

        private @Getter int refreshTimes;

        private @Getter int covenantBookmarksTimes;
        private @Getter int mysticMedalsTimes;

        // *********************************************************************************
        // Methods, getting stat string
        // *********************************************************************************

        public String refreshStatText() {
            return format("%d, cost: %d", refreshTimes, refreshTimes * 3);
        }

        public String covenantBookmarksStatText() {
            if (refreshTimes == 0) {return "0 (0.00%)";}
            return format(
                    "%d (%.2f%%)",
                    covenantBookmarksTimes, 100.0 * covenantBookmarksTimes / refreshTimes
            );
        }

        public String mysticMedalsStatText() {
            if (refreshTimes == 0) {return "0 (0.00%)";}
            return format(
                    "%d (%.2f%%)",
                    mysticMedalsTimes, 100.0 * mysticMedalsTimes / refreshTimes
            );
        }

        // *********************************************************************************
        // Methods, recording stat
        // *********************************************************************************

        public void incrementRefreshTimes() {
            this.refreshTimes++;
        }

        public void incrementCovenantBookmarksTimes() {
            this.covenantBookmarksTimes++;
        }

        public void incrementMysticMedalsTimes() {
            this.mysticMedalsTimes++;
        }

        public void reset() {
            this.refreshTimes = 0;
            this.covenantBookmarksTimes = 0;
            this.mysticMedalsTimes = 0;
        }

    }

}
