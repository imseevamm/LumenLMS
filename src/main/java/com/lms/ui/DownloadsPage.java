package com.lms.ui;

import com.lms.model.DownloadRecord;
import com.lms.service.DownloadService;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

import java.time.format.DateTimeFormatter;
import java.util.List;

public final class DownloadsPage {
    private static final DateTimeFormatter DTF = DateTimeFormatter.ofPattern("dd MMM yyyy, HH:mm");
    private DownloadsPage() {}

    public static Node build(DashboardShell shell) {
        VBox root = new VBox(20);
        HBox header = new HBox(UIComponents.pageTitle("Downloads"), UIComponents.spacer());
        Button clear = UIComponents.ghostButton("Clear History");
        clear.setOnAction(e -> {
            try {
                new DownloadService().clearHistory();
                Toast.success("Download history cleared", "Your download list is now empty.");
                shell.navigate("Downloads");
            } catch (RuntimeException ex) {
                Toast.error("Could not clear history", ex.getMessage());
            }
        });
        header.getChildren().add(clear);
        header.setAlignment(Pos.CENTER_LEFT);
        root.getChildren().addAll(header,
                UIComponents.pageSubtitle("Files you have downloaded from Lumen LMS."));

        List<DownloadRecord> records = new DownloadService().getHistory();
        VBox card = UIComponents.card();
        VBox list = new VBox(8);
        if (records.isEmpty()) {
            list.getChildren().add(UIComponents.emptyState("No downloads yet", "Files you download from courses or submissions will appear here."));
        } else {
            for (DownloadRecord record : records) list.getChildren().add(row(record));
        }
        card.getChildren().add(list);
        root.getChildren().add(card);
        return root;
    }

    private static Node row(DownloadRecord record) {
        HBox row = new HBox(14);
        row.setAlignment(Pos.CENTER_LEFT);
        row.setPadding(new Insets(12));
        row.setStyle("-fx-background-color: rgba(40,123,239,0.04); -fx-background-radius: 10px; -fx-border-color: rgba(40,123,239,0.10); -fx-border-radius: 10px;");
        Label icon = new Label("↓");
        icon.setStyle("-fx-font-size: 18px; -fx-font-weight: 800; -fx-text-fill: #287BEF;");
        VBox info = new VBox(3);
        Label title = new Label(record.getTitle());
        title.setStyle("-fx-font-weight: 700; -fx-text-fill: -text;");
        Label file = new Label(record.getFileName());
        file.getStyleClass().add("muted-text");
        info.getChildren().addAll(title, file);
        VBox.setVgrow(info, Priority.ALWAYS);
        Label category = UIComponents.badge(record.getCategory(), "badge-info");
        Label time = new Label(record.getDownloadedAt() == null ? "" : record.getDownloadedAt().format(DTF));
        time.getStyleClass().add("muted-text");
        row.getChildren().addAll(icon, info, category, time);
        HBox.setHgrow(info, Priority.ALWAYS);
        return row;
    }
}
