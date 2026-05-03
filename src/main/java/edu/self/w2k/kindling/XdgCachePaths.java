package edu.self.w2k.kindling;

import java.nio.file.Path;
import java.util.Locale;
import java.util.Map;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class XdgCachePaths {

    private XdgCachePaths() {}

    public static Path kindlingCacheDir() {
        return kindlingCacheDir(System.getenv(), System.getProperty("os.name", ""));
    }

    static Path kindlingCacheDir(Map<String, String> env, String osName) {
        if (osName.toLowerCase(Locale.ROOT).contains("windows")) {
            return windowsBase(env).resolve("wiktionary-to-kindle").resolve("Cache").resolve("kindling");
        }
        return unixBase(env).resolve("wiktionary-to-kindle").resolve("kindling");
    }

    private static Path unixBase(Map<String, String> env) {
        String xdg = env.get("XDG_CACHE_HOME");
        if (xdg != null && !xdg.isBlank()) return Path.of(xdg);
        String home = env.get("HOME");
        if (home != null && !home.isBlank()) return Path.of(home, ".cache");
        String tmp = System.getProperty("java.io.tmpdir");
        log.warn("HOME not set; using java.io.tmpdir ({}) as cache root", tmp);
        return Path.of(tmp);
    }

    private static Path windowsBase(Map<String, String> env) {
        String local = env.get("LOCALAPPDATA");
        if (local != null && !local.isBlank()) return Path.of(local);
        String profile = env.get("USERPROFILE");
        if (profile != null && !profile.isBlank()) return Path.of(profile, "AppData", "Local");
        String tmp = System.getProperty("java.io.tmpdir");
        log.warn("LOCALAPPDATA and USERPROFILE not set; using java.io.tmpdir ({}) as cache root", tmp);
        return Path.of(tmp);
    }
}
