package edu.self.w2k.gui;

import java.lang.reflect.AccessibleObject;
import java.lang.reflect.Method;
import java.util.List;

import lombok.extern.slf4j.Slf4j;

/**
 * JavaFX draws the macOS system font through CoreText, which picks SF's optical size for the point size, but spaces
 * Latin text with the font file's own advances, which belong to SF's 28pt display design: 13px labels came out
 * about 10% narrower than AppKit's, and bold no wider than regular. This gives JavaFX CoreText's advances instead.
 * It reaches into JavaFX internals, so any failure leaves JavaFX's own spacing in place.
 */
@Slf4j
@SuppressWarnings("java:S3011")
final class SystemFontAdvances {

    /** CoreText uses SF's 17pt design at every size up to 17pt, so advances taken at 13pt fit all of the app's text. */
    private static final float REFERENCE_SIZE = 13f;

    private static final List<String> FACES = List.of("System Regular", "System Bold");

    private static final String CORE_TEXT_FONT = "com.sun.javafx.font.coretext.CTFontFile";

    private SystemFontAdvances() {
    }

    static void install() {
        for (String face : FACES) {
            try {
                adopt(face);
            }
            catch (ReflectiveOperationException | RuntimeException e) {
                log.debug("Kept JavaFX's own spacing for {}", face, e);
            }
        }
    }

    private static void adopt(String face) throws ReflectiveOperationException {
        Object font = fontFile(face);
        if (!font.getClass().getName().equals(CORE_TEXT_FONT) || !postScriptName(font).startsWith(".")) {
            return;
        }
        Class<?> prismFontFile = Class.forName("com.sun.javafx.font.PrismFontFile");
        int glyphs = accessible(prismFontFile.getDeclaredField("numHMetrics")).getInt(font);
        float unitsPerEm = accessible(prismFontFile.getDeclaredField("upem")).getFloat(font);
        Method platformAdvance = accessible(font.getClass().getDeclaredMethod("getAdvanceFromPlatform", int.class, float.class));
        char[] advances = new char[glyphs];
        for (int glyph = 0; glyph < glyphs; glyph++) {
            float advance = (float) platformAdvance.invoke(font, glyph, REFERENCE_SIZE);
            advances[glyph] = (char) Math.round(advance * unitsPerEm / REFERENCE_SIZE);
        }
        accessible(prismFontFile.getDeclaredField("advanceWidths")).set(font, advances);
    }

    private static Object fontFile(String face) throws ReflectiveOperationException {
        Class<?> factoryClass = Class.forName("com.sun.javafx.font.PrismFontFactory");
        Object factory = factoryClass.getMethod("getFontFactory").invoke(null);
        Object font = factoryClass.getMethod("createFont", String.class, float.class).invoke(factory, face, REFERENCE_SIZE);
        Object resource = Class.forName("com.sun.javafx.font.PGFont").getMethod("getFontResource").invoke(font);
        Class<?> composite = Class.forName("com.sun.javafx.font.CompositeFontResource");
        return composite.isInstance(resource) ? composite.getMethod("getSlotResource", int.class).invoke(resource, 0) : resource;
    }

    private static String postScriptName(Object font) throws ReflectiveOperationException {
        return (String) Class.forName("com.sun.javafx.font.FontResource").getMethod("getPSName").invoke(font);
    }

    private static <T extends AccessibleObject> T accessible(T member) {
        member.setAccessible(true);
        return member;
    }
}
