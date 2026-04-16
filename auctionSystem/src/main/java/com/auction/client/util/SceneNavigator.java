package com.auction.client.util;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.layout.AnchorPane;
import javafx.stage.Stage;
import java.io.IOException;
import java.util.function.Consumer;
public final class SceneNavigator {
    public static <T> void switchScene(Node sourceNode, String fxmlPath, String title, double w, double h, Consumer<T> init) throws IOException {
        FXMLLoader loader = new FXMLLoader(SceneNavigator.class.getResource(fxmlPath));
        AnchorPane view = loader.load();
        if (init != null) init.accept(loader.getController());
        Stage stage = (Stage) sourceNode.getScene().getWindow();
        stage.setScene(new Scene(view, w, h));
        stage.setTitle(title);
    }
    public static void switchScene(Node sourceNode, String fxmlPath, String title, double w, double h) throws IOException {
        switchScene(sourceNode, fxmlPath, title, w, h, null);
    }
}
