package edu.self.w2k.gui;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import edu.self.w2k.config.AppInfo;
import javafx.application.Platform;
import javafx.scene.Node;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Labeled;
import javafx.scene.image.ImageView;
import javafx.scene.layout.VBox;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

class AboutDialogTest {

    @BeforeAll
    static void startToolkit() throws InterruptedException {
        boolean ready;
        try {
            CountDownLatch started = new CountDownLatch(1);
            Platform.startup(started::countDown);
            ready = started.await(30, TimeUnit.SECONDS);
        }
        catch (IllegalStateException _) {
            ready = true;
        }
        catch (UnsupportedOperationException | Error _) {
            ready = false;
        }
        assumeTrue(ready, "No JavaFX toolkit available");
        Platform.setImplicitExit(false);
    }

    @Test
    void should_show_the_icon_name_version_and_website_when_opened() throws InterruptedException {
        // Given
        AtomicReference<AboutDialog> unit = new AtomicReference<>();
        CountDownLatch built = new CountDownLatch(1);

        // When
        Platform.runLater(() -> {
            unit.set(new AboutDialog("9.9.9", DesktopPlatform.GNOME));
            built.countDown();
        });

        // Then
        assertThat(built.await(30, TimeUnit.SECONDS)).isTrue();
        List<Node> content = ((VBox) unit.get().getDialogPane().getContent()).getChildren();
        assertThat(unit.get().getTitle()).isEqualTo("About Wiktionary to Kindle");
        assertThat(content.getFirst()).isInstanceOf(ImageView.class);
        assertThat(content.stream().filter(Labeled.class::isInstance).map(node -> ((Labeled) node).getText()))
                .containsExactly(AppInfo.DISPLAY_NAME, "Version 9.9.9", AboutDialog.DESCRIPTION, AppInfo.WEBSITE,
                                 AboutDialog.LICENSE);
        assertThat(unit.get().getDialogPane().getButtonTypes()).containsExactly(ButtonType.CLOSE);
    }

    @Test
    void should_close_with_ok_when_on_windows() throws InterruptedException {
        // Given
        AtomicReference<AboutDialog> unit = new AtomicReference<>();
        CountDownLatch built = new CountDownLatch(1);

        // When
        Platform.runLater(() -> {
            unit.set(new AboutDialog("9.9.9", DesktopPlatform.WINDOWS));
            built.countDown();
        });

        // Then
        assertThat(built.await(30, TimeUnit.SECONDS)).isTrue();
        assertThat(unit.get().getDialogPane().getButtonTypes()).containsExactly(ButtonType.OK);
    }
}
