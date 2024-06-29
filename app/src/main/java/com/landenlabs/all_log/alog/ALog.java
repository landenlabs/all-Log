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

import android.app.ActivityManager;
import android.content.Context;
import android.net.TrafficStats;
import android.os.Debug;
import android.os.Looper;
import android.util.Log;
import android.widget.Toast;

import java.lang.ref.WeakReference;

import static android.content.Context.ACTIVITY_SERVICE;

/**
 * Log wrapper (helper) enumeration class. Built-in joining of object strings or formatting
 * delayed until logging is required.
 * <p>
 * Avoid pre-joining strings, such as:
 * <pre><font color="#006000">   ALog.d.tagMsg(this, " var1=" + var1 + " var2=" + var2);
 * </font></pre>
 * Instead, let ALog do the joining to avoid overhead when logging is disabled.
 * <pre><font color="#006000">   ALog.d.tagMsg(this, " var1=", var1, " var2=", var2);
 * </font></pre>
 * <p>
 * Primary methods:
 * <ul>
 *     <li>tagMsg(Object tag, Object... args)</li>
 *     <li>tagMsgStack(Object tag, Object... args )</li>
 *     <li>tagFmt(Object tag, String fmt, Object... args)</li>
 *     <li>tagCat(Object tag, String separator, Object... args)</li>
 * </ul>
 * <p>
 * Slower methods will automatically generate TAG from stack trace.
 * Alternate logging API, which uses a cascaded API to control and delay presentation.
 * <ul>
 *     <li>tag(String tag)</li>
 *     <li>self()</li>
 *     <li>msg(String msg)</li>
 *     <li>msg(String msg, Throwable tr)</li>
 *     <li>fmt(String fmt, Object... args)</li>
 *     <li>cat(String separator, Object... args)</li>
 *     <li>ex(Throwable tr)</li>
 * </ul>
 * <p>
 * <b>Examples:</b> 
 * <br><pre><font color="#006000">
 *    // Optimized methods, caller provides TAG.
 *    ALog.d.tagMsg(this, "log this message");
 *    ALog.d.tagMsg(this, "log this message with exception", ex);
 *    ALog.d.tagMsg(this, "Data", badData, " should be", goodData);
 *    ALog.d.tagFmt(TAG, "First:%s Last:%s", firstName, lastName);
 * </font><font color="#a06000">
 *    // Slower calls will generate TAG from stack trace.
 *    ALog.d.msg("log this message");
 *    ALog.d.msg("log this message with exception", ex);
 *    ALog.d.fmt("First:%s Last:%s", firstName, lastName);
 *</font>
 *    // Cascaded usage:
 *    ALog.e.tag("MyFooClass").fmt("First:%s Last:%s", mFirst, mLast);
 *
 *    // Redirect to file.
 *    ALog.i.out(ALogFileWriter.Default).tag("FooBar").cat(" ", item.name, item.desc, item.tag);

 * </pre>
 * @see AppLog
 * @author Dennis Lang
 *
 */

@SuppressWarnings({"WeakerAccess", "unused"})
public enum ALog {

    // Logging levels (2=V, 3=D, 4=I, 5=W 6=E 7=A)

    // ==== Log levels to system log file.

    /** Verbose log priority level 2 */
    v(ALog.VERBOSE),
    /** Debug log priority level 3 */
    d(ALog.DEBUG),
    /** Info log priority level 4 */
    i(ALog.INFO),
    /** Warning log priority level 5 */
    w(ALog.WARN),
    /** Error log priority level 6 */
    e(ALog.ERROR),
    /** Assert log priority level 7 */
    a(ALog.ASSERT),

    /** Disabled log priority level 8 */
    none(ALog.ASSERT+1),

    // Log levels to private log file.
    fv(ALog.VERBOSE, ALogFileWriter.Default),
    fd(ALog.DEBUG, ALogFileWriter.Default),
    fi(ALog.INFO, ALogFileWriter.Default),
    fw(ALog.WARN, ALogFileWriter.Default),
    fe(ALog.ERROR, ALogFileWriter.Default),
    fa(ALog.ASSERT, ALogFileWriter.Default),
    ;

