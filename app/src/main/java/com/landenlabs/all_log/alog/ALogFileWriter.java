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

package com.landenlabs.all_log.alog;

import android.content.Context;
import android.util.Log;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.zip.GZIPOutputStream;

/**
 * Custom Log output saves to a private log file
 *
 * Derived from work created by volker on 06.02.15.
 * @See https://raw.githubusercontent.com/volkerv/ALogFileWriter/master/ALogFileWriter.java
 *
 * @author Dennis Lang
 */
public class ALogFileWriter implements ALogOut.LogPrinter {
    private static final String TAG = "ALogFileWriter";
    private static final String TIMESTAMP_FORMAT = "yyyy-MM-dd HH:mm:ss.SSS";
    private static final String MSG_FORMAT = "%s/%c %s - %s";  // timestamp, level, tag, message
    private static char[] LEVELS = { '0', '1', 'V','D', 'I', 'W', 'E', 'A' };

    private static String FILENAME = "filelog.txt";
    private static long FILESIZELIMIT = 1024*10;
    private static String mLogDir;

    private String mLogFileName = FILENAME;
    private long mFileSizeLimit;           // bytes
    private File mLogFile;
    private BufferedWriter mBufferedWriter;

    public static ALogFileWriter Default = new ALogFileWriter();

    public static void init(Context context) {
        setDir(context.getCacheDir().getAbsolutePath());
        Default.open(context);
    }

    /**
     * Set file directory. Defaults to Cache directory.
     * <ul>
     *  <li>getCacheDir
     *  <li>getFilesDir()
     * </ul>
     *
     * @param logDir
     */
    public  static void setDir(String logDir) {
        mLogDir = logDir;
    }

    /**
     * Open default file with default file size.
     *
     * @param context
     */
    @Override
    public void open(Context context) {
        setDir(context.getCacheDir().getAbsolutePath());
        open(FILENAME, FILESIZELIMIT);
    }

    /**
     * Open new log file with maximum file size.
     * When logging exceeds maximum size it will be archived and a new file opened.
     * Only one archived file kept.
     *
     * @param logFileName
     * @param fileSizeLimit
     */
    public void open(String logFileName, long fileSizeLimit) {
        mFileSizeLimit = fileSizeLimit;

/*
        try {
            mBufferedWriter = context.openFileOutput(logFilePath,  Context.MODE_APPEND | Context.MODE_WORLD_READABLE);
        }
        catch ( IOException e )
        {
            Log.e( TAG, Log.getStackTraceString( e ) );
        }
*/

        mLogFile = new File(mLogDir, logFileName);
        mLogFileName = mLogFile.getName();
        mLogFile.setReadable(true, false);
        mLogFile.setWritable(true, false);

        if (!mLogFile.exists()) {
            try {
                mLogFile.createNewFile();
                mLogFile.setReadable(true, false);
                mLogFile.setWritable(true, false);
            } catch (IOException e) {
                Log.e(TAG, Log.getStackTraceString(e));
            }
        }

        checkFileSize();

        try {
            mBufferedWriter = new BufferedWriter(new java.io.FileWriter(mLogFile, true));
        } catch (IOException e) {
            Log.e(TAG, Log.getStackTraceString(e));
        }
    }

    public File getFile() {
        if (mBufferedWriter != null) {
            try {
                mBufferedWriter.flush();
            } catch (IOException e) {
                // Log.e(TAG, Log.getStackTraceString(e));
            }
        }
        return mLogFile;
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
            }
        } catch (IOException e) {
            // Log.e(TAG, Log.getStackTraceString(e));
        }
    }

    /**
     * Close and Delete file. Log file is not re-opened, so subsequent logging will fail.
     */
    public void delete() {
        close();
        if (mLogFile != null) {
            mLogFile.delete();
        }
    }

    /**
     * Clear current logging by closing and deleting current file, then re-open file.
     */
    public void clear() {
        close();
        if (mLogFile != null) {
            boolean ok = mLogFile.delete();
            open(mLogFileName, mFileSizeLimit);
        }
    }

    /**
     * Print log level, tag and message.
     *
     * @param level
     * @param tag
     * @param msg
     */
    @Override
    public  void println(int level, String tag, String msg) {
        if (mBufferedWriter != null) {
            synchronized (mBufferedWriter) {
                try {
                    if (checkFileSize()) {
                        mBufferedWriter = new BufferedWriter(new java.io.FileWriter(mLogFile, true));
                    }

                    mBufferedWriter.write(formatMsg(level, tag, msg));
                    mBufferedWriter.newLine();
                    mBufferedWriter.flush();            // TODO - avoid flushing
                } catch (IOException e) {
                    // Log.e(TAG, Log.getStackTraceString(e));
                }
            }
        }

        if (mBufferedWriter == null) {
            Log.e(TAG, "You have to call ALogFileWriter.open(...) before starting to log");
        }
    }

    private static String formatMsg(int level, String tag, String msg) {
        return String.format(MSG_FORMAT, getCurrentTimeStamp(), LEVELS[level&7], tag, msg);
    }

    /**
     * @return Current time formatted for logging.
     */
    private static String getCurrentTimeStamp() {
        String currentTimeStamp = null;

        try {
            SimpleDateFormat dateFormat = new SimpleDateFormat(TIMESTAMP_FORMAT,
                    java.util.Locale.getDefault());
            currentTimeStamp = dateFormat.format(new Date());
        } catch (Exception e) {
            // Log.e(TAG, Log.getStackTraceString(e));
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
                dstFile.delete();
            }

            GZIPOutputStream gzout = new GZIPOutputStream(new FileOutputStream(dstFile));
            FileInputStream in = new FileInputStream(mLogFile);

            int len;
            while ((len = in.read(buffer)) > 0) {
                gzout.write(buffer, 0, len);
            }

            in.close();

            gzout.finish();
            gzout.close();

            dstFile.setReadable(true, false);
            dstFile.setWritable(true, false);
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
                mLogFile.createNewFile();
                mLogFile.setReadable(true, false);
                mLogFile.setWritable(true, false);

                createdNewLogFile = true;
            }
        } catch (IOException e) {
            // Log.e(TAG, Log.getStackTraceString(e));
        }

        return createdNewLogFile;
    }
}
