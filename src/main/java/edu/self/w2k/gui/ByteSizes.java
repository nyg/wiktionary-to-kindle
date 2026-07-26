package edu.self.w2k.gui;

/** Human-readable byte counts for the dumps table. */
public final class ByteSizes {

    private static final long KB = 1024L;
    private static final long MB = 1024L * KB;
    private static final long GB = 1024L * MB;

    private ByteSizes() {}

    /** Formats {@code bytes}, or {@code "?"} for the negative value used when the size is unknown. */
    public static String format(long bytes) {
        if (bytes < 0) {
            return "?";
        }
        if (bytes < MB) {
            return "%d KB".formatted(bytes / KB);
        }
        if (bytes < GB) {
            return "%d MB".formatted(bytes / MB);
        }
        return "%.1f GB".formatted((double) bytes / GB);
    }
}
