package com.debloatyourtv.launcher;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.content.pm.ResolveInfo;
import android.os.Bundle;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.List;

public class AppSecurityActivity extends Activity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_app_security);

        TextView btnChangePin = findViewById(R.id.btnChangePin);
        if (btnChangePin != null) {
            btnChangePin.setOnClickListener(v -> promptNewPin());
        }

        TextView btnSelectLockedApps = findViewById(R.id.btnSelectLockedApps);
        if (btnSelectLockedApps != null) {
            btnSelectLockedApps.setOnClickListener(v -> showAppLockPicker());
        }

        TextView btnSetDailyTimer = findViewById(R.id.btnSetDailyTimer);
        if (btnSetDailyTimer != null) {
            btnSetDailyTimer.setOnClickListener(v -> showTimerOptions());
        }
    }

    private void promptNewPin() {
        Intent pinIntent = new Intent(this, PinLockActivity.class)
                .putExtra(PinLockActivity.EXTRA_LABEL, "Set New PIN");
        startActivityForResult(pinIntent, 7777);
    }

    private void showAppLockPicker() {
        Intent pmIntent = new Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LEANBACK_LAUNCHER);
        List<ResolveInfo> activities = getPackageManager().queryIntentActivities(pmIntent, 0);
        List<String> labels = new ArrayList<>();
        List<String> pkgs = new ArrayList<>();
        List<Boolean> states = new ArrayList<>();

        for (ResolveInfo ri : activities) {
            String pkg = ri.activityInfo.packageName;
            if (pkg.equals(getPackageName())) continue;
            labels.add(ri.loadLabel(getPackageManager()).toString());
            pkgs.add(pkg);
            states.add(AppLockManager.isAppLocked(this, pkg));
        }

        boolean[] checked = new boolean[states.size()];
        for (int i = 0; i < states.size(); i++) checked[i] = states.get(i);

        new AlertDialog.Builder(this)
                .setTitle("Select Apps to Lock")
                .setMultiChoiceItems(labels.toArray(new CharSequence[0]), checked, (dialog, which, isChecked) -> {
                    String pkg = pkgs.get(which);
                    AppLockManager.setAppLocked(this, pkg, isChecked);
                })
                .setPositiveButton("Done", null)
                .show();
    }

    private void showTimerOptions() {
        String[] options = {"No Limit (Off)", "30 Minutes", "45 Minutes", "60 Minutes (1 Hour)", "90 Minutes", "120 Minutes (2 Hours)"};
        int[] minutes = {0, 30, 45, 60, 90, 120};

        new AlertDialog.Builder(this)
                .setTitle("Daily Streaming Limit")
                .setItems(options, (dialog, which) -> {
                    int chosenMins = minutes[which];
                    // Apply to major OTT apps
                    String[] otts = {"com.google.android.youtube.tv", "in.startv.hotstar", "com.sonyliv", "ahaflix.tv", "com.etvwin.androidtv"};
                    for (String pkg : otts) {
                        AppLockManager.setTimerLimitMinutes(this, pkg, chosenMins);
                    }
                    Toast.makeText(this, "Daily limit set: " + options[which], Toast.LENGTH_SHORT).show();
                })
                .show();
    }
}
