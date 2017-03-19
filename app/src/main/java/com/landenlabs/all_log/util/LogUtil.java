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

import android.os.AsyncTask;
import android.util.Log;
import android.view.View;
import android.widget.ScrollView;
import android.widget.TextView;

import com.landenlabs.all_log.alog.ALogFileWriter;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;

/**
 * Created by Dennis Lang on 1/1/17.
 */

public class LogUtil {
    private static String TAG = "LogUtil";

    /**
     * Clear device log file and private logFile.
     */
    public static void clearLogCat() {
        try {
            Process process = new ProcessBuilder()
                    .command("logcat", "-c")
                    .redirectErrorStream(true)
                    .start();
        } catch (IOException ex) {
            Log.e(TAG, ex.getMessage());
        }

        try {
            ALogFileWriter.Default.clear();
        } catch (Exception ex) {
            Log.e(TAG, ex.getMessage());
        }
    }

    /**
     * Get logcat output relevant for our logged messages.
     *
     * @param tag
     * @return
     */
    public static String getLogCat(String tag) {
        StringBuilder logStr = new StringBuilder();
        try {
            String line = "";

            // String logcmd = String.format("logcat -d -v brief -s \"java\",\"%s\"", tag);
            String logcmd = "logcat -d -v brief -t 4";
            Process process = Runtime.getRuntime().exec(logcmd);
            BufferedReader bufferedReader = new BufferedReader(
                    new InputStreamReader(process.getInputStream()));

            while ((line = bufferedReader.readLine()) != null) {
                // Exclude junk and only show info this app has generated.
                if (!line.startsWith("---") /* && (line.contains(tag) || line.contains("java")) */ ) {
                    if (line.trim().length() > 2) {
                        logStr.append(line + "\n");
                    }
                }
            }
        } catch(IOException ex) {
            // Log.e("foo", ex.getMessage());
        }

        return logStr.toString();
    }

    /**
     * Get logged messages send to private log file.
     *
     * @param tag
     * @return
     */
    public static String getLogFile(String tag) {
        StringBuilder logStr = new StringBuilder();
        try {
            String line = "";
            File logFile = ALogFileWriter.Default.getFile();
            logFile = new File(logFile.getAbsolutePath());
            BufferedReader bufferedReader = new BufferedReader(new FileReader(logFile));
            while ((line = bufferedReader.readLine()) != null) {
                if (line.trim().length() > 2) {
                    logStr.append("(File)" + line + "\n");
                }
            }
        } catch (IOException ex) {
            // Log.e("foo", ex.getMessage());
        }
        return logStr.toString();
    }


    /**
     * Async Task which continuously reads LogCat output and updates TextView and advances scrollView.
     * Call must call execute() to start task.
     *
     * @param textView
     * @param scrollView
     * @return Created async task.
     */
    public static AsyncTask<Void, String, Void> getAsyncLogCat(
            final TextView textView, final ScrollView scrollView)  {
        AsyncTask<Void, String, Void> asyncLogCat =
            new AsyncTask<Void, String, Void>() {

            @Override
            protected Void doInBackground(Void... params) {
                try {
                    Process process = Runtime.getRuntime().exec("logcat");
                    BufferedReader bufferedReader = new BufferedReader(
                            new InputStreamReader(process.getInputStream()));

                    String line = "";
                    while ((line = bufferedReader.readLine()) != null) {
                        if (!line.startsWith("----") && line.trim().length() > 2) {
                            publishProgress(line);
                        }
                    }
                }
                catch (IOException ex) {
                    Log.e(TAG, ex.getMessage());
                }
                return null;
            }

            @Override
            protected void onProgressUpdate(String... values) {
                textView.append(values[0] + "\n");
                scrollView.post(new Runnable() {
                    @Override
                    public void run() {
                        scrollView.fullScroll(View.FOCUS_DOWN);
                    }
                });
            }
        };

        return asyncLogCat;
    }

    /**
     * Async Task which continuously reads file output and updates TextView and advances scrollView.
     * Call must call execute() to start task.
     *
     * @param file   File to read.
     * @param textView
     * @param scrollView
     * @return Created async task.
     */
    public static AsyncTask<Void, String, Void> getAsyncReadFile(final File file,
            final TextView textView, final ScrollView scrollView)  {
        AsyncTask<Void, String, Void> asyncLogFile =
                new AsyncTask<Void, String, Void>() {

                    BufferedReader bufferedReader;
                    long lastFileLen = file.length();
                    long lastMod = file.lastModified();

                    @Override
                    protected Void doInBackground(Void... params) {
                        try {
                            bufferedReader = new BufferedReader(new FileReader(file));
                            while (!this.isCancelled()) {
                                long currentLen = file.length();
                                if (currentLen < lastFileLen) {
                                    bufferedReader = new BufferedReader(new FileReader(file));
                                }
                                lastFileLen = currentLen;

                                String line = "";
                                while ((line = bufferedReader.readLine()) != null) {
                                    if (line.trim().length() > 2) {
                                        publishProgress(line);
                                    }
                                }
                                Thread.sleep(1000);
                            }
                        }
                        catch (Exception ex) {
                            Log.e(TAG, ex.getMessage());
                        }
                        finally {
                            if (bufferedReader != null) {
                                // bufferedReader.close();
                            }
                        }
                        return null;
                    }

                    @Override
                    protected void onProgressUpdate(String... values) {
                        textView.append(values[0] + "\n");
                        scrollView.post(new Runnable() {
                            @Override
                            public void run() {
                                scrollView.fullScroll(View.FOCUS_DOWN);
                            }
                        });
                    }
                };

        return asyncLogFile;
    }
}
