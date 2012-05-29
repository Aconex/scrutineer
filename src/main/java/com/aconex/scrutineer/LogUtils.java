package com.aconex.scrutineer;


import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class LogUtils {

    private static final int MILLIES_PER_SECOND = 1000;

    private LogUtils() {
        // For Checkstyle
    }

    public static void debug(Logger log, String message, Object... args) {
        if (!log.isDebugEnabled()) {
            return;
        }
        log.debug(getFormattedMessage(message, args));
    }

    public static void warn(Logger log, String message, Throwable throwable, Object... args) {
        log.warn(getFormattedMessage(message, args), throwable);
    }

    public static void error(Logger log, String message, Throwable throwable, Object... args) {
        log.error(getFormattedMessage(message, args), throwable);
    }

    public static void error(Logger log, String message, Object... args) {
        log.error(getFormattedMessage(message, args));
    }

    public static void info(Logger log, String message, Object... args) {
        log.info(getFormattedMessage(message, args));
    }

    private static String getFormattedMessage(String message, Object... args) {
        String formattedMessage = message;
        if (args != null && args.length > 0) {
            formattedMessage = String.format(message, args);
        }
        return formattedMessage;
    }

    public static void infoTimeTaken(Logger log, long startTime, long numItems, String message, Object... args) {  //NOPMD
        double elapsedTimeInSeconds = (((double)System.currentTimeMillis()) - startTime)/ MILLIES_PER_SECOND;
        double itemsPerSecond = numItems / elapsedTimeInSeconds;
        String timeInformation = String.format(" - took %.2f seconds to do %d items at %.2f per second.",elapsedTimeInSeconds,numItems,itemsPerSecond);
        info(log, message+timeInformation, args);
    }

    public static Logger loggerForThisClass() {
        // We use the third stack element; second is this method, first is .getStackTrace()
        StackTraceElement myCaller = Thread.currentThread().getStackTrace()[2];
        if(!"<clinit>".equals(myCaller.getMethodName())) {
            throw new RuntimeException("Logger must be static");
        }
        return LoggerFactory.getLogger(myCaller.getClassName());
    }
}
