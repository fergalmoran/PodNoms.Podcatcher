package com.podnoms.android.podcatcher.ui.widgets;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import com.podnoms.android.podcatcher.R;
import com.podnoms.android.podcatcher.util.LogHandler;

public class AlertDialogs {
    public interface IAlertDialogActionHandler {
        public void action(Boolean result);
    }

    public static void InfoDialog(final Context context, String text) {
        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        builder.setMessage(text);
        builder.setCancelable(true);
        builder.setPositiveButton("OK", null);
        AlertDialog dialog = builder.create();
        dialog.show();
    }

    public static void YesNoDialog(final Context context, String text, final IAlertDialogActionHandler action) {
        DialogInterface.OnClickListener dialogClickListener = new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                try {
                    switch (which) {
                        case DialogInterface.BUTTON_POSITIVE:
                            action.action(true);
                            break;

                        case DialogInterface.BUTTON_NEGATIVE:
                            action.action(false);
                            break;
                    }
                } catch (Exception ex) {
                    LogHandler.reportError("Error showing YND: ", ex);
                }
            }
        };

        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        builder
                .setMessage(text)
                .setTitle("Question?")
                .setPositiveButton("Yes", dialogClickListener)
                .setNegativeButton("No", dialogClickListener)
                .setIcon(R.drawable.ic_question)
                .show();
    }
}
