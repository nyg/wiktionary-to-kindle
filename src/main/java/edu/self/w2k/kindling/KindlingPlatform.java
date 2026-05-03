package edu.self.w2k.kindling;

import java.util.Locale;

public enum KindlingPlatform {
    LINUX_X64("kindling-cli-linux"),
    MAC_APPLE_SILICON("kindling-cli-mac-apple-silicon"),
    MAC_INTEL("kindling-cli-mac-intel"),
    WINDOWS_X64("kindling-cli-windows.exe");

    private final String assetName;

    KindlingPlatform(String assetName) {
        this.assetName = assetName;
    }

    public String assetName() {
        return assetName;
    }

    public static KindlingPlatform detect() throws KindlingException {
        return detect(System.getProperty("os.name", ""), System.getProperty("os.arch", ""));
    }

    static KindlingPlatform detect(String osName, String arch) throws KindlingException {
        String os = osName.toLowerCase(Locale.ROOT);
        String archLc = arch.toLowerCase(Locale.ROOT);

        if (os.contains("linux")) {
            if (archLc.equals("amd64") || archLc.equals("x86_64")) return LINUX_X64;
            throw new KindlingException(
                    "Linux ARM64 not supported by kindling; pass --kindling-cli to use a self-built binary.");
        }
        if (os.contains("mac") || os.contains("darwin")) {
            return archLc.equals("aarch64") ? MAC_APPLE_SILICON : MAC_INTEL;
        }
        if (os.contains("windows") && (archLc.equals("amd64") || archLc.equals("x86_64"))) {
            return WINDOWS_X64;
        }
        throw new KindlingException(
                "Unsupported platform: " + osName + "/" + arch + "; pass --kindling-cli to use a self-built binary.");
    }
}
