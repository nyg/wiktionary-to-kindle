package edu.self.w2k.gui;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.ArrayList;
import java.util.List;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.spi.LoggingEvent;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class UiLogAppenderTest {

    private final LoggerContext context = new LoggerContext();
    private UiLogAppender unit;

    @BeforeEach
    void setUp() {
        unit = new UiLogAppender(4);
        unit.setContext(context);
        unit.start();
    }

    @Test
    void should_buffer_events_until_drained() {
        // Given
        unit.doAppend(event(Level.INFO, "first"));
        unit.doAppend(event(Level.INFO, "second"));

        // When
        List<String> sink = new ArrayList<>();
        int drained = unit.drainTo(sink, 10);

        // Then
        assertThat(drained).isEqualTo(2);
        assertThat(sink).hasSize(2);
        assertThat(sink.getFirst()).endsWith("- first");
        assertThat(unit.droppedCount()).isZero();
    }

    @Test
    void should_leave_remainder_queued_when_drain_is_capped() {
        // Given
        unit.doAppend(event(Level.INFO, "a"));
        unit.doAppend(event(Level.INFO, "b"));
        unit.doAppend(event(Level.INFO, "c"));

        // When
        List<String> first = new ArrayList<>();
        unit.drainTo(first, 2);
        List<String> second = new ArrayList<>();
        unit.drainTo(second, 2);

        // Then
        assertThat(first).hasSize(2);
        assertThat(second).hasSize(1);
    }

    @Test
    void should_drop_and_count_when_buffer_is_full_rather_than_block() {
        // Given a capacity of 4 and six events; blocking here would slow the worker thread down
        for (int i = 0; i < 6; i++) {
            unit.doAppend(event(Level.INFO, "line " + i));
        }

        // When
        List<String> sink = new ArrayList<>();
        int drained = unit.drainTo(sink, 100);

        // Then
        assertThat(drained).isEqualTo(4);
        assertThat(unit.droppedCount()).isEqualTo(2);
    }

    @Test
    void should_report_nothing_drained_when_empty() {
        // When / Then
        assertThat(unit.drainTo(new ArrayList<>(), 10)).isZero();
    }

    @Test
    void should_include_level_and_timestamp_when_formatting() {
        // When
        String line = UiLogAppender.format(event(Level.WARN, "careful"));

        // Then
        assertThat(line).matches("\\d{2}:\\d{2}:\\d{2} WARN  - careful");
    }

    @Test
    void should_interpolate_placeholders_when_formatting() {
        // Given
        LoggingEvent event = event(Level.INFO, "downloaded {} of {} MB");
        event.setArgumentArray(new Object[] {25, 100});

        // When / Then
        assertThat(UiLogAppender.format(event)).endsWith("- downloaded 25 of 100 MB");
    }

    @Test
    void should_append_throwable_details_when_present() {
        // Given a failure logged with an exception: without the cause the pane would show only the
        // bare message, and the reason for the failure would be invisible.
        LoggingEvent event = event(Level.ERROR, "Download failed");
        event.setThrowableProxy(new ch.qos.logback.classic.spi.ThrowableProxy(
                new java.io.IOException("HTTP 404")));

        // When
        String line = UiLogAppender.format(event);

        // Then
        assertThat(line).contains("Download failed")
                .contains("IOException")
                .contains("HTTP 404");
    }

    private LoggingEvent event(Level level, String message) {
        LoggingEvent event = new LoggingEvent();
        event.setLevel(level);
        event.setMessage(message);
        event.setLoggerName("test");
        event.setTimeStamp(System.currentTimeMillis());
        return event;
    }

    @Test
    void should_expose_a_named_appender_so_it_can_be_found_on_the_root_logger() {
        // When / Then
        assertThat(unit.getName()).isEqualTo("ui");
    }
}
