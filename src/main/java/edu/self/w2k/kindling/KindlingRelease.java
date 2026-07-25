package edu.self.w2k.kindling;

import java.util.Map;

public final class KindlingRelease {

    public static final String DEFAULT_VERSION = "v0.28.0";

    public record Asset(String fileName, String sha256) {}

    public static final Map<KindlingPlatform, Asset> DEFAULT_ASSETS = Map.of(
            KindlingPlatform.LINUX_X64,
                new Asset("kindling-cli-linux",
                        "de9b13813100e246ab5681e4d82b78e6c6f4248337cee17ae38ff4165ef11266"),
            KindlingPlatform.MAC_APPLE_SILICON,
                new Asset("kindling-cli-mac-apple-silicon",
                        "889ea08dca2ebd5d39c39ac2a03f0e59351185a6229c170fca0a9689df9e0bfb"),
            KindlingPlatform.MAC_INTEL,
                new Asset("kindling-cli-mac-intel",
                        "a22d6d76cea226bcfd724489d219b56d5fcf5989cf925bb726fb77961a1cc24e"),
            KindlingPlatform.WINDOWS_X64,
                new Asset("kindling-cli-windows.exe",
                        "64d6a32b169ddc552c2695367ec3091848223f2c68c356a057a9d2cd2faedf79"));

    private KindlingRelease() {}
}
