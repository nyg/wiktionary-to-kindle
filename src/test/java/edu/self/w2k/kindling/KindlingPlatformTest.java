package edu.self.w2k.kindling;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class KindlingPlatformTest {

    @Test
    void linux_amd64() throws KindlingException {
        assertEquals(KindlingPlatform.LINUX_X64, KindlingPlatform.detect("Linux", "amd64"));
    }

    @Test
    void linux_x86_64() throws KindlingException {
        assertEquals(KindlingPlatform.LINUX_X64, KindlingPlatform.detect("Linux", "x86_64"));
    }

    @Test
    void linux_aarch64_throwsWithArm64Message() {
        KindlingException ex = assertThrows(KindlingException.class,
                () -> KindlingPlatform.detect("Linux", "aarch64"));
        assertTrue(ex.getMessage().contains("ARM64"), "message must mention ARM64");
    }

    @Test
    void macos_aarch64() throws KindlingException {
        assertEquals(KindlingPlatform.MAC_APPLE_SILICON, KindlingPlatform.detect("Mac OS X", "aarch64"));
    }

    @Test
    void macos_x86_64() throws KindlingException {
        assertEquals(KindlingPlatform.MAC_INTEL, KindlingPlatform.detect("Mac OS X", "x86_64"));
    }

    @Test
    void windows_amd64() throws KindlingException {
        assertEquals(KindlingPlatform.WINDOWS_X64, KindlingPlatform.detect("Windows 10", "amd64"));
    }

    @Test
    void unsupportedPlatform_throws() {
        assertThrows(KindlingException.class, () -> KindlingPlatform.detect("SunOS", "sparc"));
    }
}
