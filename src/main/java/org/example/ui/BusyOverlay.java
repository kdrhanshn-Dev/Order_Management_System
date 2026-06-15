package org.example.ui;

import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;

/** Basit bir “meşgul” overlay: panelin üstünde yarı saydam perde + ProgressIndicator. */
public class BusyOverlay extends StackPane {

    private final Rectangle veil = new Rectangle();
    private final ProgressIndicator spinner = new ProgressIndicator();
    private final Label label = new Label("İşleniyor...");

    public BusyOverlay() {
        setPickOnBounds(true); // overlay görünürken tıklamaları yakala
        setVisible(false);

        veil.setFill(Color.rgb(0,0,0,0.15));
        veil.widthProperty().bind(widthProperty());
        veil.heightProperty().bind(heightProperty());

        spinner.setMaxSize(80, 80);
        StackPane.setAlignment(spinner, Pos.CENTER);

        label.setTextFill(Color.web("#222"));
        StackPane.setAlignment(label, Pos.BOTTOM_CENTER);

        getChildren().addAll(veil, spinner, label);
    }

    public void show() { setVisible(true); }
    public void hide() { setVisible(false); }
}
