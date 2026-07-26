package edu.self.w2k.progress;

/**
 * Coarse progress feedback for the long-running stages of the pipeline.
 * <p>
 * Deliberately minimal and UI-free so the service layer stays independent of any front-end: the CLI
 * passes {@link #NOOP} and relies on logging, while the GUI passes an adapter that drives a progress
 * bar. Textual detail keeps flowing through SLF4J — this is not a second message channel.
 * <p>
 * Implementations are called from worker threads and must be cheap and thread-safe. Callers throttle
 * their emissions (the dump is millions of lines), so implementations need not debounce.
 */
public interface ProgressListener {

    /** Passed as {@code total} when the amount of work cannot be known up front. */
    long TOTAL_UNKNOWN = -1;

    /** A listener that discards everything; the default for the CLI. */
    ProgressListener NOOP = (stage, done, total) -> {};

    /**
     * @param stage which pipeline stage is reporting
     * @param done  units completed so far, in the stage's own unit
     * @param total total units, or {@link #TOTAL_UNKNOWN} when indeterminate
     */
    void onProgress(Stage stage, long done, long total);

    /**
     * The pipeline stages, in execution order. {@code DOWNLOAD} and {@code PARSE} report byte counts;
     * {@code WRITE_HTML} reports chapter counts; {@code FOLD} and {@code KINDLING} are indeterminate
     * and report only that they have started.
     */
    enum Stage {
        DOWNLOAD,
        PARSE,
        FOLD,
        WRITE_HTML,
        KINDLING
    }
}
