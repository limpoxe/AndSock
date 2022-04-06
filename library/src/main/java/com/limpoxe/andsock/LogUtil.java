package com.limpoxe.andsock;

public class LogUtil {
    private static Logger sLogger = null;

    public static void setLogger(Logger logger) {
        sLogger = logger;
    }

    public static void log(String tag, String msg, Throwable e) {
        if (sLogger != null) {
            sLogger.log("[" + Thread.currentThread().getName() + "][" + tag +"]" + msg + ", " + e.getMessage());
        }
    }

    public static void log(String tag, String msg) {
        if (sLogger != null) {
            sLogger.log("[" + Thread.currentThread().getName() + "][" + tag +"]" + msg);
        }
    }

    public interface Logger {
        public void log(String msg);
    }
}
