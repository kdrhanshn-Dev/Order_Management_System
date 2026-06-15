package org.example.ui;

import javafx.application.Platform;
import javafx.scene.control.TextField;
import javafx.scene.control.TextFormatter;
import javafx.util.StringConverter;

import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;
import java.util.function.UnaryOperator;

/** JavaFX yardımcıları: numeric TextFormatter'lar ve güvenli arka plan çalıştırma. */
public final class UiFX {

    private UiFX() {}

    /** Yalnızca [min, max] aralığında TAMSAYI kabul eden TextFormatter. Boş değere izin verir. */
    public static TextFormatter<Integer> integerFormatter(int min, int max) {
        final StringConverter<Integer> conv = new StringConverter<>() {
            @Override public String toString(Integer v) { return v == null ? "" : v.toString(); }
            @Override public Integer fromString(String s) {
                if (s == null || s.isBlank()) return null;
                return Integer.parseInt(s);
            }
        };
        final UnaryOperator<TextFormatter.Change> filter = change -> {
            String nt = change.getControlNewText();
            if (nt == null || nt.isBlank() || nt.equals("-")) return change;
            try {
                int v = Integer.parseInt(nt);
                return (v < min || v > max) ? null : change;
            } catch (NumberFormatException e) {
                return null;
            }
        };
        return new TextFormatter<>(conv, null, filter);
    }

    /** Yalnızca [min, +∞) ARALIKLI ONDALIK sayı kabul eden TextFormatter. Boş değere izin verir. */
    public static TextFormatter<Double> doubleFormatter(double min) {
        final StringConverter<Double> conv = new StringConverter<>() {
            @Override public String toString(Double v) { return v == null ? "" : v.toString(); }
            @Override public Double fromString(String s) {
                if (s == null || s.isBlank() || s.equals("-") || s.equals(".")) return null;
                return Double.parseDouble(s);
            }
        };
        final UnaryOperator<TextFormatter.Change> filter = change -> {
            String nt = change.getControlNewText();
            if (nt == null || nt.isBlank() || nt.equals("-") || nt.equals(".")) return change;
            try {
                double v = Double.parseDouble(nt);
                return (v < min) ? null : change;
            } catch (NumberFormatException e) {
                return null;
            }
        };
        return new TextFormatter<>(conv, null, filter);
    }

    /** TextField → güvenli int parse (null ise defaultValue döner). */
    public static int parseIntOr(TextField tf, int defaultValue) {
        try {
            String s = tf.getText();
            if (s == null || s.isBlank()) return defaultValue;
            return Integer.parseInt(s.trim());
        } catch (Exception e) { return defaultValue; }
    }
    /** TextField → güvenli double parse (null ise defaultValue döner). */
    public static double parseDoubleOr(TextField tf, double defaultValue) {
        try {
            String s = tf.getText();
            if (s == null || s.isBlank()) return defaultValue;
            return Double.parseDouble(s.trim());
        } catch (Exception e) { return defaultValue; }
    }

    /**
     * IO/DB işi UI dışı bir thread’de çalıştır, bitince UI thread’e dön.
     * @param ioTask    : ağır iş (DB/IO) — UI thread dışında koşar
     * @param onUiDone  : başarıyla bitince UI thread’de çağrılır (opsiyonel)
     * @param onUiError : hata olursa UI thread’de çağrılır (opsiyonel)
     */
    public static void runAsync(Runnable ioTask, Runnable onUiDone, Consumer<Throwable> onUiError) {
        CompletableFuture
                .runAsync(ioTask)
                .whenComplete((v, ex) -> Platform.runLater(() -> {
                    if (ex == null) {
                        if (onUiDone != null) onUiDone.run();
                    } else {
                        if (onUiError != null) onUiError.accept(ex);
                        else ex.printStackTrace();
                    }
                }));
    }
}

