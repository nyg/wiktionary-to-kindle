package edu.self.w2k.kindling;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.function.BiFunction;
import java.util.function.Function;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RequiredArgsConstructor(access = AccessLevel.PACKAGE)
public class KindlingCliResolver {

    private final String version;
    private final Optional<Path> overrideBinary;
    private final KindlingDownloader downloader;
    private final Function<String, Optional<Path>> pathProbe;
    private final BiFunction<String, KindlingPlatform, String> digestProvider;

    public KindlingCliResolver(String version, Optional<Path> overrideBinary, KindlingDownloader downloader) {
        this(version,
             overrideBinary,
             downloader,
             KindlingCliResolver::probePath,
             (v, p) -> {
                 if (!v.equals(KindlingRelease.DEFAULT_VERSION)) {
                     return null;
                 }
                 KindlingRelease.Asset asset = KindlingRelease.DEFAULT_ASSETS.get(p);
                 return asset != null ? asset.sha256() : null;
             });
    }

    public Path resolve() throws IOException, KindlingException {
        if (overrideBinary.isPresent()) {
            return resolveOverride(overrideBinary.get());
        }

        Optional<Path> fromPath = pathProbe.apply("kindling-cli");
        if (fromPath.isPresent()) {
            log.info("Using kindling-cli from PATH: {}", fromPath.get());
            return fromPath.get();
        }

        KindlingPlatform platform = KindlingPlatform.detect();
        return resolveFromCacheOrDownload(platform);
    }

    private Path resolveOverride(Path override) throws KindlingException {
        if (!Files.isExecutable(override)) {
            throw new KindlingException("--kindling-cli points to non-executable file: " + override);
        }
        log.info("Using kindling-cli override: {}", override);
        return override;
    }

    private Path resolveFromCacheOrDownload(KindlingPlatform platform) throws IOException, KindlingException {
        Path cacheDir = XdgCachePaths.kindlingCacheDir().resolve(version);
        Path cached = cacheDir.resolve(platform.assetName());

        if (Files.exists(cached)) {
            if (cachedBinaryValid(cached, platform)) {
                log.info("Using cached kindling binary: {}", cached);
                return cached;
            }
            log.warn("Cached kindling binary failed checksum check; re-downloading");
            Files.deleteIfExists(cached);
        }

        return downloader.download(version, platform, cacheDir);
    }

    private boolean cachedBinaryValid(Path cached, KindlingPlatform platform) {
        String expected = digestProvider.apply(version, platform);
        if (expected == null) {
            return true;
        }
        try {
            return expected.equalsIgnoreCase(KindlingDownloader.sha256(cached));
        } catch (IOException e) {
            log.warn("Could not verify cached binary checksum: {}", e.getMessage());
            return false;
        }
    }

    private static Optional<Path> probePath(String binaryName) {
        try {
            String os = System.getProperty("os.name", "").toLowerCase(Locale.ROOT);
            boolean isWindows = os.contains("windows");
            List<String> cmd = isWindows
                    ? List.of("where", binaryName + ".exe")
                    : List.of("which", binaryName);
            ProcessBuilder pb = new ProcessBuilder(cmd);
            pb.redirectErrorStream(true);
            Process p = pb.start();
            String output = new String(p.getInputStream().readAllBytes(), StandardCharsets.UTF_8).trim();
            p.waitFor();
            if (!output.isBlank()) {
                Path found = Path.of(output.lines().findFirst().orElse("").trim());
                if (Files.isExecutable(found)) {
                    return Optional.of(found);
                }
            }
        } catch (InterruptedException _) {
            Thread.currentThread().interrupt();
        } catch (Exception _) {
            // which/where probe failed — fall through
        }
        return Optional.empty();
    }
}
