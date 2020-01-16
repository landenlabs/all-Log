package com.landenlabs.all_log;

import com.landenlabs.all_log.alog.ALog;

/**
 * Demonstrate log calls
 */
public class LogTester {
    private static final String TAG = "LogTester";

    public static void test1() {
        ALog.d.tagMsg(TAG, "test1 tagMsg debug");
        ALog.i.tagMsg(TAG, "test1 tagMsg info");
        ALog.w.tagMsg(TAG, "test1 tagMsg warn");
        ALog.e.tagMsg(TAG, "test1 tagMsg error");

        ALog.d.tagFmt(TAG, "test1 tagFmt debug");
        ALog.i.tagFmt(TAG, "test1 tagFmt info");
        ALog.w.tagFmt(TAG, "test1 tagFmt warn");
        ALog.e.tagFmt(TAG, "test1 tagFmt error");

        ALog.d.self().msg("test1 msg debug");
    }
}
