package com.gathin.tvhome;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.text.InputFilter;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.Toast;
import java.util.function.Consumer;

final class DesignDialogs {
    static void name(Activity activity, String title, String initial, Consumer<String> save) {
        FrameLayout frame = new FrameLayout(activity);
        int padding = SettingsSheet.dp(activity, 22);
        frame.setPadding(padding, 0, padding, 0);
        EditText input = new EditText(activity);
        input.setSingleLine(true);
        input.setFilters(new InputFilter[]{new InputFilter.LengthFilter(40)});
        input.setSelectAllOnFocus(true);
        input.setText(initial);
        frame.addView(input);
        AlertDialog dialog = new AlertDialog.Builder(activity)
                .setTitle(title).setView(frame).setNegativeButton(android.R.string.cancel, null)
                .setPositiveButton(android.R.string.ok, null).show();
        SettingsSheet.track(activity, dialog);
        dialog.getButton(DialogInterface.BUTTON_POSITIVE).setOnClickListener(view -> {
            String name = Cards.cleanLabel(input.getText().toString().trim(), "");
            if (name.isEmpty()) {
                input.setError("Bitte einen Namen eingeben");
                return;
            }
            dialog.dismiss();
            save.accept(name);
        });
        input.requestFocus();
    }

    static void confirm(Activity activity, String title, String message, String action, Runnable run) {
        AlertDialog dialog = new AlertDialog.Builder(activity)
                .setTitle(title).setMessage(message).setNegativeButton(android.R.string.cancel, null)
                .setPositiveButton(action, (ignored, which) -> run.run()).show();
        SettingsSheet.track(activity, dialog);
        dialog.getButton(DialogInterface.BUTTON_NEGATIVE).requestFocus();
    }

    static void message(Activity activity, String text) {
        Toast.makeText(activity, text, Toast.LENGTH_LONG).show();
    }
}
