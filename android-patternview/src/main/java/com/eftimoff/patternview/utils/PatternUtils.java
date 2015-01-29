/*
 * Copyright (C) 2007 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.eftimoff.patternview.utils;

import com.eftimoff.patternview.cells.Cell;
import com.eftimoff.patternview.cells.CellManager;

import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.List;

public class PatternUtils {

    private static final String UTF8 = "UTF-8";

    public static final String SHA1 = "SHA-1";


    public static List<Cell> stringToPatternOld(final String string, final CellManager cellManager) {
        List<Cell> result = new ArrayList<>();

        try {
            final byte[] bytes = string.getBytes(UTF8);
            for (byte b : bytes) {
                result.add(cellManager.get(b / 3, b % 3));
            }
        } catch (UnsupportedEncodingException e) {
        }

        return result;
    }


    /**
     * Converts a string to a pattern
     *
     * @param string
     * @return
     */
    public static List<Cell> stringToPattern(final String string, final CellManager cellManager) {
        List<Cell> result = new ArrayList<>();

        for (int i = 0; i < string.length(); i++) {
            int b = Integer.valueOf(string.substring(i, i + 1));
            result.add(cellManager.get(b / 3, b % 3));
        }

        return result;
    }

}