    /**
     * Log levels.
     */
    public static final int VERBOSE = Log.VERBOSE;
    public static final int DEBUG = Log.DEBUG;
    public static final int INFO = Log.INFO;
    public static final int WARN = Log.WARN;
    public static final int ERROR = Log.ERROR;
    public static final int ASSERT = Log.ASSERT;
    public static final int NOLOGGING = Log.ASSERT + 1;


    /**
     * Global  Minimum priority level to log, defaults to WARN.
     */
    public static int minLevel = WARN;
    public static final String TAG_PREFIX = "ALOG_";

    /**
     * Optional context to all Toast message to appear when ERROR occurs.
     */
    public static WeakReference<Context> contextRef;

    private final int mLevel;
    private final ALogOut mOut = new ALogOut();
    private static final ThreadLocal<String> THREAD_TAG = new ThreadLocal<>();

    // Helper to make Log tag from stack, provide class and line number.
    private static final String NAME = ALog.class.getCanonicalName();


    // ---------------------------------------------------------------------------------------------

    /**
     * Custom formatting TAGs - uses optional parameters following ALog.Fmt.xxx tag
     * <p>
     * Extend this method to support your own custom formatting needs. The goal is to avoid
     * preformatting any log information until the log message is needed (inside log level threshold).
     * <p>
     * ID -
     * The Id is identical to the default Tag value which is the objects class name and unique id.
     * The Id is useful to monitor a collection of similar objects where the unique id helps distinguish
     * instance from one another.
     * <p>
     *   Example ID for Instance of Foo
     *     Foo@12345
     * <p>
     * Example:
     *    ALog.tagMsg(this, "pre msg", " Foo=", ALog.Fmt.Id, objFoo, " tailer msg");
     */
    public enum Fmt {
        Id() {
            void append(CharSequence delimiter, int idx, Object[] tokens, StringBuilder sb)  {
                // Throws exception if missing argument.
                sb.append(tagId(tokens[idx]));
                join(delimiter, idx+1, tokens, sb);
            }
        },
        ;

        void append(CharSequence delimiter, int idx, Object[] tokens, StringBuilder sb)  {
            join(delimiter, idx, tokens, sb);
        }
    }

    // ---------------------------------------------------------------------------------------------

    ALog(int level) {
        mLevel = level;
    }

    /**
     * Set custom Log Output stream.
     *
     * @param level to log (2=V, 3=D, 4=I, 5=W 6=E 7=A)
     * @param logPrn custom output stream
     *
     * @see ALogOut
     * @see ALogFileWriter
     */
    ALog(int level, ALogOut.LogPrinter logPrn) {
        mLevel = level;
        mOut.outPrn = logPrn;
    }

    /**
     * Replace default output log target with custom output log target.
     * <p>
     * Example:
     * <br><font color="green">
     *   ALog.i.out(ALogFileWriter.Default).tag("FooBar").cat(" ", "aaaa", "bbbbb", "ccccc");
     * </font>
     * @param logPrn Output print target
     * @return ALog chained instance
     *
     * @see ALogOut
     * @see ALogFileWriter
     */
    public ALog out(ALogOut.LogPrinter logPrn) {
        mOut.outPrn = logPrn;
        return this;
    }

    // =============================================================================================
    // Common API for logging messages.
    // =============================================================================================

    /**
     * Generate object ID  className@uniqueHashCode, ex Foo@12345
     */
    public static String id(Object object) {
        return "@" + Integer.toHexString(System.identityHashCode(object))
                + ":" + object.getClass().getName();
    }

    /**
     * If valid log level, Print tag and msg.
     *
     * @param tagObj Tag to print to log output target.
     * @param msgStr Message to print to log output target.
     */
    public void tagMsg(Object tagObj, String msgStr) {
        if (mLevel >= minLevel) {
            println(tagStr(tagObj), msgStr);
        }
    }

    /**
     * If valid log level, Print tag with args joined together
     *
     * @param tagObj  Present as tag
     * @param args    If valid level, print all args.
     */
    public void tagMsg(Object tagObj, Object... args) {
        if (mLevel >= minLevel) {
            String msgStr = join("", 0, args, null);
            println(tagStr(tagObj), msgStr);
        }
    }

