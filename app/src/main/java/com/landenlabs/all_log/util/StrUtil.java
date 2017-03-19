/*
 *  Copyright (c) 2017 Dennis Lang (LanDen Labs) landenlabs@gmail.com
 *
 *  Permission is hereby granted, free of charge, to any person obtaining a copy of this software and
 *  associated documentation files (the "Software"), to deal in the Software without restriction, including
 *  without limitation the rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 *  copies of the Software, and to permit persons to whom the Software is furnished to do so, subject to the
 *  following conditions:
 *
 *  The above copyright notice and this permission notice shall be included in all copies or substantial
 *  portions of the Software.
 *
 *  THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT
 *  LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN
 *  NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY,
 *  WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE
 *  SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 *
 *  @author Dennis Lang  (Jan-2017)
 *  @see http://landenlabs.com
 *
 */


package com.landenlabs.all_log.util;

import android.text.SpannableString;
import android.text.Spanned;
import android.text.style.BackgroundColorSpan;
import android.util.Log;

/**
 * Created by Dennis Lang on 1/1/17.
 */

public class StrUtil {

    public static String joinLines(String str1, String str2) {
        String sep =  (str1.trim().length() != 0) ? "\n" : "";
        return (str1 + sep + str2);
    }

    private static final int LineColors[] = { 0xffe0ffff, 0xffffe0ff };

    /**
     * Colorize multi line data (alternating colors per line).
     *
     * @param str
     * @return Alternate colorized lines.
     */
    public static SpannableString colorizeLines(String str) {
        String full = str.replaceAll("[\n]+", "\n");
        String[] lines = full.split("\n");

        SpannableString spanStr = new SpannableString(full);

        int idx = 0;
        int first = 0;
        int last = 0;

        for (String line : lines) {
            last = first + line.length();

            int bgColor = LineColors[idx++ % LineColors.length];
            BackgroundColorSpan colorSpan = new BackgroundColorSpan(bgColor);

            spanStr.setSpan(colorSpan, first, last, Spanned.SPAN_INCLUSIVE_INCLUSIVE);
            Log.i("fxx", String.format("%d to %d", first, last));
            first = last + 1;
        }

        return spanStr;
    }


}
