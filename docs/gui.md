# GUI conventions

What the `edu.self.w2k.gui` package relies on that the code alone does not say. FXML and CSS live in `src/main/resources/edu/self/w2k/gui/`.

## Classes

- `Launcher` — plain `main`; never extends `Application`
- `App` — FXML load, appender install
- `MainViewModel` — state and derivations; depends on `javafx.base` only, so it is unit-testable without a toolkit. Anything worth testing belongs here
- `MainController` — bindings
- `PipelineService` / `PipelineTask` — threading and cancellation
- `ComboBoxFilter`, `LanguageConverter`, `WordLanguageConverter` — the language pickers
- `DesktopPlatform`, `AppMenus`, `SystemTheme` — per-desktop conventions and theming
- `UiLogAppender`, `LogClipboard`, `LogFile` — the log console and the log file
- `PreferencesDialog`, `AboutDialog`, `ProgressSnapshot`, `ByteSizes`

## Dumps table

Cell values are wired with explicit accessor lambdas. `PropertyValueFactory` introspects JavaBean names (`langProperty()`, then `getLang()`); `DumpFile` is a record exposing `lang()`, so the factory matches nothing and renders a blank column with no compile error and no exception. `MainFxmlLoadTest.should_render_a_value_in_every_dumps_column` locks this in.

## Language pickers

