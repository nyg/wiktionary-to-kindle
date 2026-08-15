package edu.self.w2k.gui;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.encoder.PatternLayoutEncoder;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.rolling.RollingFileAppender;
import ch.qos.logback.core.rolling.TimeBasedRollingPolicy;
import ch.qos.logback.core.util.FileSize;

/**
 * The desktop app's log file, rolled once a day.
 * <p>
 * The active file keeps the fixed name {@code app.log}, so a bug report can always point at the same
 * path; each day's predecessor is compressed to {@code app-YYYY-MM-DD.log.gz} beside it. Truncating
 * at startup, as this once did, was the only thing keeping the file from growing without bound, and
 * it cost the user every earlier session in the same sitting — a crash reproduced on the second run
 * erased the evidence of the first. Appending plus a daily roll keeps a session's full history and
 * still bounds what accumulates.
 * <p>
 * {@code cleanHistoryOnStart} matters here rather than being belt-and-braces: a rolling policy
 * normally prunes when it rolls, and a desktop app that is opened and closed within a day may go
 * weeks without ever rolling.
 */
final class LogFile {

    static final String FILE_NAME = "app.log";

    static final String ARCHIVE_NAME_PATTERN = "app-%d{yyyy-MM-dd}.log.gz";

    static final int MAX_HISTORY_DAYS = 14;

    static final String TOTAL_SIZE_CAP = "200MB";

    private static final String PATTERN = "%d{HH:mm:ss} %-5level - %msg%n";

    private LogFile() {
    }

    /** Installs the appender on {@code root} and returns it, creating {@code logDir} if needed. */
    static RollingFileAppender<ILoggingEvent> install(LoggerContext context,
                                                      ch.qos.logback.classic.Logger root,
                                                      Path logDir) throws IOException {
        Files.createDirectories(logDir);

        PatternLayoutEncoder encoder = new PatternLayoutEncoder();
        encoder.setContext(context);
        encoder.setPattern(PATTERN);
        encoder.start();

        RollingFileAppender<ILoggingEvent> appender = new RollingFileAppender<>();
        appender.setContext(context);
        appender.setName("file");
        appender.setFile(logDir.resolve(FILE_NAME).toString());
        appender.setEncoder(encoder);

        TimeBasedRollingPolicy<ILoggingEvent> policy = new TimeBasedRollingPolicy<>();
        policy.setContext(context);
        policy.setParent(appender);
        policy.setFileNamePattern(logDir.resolve(ARCHIVE_NAME_PATTERN).toString());
        policy.setMaxHistory(MAX_HISTORY_DAYS);
        policy.setTotalSizeCap(FileSize.valueOf(TOTAL_SIZE_CAP));
        policy.setCleanHistoryOnStart(true);
        policy.start();

        appender.setRollingPolicy(policy);
        appender.start();
        root.addAppender(appender);
        return appender;
    }
}
