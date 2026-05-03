package edu.self.w2k.kindling;

import java.util.Map;

public final class KindlingRelease {

    public static final String DEFAULT_VERSION = "v0.14.5";

    public record Asset(String fileName, String sha256) {}

    public static final Map<KindlingPlatform, Asset> DEFAULT_ASSETS = Map.of(
            KindlingPlatform.LINUX_X64,
                new Asset("kindling-cli-linux",
                        "11369537b1d82bd835a06ffa940e8e75a5ee034e5eaee5d7ff4352c15b66f137"),
            KindlingPlatform.MAC_APPLE_SILICON,
                new Asset("kindling-cli-mac-apple-silicon",
                        "ac15153df818e4fea2608627a5f5cb2ee59f762be02c535160a8b6871f29ea3c"),
            KindlingPlatform.MAC_INTEL,
                new Asset("kindling-cli-mac-intel",
                        "a7d1657beacd30ec6016caf44b1738bad679783a06ef8dd6a08b9a961a9bc0c6"),
            KindlingPlatform.WINDOWS_X64,
                new Asset("kindling-cli-windows.exe",
                        "dc06c1059682949eacc17d49c1ebb03c1b70a60ca8c2b8f0f508ba0fe622d62c"));

    private KindlingRelease() {}
}
