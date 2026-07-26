package edu.self.w2k.gui;

import edu.self.w2k.progress.ProgressListener;
import edu.self.w2k.progress.ProgressListener.Stage;

/**
 * A {@link ProgressListener} callback rendered for display: a bar fraction plus a status line.
 * <p>
 * Progress is reported <em>per stage</em> rather than as one overall figure. A combined number would
 * need weights for how long each stage takes relative to the others, and those ratios swing wildly
 * with dump size and language — a bar that jumps to 90% and sits there is worse than an honest
 * per-stage bar with the stage named next to it.
 *
 * @param fraction 0.0–1.0, or {@link #INDETERMINATE} when the total is unknown
 * @param message  status line for the user
 */
public record ProgressSnapshot(double fraction, String message) {

    /** Matches {@code ProgressIndicator.INDETERMINATE_PROGRESS}, so it can be bound straight to a bar. */
    public static final double INDETERMINATE = -1;

    private static final long BYTES_PER_MB = 1024L * 1024L;

    public static ProgressSnapshot idle() {
        return new ProgressSnapshot(0, "Ready");
    }

    public static ProgressSnapshot of(Stage stage, long done, long total) {
        return switch (stage) {
            case DOWNLOAD -> new ProgressSnapshot(fraction(done, total), downloadMessage(done, total));
            case PARSE -> new ProgressSnapshot(fraction(done, total),
                                               "Parsing entries… " + percent(done, total));
            case FOLD -> new ProgressSnapshot(INDETERMINATE, "Folding inflected forms…");
            case WRITE_HTML -> new ProgressSnapshot(fraction(done, total),
                                                    "Writing HTML %d of %d…".formatted(done, total));
            case KINDLING -> new ProgressSnapshot(INDETERMINATE, "Building MOBI with kindling-cli…");
        };
    }

    private static String downloadMessage(long done, long total) {
        if (total <= 0) {
            return "Downloading dump… %d MB".formatted(done / BYTES_PER_MB);
        }
        return "Downloading dump… %s (%d of %d MB)"
                .formatted(percent(done, total), done / BYTES_PER_MB, total / BYTES_PER_MB);
    }

    private static double fraction(long done, long total) {
        if (total <= 0) {
            return INDETERMINATE;
        }
        return Math.clamp((double) done / total, 0.0, 1.0);
    }

    private static String percent(long done, long total) {
        if (total <= 0) {
            return "";
        }
        return Math.round(fraction(done, total) * 100) + "%";
    }

    /** True when the total was reported as {@link ProgressListener#TOTAL_UNKNOWN}. */
    public boolean indeterminate() {
        return fraction == INDETERMINATE;
    }
}
