package edu.self.w2k.gui;

import java.nio.file.Files;
import java.nio.file.Path;

import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.classic.util.LogbackMDCAdapter;
import ch.qos.logback.core.rolling.RollingFileAppender;
import ch.qos.logback.core.rolling.TimeBasedRollingPolicy;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.junit.jupiter.MockitoExtension;
import org.slf4j.Logger;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(MockitoExtension.class)
class LogFileTest {

    @TempDir
    Path logDir;

    @Test
    void should_roll_the_log_file_daily_when_installed() throws Exception {
        // Given
        LoggerContext context = new LoggerContext();

        // When
        RollingFileAppender<ILoggingEvent> unit =
                LogFile.install(context, context.getLogger(Logger.ROOT_LOGGER_NAME), logDir);
        unit.stop();

        // Then
        assertThat(unit.getFile()).isEqualTo(logDir.resolve("app.log").toString());
        assertThat(unit.getRollingPolicy()).isInstanceOf(TimeBasedRollingPolicy.class);

        TimeBasedRollingPolicy<?> policy = (TimeBasedRollingPolicy<?>) unit.getRollingPolicy();
        assertThat(policy.getFileNamePattern()).contains("%d{yyyy-MM-dd}");
        assertThat(policy.getMaxHistory()).isEqualTo(LogFile.MAX_HISTORY_DAYS);
        assertThat(policy.isCleanHistoryOnStart()).isTrue();
    }

    @Test
    void should_write_events_to_the_log_file_when_installed() throws Exception {
        // Given
        LoggerContext context = new LoggerContext();
        // The real context gets its adapter from logback's service provider; a bare one does not,
        // and appending an event without one fails inside the appender.
        context.setMDCAdapter(new LogbackMDCAdapter());
        ch.qos.logback.classic.Logger root = context.getLogger(Logger.ROOT_LOGGER_NAME);

        // When
        RollingFileAppender<ILoggingEvent> unit = LogFile.install(context, root, logDir);
        root.info("a line worth attaching to a bug report");
        unit.stop();

        // Then
        assertThat(logDir.resolve("app.log")).content().contains("a line worth attaching to a bug report");
    }

    @Test
    void should_keep_earlier_sessions_of_the_same_day_when_installed() throws Exception {
        // Given a log file left by an earlier run, which truncating at startup used to erase
        LoggerContext context = new LoggerContext();
        context.setMDCAdapter(new LogbackMDCAdapter());
        ch.qos.logback.classic.Logger root = context.getLogger(Logger.ROOT_LOGGER_NAME);
        Files.writeString(logDir.resolve("app.log"), "from the first run\n");

        // When
        RollingFileAppender<ILoggingEvent> unit = LogFile.install(context, root, logDir);
        root.info("from the second run");
        unit.stop();

        // Then
        assertThat(logDir.resolve("app.log")).content()
                .contains("from the first run")
                .contains("from the second run");
    }

    @Test
    void should_create_the_log_directory_when_it_does_not_exist() throws Exception {
        // Given
        LoggerContext context = new LoggerContext();
        Path nested = logDir.resolve("state").resolve("logs");

        // When
        RollingFileAppender<ILoggingEvent> unit =
                LogFile.install(context, context.getLogger(Logger.ROOT_LOGGER_NAME), nested);
        unit.stop();

        // Then
        assertThat(nested).isDirectory();
    }
}
