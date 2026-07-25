package edu.self.w2k.kindling;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.util.EnumMap;
import java.util.Map;
import java.util.Properties;

public record KindlingRelease(String version, Map<KindlingPlatform, String> digests) {

    static final String RESOURCE_NAME = "/kindling-release.properties";

    public static KindlingRelease load() {
        try (InputStream in = KindlingRelease.class.getResourceAsStream(RESOURCE_NAME)) {
            if (in == null) {
                throw new IllegalStateException("Classpath resource " + RESOURCE_NAME + " is missing");
            }
            return parse(in);
        } catch (IOException e) {
            throw new UncheckedIOException("Could not read " + RESOURCE_NAME, e);
        }
    }

    static KindlingRelease parse(InputStream in) throws IOException {
        Properties props = new Properties();
        props.load(in);

        String version = props.getProperty("version");
        if (version == null || version.isBlank()) {
            throw new IllegalStateException(RESOURCE_NAME + " has no version property");
        }

        Map<KindlingPlatform, String> digests = new EnumMap<>(KindlingPlatform.class);
        for (KindlingPlatform platform : KindlingPlatform.values()) {
            String key = "sha256." + platform.assetName();
            String digest = props.getProperty(key);
            if (digest == null || !digest.matches("[0-9a-f]{64}")) {
                throw new IllegalStateException(RESOURCE_NAME + " is missing a valid " + key + " property");
            }
            digests.put(platform, digest);
        }
        return new KindlingRelease(version.strip(), Map.copyOf(digests));
    }

    public String digest(KindlingPlatform platform) {
        return digests.get(platform); // never null: parse() validates every enum value
    }
}