    /**
     * If valid log level, Print tag and msg.
     *
     * @param tagObj Tag to print to log output target.
     * @param msgStr Message to print to log output target.
     * @param tr Trowable stack trace added to output target.
     */
    public void tagMsg(String tagObj, String msgStr, Throwable tr) {
        if (mLevel >= minLevel) {
            println(tagStr(tagObj), msgStr + "\n" + Log.getStackTraceString(tr));
        }
    }

    /**
     * If valid log level, Print tag, msg and stack trace.
     *
     * @param tagObj  Present as tag
     * @param args    If valid level, print all args.
     */
    public void tagMsgStack(Object tagObj, Object... args) {
        if (mLevel >= minLevel) {
            String msgStr = join("", 0, args, null);
            println(tagStr(tagObj), msgStr + " -stack- " + getMsgStack(null));
        }
    }

    /**
     * If valid log level, format message and print.
     * <p>
     * Example:
     * <br> <font color="green">
     *   AppLog.LOG.d().fmt("First:%s Last:%s", firstName, lastName);
     * </font>
     * <p><font color="#ff0000">
     * Warning - Slower then tagFmt(tag, fmt, ...) because Tag generated from stack.
     * </font><p>
     * @param fmt  Format used by String.format to build message to print to log output target.
     * @param args Optional arguments passed to String.format(fmt, ....)
     */
    public void tagFmt(Object tagObj, String fmt, Object... args) {
        if (mLevel >= minLevel) {
            println(tagStr(tagObj), String.format(fmt, args));
        }
    }

    /**
     * Helper to format objects into strings.
     */
    public String toString(Object obj) {
        if (mLevel >= minLevel) {
            if (obj instanceof Throwable) {
                Throwable tr = (Throwable)obj;
                return "Exception Msg=" + tr.getLocalizedMessage()
                        + (tr.getCause()!=null ? " Cause=" + tr.getCause() : "");
            } else {
                return obj.toString();
            }
        } else {
            return "";
        }
    }


    // =============================================================================================
    // Tag manipulation
    // =============================================================================================

    /**
     * Set log tag, if not set or set with empty string, ALog will auto generate a log from stack trace.
     * @param tagStr Tag to use in subsequent log printing.
     * @return ALog chained instance
     *
     * @see #self()
     */
    public ALog tag(String tagStr) {
        if (mLevel >= minLevel) {
            THREAD_TAG.set(tagStr);
        }
        return this;
    }

    /**
     * Set log tag, if not set or set with empty string, ALog will auto generate a log from stack trace.
     * @param obj Tag to use in subsequent log printing. Use object's simple class name.
     * @return ALog chained instance
     *
     * @see #self()
     */
    public ALog tag(Object obj) {
        if (mLevel >= minLevel) {
            THREAD_TAG.set(tagStr(obj));
        }
        return this;
    }

    public static String tagStr(Object obj) {
        String str = tagId(obj);

        if (Looper.getMainLooper().getThread() == Thread.currentThread()) {
            str = str + "#Tmain";
        } else {
            str = str + "#T" + Thread.currentThread().getId();
        }
        return str;
    }

    public static String tagId(Object obj) {
        String str;
        if (obj == null) {
            str = "(null)";
        } else  if (obj instanceof String) {
            str = obj.toString();
        } else {
            str = obj.getClass().getSimpleName() + "@" + Integer
                    .toHexString(System.identityHashCode(obj));
        }
        return str;
    }

    /**
     * Set tag to automatically identify self (class which is calling ALog by stack inspection).
     * <br><font color="red">
     * Warning - Stack inspection is very slow.
     * </font>
     *
     * @return ALog chained instance
     * @see #tag(String)
     */
    public ALog self() {
        if (mLevel >= minLevel) {
            THREAD_TAG.set(null);
        }
        return this;
    }

    // =============================================================================================
    // Slow methods which automatically generate TAG from stack trace.
    // =============================================================================================

