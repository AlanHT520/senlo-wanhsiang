package net.alan.senlo.controller;

import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.Hyperlink;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;
import javafx.scene.text.Text;
import javafx.scene.text.TextFlow;
import net.alan.senlo.config.Config;
import net.alan.senlo.util.Language;

import java.awt.Desktop;
import java.net.URI;

public class AboutAPP {

    public static void showAboutDialog() {
        Alert alert = new Alert(AlertType.INFORMATION);
        alert.setTitle(Language.get("about.title"));
        alert.setHeaderText(null);

        VBox content = new VBox(12);
        content.setPadding(new javafx.geometry.Insets(12));

        Label titleLabel = new Label(Config.SOFTWARE_NAME);
        titleLabel.setStyle("-fx-font-size: 18px; -fx-font-weight: bold; -fx-text-fill: #0078D7;");
        Label versionLabel = new Label(Language.get("about.version") + " " + Config.VERSION + " (" + Config.CH + ")");
        versionLabel.setStyle("-fx-font-size: 12px; -fx-text-fill: #B0B0B0;");

        Text descText = new Text(Language.get("about.description"));
        descText.setStyle("-fx-font-size: 12px; -fx-text-fill: #B0B0B0;");
        TextFlow descFlow = new TextFlow(descText);

        Label copyrightLabel = new Label(Language.get("about.copyright"));
        copyrightLabel.setStyle("-fx-font-size: 11px;");
        Hyperlink licenseLink = new Hyperlink(Language.get("about.license"));
        licenseLink.setStyle("-fx-font-size: 11px;");
        licenseLink.setOnAction(e -> {
            try {
                Desktop.getDesktop().browse(new URI("https://www.gnu.org/licenses/gpl-3.0.html"));
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        });

        Hyperlink homeLink = new Hyperlink(Language.get("about.homepage"));
        homeLink.setStyle("-fx-font-size: 11px;");
        homeLink.setOnAction(e -> {
            try {
                Desktop.getDesktop().browse(new URI("https://github.com/AlanHT520/senlo-wanhsiang"));
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        });

        Hyperlink issueLink = new Hyperlink(Language.get("about.issues"));
        issueLink.setStyle("-fx-font-size: 11px;");
        issueLink.setOnAction(e -> {
            try {
                Desktop.getDesktop().browse(new URI("https://github.com/AlanHT520/senlo-wanhsiang/issues"));
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        });

        Label thanksLabel = new Label(Language.get("about.thanks"));
        thanksLabel.setStyle("-fx-font-size: 10px; -fx-text-fill: #808080;");

        content.getChildren().addAll(
                titleLabel, versionLabel,
                descFlow,
                copyrightLabel, licenseLink,
                homeLink, issueLink,
                thanksLabel
        );

        alert.getDialogPane().setContent(content);
        alert.getDialogPane().getStylesheets().add(
                AboutAPP.class.getResource("/css/style.css").toExternalForm()
        );
        alert.showAndWait();
    }
}