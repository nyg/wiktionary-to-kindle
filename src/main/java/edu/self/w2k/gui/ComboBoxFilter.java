package edu.self.w2k.gui;

import java.util.List;
import java.util.Locale;
import java.util.concurrent.atomic.AtomicBoolean;

import edu.self.w2k.config.LanguageCatalog.Language;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.event.EventTarget;
import javafx.scene.Node;
import javafx.scene.control.ComboBox;
import javafx.scene.control.ListView;
import javafx.scene.control.Skin;
import javafx.scene.control.TextField;
import javafx.scene.control.skin.ComboBoxListViewSkin;
import javafx.scene.control.skin.VirtualFlow;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.MouseEvent;
import javafx.scene.input.ScrollEvent;
import javafx.util.StringConverter;

/**
 * Turns a language picker into a filter box: what the user types narrows the drop-down, and only an
 * entry that survives the filter can become the value.
 * <p>
 * The picker is editable, but that does not make it free text. Typing sets the value only when the
 * text resolves through the converter to one of the offered languages; anything else leaves the value
 * {@code null}, which disables the Start button. An unserved edition therefore still cannot reach the
 * downloader — the guarantee non-editable pickers used to provide.
 * <p>
 * The {@code syncing} flag is what makes this workable. Clearing the value makes the skin rewrite the
 * editor with {@code converter.toString(null)}, so a keystroke that matches nothing would erase itself
 * as it was typed. Every reaction to a text change therefore runs re-entrant-guarded, restoring the
 * text and caret the user actually has.
 */
public final class ComboBoxFilter {

    private ComboBoxFilter() {}

    public static void install(ComboBox<Language> combo) {
        FilteredList<Language> filtered =
                new FilteredList<>(FXCollections.observableArrayList(combo.getItems()));
        combo.setItems(filtered);
        combo.setEditable(true);

        TextField editor = combo.getEditor();
        AtomicBoolean syncing = new AtomicBoolean();

        editor.textProperty().addListener((_, _, text) -> {
            if (syncing.compareAndSet(false, true)) {
                try {
                    apply(combo, filtered, editor, text);
                }
                finally {
                    syncing.set(false);
                }
            }
        });

        AtomicBoolean arrowPressed = new AtomicBoolean();
        combo.addEventFilter(MouseEvent.MOUSE_PRESSED, event -> arrowPressed.set(onArrow(event.getTarget())));
        combo.addEventFilter(MouseEvent.MOUSE_RELEASED, _ -> arrowPressed.set(false));

        editor.focusedProperty().addListener((_, _, focused) -> {
            if (Boolean.FALSE.equals(focused)) {
                return;
            }
            editor.selectAll();
            if (!arrowPressed.getAndSet(false)) {
                combo.show();
                filtered.setPredicate(null);
            }
        });

        combo.addEventHandler(KeyEvent.KEY_PRESSED, event -> {
            if (event.getCode() == KeyCode.ESCAPE) {
                reset(combo, filtered, editor, syncing);
            }
        });

        combo.skinProperty().addListener((_, _, skin) -> keepScrollingWhereverThePointerIs(skin));
        keepScrollingWhereverThePointerIs(combo.getSkin());
    }

    private static boolean onArrow(EventTarget target) {
        for (Node node = target instanceof Node n ? n : null; node != null; node = node.getParent()) {
            if (node.getStyleClass().contains("arrow-button") || node.getStyleClass().contains("arrow")) {
                return true;
            }
        }
        return false;
    }

    private static void keepScrollingWhereverThePointerIs(Skin<?> skin) {
        if (!(skin instanceof ComboBoxListViewSkin<?> comboSkin)
                || !(comboSkin.getPopupContent() instanceof ListView<?> popup)) {
            return;
        }
        popup.addEventHandler(ScrollEvent.SCROLL, event -> {
            if (event.isConsumed() || event.getDeltaY() == 0
                    || !(popup.lookup(".virtual-flow") instanceof VirtualFlow<?> flow)) {
                return;
            }
            if (flow.scrollPixels(-event.getDeltaY()) != 0) {
                event.consume();
            }
        });
    }

    /**
     * The unfiltered backing list. {@code getItems()} is the filtered view once {@link #install} has
     * run, and writing to that throws.
     */
    @SuppressWarnings("unchecked")
    public static ObservableList<Language> sourceOf(ComboBox<Language> combo) {
        return combo.getItems() instanceof FilteredList<Language> filtered
                ? (ObservableList<Language>) filtered.getSource()
                : combo.getItems();
    }

    static boolean matches(Language language, String query) {
        if (query == null || query.isBlank()) {
            return true;
        }
        String needle = query.strip().toLowerCase(Locale.ROOT);
        return language.code().toLowerCase(Locale.ROOT).startsWith(needle)
                || language.displayName().toLowerCase(Locale.ROOT).contains(needle);
    }

    static List<Language> filter(List<Language> items, String query) {
        return items.stream().filter(language -> matches(language, query)).toList();
    }

    private static void apply(ComboBox<Language> combo, FilteredList<Language> filtered,
                              TextField editor, String text) {
        StringConverter<Language> converter = combo.getConverter();
        Language selected = combo.getValue();
        if (selected != null && converter.toString(selected).equals(text)) {
            filtered.setPredicate(null);
            return;
        }

        int caret = editor.getCaretPosition();
        filtered.setPredicate(language -> matches(language, text));
        combo.setValue(converter.fromString(text));

        if (!text.equals(editor.getText())) {
            editor.setText(text);
        }
        editor.positionCaret(Math.min(caret, text.length()));

        if (filtered.isEmpty() || !editor.isFocused()) {
            combo.hide();
        }
        else {
            combo.show();
        }
    }

    private static void reset(ComboBox<Language> combo, FilteredList<Language> filtered,
                              TextField editor, AtomicBoolean syncing) {
        if (!syncing.compareAndSet(false, true)) {
            return;
        }
        try {
            filtered.setPredicate(null);
            String text = combo.getConverter().toString(combo.getValue());
            editor.setText(text);
            editor.positionCaret(text.length());
        }
        finally {
            syncing.set(false);
        }
        combo.hide();
    }
}
