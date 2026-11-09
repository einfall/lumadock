package com.gathin.tvhome;

import android.app.Activity;
import android.app.AlertDialog;
import android.text.InputFilter;
import android.text.InputType;
import android.view.View;
import android.view.WindowManager;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import java.net.URI;
import java.util.List;
import java.util.function.Consumer;

final class EchoSetup {
    static void show(Activity activity, EchoFeed feed) {
        EchoConnection settings = feed.settings;
        LinearLayout fields = new LinearLayout(activity);
        fields.setOrientation(LinearLayout.VERTICAL);
        int padding = SettingsSheet.dp(activity, 22);
        fields.setPadding(padding, 0, padding, padding);
        fields.addView(GlassDialog.text(activity,
                "Melde dich mit einem langlebigen Zugriffstoken aus deinem Home-Assistant-Profil an und wähle dann einen Mediaplayer.", 12));

        EditText address = field(activity, fields, "Home-Assistant-Adresse", settings.address(),
                InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_URI, 400);
        EditText token = field(activity, fields, settings.configured() ? "Neues Token (optional)" : "Langlebiges Zugriffstoken", "",
                InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD, 4096);
        token.setImportantForAutofill(View.IMPORTANT_FOR_AUTOFILL_NO);
        fields.addView(GlassDialog.text(activity,
                "HTTPS wird unterstützt. HTTP ist mit einer privaten IPv4-Adresse im Heimnetz möglich.", 11));
        TextView status = GlassDialog.text(activity, "", 12);
        fields.addView(status);

        ScrollView scroll = new ScrollView(activity);
        scroll.addView(fields);
        AlertDialog dialog = new AlertDialog.Builder(activity)
                .setTitle("Home Assistant verbinden").setView(scroll)
                .setNegativeButton(android.R.string.cancel, null)
                .setPositiveButton("Mediaplayer suchen", null).create();
        if (dialog.getWindow() != null) dialog.getWindow().addFlags(WindowManager.LayoutParams.FLAG_SECURE);
        dialog.show();
        SettingsSheet.track(activity, dialog);
        dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(ignored -> {
            URI base = EchoConnection.base(address.getText().toString());
            if (base == null) {
                address.setError("HTTPS-Adresse oder private IPv4-Adresse mit http:// eingeben");
                address.requestFocus();
                return;
            }
            String draftToken = token.getText().toString().trim();
            if (!(draftToken.isEmpty() ? settings.canReuseToken(base) : EchoConnection.validToken(draftToken))) {
                token.setError("Ein gültiges Zugriffstoken für diese Adresse eingeben");
                token.requestFocus();
                return;
            }
            lookup(activity, feed, dialog, token, status, base, draftToken);
        });
        address.requestFocus();
    }

    static void switchPlayer(Activity activity, EchoFeed feed) {
        URI base = EchoConnection.base(feed.settings.address());
        if (base == null || !feed.settings.canReuseToken(base)) {
            show(activity, feed);
            return;
        }

        SettingsSheet sheet = new SettingsSheet(activity, "Mediaplayer auswählen");
        sheet.info("Suche Mediaplayer …");
        sheet.show();
        discover(activity, feed.settings, base, "", players -> {
            if (!sheet.dialog.isShowing()) return;
            if (players.isEmpty()) {
                showSwitchError(activity, feed, sheet, "Keine sichtbaren Mediaplayer gefunden.");
            } else {
                fillPlayers(activity, feed, sheet, base, players, () -> {});
            }
        }, message -> {
            if (sheet.dialog.isShowing()) showSwitchError(activity, feed, sheet, message);
        });
    }

    private static void showSwitchError(Activity activity, EchoFeed feed, SettingsSheet sheet, String message) {
        sheet.clearRows();
        sheet.info(message);
        sheet.row("network", "Home-Assistant-Zugang bearbeiten", null, () -> {
            sheet.dialog.dismiss();
            show(activity, feed);
        }).requestFocus();
    }