    /**
     * If valid log level, Print msg with any previously set tag.
     * <p><font color="#ff0000">
     * Warning - Slower then tagMsg(this, msg) because Tag generated from stack.
     * </font><p>
     * @param args  Message to print to log output target
     * @see #minLevel
     */
    public void msg(Object ... args) {
        if (mLevel >= minLevel) {
            String msgStr = join("",0,  args, null);
            println(findTag(), msgStr);
        }
    }


    /**
     * If valid log level, Print msg with Throwable and any previously set tag.
     * <p><font color="#ff0000">
     * Warning - Slower then tagMsg(this, msg, tr) because Tag generated from stack.
     * </font><p>
     * @param msgStr Message to print to log output target
     * @param tr Throwable stack trace logged.
     */
    public void msg(String msgStr, Throwable tr) {
        if (mLevel >= minLevel) {
            cat("\n", msgStr, Log.getStackTraceString(tr));
        }
    }

    /**
     * If valid log level, format message and print.
     * <p>
     * Example:
     * <br> <font color="green">
     *   AppLog.LOG.d().fmt("First:%s Last:%s", firstName, lastName);
     * </font>
     * <p><font color="#ff0000">
     * Warning - Slower then tagFmt(tag, fmt, ...) because Tag generated from stack.
     * </font><p>
     * @param fmt  Format used by String.format to build message to print to log output target.
     * @param args Optional arguments passed to String.format(fmt, ....)
     */
    public void fmt(String fmt, Object... args) {
        if (mLevel >= minLevel) {
            String msgStr = String.format(fmt, args);
            println(findTag(), msgStr);
        }
    }

    /**
     * If valid log level, Concatenate strings with <b>separator</b>
     * <p>
     * Example:
     * <br><font color="green">
     *     AppLog.LOG.d().cat(" to ", fromTag, toTag);
     * <br>
     *     AppLog.LOG.d().cat(", ", firstName, middleName, lastName);
     * </font>
     *
     * @param separator String place between argument values.
     * @param args One or more object to stringize.
     */
    public void cat(String separator, Object... args) {
        if (mLevel >= minLevel) {
            String msgStr = join(separator, 0, args, null);
            println(findTag(), msgStr);
        }
    }

    /**
     * If valid log level, Log Throwable message and stacktrace.
     *
     * @param tr Throwable logged, message and stack.
     */
    public void tr(Throwable tr) {
        if (mLevel >= minLevel) {
            cat("\n", tr.getLocalizedMessage(), Log.getStackTraceString(tr));
        }
    }

    /**
     * Returns a string containing the tokens joined by delimiters.
     * @param tokens an array objects to be joined. Strings will be formed from
     *     the objects by calling object.toString().
     *
     * Similar to TextUtils.join with custom support for stringizing Throwable.
     */
    public static String join(CharSequence delimiter, int idx, Object[] tokens, StringBuilder sb) {
        if (sb == null) {
            sb = new StringBuilder();
        }
        while (idx < tokens.length) {
            Object token = tokens[idx];
            if (idx != 0) {
                sb.append(delimiter);
            }
            idx++;
            if (token instanceof Throwable) {
                Throwable tr = (Throwable) token;
                sb.append("Exception Msg=").append(tr.getLocalizedMessage());
                if (tr.getCause() != null) {
                    sb.append(" Cause=").append(tr.getCause());
                }
            } else if (token instanceof Fmt) {
                ((Fmt)token).append(delimiter, idx, tokens, sb);  // format All remainnng tokens.
                break;
            } else {
                sb.append(token);
            }
        }
        return sb.toString();
    }

    /**
     * Log exception and throw it.
     */
    public static  void throwIt(Object tag, Error ex) throws Error  {
        ALog.e.tagMsg(tag, getMsgStack(ex));
        throw ex;
    }
    public static  void throwIt(Object tag, Exception ex)  throws Exception {
        ALog.e.tagMsg(tag, getMsgStack(ex));
        throw ex;
    }

    /**
     * Helper to return string of Throwable (exception)  message and stack trace.
     *
     * @param tr Exception to inspect or null for current stack trace.
     * @return message and stack trace
     */
    public static String getMsgStack(Throwable tr) {
        if (tr == null) {
            tr = new Exception("");
        }
        return tr.getLocalizedMessage() + "\n" + Log.getStackTraceString(tr);
    }

