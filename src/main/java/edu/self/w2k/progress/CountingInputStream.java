package edu.self.w2k.progress;

import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.function.LongConsumer;

/**
 * Counts the bytes read from the wrapped stream and reports the running total whenever it advances
 * by at least {@code reportEvery} bytes.
 * <p>
 * Used to wrap the <em>compressed</em> dump stream before it reaches the {@code GZIPInputStream}:
 * the uncompressed size of a gzip member is not knowable up front, but the compressed file size is,
 * so counting on the raw side yields a genuine 0–100 % figure.
 * <p>
 * {@link #mark} / {@link #reset} are not supported — rewinding would desynchronise the count.
 */
public final class CountingInputStream extends FilterInputStream {

    private final long reportEvery;
    private final LongConsumer reporter;

    private long count;
    private long lastReported;

    /**
     * @param in          the stream to wrap
     * @param reportEvery minimum byte delta between two reports; must be positive
     * @param reporter    receives the cumulative byte count
     */
    public CountingInputStream(InputStream in, long reportEvery, LongConsumer reporter) {
        super(in);
        if (reportEvery <= 0) {
            throw new IllegalArgumentException("reportEvery must be positive, got " + reportEvery);
        }
        this.reportEvery = reportEvery;
        this.reporter = reporter;
    }

    /** Total bytes read so far, regardless of how many reports have been emitted. */
    public long count() {
        return count;
    }

    @Override
    public int read() throws IOException {
        int b = in.read();
        if (b != -1) {
            advance(1);
        }
        return b;
    }

    @Override
    public int read(byte[] b, int off, int len) throws IOException {
        int read = in.read(b, off, len);
        if (read > 0) {
            advance(read);
        }
        return read;
    }

    @Override
    public long skip(long n) throws IOException {
        long skipped = in.skip(n);
        if (skipped > 0) {
            advance(skipped);
        }
        return skipped;
    }

    @Override
    public boolean markSupported() {
        return false;
    }

    private void advance(long delta) {
        count += delta;
        if (count - lastReported >= reportEvery) {
            lastReported = count;
            reporter.accept(count);
        }
    }
}
