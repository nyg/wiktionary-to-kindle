package edu.self.w2k.kindling;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.nio.file.Path;
import java.util.Map;

import org.junit.jupiter.api.Test;

class XdgCachePathsTest {

    @Test
    void linux_withXdgCacheHome() {
        Map<String, String> env = Map.of("XDG_CACHE_HOME", "/tmp/x");
        Path result = XdgCachePaths.kindlingCacheDir(env, "Linux");
        assertEquals(Path.of("/tmp/x/wiktionary-to-kindle/kindling"), result);
    }

    @Test
    void linux_withHomeNoXdg() {
        Map<String, String> env = Map.of("HOME", "/home/u");
        Path result = XdgCachePaths.kindlingCacheDir(env, "Linux");
        assertEquals(Path.of("/home/u/.cache/wiktionary-to-kindle/kindling"), result);
    }

    @Test
    void macos_withXdgCacheHome() {
        Map<String, String> env = Map.of("XDG_CACHE_HOME", "/tmp/x");
        Path result = XdgCachePaths.kindlingCacheDir(env, "Mac OS X");
        assertEquals(Path.of("/tmp/x/wiktionary-to-kindle/kindling"), result);
    }

    @Test
    void macos_withHomeNoXdg() {
        Map<String, String> env = Map.of("HOME", "/home/u");
        Path result = XdgCachePaths.kindlingCacheDir(env, "Mac OS X");
        assertEquals(Path.of("/home/u/.cache/wiktionary-to-kindle/kindling"), result);
    }

    @Test
    void windows_withLocalAppData() {
        Map<String, String> env = Map.of("LOCALAPPDATA", "C:\\Users\\u\\AppData\\Local");
        Path result = XdgCachePaths.kindlingCacheDir(env, "Windows 10");
        // Use Path.of with separate segments to get correct OS-independent separators
        Path expected = Path.of("C:\\Users\\u\\AppData\\Local")
                .resolve("wiktionary-to-kindle").resolve("Cache").resolve("kindling");
        assertEquals(expected, result);
    }

    @Test
    void windows_withUserProfileNoLocalAppData() {
        Map<String, String> env = Map.of("USERPROFILE", "C:\\Users\\u");
        Path result = XdgCachePaths.kindlingCacheDir(env, "Windows 10");
        Path expected = Path.of("C:\\Users\\u")
                .resolve("AppData").resolve("Local")
                .resolve("wiktionary-to-kindle").resolve("Cache").resolve("kindling");
        assertEquals(expected, result);
    }
}
