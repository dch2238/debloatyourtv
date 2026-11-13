package com.debloatyourtv.launcher;

import android.app.Dialog;
import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

/**
 * TV Leanback compatible PIN prompt dialog with DPAD number pad and key event capture.
 */
public final class AppLockDialog {
    public interface Callback {
        void onUnlockSuccess();
    }

    public static void show(Context context, String appLabel, String packageName, Callback callback) {
        Dialog dialog = new Dialog(context);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        View root = LayoutInflater.from(context).inflate(R.layout.dialog_app_lock, null);
        dialog.setContentView(root);

        Window window = dialog.getWindow();
        if (window != null) {
            window.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
            window.setLayout(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            window.setGravity(Gravity.CENTER);
        }

        TextView title = root.findViewById(R.id.lock_title);
        TextView subtitle = root.findViewById(R.id.lock_subtitle);
        TextView pinDisplay = root.findViewById(R.id.pin_display);
        Button btnCancel = root.findViewById(R.id.btn_cancel);

        if (appLabel != null) {
            subtitle.setText(appLabel + " is locked");
        }

        StringBuilder enteredPin = new StringBuilder();

        Runnable updateDisplay = () -> {
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < 4; i++) {
                if (i < enteredPin.length()) sb.append("●");
                else sb.append("○");
                if (i < 3) sb.append("   ");
            }
            pinDisplay.setText(sb.toString());
        };

        Runnable checkPin = () -> {
            if (enteredPin.length() == 4) {
                if (AppLockManager.verifyPin(context, enteredPin.toString())) {
                    AppLockManager.markSessionUnlocked(packageName);
                    dialog.dismiss();
                    if (callback != null) callback.onUnlockSuccess();
                } else {
                    Toast.makeText(context, "Incorrect PIN", Toast.LENGTH_SHORT).show();
                    enteredPin.setLength(0);
                    updateDisplay.run();
                }
            }
        };

        // Wire up buttons explicitly
        int[] digitIds = {
                R.id.btn_0, R.id.btn_1, R.id.btn_2, R.id.btn_3, R.id.btn_4,
                R.id.btn_5, R.id.btn_6, R.id.btn_7, R.id.btn_8, R.id.btn_9
        };

        for (int i = 0; i <= 9; i++) {
            final String digitStr = String.valueOf(i);
            Button b = root.findViewById(digitIds[i]);
            if (b != null) {
                b.setOnClickListener(v -> {
                    if (enteredPin.length() < 4) enteredPin.append(digitStr);
                    updateDisplay.run();
                    checkPin.run();
                });
            }
        }

        Button btnC = root.findViewById(R.id.btn_c);
        if (btnC != null) {
            btnC.setOnClickListener(v -> {
                enteredPin.setLength(0);
                updateDisplay.run();
            });
        }

        Button btnDel = root.findViewById(R.id.btn_del);
        if (btnDel != null) {
            btnDel.setOnClickListener(v -> {
                if (enteredPin.length() > 0) enteredPin.deleteCharAt(enteredPin.length() - 1);
                updateDisplay.run();
            });
        }

        if (btnCancel != null) btnCancel.setOnClickListener(v -> dialog.dismiss());

        // Listen for direct remote numeric keys (KEYCODE_0 through KEYCODE_9, KEYCODE_DEL)
        dialog.setOnKeyListener((d, keyCode, event) -> {
            if (event.getAction() != KeyEvent.ACTION_DOWN) return false;
            if (keyCode >= KeyEvent.KEYCODE_0 && keyCode <= KeyEvent.KEYCODE_9) {
                int digit = keyCode - KeyEvent.KEYCODE_0;
                if (enteredPin.length() < 4) {
                    enteredPin.append(digit);
                    updateDisplay.run();
                    checkPin.run();
                }
                return true;
            } else if (keyCode == KeyEvent.KEYCODE_DEL) {
                if (enteredPin.length() > 0) {
                    enteredPin.deleteCharAt(enteredPin.length() - 1);
                    updateDisplay.run();
                }
                return true;
            }
            return false;
        });

        dialog.show();
        Button btn1 = root.findViewById(R.id.btn_1);
        if (btn1 != null) btn1.requestFocus();
    }
}
