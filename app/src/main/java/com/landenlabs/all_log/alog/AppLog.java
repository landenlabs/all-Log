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

import static com.landenlabs.all_log.alog.AppLog.OutLog.LogFile;
import static com.landenlabs.all_log.alog.AppLog.OutLog.LogNone;
import static com.landenlabs.all_log.alog.AppLog.OutLog.LogSys;

/**
 * Application top level logger, defines flavors of logging used to group common activities
 * together like  Fragments, Network, Parsing, etc.
 * <p>
 * Each enumeration can set its default logging enable state. Logging is only possible if
 * both the items logging is enabled and the global Min Level is below the log level.
 *
 * <p>
 * Example:
 * <Pre color="red">
 *     // Set minimum logging level for all logging.
 *     AppLog.setMinLevel(Log.DEBUG);
 *
 *     AppLog.LOG_FRAGMENTS.d().tag(mTag).cat(" ",
 *          "mapViewWidth=", mapViewWidth, " mapVisibleRegion=", mapVisibleRegion);
 *
 *     AppLog.LOGFILE.d().self().fmt("%s=%s", "key", "vaue");
 *
 *     AppLog.LOG_NETWORK.enabled = true;
 * </Pre>
 *
 * @author  Dennis Lang
 */

public enum AppLog {

    /**
     * Logging to android log system.
     */
    LOG(LogSys),

    /**
     * Logging to private log file.
     */
    LOGFILE(LogFile),

    // =========== Application specific log flavors ===========

    /**
     * General Fragment logging
     */
    LOG_FRAG(LogSys),

    /**
     * General Network activity (currently disabled).
     */
    LOG_NETWORK(LogNone),

    /**
     * General Parinsg activity (currently disabled).
     */
    LOG_PARSING(LogNone),

    ;


    enum OutLog {
        LogSys() ,

        LogFile() {
            ALog v() {
                return ALog.fv;
            }
            ALog d() {
                return ALog.fd;
            }
            ALog i() {
                return ALog.fi;
            }
            ALog w() {
                return ALog.fw;
            }
            ALog e() {
                return ALog.fe;
            }
            ALog a() {
                return ALog.fa;
            }
        },

        LogNone(){
            ALog v() {
                return ALog.none;
            }
            ALog d() {
                return ALog.none;
            }
            ALog i() {
                return ALog.none;
            }
            ALog w() {
                return ALog.none;
            }
            ALog e() {
                return ALog.none;
            }
            ALog a() {
                return ALog.none;
            }
        }
        ;

        ALog v() {
            return ALog.v;
        }
        ALog d() {
            return ALog.d;
        }
        ALog i() {
            return ALog.i;
        }
        ALog w() {
            return ALog.w;
        }
        ALog e() {
            return ALog.e;
        }
        ALog a() {
            return ALog.a;
        }
    }

    OutLog out = LogSys;

    AppLog(OutLog outLog) {
        out = outLog;
    }


    // Logging levels.
    //
    public ALog v() {
        return out.v();
    }
    public ALog d() {
        return out.d();
    }
    public ALog i() {
        return out.i();
    }
    public ALog w() {
        return out.w();
    }
    public ALog e() {
        return out.e();
    }
    public ALog a() {
        return out.a();
    }

    /**
     * Set global minimum log level
     *
     * @param level
     */
    public static void setMinLevel(int level) {
        ALog.minLevel = level;
    }
}
