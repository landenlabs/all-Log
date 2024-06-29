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
import android.os.Looper;
import androidx.annotation.Nullable;
import androidx.annotation.WorkerThread;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.zip.GZIPOutputStream;

/**
 * Custom Log output saves to a private log file
 *
 * @author Dennis Lang
 */
@SuppressWarnings({"unused", "WeakerAccess"})
public class ALogFileWriter implements ALogOut.LogPrinter {
    private static final String TAG = "ALogFileWriter";
    private static final String TIMESTAMP_FORMAT = "yyyy-MM-dd HH:mm:ss.SSS";
    private static final char[] LEVELS = { '0', '1', 'V','D', 'I', 'W', 'E', 'A' };
    private static SimpleDateFormat dateFmt;

    private String mMsgFmt = "%s/%c %s - %s";  // timestamp, level, tag, message
    private final String mFilename = "filelog.txt";
    private String mLogDir;

    private String mLogFileName = mFilename;
    private long mFileSizeLimit;           // bytes
    private File mLogFile;
    private BufferedWriter mBufferedWriter;
    private Thread mWriterThread;

    public static final ALogFileWriter Default = new ALogFileWriter();
    private static final ArrayBlockingQueue<String> mWriteQueue = new ArrayBlockingQueue<>(20);


    @SuppressWarnings("UnusedReturnValue")
    public static boolean init(Context context) {
        boolean okay = true;
        dateFmt = new SimpleDateFormat(TIMESTAMP_FORMAT, Locale.getDefault());

        try {
            Default.setDir(context.getFilesDir().getAbsolutePath() + "/logs");
            Default.open(context);
        } catch (Exception ex) {
            okay = false;
            ALog.e.tagMsg(TAG, ex);
        }

        return okay;
    }

    /**
     *  Set row format for four fields:
     *  <ul>
     *  <li>Date Time string
     *  <li>Severity character
     *  <li>Tag string
     *  <li>Message string
     *  </ul>
     *  Examples:
     *  <ul>
     *  <li>"%s/%c %s - %s"
     *  <li>"%s,%c,%s,%s"
     *  <li>%1$s,%4$s
     *  </ul>
     */
    public void setFormat(String fmt) {
        mMsgFmt = fmt;
    }

    /**
     * Set file directory. Defaults to Cache directory.
     * <ul>
     *  <li>getCacheDir
     *  <li>getFilesDir()
     * </ul>
     */
    public void setDir(String logDir) {
        mLogDir = logDir;
    }

    /**
     * Open default file with default file size.
     *
     * <pre>
     * File stored in Download directory:
     *     /storage/emulated/0/Download/package.name.filelog.txt
     * Example:
     *     /storage/emulated/0/Download/com.wsicarousel.android.weather.filelog.txt
     * </pre>
     */
    @Override
    public void open(Context context) {
        // AndroidManifest sets up sharable directory for logs
        // setDir(context.getFilesDir().getAbsolutePath() + "/logs");
        final long FILE_SIZE_LIMIT = 1024 * 1024 * 10;
        open(context.getPackageName() + "." + mFilename, FILE_SIZE_LIMIT);
    }

    /**
     * Open new log file with maximum file size.
     * When logging exceeds maximum size it will be archived and a new file opened.
     * Only one archived file kept.
     */
    public void open(String logFileName, long fileSizeLimit) {
        mFileSizeLimit = fileSizeLimit;

        File dir = new File(mLogDir);
        makeDirs(dir);

        mLogFile = new File(mLogDir, logFileName);
        mLogFileName = mLogFile.getName();
        setPermissions(mLogFile);

        if (!mLogFile.exists()) {
            try {
                //noinspection ResultOfMethodCallIgnored
                mLogFile.createNewFile();
                setPermissions(mLogFile);
            } catch (Exception ex) {
                ALog.e.tagMsg(this, ex);
            }
        }

        checkFileSize();

        try {
            mBufferedWriter = new BufferedWriter(new java.io.FileWriter(mLogFile, true));
        } catch (IOException ex) {
            ALog.e.tagMsg(this, ex);
        }
    }

    public File getFile() {
        if (mBufferedWriter != null) {
            try {
                mBufferedWriter.flush();
            } catch (IOException e) {
                // ALog.e.tagMsg(this, Log.getStackTraceString(e));
            }
        }
        return mLogFile;
    }

    public boolean isOpen() {
        return mBufferedWriter != null;
    }

    /**
     * Close current log file.
     * Subsequent logging will fail until re-opened.
     */
    public void close() {
        try {
            if (mBufferedWriter != null) {
                mBufferedWriter.write('\n');
                // mBufferedWriter.flush( );
                mBufferedWriter.close();
                mBufferedWriter = null;
            }
        } catch (IOException e) {
            // ALog.e.tagMsg(this, Log.getStackTraceString(e));
        }
    }

    /**
     * Close and Delete file. Log file is not re-opened, so subsequent logging will fail.
     */
    public void delete() {
        close();
        if (mLogFile != null) {
            deleteFile(mLogFile);
        }
    }


