package com.github.gtexpert.blpc.client.gui.party.widget;

import java.util.function.Consumer;

import com.cleanroommc.modularui.api.drawable.IKey;
import com.cleanroommc.modularui.utils.Alignment;
import com.cleanroommc.modularui.widgets.ButtonWidget;
import com.cleanroommc.modularui.widgets.Dialog;
import com.cleanroommc.modularui.widgets.layout.Flow;
import com.cleanroommc.modularui.widgets.textfield.TextFieldWidget;

/**
 * Reusable text input dialog template.
 * <p>
 * Produces a {@link Dialog Dialog&lt;Void&gt;} with a title, optional message,
 * a text field, and a submit button. On submit, the provided callback receives
 * the trimmed text input.
 * <p>
 * Usage:
 * 
 * <pre>
 * {@code
 * InputDialog.builder("blpc.party.dialog.invite")
 *     .title("blpc.party.invite_title")
 *     .confirmLabel("blpc.party.send")
 *     .onSubmit(username -> { ... })
 *     .build();
 * }
 * </pre>
 */
public final class InputDialog {

    private InputDialog() {}

    /** Creates a new builder for an input dialog. */
    public static Builder builder(String panelId) {
        return new Builder(panelId);
    }

    public static class Builder {

        private final String panelId;
        private String titleKey = "";
        private String messageKey;
        private String confirmKey = "blpc.party.send";
        private Consumer<String> onSubmit = s -> {};
        private int width = 200;
        private int height = 60;

        Builder(String panelId) {
            this.panelId = panelId;
        }

        /** Lang key for the dialog title. */
        public Builder title(String langKey) {
            this.titleKey = langKey;
            return this;
        }

        /** Optional lang key for a subtitle/message below the title. */
        public Builder message(String langKey) {
            this.messageKey = langKey;
            return this;
        }

        /** Lang key for the submit button. Defaults to {@code blpc.party.send}. */
        public Builder confirmLabel(String langKey) {
            this.confirmKey = langKey;
            return this;
        }

        /** Action to run with the trimmed text when the user submits. */
        public Builder onSubmit(Consumer<String> action) {
            this.onSubmit = action;
            return this;
        }

        /** Dialog size in pixels. Defaults to 200×60. */
        public Builder size(int w, int h) {
            this.width = w;
            this.height = h;
            return this;
        }

        /** Builds the dialog. */
        public Dialog<Void> build() {
            Dialog<Void> dialog = new Dialog<>(panelId);
            dialog.setDisablePanelsBelow(true);
            dialog.setCloseOnOutOfBoundsClick(true);
            dialog.size(width, height);

            // Title (and optional message) at the top
            Flow header = Flow.col()
                    .childPadding(4)
                    .crossAxisAlignment(Alignment.CrossAxis.START)
                    .left(8).right(8).top(6);
            header.child(IKey.lang(titleKey).color(0xFFFFFFFF).shadow(true)
                    .asWidget());
            if (messageKey != null) {
                header.child(IKey.lang(messageKey).color(0xFFAAAAAA).shadow(true)
                        .asWidget());
            }
            dialog.child(header);

            // Text field + submit button in a row with automatic spacing
            int fieldY = messageKey != null ? 30 : 24;
            TextFieldWidget textField = new TextFieldWidget();
            textField.size(width - 70, 14);

            Consumer<String> submitAction = this.onSubmit;
            String id = this.panelId;

            Flow inputRow = Flow.row()
                    .childPadding(4)
                    .crossAxisAlignment(Alignment.CrossAxis.CENTER)
                    .left(8).right(8).top(fieldY).height(14);
            inputRow.child(textField);
            inputRow.child(new ButtonWidget<>().size(50, 14)
                    .overlay(IKey.lang(confirmKey))
                    .onMousePressed(btn -> {
                        String text = textField.getText().trim();
                        if (!text.isEmpty()) {
                            submitAction.accept(text);
                        }
                        dialog.closeWith(null);
                        return true;
                    }));

            dialog.child(inputRow)
                    .child(ButtonWidget.panelCloseButton());

            return dialog;
        }
    }
}
