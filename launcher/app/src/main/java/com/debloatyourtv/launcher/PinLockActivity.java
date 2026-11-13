package com.debloatyourtv.launcher;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.view.KeyEvent;
import android.view.View;
import android.view.Window;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

/**
 * TV Leanback compatible PIN Pad & Daytime Bypass dialog.
 * Handles:
 * - 4-digit PIN verification.
 * - Two-option choice on expired timer: "Bypass for the Day" or "Set Timer Again (+30 Mins)".
 * - Voice guard cancel redirect.
 */
public class PinLockActivity extends Activity {
    public static final String EXTRA_PKG = "pkg";
    public static final String EXTRA_LABEL = "label";
    public static final String EXTRA_VOICE_GUARD = "voice_guard";

    private final StringBuilder enteredPin = new StringBuilder();
    private TextView pinDisplay;
    private TextView lockSubtitle;
    private LinearLayout numpadContainer;
    private LinearLayout bypassContainer;
    private String pkg;
    private String label;
    private boolean voiceGuard;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        setContentView(R.layout.dialog_app_lock);

        pkg = getIntent().getStringExtra(EXTRA_PKG);
        label = getIntent().getStringExtra(EXTRA_LABEL);
        voiceGuard = getIntent().getBooleanExtra(EXTRA_VOICE_GUARD, false);

        lockSubtitle = findViewById(R.id.lock_subtitle);
        if (label != null) lockSubtitle.setText(label + " is locked");

        pinDisplay = findViewById(R.id.pin_display);
        numpadContainer = findViewById(R.id.numpad_container);
        bypassContainer = findViewById(R.id.bypass_options_container);

        Button btnCancel = findViewById(R.id.btn_cancel);
        if (btnCancel != null) {
            btnCancel.setOnClickListener(v -> cancelAndReturnHome());
        }

        int[] digitIds = {
                R.id.btn_0, R.id.btn_1, R.id.btn_2, R.id.btn_3, R.id.btn_4,
                R.id.btn_5, R.id.btn_6, R.id.btn_7, R.id.btn_8, R.id.btn_9
        };

        for (int i = 0; i <= 9; i++) {
            final String digitStr = String.valueOf(i);
            Button b = findViewById(digitIds[i]);
            if (b != null) {
                b.setOnClickListener(v -> {
                    if (enteredPin.length() < 4) enteredPin.append(digitStr);
                    updateDisplay();
                    checkPin();
                });
            }
        }

        Button btnC = findViewById(R.id.btn_c);
        if (btnC != null) {
            btnC.setOnClickListener(v -> {
                enteredPin.setLength(0);
                updateDisplay();
            });
        }

        Button btnDel = findViewById(R.id.btn_del);
        if (btnDel != null) {
            btnDel.setOnClickListener(v -> {
                if (enteredPin.length() > 0) enteredPin.deleteCharAt(enteredPin.length() - 1);
                updateDisplay();
            });
        }

        // Two-Option Bypass Buttons
        Button btnBypassDay = findViewById(R.id.btn_bypass_day);
        if (btnBypassDay != null) {
            btnBypassDay.setOnClickListener(v -> {
                AppLockManager.bypassForToday(this, pkg);
                Toast.makeText(this, "Bypassed for today", Toast.LENGTH_SHORT).show();
                setResult(RESULT_OK);
                finish();
            });
        }

        Button btnExtendTimer = findViewById(R.id.btn_extend_timer);
        if (btnExtendTimer != null) {
            btnExtendTimer.setOnClickListener(v -> {
                AppLockManager.addExtraMinutes(this, pkg, 30);
                Toast.makeText(this, "+30 Minutes added", Toast.LENGTH_SHORT).show();
                setResult(RESULT_OK);
                finish();
            });
        }

        Button btn1 = findViewById(R.id.btn_1);
        if (btn1 != null) btn1.requestFocus();
    }

    private void updateDisplay() {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < 4; i++) {
            if (i < enteredPin.length()) sb.append("●");
            else sb.append("○");
            if (i < 3) sb.append("   ");
        }
        pinDisplay.setText(sb.toString());
    }

    private void checkPin() {
        if (enteredPin.length() == 4) {
            if (AppLockManager.verifyPin(this, enteredPin.toString())) {
                boolean isTimeExpired = AppLockManager.isTimeLimitExceeded(this, pkg);
                if (isTimeExpired) {
                    // Show Two-Option Bypass Menu
                    numpadContainer.setVisibility(View.GONE);
                    pinDisplay.setVisibility(View.GONE);
                    bypassContainer.setVisibility(View.VISIBLE);
                    Button btnBypassDay = findViewById(R.id.btn_bypass_day);
                    if (btnBypassDay != null) btnBypassDay.requestFocus();
                } else {
                    AppLockManager.markSessionUnlocked(pkg);
                    setResult(RESULT_OK);
                    finish();
                }
            } else {
                Toast.makeText(this, "Incorrect PIN", Toast.LENGTH_SHORT).show();
                enteredPin.setLength(0);
                updateDisplay();
            }
        }
    }

    private void cancelAndReturnHome() {
        if (voiceGuard) {
            Intent home = new Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_HOME);
            startActivity(home.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
        }
        setResult(RESULT_CANCELED);
        finish();
    }

    @Override
    public void onBackPressed() {
        cancelAndReturnHome();
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (keyCode >= KeyEvent.KEYCODE_0 && keyCode <= KeyEvent.KEYCODE_9) {
            int digit = keyCode - KeyEvent.KEYCODE_0;
            if (enteredPin.length() < 4) {
                enteredPin.append(digit);
                updateDisplay();
                checkPin();
            }
            return true;
        } else if (keyCode == KeyEvent.KEYCODE_DEL) {
            if (enteredPin.length() > 0) {
                enteredPin.deleteCharAt(enteredPin.length() - 1);
                updateDisplay();
            }
            return true;
        }
        return super.onKeyDown(keyCode, event);
    }
}
