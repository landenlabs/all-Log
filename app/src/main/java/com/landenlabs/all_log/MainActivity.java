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
 *  @see http://landenlabs.com
 *
 */

package com.landenlabs.all_log;

import android.app.Activity;
import android.content.pm.ApplicationInfo;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.RadioButton;
import android.widget.ScrollView;
import android.widget.Spinner;
import android.widget.TextView;

import com.landenlabs.all_log.alog.ALog;
import com.landenlabs.all_log.alog.ALogFileWriter;
import com.landenlabs.all_log.alog.AppLog;
import com.landenlabs.all_log.util.LogUtil;

import java.util.concurrent.CountDownLatch;

import static com.landenlabs.all_log.MainActivity.LogTypes.logCat;
import static com.landenlabs.all_log.MainActivity.LogTypes.logFmt;
import static com.landenlabs.all_log.MainActivity.LogTypes.logJoin;
import static com.landenlabs.all_log.MainActivity.LogTypes.logMsg;
import static com.landenlabs.all_log.MainActivity.LogTypes.logThrow;
import static com.landenlabs.all_log.alog.ALog.NOLOGGING;
import static com.landenlabs.all_log.alog.ALog.VERBOSE;
import static com.landenlabs.all_log.alog.ALog.w;
import static com.landenlabs.all_log.alog.ALogFileWriter.Default;

/**
 * ALog API Demo and Tester application.
 *
 * Demo AppLog and ALog API.
 *
 * @author Dennis Lang
 */
