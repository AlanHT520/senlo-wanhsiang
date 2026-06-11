package net.alan.senlo;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.scene.Scene;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ChoiceDialog;
import javafx.scene.image.Image;
import javafx.stage.Stage;
import net.alan.senlo.controller.MainController;
import net.alan.senlo.gui.MainWindow;
import net.alan.senlo.gui.PreviewPaneBuilder;
import net.alan.senlo.gui.SplashScreen;
import net.alan.senlo.model.ApplicationSettings;
import net.alan.senlo.util.Language;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Optional;

public class MainFX extends Application {
    private static final Logger logger = LoggerFactory.getLogger(MainFX.class);

    @Override
    public void start(Stage primaryStage) {
        SplashScreen splash = new SplashScreen();
        splash.show();

        // 启动画面固定停留 0.9 秒后关闭
        new Thread(() -> {
            try {
                Thread.sleep(900);
            } catch (InterruptedException ignored) {}
            Platform.runLater(() -> {
                splash.close();
                afterSplash(primaryStage);
            });
        }).start();
    }

    private void afterSplash(Stage primaryStage) {
        ApplicationSettings settings = ApplicationSettings.getInstance();

        // 如果是首次运行，弹出语言选择对话框
        if (settings.isFirstRun()) {
            ChoiceDialog<String> dialog = new ChoiceDialog<>("English", "English", "简体中文", "繁體中文");
            dialog.setTitle(Language.get("dialog.language.title"));
            dialog.setHeaderText(Language.get("dialog.language.header"));
            dialog.setContentText(Language.get("dialog.language.content"));
            dialog.getDialogPane().getStylesheets().add(getClass().getResource("/css/style.css").toExternalForm());

            // 强制将对话框的确定/取消按钮文本设置为英文（也可以根据需要动态设置）
            ButtonType okButton = ButtonType.OK;
            ButtonType cancelButton = ButtonType.CANCEL;
            dialog.getDialogPane().lookupButton(okButton).setVisible(false); // 隐藏默认的确定按钮
            dialog.getDialogPane().lookupButton(cancelButton).setVisible(false);
            // 添加自定义按钮并设置文本
            javafx.scene.control.Button okBtn = new javafx.scene.control.Button("OK");
            javafx.scene.control.Button cancelBtn = new javafx.scene.control.Button("Cancel");
            dialog.getDialogPane().getButtonTypes().setAll(ButtonType.CLOSE);
            dialog.getDialogPane().getButtonTypes().clear();
            dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);
            javafx.scene.control.Button ok = (javafx.scene.control.Button) dialog.getDialogPane().lookupButton(ButtonType.OK);
            javafx.scene.control.Button cancel = (javafx.scene.control.Button) dialog.getDialogPane().lookupButton(ButtonType.CANCEL);
            ok.setText("OK");
            cancel.setText("Cancel");

            Optional<String> result = dialog.showAndWait();
            if (result.isPresent()) {
                String selected = result.get();
                if ("English".equals(selected)) {
                    settings.setLanguage("en_us");
                } else if ("简体中文".equals(selected)) {
                    settings.setLanguage("zh_cn");
                } else if ("繁體中文".equals(selected)) {
                    settings.setLanguage("zh_tw");
                }
            } else {
                // 用户直接关闭对话框，默认使用英文
                settings.setLanguage("en_us");
            }
            settings.setFirstRun(false);
            settings.save();
            Language.load();
        } else {
            Language.load();
        }

        // 显示主窗口
        showMainWindow(primaryStage);
    }

    private void showMainWindow(Stage primaryStage) {
        try {
            Image icon = new Image(getClass().getResourceAsStream("/textures/icon.png"));
            primaryStage.getIcons().add(icon);

            MainWindow view = new MainWindow();
            new MainController(view);

            Scene scene = new Scene(view.getRoot(), 1100, 720);
            scene.getStylesheets().add(getClass().getResource("/css/style.css").toExternalForm());
            primaryStage.setTitle(Language.get("app.name"));
            primaryStage.setScene(scene);
            primaryStage.setOnCloseRequest(event -> PreviewPaneBuilder.releaseCurrentMedia());
            primaryStage.show();
        } catch (Exception e) {
            logger.error("启动主窗口失败", e);
            Platform.exit();
        }
    }

    public static void mainfx(String[] args) {
        launch(args);
    }
}