    private static void lookup(Activity activity, EchoFeed feed, AlertDialog dialog,
                               EditText tokenField, TextView status, URI base, String draftToken) {
        dialog.getButton(AlertDialog.BUTTON_POSITIVE).setEnabled(false);
        status.setText("Suche Mediaplayer …");
        discover(activity, feed.settings, base, draftToken, players -> {
            if (!dialog.isShowing()) return;
            dialog.getButton(AlertDialog.BUTTON_POSITIVE).setEnabled(true);
            status.setText(players.isEmpty() ? "Keine sichtbaren Mediaplayer gefunden." : "");
            if (players.isEmpty()) return;
            if (!draftToken.isEmpty()) {
                try {
                    EchoConnection settings = feed.settings;
                    String previous = settings.canReuseToken(base) ? settings.entity() : "";
                    settings.save(base, previous, draftToken);
                } catch (Exception failure) {
                    status.setText("Die Verbindung konnte nicht gespeichert werden.");
                    return;
                }
                tokenField.getText().clear();
                feed.refresh();
            }
            SettingsSheet sheet = new SettingsSheet(activity, "Mediaplayer auswählen");
            fillPlayers(activity, feed, sheet, base, players, dialog::dismiss);
            sheet.show();
        }, message -> {
            if (!dialog.isShowing()) return;
            dialog.getButton(AlertDialog.BUTTON_POSITIVE).setEnabled(true);
            status.setText(message);
        });
    }

    private static void discover(Activity activity, EchoConnection settings, URI base, String draftToken,
                                 Consumer<List<EchoPlayback.Player>> success, Consumer<String> failure) {
        new Thread(() -> {
            try {
                String token = draftToken.isEmpty() ? settings.token(base.toASCIIString()) : draftToken;
                List<EchoPlayback.Player> players = EchoPlayback.players(base, token);
                deliver(activity, () -> success.accept(players));
            } catch (EchoPlayback.HttpError error) {
                String message = error.code == 401 || error.code == 403
                        ? "Zugriffstoken prüfen." : "Home Assistant konnte die Mediaplayer nicht laden.";
                deliver(activity, () -> failure.accept(message));
            } catch (Exception error) {
                deliver(activity, () -> failure.accept("Verbindung fehlgeschlagen. Adresse und Zugriffstoken prüfen."));
            }
        }, "luma-echo-discovery").start();
    }

    private static void deliver(Activity activity, Runnable update) {
        activity.runOnUiThread(() -> {
            if (!activity.isFinishing() && !activity.isDestroyed()) update.run();
        });
    }

    private static void fillPlayers(Activity activity, EchoFeed feed, SettingsSheet sheet, URI base,
                                    List<EchoPlayback.Player> players, Runnable afterSelection) {
        sheet.clearRows();
        View focus = null;
        for (EchoPlayback.Player player : players) {
            View row = sheet.choice("music", player.name, player.id,
                    () -> base.toASCIIString().equals(feed.settings.address()) && player.id.equals(feed.settings.entity()),
                    () -> {
                        try {
                            feed.settings.save(base, player.id, "");
                        } catch (Exception failure) {
                            DesignDialogs.message(activity, "Die Verbindung konnte nicht gespeichert werden.");
                            return;
                        }
                        sheet.dialog.dismiss();
                        afterSelection.run();
                        feed.refresh();
                    });
            if (focus == null || player.id.equals(feed.settings.entity())) focus = row;
        }
        if (focus != null) {
            View initialFocus = focus;
            sheet.body.post(() -> {
                if (sheet.dialog.isShowing()) initialFocus.requestFocus();
            });
        }
    }

    private static EditText field(Activity activity, LinearLayout parent, String hint, String value,
                                  int inputType, int max) {
        EditText field = new EditText(activity);
        field.setInputType(inputType);
        field.setSingleLine(true);
        field.setFilters(new InputFilter[]{new InputFilter.LengthFilter(max)});
        field.setHint(hint);
        field.setText(value);
        field.setSelectAllOnFocus(true);
        parent.addView(field, new LinearLayout.LayoutParams(-1, -2));
        return field;
    }
}
