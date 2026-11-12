package edu.self.w2k.gui;

import java.util.List;

import edu.self.w2k.config.AppInfo;
import javafx.scene.control.Menu;
import javafx.scene.control.MenuItem;
import javafx.scene.control.SeparatorMenuItem;
import javafx.scene.input.KeyCombination;

/**
 * Each desktop's menus. Windows and KDE menus carry access keys; macOS and GNOME menus have none,
 * so their labels are left without underscores.
 */
final class AppMenus {

    static final String ABOUT = "About " + AppInfo.DISPLAY_NAME;

    private static final String SETTINGS_SHORTCUT = "Shortcut+Comma";

    record Actions(Runnable settings, Runnable about, Runnable quit) {
    }

    private AppMenus() {
    }

    static List<Menu> menuBar(DesktopPlatform platform, Actions actions) {
        return switch (platform) {
            case MACOS -> List.of(menu("File", item("Settings…", SETTINGS_SHORTCUT, actions.settings())));
            case WINDOWS -> List.of(menu("_File",
                                         item("_Settings…", SETTINGS_SHORTCUT, actions.settings()),
                                         new SeparatorMenuItem(),
                                         item("E_xit", null, actions.quit())),
                                    menu("_Help", item("_" + ABOUT, null, actions.about())));
            case KDE -> List.of(menu("_File", item("_Quit", "Shortcut+Q", actions.quit())),
                                menu("_Settings",
                                     item("_Configure " + AppInfo.DISPLAY_NAME + "…", "Shortcut+Shift+Comma",
                                          actions.settings())),
                                menu("_Help", item("_" + ABOUT, null, actions.about())));
            case GNOME -> List.of();
        };
    }

    static List<MenuItem> primaryMenu(DesktopPlatform platform, Actions actions) {
        if (platform != DesktopPlatform.GNOME) {
            return List.of();
        }
        return List.of(item("Preferences", SETTINGS_SHORTCUT, actions.settings()),
                       new SeparatorMenuItem(),
                       item(ABOUT, null, actions.about()));
    }

    private static Menu menu(String text, MenuItem... items) {
        return new Menu(text, null, items);
    }

    private static MenuItem item(String text, String accelerator, Runnable action) {
        MenuItem item = new MenuItem(text);
        if (accelerator != null) {
            item.setAccelerator(KeyCombination.keyCombination(accelerator));
        }
        item.setOnAction(_ -> action.run());
        return item;
    }
}
