package edu.self.w2k.convert;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class CalibreEbookConverter implements EbookConverter {

    @FunctionalInterface
    interface ProcessRunner {
        int run(List<String> command) throws IOException;
    }

    private static final ProcessRunner DEFAULT_RUNNER = cmd -> {
        ProcessBuilder pb = new ProcessBuilder(cmd);
        pb.inheritIO();
        try {
            return pb.start().waitFor();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IOException("ebook-convert interrupted", e);
        }
    };

    private final Optional<Path> overrideBinary;
    private final ProcessRunner runner;

    public CalibreEbookConverter(Optional<Path> overrideBinary) {
        this(overrideBinary, DEFAULT_RUNNER);
    }

    CalibreEbookConverter(Optional<Path> overrideBinary, ProcessRunner runner) {
        this.overrideBinary = overrideBinary;
        this.runner = runner;
    }

    @Override
    public Path convert(Path input, Path outputDir, String outputFormat) throws IOException {
        Path executable = findEbookConvert(overrideBinary);
        String baseName = input.getFileName().toString().replaceAll("\\.[^.]+$", "");
        Path outputFile = outputDir.resolve(baseName + "." + outputFormat);

        List<String> cmd = List.of(
                executable.toString(),
                input.toAbsolutePath().toString(),
                outputFile.toAbsolutePath().toString());

        log.info("Running: {}", String.join(" ", cmd));
        int exitCode = runner.run(cmd);
        if (exitCode != 0) {
            throw new IOException("ebook-convert exited with code " + exitCode);
        }
        return outputFile;
    }

    public static Path findEbookConvert(Optional<Path> override) throws EbookConverterNotFoundException {
        if (override.isPresent()) {
            Path p = override.get();
            if (Files.isExecutable(p)) {
                return p;
            }
            throw new EbookConverterNotFoundException();
        }

        String os = System.getProperty("os.name", "").toLowerCase(Locale.ROOT);
        boolean isWindows = os.contains("windows");

        // Try PATH
        try {
            List<String> cmd = isWindows ? List.of("where", "ebook-convert")
                                         : List.of("which", "ebook-convert");
            ProcessBuilder pb = new ProcessBuilder(cmd);
            pb.redirectErrorStream(true);
            Process p = pb.start();
            String output = new String(p.getInputStream().readAllBytes(), StandardCharsets.UTF_8).trim();
            p.waitFor();
            if (!output.isBlank()) {
                Path found = Path.of(output.lines().findFirst().orElse("").trim());
                if (Files.isExecutable(found)) {
                    return found;
                }
            }
        } catch (Exception ignored) {}

        // OS-specific fallbacks
        List<String> fallbacks;
        if (isWindows) {
            fallbacks = List.of(
                    "C:\\Program Files\\Calibre2\\ebook-convert.exe",
                    "C:\\Program Files (x86)\\Calibre2\\ebook-convert.exe");
        } else if (os.contains("mac") || os.contains("darwin")) {
            fallbacks = List.of(
                    "/Applications/calibre.app/Contents/MacOS/ebook-convert",
                    "/usr/local/bin/ebook-convert",
                    "/usr/bin/ebook-convert");
        } else {
            fallbacks = List.of(
                    "/usr/bin/ebook-convert",
                    "/usr/local/bin/ebook-convert");
        }

        for (String path : fallbacks) {
            Path candidate = Path.of(path);
            if (Files.isExecutable(candidate)) {
                return candidate;
            }
        }

        throw new EbookConverterNotFoundException();
    }
}
