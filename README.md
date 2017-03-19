### ALog - by LanDen Labs 
Android ALog - Enhanced Log Wrapper

***apk v1.1  Available in dist directgory to demonstrate and test ALog ***

WebSite
[http://landenlabs.com/android/alog/index.html](http://landenlabs.com/android/alog/index.html)

![ALog](http://landenlabs.com//android/alog/alog.png)

ALog is an Android Log wrapper which has the following features:

>   1. Small \- Four small classes.

>   2. Light weight \- No explict memory allocations. 

>   3. Performant \- No processing beyond if-test when logging disabled or 
log level below min level.  Message presentation (formatting or concatenation) 
only occur if logging is required. 

>   4. Chainable \- Calls are chainable making it easier to extend parameter processing. 

>   5. ThreadSafe \- Logs can be generated in concurrent threads.

<a name="table"></a>
#Table of Contents
1. [Overview] (#overview)
2. [Code change] (#change)
3. [ALog] (#alog)
4. [AppLog] (#applog)
5. [Install] (#install)
6. [Build] (#build)
7. [Use] (#use)
8. [License] (#license)
9. [Websites] (#website)

***
<a name="overview"></a>
**Overview** 

The core class ALog is an enumeration. Using an enueration restricts memory allocation
of the class to startup and subsequent use is light-weight singleton.

Core class or enum files:
* ALog  - enumerated log level priorities and format controls.
* ALogOut - Output target abstraction to support alternate targets such as private file.
* ALogFileWriter - Implementation of private output file target.

Optional Extended abstraction enum file:
* AppLog - Enumeration to manage <b>named</b> logging instances which can have different targets.

***
Change your code from <a name="change"></a>

```java
// Determine debug state once in main activity.
// Use flag in code to determine if logging is active. 
boolean showOldLog = ((getApplicationInfo().flags & ApplicationInfo.FLAG_DEBUGGABLE) != 0);


// Places in code where logging is required.
final String TAG = getClass().getSimpleName();
if (showOldLog) {
    Log.d(TAG, "old style log message #1");
}
// ... do more stuff ...
if (showOldLog) {
    Log.d(TAG, "old style log message #2");
}
```

To new Style:

```java
// Determine debug state once in main activity.
// Use flag in code to determine if logging is active. 
boolean showNewLog = ((getApplicationInfo().flags & ApplicationInfo.FLAG_DEBUGGABLE) != 0);
AppLog.setMinLevel(showNewLog ? VERBOSE : NOLOGGING);


// Places in code where logging is required.
AppLog.LOG.d().msg("new style log message #1");
// ... do more stuff
AppLog.LOG.d().msg("new style log message #2");
```

[To Top](#table)

***
<a name="alog"></a>
**ALog** 

ALog is the main log wrapper class which supports the standard priority levels:
* (2) Verbose    
* (3) Debug
* (4) Info
* (5) Warning
* (6) Error
* (7) Assert


```java
public enum ALog {

    // Log levels to system log file.
    v(ALog.VERBOSE),
    d(ALog.DEBUG),
    i(ALog.INFO),
    w(ALog.WARN),
    e(ALog.ERROR),
    a(ALog.ASSERT),
    none(ALog.ASSERT+1),

    . . . 
```


ALog enumeration includes chainable methods to configure output.

Example Syntax of ALog

Call | Logged message
-----| --------------
ALog.i.self().msg("#log info message");  | /I (MainActivity.java:211)(pid):#log info message
ALog.d.tag("myTag1").msg("#log debug message");  | D/myTag1 (pid):#log debug message
ALog.w.tagMsg("myTag2", "#log warning message"); | W/myTag2 (pid):#log warning message
ALog.e.tag("classTag").fmt("#error FIRST:%s LAST:%s", "first", "last"); | E/classTag (pid):#error FIRST:first LAST:last
ALog.i.tag("catTag").cat(" ", "Info", "Log", "a", "new", "msg"); | I/catTag (pid):info Log a new msg

To control logging, set the static global minLevel in ALog. 

```java
File: ALog.java:

    /**
     * Minimum level to log.
     */
    public static int minLevel = ALog.VERBOSE;
```

The logging is <b>active</b> if the calling log level exceeds or is equal to the priority of the minimum global log level.

```java
    ALog.minLevel = ALog.WARN;
    ALog.d.msg("this log is ignored, below min level");
    ALog.w.msg("this log is sent");
    ALog.e.msg("this log is also sent");
```

The ALog enumeration supports chaining of methods to customize the logging output. 
The <b>tag</b> and <b>self</b> method set the Tag field which persists between calls within a thread. 

Method | Description
------ | -----------
tag(String tagStr) | Set output tag field, default is 'self()'. Setting persists between calls.
self()      | Set output tag to self which parses the call stack and generates a tag value of syntax filename:lineNumber


The key advantage of ALog is the <b>cat</b> and <b>fmt</b>
methods do <b>no</b> processing unless the log is active. 
This avoids wasting time and memory formating strings only to be discarded.

The following methods cause an immediate printing of the log message if logging is active. 

Method | Description
------ | -----------
msg(String msgStr) | Print msgStr using current tag to log target
msg(String msgStr, Throwable tr) | Print msgStr using current tag to log target along with Throwable stack trace
tagMsg(String tagStr, String msgStr) | Print tag  and message
cat(String separator, Object... args) | Print concatenaated objects
fmt(String fmt, Object... args) | Print formatted objects

Example when Tag set once in a thread and multiple logs generate without setting tag:

Call | Logged message
-----| --------------
ALog.w.tag("tag3"); |
ALog.w.msg("with tag3, msg#1"); | W/tag3 (pid):msg#1
w.msg("with tag3, msg#2"); | W/tag3 (pid):msg#2

Using following syntax to log an exception:

```java
Exception ex = new Exception("test exception");
ALog.e.tr(ex);
```

[To Top](#table)

***
<a name="applog"></a>
**AppLog** 


This implementation futher extends the logging by wrapping ALog in another 
enumeration called <b>AppLog</b>.  The AppLog enumeration is used to create and manage named loggers
which can be used by common code or actions such as Network access, Parsing XML, Fragments, etc.
The AppLog enumeration futher extends the logging flavors by supporting alternate
logging destinations, such as a private log file or No Logging. By configuring these named
loggers you can control which features in your code are actively logging and where they log.

```java
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

```

Example of using higher level Application level wrapper which provides flavors of logging which gives
an added level of abstraction to setup default behavior.


```java
AppLog.LOG.i().tag("TestTag").msg("Log fixed test");
AppLog.LOG_FRAG.i().self().msg("Frag fixed test");
AppLog.LOGFILE.e().tag("LogFile").msg("LogFile fixed Test");
```

[To Top](#table)

***
<a name="install"></a>
**Install** 

Since ALog is just an enumeration plus support files you just need to include these files in your project. 

To use the full implemenation copy all four files from the alog subdirectory into your project and tune
the AppLog enumeration to your needs. 

The private log fie target <b>ALogFileWriter</b> requires initialization to create the file. Call
init(Context) on this class in your startup code.

```java
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        
        // Initialize ALog private log file target.
        ALogFileWriter.init(this);
```

[To Top](#table)

***
<a name="build"></a>
**Build** 

No special building is required.  Just include the files in your Android project.

Core class files:
* ALog  - enumerated log level priorities and format controls.
* ALogOut - Output target abstraction to support alternate targets such as private file.
* ALogFileWriter - Implementation of private output file target.

Optional Extended abstraction class file:
* AppLog - Enumeration to manage <b>named</b> logging instances which can have different targets.

[To Top](#table)

***
<a name="use"></a>
**Use** 

See above API samples, JavaDoc, Code and Demo app for examples of usage. 

[To Top](#table)

***
<a name="license"></a>
**License** 

```java
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
```

[To Top](#table)

***
<a name="website"></a>
**WebSite** 

Code is provided, documentation and examples provided at these locations:

* https://github.com/landenlabs2/all_Log

* http://www.landenlabs.com/android

[To Top](#table)