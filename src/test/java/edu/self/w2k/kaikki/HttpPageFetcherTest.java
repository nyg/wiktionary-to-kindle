package edu.self.w2k.kaikki;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIOException;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpResponse;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class HttpPageFetcherTest {

    @Mock
    HttpClient client;

    @Mock
    HttpResponse<String> response;

    @Test
    void should_return_the_body_when_the_request_succeeds() throws Exception {
        // Given
        when(response.statusCode()).thenReturn(200);
        when(response.body()).thenReturn("<html>editions</html>");
        doReturn(response).when(client).send(any(), any());
        KaikkiCatalog.HttpPageFetcher unit = new KaikkiCatalog.HttpPageFetcher(client);

        // When
        String body = unit.fetch(URI.create("https://kaikki.org/"));

        // Then
        assertThat(body).isEqualTo("<html>editions</html>");
    }

    @Test
    void should_fail_when_the_server_does_not_return_200() throws Exception {
        // Given
        when(response.statusCode()).thenReturn(404);
        doReturn(response).when(client).send(any(), any());
        KaikkiCatalog.HttpPageFetcher unit = new KaikkiCatalog.HttpPageFetcher(client);

        // When / Then
        assertThatIOException()
                .isThrownBy(() -> unit.fetch(URI.create("https://kaikki.org/nope/")))
                .withMessageContaining("HTTP 404");
    }

    @Test
    void should_restore_the_interrupt_flag_when_the_request_is_interrupted() throws Exception {
        // Given
        doThrow(new InterruptedException("stopped")).when(client).send(any(), any());
        KaikkiCatalog.HttpPageFetcher unit = new KaikkiCatalog.HttpPageFetcher(client);

        try {
            // When / Then
            assertThatIOException()
                    .isThrownBy(() -> unit.fetch(URI.create("https://kaikki.org/")))
                    .withMessageContaining("interrupted");
        }
        finally {
            Thread.interrupted(); // clear the flag so it cannot leak into other tests
        }
    }
}