- **Editable, but not free text.** `ComboBoxFilter` puts a `FilteredList` behind each picker, so typing narrows the drop-down (code prefix or name substring, case-insensitively) and text matching nothing leaves an empty list. The value is only ever set from `LanguageConverter.fromString`, which resolves against the languages that picker currently offers and returns `null` for anything else; a null value disables Start through `MainViewModel.startable`. A code kaikki does not serve can therefore be *typed* but never *selected*, so it cannot reach the downloader and fail at the HTTP 404 (issue #83). Keep `fromString` returning `null` for unknown text: a fallback that builds a `Language` from the typed code is the whole hole.
- **Clearing the value rewrites the editor.** The skin calls `converter.toString(null)`, so `ComboBoxFilter` guards every text reaction re-entrantly and restores the text and caret. Without the guard, a keystroke matching nothing erases itself as it is typed.
- **`getItems()` is the filtered view**, and writing to it throws. Background refreshes go through `ComboBoxFilter.sourceOf`.
- **Focus opens the drop-down**, with the filter cleared. The arrow needs the `arrowPressed` guard in `ComboBoxFilter`: it toggles the popup on mouse *release*, so opening on the focus that the same press delivers would leave the toggle to close it again, and a click on the arrow would do nothing.
- **A drop-down scroll must not depend on what the pointer is over.** `VirtualFlow` registers its scroll handler on itself, so an event landing on the popup's own padding (AtlantaFX gives it 5px) bubbles past it and the list appears to stop scrolling part-way down (issue #85). `ComboBoxFilter.keepScrollingWhereverThePointerIs` handles `ScrollEvent.SCROLL` on the popup's `ListView` and applies anything still unconsumed to the flow. The `isConsumed` guard is what stops it double-scrolling: `VirtualFlow` consumes exactly when `scrollPixels` moved something. It hangs off `skinProperty` because the popup content does not exist until the control reaches a shown scene.
- **The word language picker is disabled until an edition is chosen.** Its languages are that edition's own, so before one is picked it could only offer the bundled fallback. `MainViewModel.wordLanguageSelectable` is the binding; `MainController.onEditionChanged` also clears the word language when the edition goes away, since the user cannot clear a disabled picker.
- **Both lists come from kaikki at runtime** through `KaikkiCatalog`, refreshed on a virtual thread and cached for 7 days under `AppPaths.cacheDir()/catalog`. `kaikki-editions.properties` and `LanguageCatalog.wordLanguages()` are the offline fallback. Every failure degrades silently at `debug`: the worst case equals the bundled lists, never a dialog or an empty drop-down.
- **The word language list is scoped to the selected edition**, because most of the ~184 ISO 639-1 codes produce an empty dictionary against any one edition, after a multi-gigabyte download.

## Themes

- **The theme is a preference; the platform only picks its default.** `AppTheme` lives in `config`, so `Preferences` stays JavaFX-free for the CLI. It has three values, `JAVAFX`, `CUPERTINO` and `PRIMER`, persisted as `theme=javafx|cupertino|primer`. `AppTheme.defaultFor(osName)` returns `CUPERTINO` on macOS and `PRIMER` everywhere else; an explicit choice wins, and an unreadable or unknown value falls back to the default.
- **Preferences restyles the open window with no restart.** `SystemTheme.install(scene, choice)` takes `MainController.themeChoice()`, which is `preferencesProperty().map(Preferences::theme)`. Under `CUPERTINO` or `PRIMER` it applies the light or dark variant (`SystemTheme.themeFor`) from `Platform.getPreferences().getColorScheme()` and re-applies from a listener on `colorSchemeProperty()`. Under `JAVAFX` it restores `Application.STYLESHEET_MODENA` **and removes `app-atlantafx.css` from the scene**: that sheet's rules name AtlantaFX tokens, which resolve to nothing under Modena. Any failure degrades at `debug` to the untouched default.
- **A colour token belongs to one theme.** `app.css` is written against Modena tokens and always loaded. `app-atlantafx.css` restates each rule whose tokens AtlantaFX does not define (notably `-fx-selection-bar`, without which selected log lines go unpainted) and is added only when an AtlantaFX theme is installed. A new colour goes in both sheets, each naming its own theme's token; a token from one theme is an unresolved lookup under the other. Give a rule in `app-atlantafx.css` the platform class in its selector wherever `app.css` has one, or the shared AtlantaFX rule outranks it.
- **Row metrics are restated per theme too.** `app.css` sets `-fx-fixed-cell-size: 24px`, measured against Modena. Cupertino pads a drop-down cell by 6px top and bottom, which cuts every language name in half at that size, and sizes list rows at `3em`, which makes log lines 34.5px instead of 16px. `MainFxmlLoadTest.should_keep_list_rows_compact_and_unclipped_under_the_cupertino_theme` measures real cells; it needs `Platform.setImplicitExit(false)`, since it shows a window and closing the last one would take the toolkit down for every later test.
- **`SystemTheme.stylesheetsFor`** is the single place that decides which app sheets go with a theme and in what order; `SystemTheme.apply` adds and removes them to match.
- **Dialogs are dressed by hand.** A dialog pane inherits the user agent stylesheet but none of the app's classes. `MainController.dressDialog` copies the main scene's stylesheets and the platform style class onto every dialog it opens.

### Cupertino

AtlantaFX's Cupertino follows iOS: 29px controls, white outlined push buttons, blue arrows on every combo box, a grey toolbar. `app-cupertino.css` replaces those with AppKit's values on macOS 27, sampled from controls rendered off-screen with `cacheDisplay`: 24px regular controls, borderless grey push buttons, a pop-up look for non-editable combo boxes and an NSComboBox look for editable ones (hairline field, chevron in a small filled box), borderless check boxes, one filled track for the page switcher, a white toolbar under a hairline and a 15px title. `app-cupertino-dark.css` restates only its tokens.

- Its rules are author rules, so they outrank every Cupertino rule they touch, variants included: a token set on `.button` replaces the one Cupertino gives `.button:default` too. The default button is restated for that reason, and any other Cupertino variant put to use needs the same.
- Focus rings follow AppKit: fields ring whenever focused, push buttons and pop-ups only on `:focus-visible`.
- CSS cannot reach the font. JavaFX's `System` family offers Regular, Bold and their italics, so semibold falls back to Regular, and SF's spacing is decided in code.

## Desktop conventions

Every per-desktop decision is read from `DesktopPlatform` (`MACOS`, `WINDOWS`, `GNOME`, `KDE`): title bar, menu style, labels, button order. `DesktopPlatform.current()` splits Linux by `XDG_CURRENT_DESKTOP`, a colon-separated list: any `KDE` entry means KDE, anything else GNOME. `MainController` takes the platform as a constructor argument, so `MainFxmlLoadTest` builds the window for all four on any host. `attachTo(stage)` applies what needs the stage and must run before the stage is shown. The root gets the platform's style class (`macos`, `windows`, `gnome`, `kde`), and every per-desktop stylesheet rule hangs off it.

| | macOS | Windows | GNOME | KDE |
|---|---|---|---|---|
| Stage | extended; the header is the title bar | decorated, for Snap Layouts | extended, drawn as a libadwaita header bar | decorated; KWin draws the title bar |
| Menus | system menu bar: File › Settings…, Help › About | in-window bar: File (Settings…, Exit), Help (About), with access keys | ☰ primary menu: Preferences, About | bar: File (Quit), Settings (Configure Wiktionary to Kindle…, Ctrl+Shift+,), Help (About) |
| Page switcher | centred, Finder-style | left, tabs underlined in the accent colour | centred view switcher | left, tabs as on Windows |
| Form | centred, right-aligned labels ending in a colon | left-aligned, no colons | boxed list on a grey page, no colons | centred, right-aligned labels with colons |
| Buttons | Cancel before the default | primary first | Cancel before the primary; borderless except the suggested one | primary first |
| Wording | title case | sentence case ("Build dictionary", "Open file location") | | title case |
| Body text | | 14px, as in WinUI | 11pt | 10pt |

- **macOS** — The title bar shows the app's name; the version is left to About. Passing the 52px toolbar height to `HeaderBar.setSystemButtonHeight` is what centres the traffic lights: without it macOS keeps the 28px title bar's 68×28 button area at the top of the taller header. Settings and About belong in the app menu, but JavaFX has no API for it. ⌘, is also put on the scene's accelerators, because macOS never hands the menu its key equivalent while a text field has focus, and the pickers are editable.
- **GNOME** — JavaFX paints the round Adwaita-style window buttons itself (`WindowDecorationGnome.css` in the Linux graphics jar). The primary menu carries no Quit, so Ctrl+Q is bound on the scene. `HeaderBar` centres its centre slot between the side slots, not on the bar, so `balancingSpacer` puts a left spacer as wide as the menu button plus the window buttons.

## Window mechanics

- **Pages** are a `ToggleGroup` of two segments over a `StackPane`, not a `TabPane`. The group's listener re-selects the previous segment when the selected one is clicked, since a toggle group otherwise allows none and the window would go blank. Where the switcher leads the header (Windows, KDE) it moves to the left slot, because the centre slot keeps its child centred.
- **Header dragging**: on an extended stage only the `HeaderBar`'s own background drags the window, and a child blocks it unless it carries a `HeaderBar.dragType`. The title takes `DRAGGABLE_SUBTREE`, so its text drags too; the switcher and GNOME's balancing spacer take `DRAGGABLE`, which leaves the buttons inside them clickable. JavaFX has no public hit test, so `MainFxmlLoadTest` reaches `Scene$ScenePeerListener.pickHeaderArea`, the one glass calls, through reflection.
- **The menu bar is built in code** (`AppMenus`). `useSystemMenuBar` is set only on macOS, where the bar stays visible, because its skin is what installs the system menus, but unmanaged, or its padding leaves a strip across the top of the window.
- **The form is a column of `form-row` HBoxes**, not a `GridPane`, so the stylesheet alone can turn it into a right-aligned column, a left-aligned one or a boxed list.
- **The log is a disclosure**, closed at launch and opened by a failure. `resizeWindowForLog` grows or shrinks the window by its height, since the launch window is sized without it.
- **The status and progress bar** live in the footer and are hidden, not unmanaged: that box is also the spacer between the buttons.

## Logging

The GUI adds `UiLogAppender` and a file appender programmatically; `logback.xml` stays CLI-only.

- **`LogFile.install`** is a `RollingFileAppender` rolling daily: active file `app.log`, each day's predecessor compressed beside it as `app-{YYYY-MM-DD}.log.gz`, `maxHistory` 14 days, `totalSizeCap` 200 MB. It appends, so a crash reproduced on a second run keeps the evidence of the first. `cleanHistoryOnStart` is set because a rolling policy prunes only when it rolls, and a desktop app opened and closed within a day may go weeks without rolling. It is separate from `App` so it can be tested: `App` is in `sonar.coverage.exclusions`. A bare `LoggerContext` in a test needs `setMDCAdapter(new LogbackMDCAdapter())`, or every append fails on a null adapter.
- **`UiLogAppender`** buffers into a bounded queue the UI drains in batches. One `Platform.runLater` per event freezes the window. The drain `Timeline` parks itself after about 2 s idle, and `UiLogAppender.setWakeListener` restarts it, so an idle window is not held awake by a running animation.
- **`LogClipboard`** makes the pane's lines copyable (multiple selection, `Shortcut+C`, a Copy / Copy all context menu), because a `ListView` carries no copy behaviour of its own.
