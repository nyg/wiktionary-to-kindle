package edu.self.w2k.gui;

import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Collection;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.atomic.AtomicLong;

import ch.qos.logback.classic.spi.IThrowableProxy;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.AppenderBase;

/**
 * Buffers log events for display in the GUI's console pane.
 * <p>
 * The appender only <em>enqueues</em>; the UI drains it in batches on a timer. Pushing each event
 * straight to the FX thread with {@code Platform.runLater} would be a mistake here: a dump produces
 * far more log traffic than a UI can repaint, and one runnable per event floods the event queue and
 * freezes the window — the very thing the progress bar exists to avoid.
 * <p>
 * The queue is bounded and drops the newest event when full, counting drops rather than blocking:
 * blocking would apply back-pressure to the worker thread and slow the actual work down just because
 * the UI cannot keep up.
 */
public class UiLogAppender extends AppenderBase<ILoggingEvent> {

    static final int DEFAULT_CAPACITY = 4096;

    /**
     * Zoned explicitly: {@link ILoggingEvent#getInstant()} is an {@link java.time.Instant}, which
     * carries no offset, and a bare wall-clock pattern cannot format one.
     */
    private static final DateTimeFormatter TIME =
            DateTimeFormatter.ofPattern("HH:mm:ss").withZone(ZoneId.systemDefault());

    private final BlockingQueue<String> queue;
    private final AtomicLong dropped = new AtomicLong();

    private volatile Runnable wakeListener = () -> {};

    public UiLogAppender() {
        this(DEFAULT_CAPACITY);
    }

    public UiLogAppender(int capacity) {
        this.queue = new ArrayBlockingQueue<>(capacity);
        setName("ui");
    }

    @Override
    protected void append(ILoggingEvent event) {
        if (!queue.offer(format(event))) {
            dropped.incrementAndGet();
        }
        wakeListener.run();
    }

    public void setWakeListener(Runnable wakeListener) {
        this.wakeListener = wakeListener == null ? () -> {} : wakeListener;
    }

    /**
     * Moves up to {@code max} buffered lines into {@code sink}.
     *
     * @return how many lines were transferred
     */
    public int drainTo(Collection<? super String> sink, int max) {
        return queue.drainTo(sink, max);
    }

    /** Lines discarded because the buffer was full; non-zero means the console is not complete. */
    public long droppedCount() {
        return dropped.get();
    }

    /**
     * Mirrors the console pattern in {@code logback.xml} so the GUI and the CLI read alike. A
     * throwable's message is appended inline — without it, a stack-trace-only failure would show up
     * in the pane as a bare "Download failed" with no cause.
     */
    static String format(ILoggingEvent event) {
        StringBuilder line = new StringBuilder()
                .append(TIME.format(event.getInstant()))
                .append(' ')
                .append("%-5s".formatted(event.getLevel()))
                .append(" - ")
                .append(event.getFormattedMessage());

        IThrowableProxy thrown = event.getThrowableProxy();
        if (thrown != null) {
            line.append(" (").append(thrown.getClassName());
            if (thrown.getMessage() != null) {
                line.append(": ").append(thrown.getMessage());
            }
            line.append(')');
        }
        return line.toString();
    }
}
