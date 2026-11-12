package edu.self.w2k.gui;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assumptions.assumeTrue;
import static org.mockito.Mockito.verify;

import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import javafx.application.Platform;
import javafx.scene.control.Menu;
import javafx.scene.control.MenuItem;
import javafx.scene.input.KeyCombination;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class AppMenusTest {

    @Mock
    private Runnable settings;

    @Mock
    private Runnable about;

    @Mock
    private Runnable quit;

    /**
     * A separator item builds a {@code Control}, whose static initialiser needs the toolkit. Reached
     * without one, it fails for good and takes every later toolkit test in the same fork with it.
     */
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
    void should_put_settings_under_file_and_about_under_help_when_on_macos() {
        // Given
        AppMenus.Actions actions = new AppMenus.Actions(settings, about, quit);

        // When
        List<Menu> menus = AppMenus.menuBar(DesktopPlatform.MACOS, actions);

        // Then
        assertThat(menus).extracting(Menu::getText).containsExactly("File", "Help");
        assertThat(menus.getFirst().getItems()).extracting(MenuItem::getText).containsExactly("Settings…");
        assertThat(menus.getFirst().getItems().getFirst().getAccelerator())
                .isEqualTo(KeyCombination.keyCombination("Shortcut+Comma"));
        assertThat(menus.getLast().getItems()).extracting(MenuItem::getText)
                .containsExactly("About Wiktionary to Kindle");
    }

    @Test
    void should_put_exit_under_file_and_about_under_help_when_on_windows() {
        // Given
        AppMenus.Actions actions = new AppMenus.Actions(settings, about, quit);

        // When
        List<Menu> menus = AppMenus.menuBar(DesktopPlatform.WINDOWS, actions);

        // Then
        assertThat(menus).extracting(Menu::getText).containsExactly("_File", "_Help");
        assertThat(menus.getFirst().getItems()).extracting(MenuItem::getText)
                .containsExactly("_Settings…", null, "E_xit");
        assertThat(menus.getLast().getItems()).extracting(MenuItem::getText)
                .containsExactly("_About Wiktionary to Kindle");
    }

    @Test
    void should_put_configure_under_settings_when_on_kde() {
        // Given
        AppMenus.Actions actions = new AppMenus.Actions(settings, about, quit);

        // When
        List<Menu> menus = AppMenus.menuBar(DesktopPlatform.KDE, actions);

        // Then
        assertThat(menus).extracting(Menu::getText).containsExactly("_File", "_Settings", "_Help");
        assertThat(menus.get(1).getItems().getFirst().getText()).isEqualTo("_Configure Wiktionary to Kindle…");
        assertThat(menus.get(1).getItems().getFirst().getAccelerator())
                .isEqualTo(KeyCombination.keyCombination("Shortcut+Shift+Comma"));
        assertThat(menus.getFirst().getItems().getFirst().getAccelerator())
                .isEqualTo(KeyCombination.keyCombination("Shortcut+Q"));
    }

    @Test
    void should_move_everything_into_the_primary_menu_when_on_gnome() {
        // Given
        AppMenus.Actions actions = new AppMenus.Actions(settings, about, quit);

        // When
        List<Menu> menuBar = AppMenus.menuBar(DesktopPlatform.GNOME, actions);
        List<MenuItem> primaryMenu = AppMenus.primaryMenu(DesktopPlatform.GNOME, actions);

        // Then
        assertThat(menuBar).isEmpty();
        assertThat(primaryMenu).extracting(MenuItem::getText)
                .containsExactly("Preferences", null, "About Wiktionary to Kindle");
    }

    @Test
    void should_leave_the_primary_menu_empty_when_not_on_gnome() {
        // Given
        AppMenus.Actions actions = new AppMenus.Actions(settings, about, quit);

        // When
        List<MenuItem> primaryMenu = AppMenus.primaryMenu(DesktopPlatform.KDE, actions);

        // Then
        assertThat(primaryMenu).isEmpty();
    }

    @Test
    void should_run_the_matching_action_when_an_item_fires() {
        // Given
        AppMenus.Actions actions = new AppMenus.Actions(settings, about, quit);
        List<Menu> menus = AppMenus.menuBar(DesktopPlatform.WINDOWS, actions);

        // When
        menus.getFirst().getItems().getFirst().fire();
        menus.getFirst().getItems().getLast().fire();
        menus.getLast().getItems().getFirst().fire();

        // Then
        verify(settings).run();
        verify(quit).run();
        verify(about).run();
    }
}