    /**
     * Include memory usage with message
     */
    public void memory(Object tagObj, Context context,  Object... args) {
        if (mLevel >= minLevel) {
            System.gc();
            ActivityManager activityManager =
                    (ActivityManager) context.getSystemService(ACTIVITY_SERVICE);
            Debug.MemoryInfo[] memInfos =
                    activityManager.getProcessMemoryInfo(new int[]{android.os.Process.myPid()});
            Debug.MemoryInfo memoryInfo = memInfos[0];

            tagMsg(tagObj,
                    join("", 0, args, null),
                    " Memory javaHeap=",
                    memoryInfo.getMemoryStat("summary.java-heap"),
                    " nativeHeap=",
                    memoryInfo.getMemoryStat("summary.native-heap"),
                    " graphics=",
                    memoryInfo.getMemoryStat("summary.graphics"),
                    " NetRcv=", TrafficStats.getUidRxBytes(android.os.Process.myUid())
            );
        }
    }

    // =============================================================================================
    // Utility methods.
    // =============================================================================================

    /**
     * Print level, tag and message to output target.
     */
    protected void println(String tag, String msg) {
        try {
            int preLen = TAG_PREFIX.length();
            int tagLen = tag.length();
            final int maxTagLen = mOut.outPrn.maxTagLen();

            // As of Nougat (7.0, api 24) the tag length must not exceed 23 characters.
            // If tag is too long, only show prefix in Tag field and present remainder
            // in message field.
            if (preLen + tagLen <= maxTagLen) {
                mOut.outPrn.println(mLevel, TAG_PREFIX + tag, msg);
            } else {
                mOut.outPrn.println(mLevel, TAG_PREFIX, tag + ": " + msg);
            }

            if (contextRef != null && mLevel >= ERROR) {
                if (Looper.getMainLooper().getThread() == Thread.currentThread()) {
                    Toast.makeText(contextRef.get(), msg, Toast.LENGTH_LONG).show();
                }
            }
        } catch (IllegalArgumentException ex) {
            mOut.outPrn.println(mLevel, TAG_PREFIX, ex.getMessage());
        }
    }

    /**
     * Helper to make Log tag from stack, provide class and line number.
     * <p>
     * Make a Log tag by locating class calling ALog.
     *
     * @return  "filename:lineNumber"
     */
    private static String makeTag() {
        String tag = "";
        final StackTraceElement[] ste = Thread.currentThread().getStackTrace();
        for (int idx = 0; idx < ste.length; idx++) {
            StackTraceElement elem = ste[idx];
            if (elem.getMethodName().equals("makeTag") && elem.getClassName().equals(NAME)) {
                while (++idx < ste.length) {
                    elem = ste[idx];
                    if (!elem.getClassName().equals(NAME)) {
                        break;
                    }
                }
                tag = elem.getFileName() + ":" + elem.getLineNumber();
                return tag;
            }
        }
        return tag;
    }


    /**
     * Make a Log tag by locating class calling ALog, then backup until found entry with
     * 'containsClass'
     *
     * @return  "filename:lineNumber"
     */
    public static String makeTag(String containsClass) {
        String tag = "";
        final StackTraceElement[] ste = Thread.currentThread().getStackTrace();
        for (int idx = 0; idx < ste.length; idx++) {
            StackTraceElement elem = ste[idx];
            if (elem.getMethodName().equals("makeTag") && elem.getClassName().equals(NAME)) {
                idx++;  // skip caller
                while (++idx < ste.length) {
                    elem = ste[idx];
                    if (elem.getClassName().contains(containsClass)) {
                        break;
                    }
                }
                tag = elem.getFileName() + ":" + elem.getLineNumber();
                return tag;
            }
        }
        return tag;
    }


    /**
     * Get previously set <b>tag</b> or generate a tag by inspecting the stacktrace.
     * @return User provided tag or "filename:lineNumber"
     */
    public String findTag() {
         String tag = THREAD_TAG.get();
        return (tag != null) ? tag : makeTag();
    }
}
