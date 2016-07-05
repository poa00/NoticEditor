package com.noticeditorteam.noticeditor.controller;

import com.noticeditorteam.noticeditor.Main;
import com.noticeditorteam.noticeditor.io.IOUtil;
import com.noticeditorteam.noticeditor.model.*;
import com.noticeditorteam.noticeditor.view.Chooser;
import com.noticeditorteam.noticeditor.view.Notification;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.event.ActionEvent;
import javafx.event.Event;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.ListView;
import javafx.scene.control.MenuItem;
import javafx.scene.control.RadioMenuItem;
import javafx.scene.control.TextArea;
import javafx.scene.web.WebEngine;
import javafx.scene.web.WebView;
import org.pegdown.PegDownProcessor;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.ResourceBundle;
import java.util.logging.Level;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static javafx.scene.control.SelectionMode.SINGLE;
import static org.pegdown.Extensions.*;

/**
 * @author Edward Minasyan <mrEDitor@mail.ru>
 */
public class NoticeViewController implements Initializable {

    // @att:filename.png
    private static final Pattern ATTACHMENT_PATTERN = Pattern.compile("@att\\:([a-zA-Z0-9._\\(\\)]+)");

    @FXML
    private MenuItem exportAttachItem, deleteAttachItem;

    @FXML
    private ListView<Attachment> attachsView;

    private ResourceBundle resources;

    @FXML
    private TextArea editor;

	@FXML
	private WebView viewer;

	protected final PegDownProcessor processor;
	protected final SyntaxHighlighter highlighter;
	private WebEngine engine;
	private Main main;

	private String codeCssName;
    private Attachment currentAttach;

	public NoticeViewController() {
		processor = new PegDownProcessor(AUTOLINKS | TABLES | FENCED_CODE_BLOCKS);
		highlighter = new SyntaxHighlighter();
	}

	@Override
	public void initialize(URL url, ResourceBundle rb) {
		highlighter.unpackHighlightJs();
		engine = viewer.getEngine();
		editor.textProperty().addListener(new ChangeListener<String>() {
			@Override
			public void changed(ObservableValue<? extends String> observable, String oldValue, String newValue) {
				changeContent(newValue);
			}
		});
        attachsView.getSelectionModel().setSelectionMode(SINGLE);
        attachsView.getSelectionModel().selectedItemProperty().addListener(new ChangeListener<Attachment>() {
            @Override
            public void changed(ObservableValue<? extends Attachment> observable, Attachment oldValue, Attachment newValue) {
                currentAttach = (Attachment) newValue;
            }
        });
        resources = rb;
    }

	public TextArea getEditor() {
		return editor;
	}

	private void changeContent(String newContent) {
        final NoticeTreeItem current = NoticeController.getNoticeTreeViewController().getCurrentNotice();
        final String parsed;
        if (current != null) {
			current.changeContent(newContent);
            parsed = parseAttachments(newContent, current.getAttachments());
		} else {
            parsed = "";
        }
		engine.loadContent(highlighter.highlight(processor.markdownToHtml(parsed), codeCssName));
	}

    private String parseAttachments(String text, Attachments attachments) {
        final Matcher m = ATTACHMENT_PATTERN.matcher(text);
        final StringBuffer sb = new StringBuffer();
        while (m.find()) {
            final String name = m.group(1);
            Attachment att = attachments.get(name);
            if (att != null) {
                m.appendReplacement(sb, "<img src=\"data:image/png;base64, " + att.getDataAsBase64() + "\"/>");
            }
        }
        m.appendTail(sb);
        return sb.toString();
    }

	public final EventHandler<ActionEvent> onPreviewStyleChange = new EventHandler<ActionEvent>() {
		@Override
		public void handle(ActionEvent e) {
			final int ordinal = (int) ((RadioMenuItem) e.getSource()).getUserData();
			PreviewStyles style = PreviewStyles.values()[ordinal];
			// CSS path
			String path = style.getCssPath();
			if (path != null) {
				path = getClass().getResource(path).toExternalForm();
			}
			codeCssName = style.getCodeCssName();
			engine.setUserStyleSheetLocation(path);
			changeContent(editor.textProperty().getValue());
		}
	};

	public final EventHandler<ActionEvent> onThemeChange = new EventHandler<ActionEvent>() {
		@Override
		public void handle(ActionEvent e) {
			// Remove all previous themes if exists
			for (Themes value : Themes.values()) {
				if (value.getCssPath() != null) {
					String path = getClass().getResource(value.getCssPath()).toExternalForm();
					main.getPrimaryStage().getScene().getStylesheets().remove(path);
				}
			}

			// Add new theme
			final int ordinal = (int) ((RadioMenuItem) e.getSource()).getUserData();
			Themes theme = Themes.values()[ordinal];
			String path = theme.getCssPath();
			if (path != null) {
				path = getClass().getResource(path).toExternalForm();
				main.getPrimaryStage().getScene().getStylesheets().add(path);
			}
		}
	};

    public void rebuildAttachsView() {
        onAttachsFocused(null);
    }

    private void exportAttachment(File file, Attachment attachment) {
        try {
            IOUtil.writeContent(file, attachment.getData());
            Notification.success(resources.getString("export.success"));
        } catch (IOException e) {
            NoticeController.getLogger().log(Level.SEVERE, null, e);
            Notification.error(resources.getString("export.error"));
        }
    }

    @FXML
    private void handleContextMenu(ActionEvent event) {
        final Object source = event.getSource();
        if (source == exportAttachItem) {
            File fileSaved = Chooser.file().save()
                    .filter(Chooser.ALL)
                    .title(resources.getString("exportfile"))
                    .show(main.getPrimaryStage());
            if (fileSaved == null) return;
            exportAttachment(fileSaved, currentAttach);
        } else if (source == deleteAttachItem) {
            final NoticeTreeItem current = NoticeController.getNoticeTreeViewController().getCurrentNotice();
            current.getAttachments().remove(currentAttach);
            rebuildAttachsView();
        }
    }

    @FXML
    private void onAttachsFocused(Event event) {
        final NoticeTreeItem current = NoticeController.getNoticeTreeViewController().getCurrentNotice();
        attachsView.getItems().clear();
        if (current.isLeaf()) {
            for (Attachment attachment : current.getAttachments()) {
                attachsView.getItems().add(attachment);
            }
        }
    }

	public void setMain(Main main) {
		this.main = main;
	}
}
