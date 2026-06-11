package net.alan.senlo.gui;

import javafx.animation.FadeTransition;
import javafx.animation.ParallelTransition;
import javafx.animation.ScaleTransition;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.geometry.Rectangle2D;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.stage.Screen;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import javafx.util.Duration;
import net.alan.senlo.config.Config;

import java.io.InputStream;

public class SplashScreen {
    private final Stage stage;
    private final Label statusLabel;
    private final Label subStatusLabel;
    private final StackPane root;

    // 浅色主题
    private static final String COLOR_BACKGROUND = "#F5F5F5";
    private static final String COLOR_SURFACE = "#FFFFFF";
    private static final String COLOR_PRIMARY = "#0063B1";
    private static final String COLOR_ON_SURFACE = "#202124";
    private static final String COLOR_ON_SURFACE_VARIANT = "#5F6368";
    private static final String COLOR_OUTLINE = "#E0E0E0";

    public SplashScreen() {
        stage = new Stage();
        stage.initStyle(StageStyle.TRANSPARENT);
        stage.setAlwaysOnTop(true);

        root = new StackPane();
        root.setStyle("-fx-background-color: " + COLOR_BACKGROUND + ";");
        root.setPrefSize(520, 320);

        // 主卡片：纯白，无圆角，细边框
        BorderPane card = new BorderPane();
        card.setStyle("-fx-background-color: " + COLOR_SURFACE + ";" +
                "-fx-background-radius: 0;" +
                "-fx-border-radius: 0;" +
                "-fx-border-color: " + COLOR_OUTLINE + ";" +
                "-fx-border-width: 1px;");

        VBox content = new VBox(20);
        content.setAlignment(Pos.CENTER);
        content.setPadding(new Insets(40, 48, 40, 48));

        // 图标区域（优先使用应用图标，否则显示文字图标）
        ImageView logoView = null;
        try (InputStream is = getClass().getResourceAsStream("/textures/icon.png")) {
            if (is != null) {
                Image logo = new Image(is, 72, 72, true, true);
                logoView = new ImageView(logo);
            }
        } catch (Exception ignored) {}

        if (logoView != null) {
            StackPane logoWrapper = new StackPane();
            logoWrapper.setStyle("-fx-background-color: rgba(0,99,177,0.08); -fx-background-radius: 48px;");
            logoWrapper.setPadding(new Insets(12));
            logoWrapper.getChildren().add(logoView);
            content.getChildren().add(logoWrapper);
        } else {
            Label fallbackIcon = new Label("🌿");
            fallbackIcon.setStyle("-fx-font-size: 48px; -fx-text-fill: " + COLOR_PRIMARY + ";");
            content.getChildren().add(fallbackIcon);
        }

        // 标题
        Label titleLabel = new Label(Config.SOFTWARE_NAME);
        titleLabel.setStyle("-fx-text-fill: " + COLOR_ON_SURFACE + ";" +
                "-fx-font-size: 24px;" +
                "-fx-font-weight: bold;" +
                "-fx-font-family: 'Segoe UI', 'Microsoft YaHei', sans-serif;");

        Label versionLabel = new Label(Config.VERSION + "  " + Config.CH);
        versionLabel.setStyle("-fx-text-fill: " + COLOR_ON_SURFACE_VARIANT + ";" +
                "-fx-font-size: 13px;" +
                "-fx-font-family: 'Segoe UI', 'Microsoft YaHei', sans-serif;");

        VBox titleBox = new VBox(6);
        titleBox.setAlignment(Pos.CENTER);
        titleBox.getChildren().addAll(titleLabel, versionLabel);
        content.getChildren().add(titleBox);

        // 状态文字
        statusLabel = new Label("正在加载...");
        statusLabel.setStyle("-fx-text-fill: " + COLOR_ON_SURFACE + ";" +
                "-fx-font-size: 14px;" +
                "-fx-font-weight: normal;" +
                "-fx-font-family: 'Segoe UI', 'Microsoft YaHei', sans-serif;");

        subStatusLabel = new Label("初始化组件");
        subStatusLabel.setStyle("-fx-text-fill: " + COLOR_ON_SURFACE_VARIANT + ";" +
                "-fx-font-size: 12px;" +
                "-fx-font-family: 'Segoe UI', 'Microsoft YaHei', sans-serif;");

        VBox statusBox = new VBox(6);
        statusBox.setAlignment(Pos.CENTER);
        statusBox.getChildren().addAll(statusLabel, subStatusLabel);
        content.getChildren().add(statusBox);

        card.setCenter(content);
        root.getChildren().add(card);

        Scene scene = new Scene(root, 520, 320);
        scene.setFill(Color.TRANSPARENT);
        stage.setScene(scene);
        centerStage();

        // 入场动画
        card.setScaleX(0.95);
        card.setScaleY(0.95);
        card.setOpacity(0);
        ScaleTransition scaleIn = new ScaleTransition(Duration.millis(350), card);
        scaleIn.setToX(1);
        scaleIn.setToY(1);
        FadeTransition fadeIn = new FadeTransition(Duration.millis(350), card);
        fadeIn.setToValue(1);
        ParallelTransition enterAnim = new ParallelTransition(scaleIn, fadeIn);
        enterAnim.play();
    }

    private void centerStage() {
        Rectangle2D screenBounds = Screen.getPrimary().getVisualBounds();
        double centerX = (screenBounds.getWidth() - stage.getWidth()) / 2;
        double centerY = (screenBounds.getHeight() - stage.getHeight()) / 2;
        stage.setX(centerX);
        stage.setY(centerY);
    }

    public void show() {
        stage.show();
        centerStage();
    }

    public void close() {
        if (stage.isShowing()) {
            stage.close();
        }
    }

    public void updateMessage(String message) {
        Platform.runLater(() -> statusLabel.setText(message));
    }

    public void updateSubMessage(String subMessage) {
        Platform.runLater(() -> subStatusLabel.setText(subMessage));
    }
}