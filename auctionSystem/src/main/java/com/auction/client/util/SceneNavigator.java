package com.auction.client.util;

import java.io.IOException;
import java.util.function.Consumer;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.layout.AnchorPane;
import javafx.stage.Stage;

/**
 * Tiện ích hỗ trợ chuyển đổi giữa các màn hình trong ứng dụng JavaFX.
 */
public final class SceneNavigator {
  private SceneNavigator() {
    throw new UnsupportedOperationException("Không thể khởi tạo trực tiếp Scene Navigator");
  }

  /**
   * Chuyển đổi sang một màn hình mới.
   *
   * @param sourceNode Node hiện tại.
   * @param fxmlPath   Đường dẫn đến file FXML.
   * @param title      Tiêu đề cửa sổ.
   * @param w          Chiều rộng.
   * @param h          Chiều cao.
   * @param init       Hành động khởi tạo controller.
   * @param <T>        Kiểu của controller.
   * @throws IOException Nếu không thể tải file FXML.
   */
  public static <T> void switchScene(Node sourceNode, String fxmlPath, String title,
                                     double w, double h,
                                     Consumer<T> init) throws IOException {
    FXMLLoader loader = new FXMLLoader(SceneNavigator.class.getResource(fxmlPath));
    AnchorPane view = loader.load();
    if (init != null) {
      init.accept(loader.getController());
    }
    Stage stage = (Stage) sourceNode.getScene().getWindow();
    stage.setScene(new Scene(view, w, h));
    stage.setTitle(title);
  }

  /**
   * Chuyển đổi sang một màn hình mới (không có khởi tạo controller).
   *
   * @param sourceNode Node hiện tại.
   * @param fxmlPath   Đường dẫn đến file FXML.
   * @param title      Tiêu đề cửa sổ.
   * @param w          Chiều rộng.
   * @param h          Chiều cao.
   * @throws IOException Nếu không thể tải file FXML.
   */
  public static void switchScene(Node sourceNode, String fxmlPath, String title, double w, double h)
      throws IOException {
    switchScene(sourceNode, fxmlPath, title, w, h, null);
  }
}
