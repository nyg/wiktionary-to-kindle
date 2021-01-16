package edu.self.w2k.util;

import java.io.File;

public final class PathUtil {

    public static String path(String... pathElements) {
        return String.join(File.separator, pathElements);
    }

    private PathUtil() {}
}
