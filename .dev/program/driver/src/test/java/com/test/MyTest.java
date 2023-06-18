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
package com.test;

import program.common.basic.logger.Logger;
import program.common.basic.task.Task;
import program.driver.E7AS;
import program.driver.smart.utility.secretshop.SecretShop;

/**
 * @author wautsns
 * @since {{{SINCE_PLACEHOLDER}}}
 */
public class MyTest {

    public static void main(String[] args) {
        String name = "user1";
        try (E7AS e7as = E7AS.of(name)) {
            e7as.config().setValue("server", "Global_official_Asia");
            e7as.device().config().setValue("adbd.address", "127.0.0.1:5555");
            e7as.ocr().config().setValue("text.language", "zhCN");
            e7as.config().save();
            SecretShop secretShop = new SecretShop(e7as);
            // noinspection InfiniteLoopStatement
            while (true) {
                secretShop.stat().incrementRefreshTimes();
                secretShop.detectAndPurchase();
                int minute = secretShop.getMinuteLeftUntilRefresh(e7as.device().screenshot()) + 1;
                for (int i = minute; i > 0; i--) {
                    Logger.info("wait %d minutes", minute);
                    Task.sleep(60_000);
                }
            }
        }
    }

}
