/*
 *  Copyright (c) 2019 Dennis Lang(LanDen Labs) landenlabs@gmail.com
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
 *  @see https://landenlabs.com
 *
 */
package com.landenlabs.all_log.alog;

import android.content.Context;
import android.os.Build;
import android.util.Log;

/**
 * Interface which defines Log println and open methods.
 *
 * @author Dennis Lang
 */

public class ALogOut {

    public interface LogPrinter {
        void println(int priority, String tag, String msg);
        void open(Context context);
        int maxTagLen();
        int MAX_TAG_LEN = 100;
    }

    // =============================================================================================
    public static class SysLog implements LogPrinter {

        // IllegalArgumentException	is thrown if the tag.length() > 23
        // for Nougat (7.0) releases (API <= 23) and prior, there is
        // no tag limit of concern after this API level.
        static final int LOG_TAG_LEN = (Build.VERSION.SDK_INT >= 24) ? 23 : MAX_TAG_LEN;

        public void println(int priority, String tag, String msg) {
            Log.println(priority, tag, msg);
        }
        public void open(Context context) {
        }
        public int maxTagLen() {
            return LOG_TAG_LEN;
        }
    }

    public LogPrinter outPrn = new SysLog();
}