    /**
     * Clear current logging by closing and deleting current file, then re-open file.
     */
    public void clear() {
        close();
        if (mLogFile != null) {
            deleteFile(mLogFile);
            open(mLogFileName, mFileSizeLimit);
        }
    }

    /**
     * Print log level, tag and message.
     */
    @Override
    public  void println(int level, String tag, String msg) {
        initWriterThread();
        try {
            mWriteQueue.add(formatMsg(level, tag, msg) + "\n");
        } catch (IllegalStateException ignore) {
            // Is full - ignore it
        }
    }

    /**
     * Start worker thread to complete file i/o.
     */
    private void initWriterThread() {
        if (mWriterThread == null) {
            mWriterThread = new Thread("ALogFileWriter") {
                @Override
                public void run() {
                    try {
                        Looper.prepare();
                        while (!isInterrupted()) {
                            String msg = mWriteQueue.take();
                            if (msg != null) {
                                writeln(msg);
                            }
                        }
                    } catch (Exception ex) {
                        ALog.e.tagMsg(this, "Writing log file ", ex);
                    }
                }
            };
            mWriterThread.start();
        }
    }

    /**
     * Wite log level, tag and message.
     */
    @WorkerThread
    private  void writeln(String msg) {
        if (mBufferedWriter != null) {
            synchronized (this) {
                try {
                    if (checkFileSize()) {
                        mBufferedWriter.close();
                        mBufferedWriter = new BufferedWriter(new java.io.FileWriter(mLogFile, true));
                    }

                    mBufferedWriter.write(msg);
                    mBufferedWriter.flush();
                } catch (IOException ex) {
                    ALog.e.tagMsg(this, ex);
                }
            }
        }

        if (mBufferedWriter == null) {
            ALog.e.tagMsg(this, "You have to call ALogFileWriter.open(...) before starting to log");
        }
    }

    @Override
    public int maxTagLen() {
        return MAX_TAG_LEN; // No real limit but 100 is a good value.
    }

    @SuppressWarnings("ResultOfMethodCallIgnored")
    private void setPermissions(File file){
        file.setReadable(true, true);
        file.setWritable(true, true);
    }

    private String formatMsg(int level, String tag, String msg) {
        return String.format(mMsgFmt, getCurrentTimeStamp(), LEVELS[level&7], tag, msg);
    }

    /**
     * @return Current time formatted for logging.
     */
    private static String getCurrentTimeStamp() {
        String currentTimeStamp = null;

        try {
            currentTimeStamp = dateFmt.format(new Date());
        } catch (Exception e) {
            // ALog.e.tagMsg(this, Log.getStackTraceString(e));
        }

        return currentTimeStamp;
    }

    /**
     * Zip Archive full log file and open a new log file.
     */
    private void archiveByGzip() {

        byte[] buffer = new byte[1024];

        try {

            File dstFile = new File(mLogDir, mLogFileName + ".gz");
            if (dstFile.exists()) {
                deleteFile(dstFile);
            }

            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                try (GZIPOutputStream gzout = new GZIPOutputStream(Files.newOutputStream(dstFile.toPath()))) {
                    try (FileInputStream in = new FileInputStream(mLogFile)) {
                        int len;
                        while ((len = in.read(buffer)) > 0) {
                            gzout.write(buffer, 0, len);
                        }
                    }
                }
            }

            setPermissions(dstFile);
        } catch (IOException ex) {
            ex.printStackTrace();
        }
    }

    /**
     *  Archive full log file and open a new log file.
     */
    private void archiveLog() {
        archiveByGzip();
    }

    /**
     * If file size has been exceeded, archive current file and open new file.
     *
     * @return True if current file archived and new file created.
     */
    private boolean checkFileSize() {
        boolean createdNewLogFile = false;
        try {
            if (mLogFile.length() > mFileSizeLimit) {
                archiveLog();

                mLogFile = new File(mLogDir, mLogFileName);
                //noinspection ResultOfMethodCallIgnored
                mLogFile.createNewFile();
                setPermissions(mLogFile);
                createdNewLogFile = true;
            }
        } catch (Exception ignore) {
            // ALog.e.tagMsg(this, Log.getStackTraceString(e));
        }

        return createdNewLogFile;
    }

    /**
     * Alternate version then system method File.mkdirs() which returns false if directory exists.
     */
    private static boolean makeDirs(@Nullable File file) {
        try {
            if (file == null || file.exists()) {
                return true;
            }

            if (file.mkdir()) {
                return true;
            }

            File canonFile;
            try {
                canonFile = file.getCanonicalFile();
            } catch (IOException ex) {
                ALog.w.tagMsg(TAG, "mkdirs failed ", ex);
                return false;
            }

            File parent = canonFile.getParentFile();
            return (parent != null && makeDirs(parent) && canonFile.mkdir());
        } catch (Exception ex) {
            ALog.w.tagMsg(TAG, "mkdirs failed ", ex);
            return false;
        }
    }

    public static void deleteFile(@Nullable File file) {
        try {
            if (file != null && !file.delete()) {
                ALog.e.tagMsg(TAG, "delete failed on=", file);
            }
        } catch (Exception ex) {
            ALog.e.tagMsg(TAG, "delete failed on=", file);
        }
    }
}