@SuppressWarnings({"FieldCanBeLocal", "unused"})
public class MainActivity extends Activity
        implements View.OnClickListener {

    private static final String TAG = "AllLog";

    private RadioButton mTagSelfRb;
    private RadioButton mTagTextRb;
    private RadioButton mTagNoneRb;

    private CheckBox mEnabledCb;
    private Spinner mMinLevelSp;
    private Spinner mLevelSp;
    private EditText mTagTextEt;

    private TextView mLogCatTv;
    private ScrollView mLogCatSv;
    AsyncTask<Void, String, Void> mAsyncLogCat;
    AsyncTask<Void, String, Void> mAsyncLogFile;

    private CheckBox mThreadUiCb;
    private CheckBox mThread1Cb;
    private CheckBox mThread2Cb;

    private UncaughtExceptionHandler uncaughtExceptionHandler = new UncaughtExceptionHandler();


    enum LogTypes {logMsg, logCat, logJoin, logFmt, logThrow}
    private LogTypes mType = logMsg;
    private AppLog mAppLog = AppLog.LOG;
    private boolean mFileLog = false;

    // Thread stuff
    private static volatile boolean mThreadRunning = true;
    private static int gLevel =1;
    CountDownLatch mStartSignal = new CountDownLatch(1);
    CountDownLatch mResumeSignal = new CountDownLatch(1);

    /**
     * Continuous cycling thread, which optionally sends log.
     * <p>
     * mStartSignal and mResumeSignal using Create CountDownLatch(1)
     * <p>
     *  Use countDown() to signal start and resume.
     * <pre>
     * mResumeSignal = new CountDownLatch(1);
     * mStartSignal.countDown();
     *
     * do stuff
     *
     * mStartSignal = new CountDownLatch(1);
     * mResumeSignal.countDown();
     *  </pre>
     */
    class Worker extends  Thread {

        boolean mIsRunning;

        Worker(String name) {
            this.setName(name);
        }

        @Override
        public void run() {
            while (mThreadRunning) {
                try {
                    mStartSignal.await();
                } catch (InterruptedException ex) {
                    return;
                }

                if (mIsRunning) {
                    sendLog(gLevel);
                }

                try {
                    mResumeSignal.await();
                } catch (InterruptedException ex) {
                    return;
                }
            }
        }
    }

    Worker mWorkerThread1 = new Worker("Th#1");
    Worker mWorkerThread2 = new Worker("Th#2");


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        ALogFileWriter.init(this);


        // ================= Setup User Interface ==================
        // Enable and levels.
        mEnabledCb = findViewById(R.id.enabledCb);
        mMinLevelSp = findViewById(R.id.minlevelSp);
        mLevelSp = findViewById(R.id.levelSp);

        // Tag handling
        mTagSelfRb = findViewById(R.id.tag_selfRb);
        mTagSelfRb.setOnClickListener(this);
        mTagTextRb = findViewById(R.id.tag_textRb);
        mTagTextRb.setOnClickListener(this);
        mTagTextEt = findViewById(R.id.tagtextEt);
        mTagNoneRb = findViewById(R.id.tag_noneRb);
        mTagNoneRb.setOnClickListener(this);

        // Log Target
        findViewById(R.id.logRb).setOnClickListener(this);
        findViewById(R.id.logfileRb).setOnClickListener(this);
        findViewById(R.id.lognetworkRb).setOnClickListener(this);

        // Log parameter handling and presentation.
        findViewById(R.id.msgRb).setOnClickListener(this);
        findViewById(R.id.catRb).setOnClickListener(this);
        findViewById(R.id.joinRb).setOnClickListener(this);
        findViewById(R.id.fmtRb).setOnClickListener(this);
        findViewById(R.id.throwRb).setOnClickListener(this);

        // Action buttons and log status
        mLogCatTv = findViewById(R.id.logcatTv);
        mLogCatSv = findViewById(R.id.status_sv);

        mThreadUiCb = findViewById(R.id.thread_ui_cb);
        mThread1Cb = findViewById(R.id.thread_1_cb);
        mThread2Cb = findViewById(R.id.thread_2_cb);

        findViewById(R.id.do_log_btn).setOnClickListener(this);
        findViewById(R.id.clear_btn).setOnClickListener(this);
        findViewById(R.id.show_status_btn).setOnClickListener(this);

        mWorkerThread1.start();
        mWorkerThread2.start();

        LogUtil.clearLogCat();
        mAsyncLogCat = LogUtil.getAsyncLogCat(mLogCatTv, mLogCatSv);
        mAsyncLogCat.execute();

        AppLog.LOGFILE.i().tag("TestFile").msg("Startup");
        mAsyncLogFile = LogUtil.getAsyncReadFile(Default.getFile(), mLogCatTv, mLogCatSv);
        mAsyncLogFile.execute();

        ((RadioButton)findViewById(R.id.logfileRb)).setChecked(true);
        fixedLogTest();
    }

    /**
     * Test Log API, generate various log messages.
     */
    private void fixedLogTest() {

        /*
         * Change code from doing this:
         */
        boolean showOldLog = ((getApplicationInfo().flags & ApplicationInfo.FLAG_DEBUGGABLE) != 0);
        final String TAG = getClass().getSimpleName();
        if (showOldLog) {
            Log.d(TAG, "old style log message #1");
        }
        // ... do more stuff ...
        if (showOldLog) {
            Log.d(TAG, "old style log message #2");
        }

        /*
         * TO this:
         */
        boolean showNewLog = ((getApplicationInfo().flags & ApplicationInfo.FLAG_DEBUGGABLE) != 0);
        AppLog.setMinLevel(showNewLog ? VERBOSE : NOLOGGING);

        AppLog.LOG.d().msg("new style log message #1");
        // ... do more stuff
        AppLog.LOG.d().msg("new style log message #2");


        // ====== More sample new style logging ====

        // First demo Application named logs.
        //
        AppLog.LOG.i().tag("TestTag").msg("Log fixed test");
        AppLog.LOG_FRAG.i().self().msg("Frag fixed test");
        AppLog.LOGFILE.i().tag("LogFile").msg("LogFile fixed Test");

        // Low level - logging samples.
        //
        ALog.i.self().msg("#log info message");
        ALog.d.tag("myTag1").msg("#log debug message");
        ALog.w.tagMsg("myTag2", "#log warning message");
        ALog.e.tag("classTag").fmt("#error FIRST:%s LAST:%s", "first", "last");
        ALog.i.tag("catTag").cat(" ", "Info", "Log", "a", "new", "msg");

        ALog.w.tag("tag3");
        ALog.w.msg("with tag3, msg#1");
        w.msg("with tag3, msg#2");

        Exception ex = new Exception("test exception");
        ALog.e.tr(ex);
    }


    @Override
    public void onClick(View view) {
        int id = view.getId();
        switch (id) {
            case R.id.do_log_btn:
                mLogCatSv.setVisibility(View.VISIBLE);
                sendAndShowLog();
                break;

            case R.id.clear_btn:
                mLogCatTv.setText("");
                break;

            case R.id.show_status_btn:
                mLogCatSv.setVisibility(mLogCatSv.getVisibility() == View.GONE ? View.VISIBLE : View.GONE);
                break;

            case R.id.tag_selfRb:
                mTagSelfRb.setChecked(true);
                mTagTextRb.setChecked(false);
                mTagNoneRb.setChecked(false);
                break;
            case R.id.tag_textRb:
                mTagTextRb.setChecked(true);
                mTagSelfRb.setChecked(false);
                mTagNoneRb.setChecked(false);
                break;
            case R.id.tag_noneRb:
                mTagNoneRb.setChecked(true);
                mTagTextRb.setChecked(false);
                mTagSelfRb.setChecked(false);
                break;

            case R.id.logRb:
                mAppLog = AppLog.LOG;
                mFileLog = false;
                break;
            case R.id.logfileRb:
                mAppLog = AppLog.LOGFILE;
                mFileLog = true;
                break;
            case R.id.lognetworkRb:
                mAppLog = AppLog.LOG_NETWORK;
                break;

            case R.id.msgRb:
                mType = logMsg;
                break;
            case R.id.catRb:
                mType = logCat;
                break;
            case R.id.joinRb:
                mType = logJoin;
                break;
            case R.id.fmtRb:
                mType = logFmt;
                break;
            case R.id.throwRb:
                mType = logThrow;
                break;
        }
    }

    /**
     * Send log using user configured log message.
     * <pre>
     * 1. Clear logs (system log and private log file).
     * 2. Send logs (via threads)
     * 3. Read logs (system log and private log file).
     * </pre>
     */
    private void sendAndShowLog() {
        int minLevel = (int) mMinLevelSp.getSelectedItemId() + Log.VERBOSE;
        AppLog.setMinLevel(minLevel);
        final int level = (int) mLevelSp.getSelectedItemId() + Log.VERBOSE;

        threadSendLog(level);
    }


    /**
     * Send log via selected concurrent threads.
     *
     * <pre>
     * 1. Set booleans to select which threads should send logs.
     *    All threads are always running, but boolean controls which send logs.
     * 2. Refresh Resume signal
     * 3. Start all 3 threads (UI, Thread#1, Thread#2)
     * 4. Refresh Start signal
     * 5. Signal Resume (threads will spin back and wait for start).
     * </pre>
     * Note:
     * CountDownLatches have to be recreated each time. (stupid java).
     */
    private void threadSendLog(final int level) {
        gLevel = level;
        boolean runUiThread = mThreadUiCb.isChecked();
        mWorkerThread1.mIsRunning = mThread1Cb.isChecked();
        mWorkerThread2.mIsRunning = mThread2Cb.isChecked();

        mResumeSignal = new CountDownLatch(1);
        mStartSignal.countDown();

        if (runUiThread) {
            sendLog(level);
        }

        try {
            Thread.sleep(100);
        } catch (Exception ex) {
            Log.e(TAG, ex.getMessage());
        }

        mStartSignal = new CountDownLatch(1);
        mResumeSignal.countDown();
    }


    private void sendLog(int level) {
        try {
            switch (mType) {
                case logMsg:
                    getLogLevel(level).msg("msg");
                    break;
                case logCat:
                    getLogLevel(level).cat("-", "first", 123.4f, this, "last");
                    break;
                case logJoin:
                    getLogLevel(level).tagMsg("ALOG", "<cat", " in ", "hat>", new NullPointerException("help"));
                    break;
                case logFmt:
                    getLogLevel(level).fmt("fmt First %s Last %s", "John", "Doe");
                    break;
                case logThrow:
                    getLogLevel(level).msg("--our msg--", new NullPointerException("help"));
                    break;

            }
        } catch (Exception ex) {
            Log.e(TAG, ex.getMessage());
        }
    }


    /**
     * Get Application logger for desired level
     *
     * @return Application logger for desired level
     */
    private ALog getLogLevel(int level) {
        ALog log = mAppLog.w();

        switch (level) {
            case Log.VERBOSE:
                log = mAppLog.v();
                break;
            case Log.DEBUG:
                log = mAppLog.d();
                break;
            case Log.INFO:
                log = mAppLog.i();
                break;
            case Log.WARN:
                log = mAppLog.w();
                break;
            case Log.ERROR:
                log = mAppLog.e();
                break;
            case Log.ASSERT:
                log = mAppLog.a();
                break;
        }

        if (mTagSelfRb.isChecked()) {
            return log.self();
        } else if (mTagTextRb.isChecked()) {
            String threadId = " [" + Thread.currentThread().getName() + "] ";
            return log.tag(threadId + mTagTextEt.getText().toString());
        }
        return log;
    }
}
