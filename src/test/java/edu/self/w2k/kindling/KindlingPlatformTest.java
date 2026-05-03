package edu.self.w2k.kindling;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@ExtendWith(MockitoExtension.class)
class KindlingPlatformTest {

    @Test
    void should_return_linux_x64_when_os_is_linux_amd64() throws KindlingException {
        // When
        KindlingPlatform result = KindlingPlatform.detect("Linux", "amd64");

        // Then
        assertThat(result).isEqualTo(KindlingPlatform.LINUX_X64);
        assertThat(result.assetName()).isEqualTo("kindling-cli-linux");
    }

    @Test
    void should_throw_when_platform_is_unsupported() {
        // When / Then
        assertThatThrownBy(() -> KindlingPlatform.detect("SunOS", "sparc"))
                .isInstanceOf(KindlingException.class);
    }
}
