package edu.self.w2k.gui;

import java.awt.Desktop;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.util.Optional;

import edu.self.w2k.config.AppInfo;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Dialog;
import javafx.scene.control.Hyperlink;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.VBox;
import lombok.extern.slf4j.Slf4j;

@Slf4j
final class AboutDialog extends Dialog<Void> {

    static final String DESCRIPTION = "Builds Kindle dictionaries from Wiktionary.";

    static final String LICENSE = "BSD 3-Clause License";

    private static final double ICON_SIZE = 64;

    AboutDialog(String version, DesktopPlatform platform) {
        setTitle(AppMenus.ABOUT);
        getDialogPane().getButtonTypes().add(platform == DesktopPlatform.WINDOWS ? ButtonType.OK : ButtonType.CLOSE);

        Label name = new Label(AppInfo.DISPLAY_NAME);
        name.getStyleClass().add("about-name");
        Label versionLabel = new Label("Version " + version);
        versionLabel.getStyleClass().add("about-secondary");
        Hyperlink website = new Hyperlink(AppInfo.WEBSITE);
        website.setOnAction(_ -> browse(AppInfo.WEBSITE));
        Label license = new Label(LICENSE);
        license.getStyleClass().add("about-secondary");

        VBox content = new VBox(name, versionLabel, new Label(DESCRIPTION), website, license);
        icon().ifPresent(image -> content.getChildren().addFirst(image));
        content.getStyleClass().add("about");
        getDialogPane().setContent(content);
    }

    private static Optional<ImageView> icon() {
        try (InputStream stream = AboutDialog.class.getResourceAsStream(App.ICON)) {
            if (stream == null) {
                return Optional.empty();
            }
            ImageView view = new ImageView(new Image(stream, ICON_SIZE, ICON_SIZE, true, true));
            return Optional.of(view);
        }
        catch (IOException e) {
            log.debug("Could not read the app icon: {}", e.getLocalizedMessage());
            return Optional.empty();
        }
    }

    /** Off the FX thread: Desktop calls can block on the platform browser. */
    private static void browse(String url) {
        Thread.ofVirtual().start(() -> {
            try {
                Desktop.getDesktop().browse(URI.create(url));
            }
            catch (IOException | UnsupportedOperationException e) {
                log.warn("Could not open {}: {}", url, e.getLocalizedMessage());
            }
        });
    }
}
