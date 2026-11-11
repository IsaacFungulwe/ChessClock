package com.example.chessclock;

import android.content.SharedPreferences;
import android.content.res.ColorStateList;
import android.graphics.drawable.GradientDrawable;
import android.media.AudioManager;
import android.media.ToneGenerator;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;
import android.view.HapticFeedbackConstants;
import android.view.View;
import android.view.WindowManager;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsControllerCompat;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import java.util.Locale;

public class MainActivity extends AppCompatActivity {

    private static final String PREFS_NAME = "chess_clock_prefs";
    private static final String KEY_LIGHT_MODE = "key_light_mode";

    private static class TimeControl {
        final String label;
        final String shortLabel;
        final long timeMillis;
        final int incrementSeconds;

        TimeControl(String label, String shortLabel, long timeMillis, int incrementSeconds) {
            this.label = label;
            this.shortLabel = shortLabel;
            this.timeMillis = timeMillis;
            this.incrementSeconds = incrementSeconds;
        }
    }

    private final TimeControl[] presets = new TimeControl[]{
            new TimeControl("1 min (Bullet)", "1 | 0", 60_000, 0),
            new TimeControl("1 min + 1s (Bullet)", "1 | 1", 60_000, 1),
            new TimeControl("2 min + 1s (Bullet)", "2 | 1", 120_000, 1),
            new TimeControl("3 min (Blitz)", "3 | 0", 180_000, 0),
            new TimeControl("3 min + 2s (Blitz)", "3 | 2", 180_000, 2),
            new TimeControl("5 min (Blitz)", "5 | 0", 300_000, 0),
            new TimeControl("5 min + 3s (Blitz)", "5 | 3", 300_000, 3),
            new TimeControl("10 min (Rapid)", "10 | 0", 600_000, 0),
            new TimeControl("15 min + 10s (Rapid)", "15 | 10", 900_000, 10),
            new TimeControl("30 min (Classical)", "30 | 0", 1_800_000, 0)
    };

    private int selectedPresetIndex = 3; // Default: 3 min (Blitz)
    private final long[] time = new long[2];
    private final int[] moves = new int[2];
    private int active = -1; // 0 = Bottom (White), 1 = Top (Black), -1 = None
    private boolean isPaused = false;
    private long lastTimestamp;
    private boolean soundEnabled = true;
    private boolean isLightMode = false;

    private ToneGenerator toneGenerator;
    private final Handler handler = new Handler(Looper.getMainLooper());

    // Views
    private View rootLayout;
    private LinearLayout centerControlBar;
    private final MaterialCardView[] cards = new MaterialCardView[2];
    private final TextView[] tvTimes = new TextView[2];
    private final TextView[] tvMoves = new TextView[2];
    private final TextView[] tvPlayers = new TextView[2];
    private final TextView[] tvStatuses = new TextView[2];
    private final TextView[] tvIncrements = new TextView[2];

    private ImageButton btnReset;
    private ImageButton btnTheme;
    private ImageButton btnPlayPause;
    private MaterialButton btnTimePreset;
    private ImageButton btnSound;

    // Dynamic Colors
    private int colorClockBg;

    private int colorInactiveBg;
    private int colorInactiveStroke;
    private int colorTextInactivePrimary;
    private int colorTextInactiveSecondary;

    private int colorActiveBg;
    private int colorActiveStroke;
    private int colorTextActivePrimary;
    private int colorTextActiveAccent;

    private int colorFlaggedBg;
    private int colorFlaggedStroke;
    private int colorTextFlaggedPrimary;
    private int colorTextFlaggedAccent;

    private int colorControlBarBg;
    private int colorControlBarStroke;
    private int colorControlIcon;
    private int colorControlIconAccent;
    private int colorBadgeBg;
    private int colorBadgeText;

    private int colorPresetBtnBg;
    private int colorPresetBtnStroke;
    private int colorPresetBtnText;

    private final Runnable tick = new Runnable() {
        @Override
        public void run() {
            if (active < 0 || isPaused) return;

            long now = SystemClock.elapsedRealtime();
            time[active] -= (now - lastTimestamp);
            lastTimestamp = now;

            if (time[active] <= 0) {
                time[active] = 0;
                active = -1;
                isPaused = false;
                playTimeoutAlert();
                updateUI();
                return;
            }

            updateUI();
            long delay = time[active] < 10000 ? 30 : 100;
            handler.postDelayed(this, delay);
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        isLightMode = prefs.getBoolean(KEY_LIGHT_MODE, false);

        setContentView(R.layout.activity_main);

        // Keep screen awake during chess matches
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        loadColors();
        initViews();
        applyThemeMode();
        initAudio();
        resetGame(selectedPresetIndex);
    }

    private void loadColors() {
        if (isLightMode) {
            colorClockBg = ContextCompat.getColor(this, R.color.clock_bg_light);

            colorInactiveBg = ContextCompat.getColor(this, R.color.card_inactive_bg_light);
            colorInactiveStroke = ContextCompat.getColor(this, R.color.card_inactive_stroke_light);
            colorTextInactivePrimary = ContextCompat.getColor(this, R.color.text_inactive_primary_light);
            colorTextInactiveSecondary = ContextCompat.getColor(this, R.color.text_inactive_secondary_light);

            colorActiveBg = ContextCompat.getColor(this, R.color.card_active_bg_light);
            colorActiveStroke = ContextCompat.getColor(this, R.color.card_active_stroke_light);
            colorTextActivePrimary = ContextCompat.getColor(this, R.color.text_active_primary_light);
            colorTextActiveAccent = ContextCompat.getColor(this, R.color.text_active_accent_light);

            colorFlaggedBg = ContextCompat.getColor(this, R.color.card_flagged_bg_light);
            colorFlaggedStroke = ContextCompat.getColor(this, R.color.card_flagged_stroke_light);
            colorTextFlaggedPrimary = ContextCompat.getColor(this, R.color.text_flagged_primary_light);
            colorTextFlaggedAccent = ContextCompat.getColor(this, R.color.text_flagged_accent_light);

            colorControlBarBg = ContextCompat.getColor(this, R.color.control_bar_bg_light);
            colorControlBarStroke = ContextCompat.getColor(this, R.color.control_bar_stroke_light);
            colorControlIcon = ContextCompat.getColor(this, R.color.control_icon_light);
            colorControlIconAccent = ContextCompat.getColor(this, R.color.control_icon_accent_light);
            colorBadgeBg = ContextCompat.getColor(this, R.color.badge_bg_light);
            colorBadgeText = ContextCompat.getColor(this, R.color.badge_text_light);

            colorPresetBtnBg = ContextCompat.getColor(this, R.color.preset_btn_bg_light);
            colorPresetBtnStroke = ContextCompat.getColor(this, R.color.preset_btn_stroke_light);
            colorPresetBtnText = ContextCompat.getColor(this, R.color.preset_btn_text_light);
        } else {
            colorClockBg = ContextCompat.getColor(this, R.color.clock_bg_dark);

            colorInactiveBg = ContextCompat.getColor(this, R.color.card_inactive_bg_dark);
            colorInactiveStroke = ContextCompat.getColor(this, R.color.card_inactive_stroke_dark);
            colorTextInactivePrimary = ContextCompat.getColor(this, R.color.text_inactive_primary_dark);
            colorTextInactiveSecondary = ContextCompat.getColor(this, R.color.text_inactive_secondary_dark);

            colorActiveBg = ContextCompat.getColor(this, R.color.card_active_bg_dark);
            colorActiveStroke = ContextCompat.getColor(this, R.color.card_active_stroke_dark);
            colorTextActivePrimary = ContextCompat.getColor(this, R.color.text_active_primary_dark);
            colorTextActiveAccent = ContextCompat.getColor(this, R.color.text_active_accent_dark);

            colorFlaggedBg = ContextCompat.getColor(this, R.color.card_flagged_bg_dark);
            colorFlaggedStroke = ContextCompat.getColor(this, R.color.card_flagged_stroke_dark);
            colorTextFlaggedPrimary = ContextCompat.getColor(this, R.color.text_flagged_primary_dark);
            colorTextFlaggedAccent = ContextCompat.getColor(this, R.color.text_flagged_accent_dark);

            colorControlBarBg = ContextCompat.getColor(this, R.color.control_bar_bg_dark);
            colorControlBarStroke = ContextCompat.getColor(this, R.color.control_bar_stroke_dark);
            colorControlIcon = ContextCompat.getColor(this, R.color.control_icon_dark);
            colorControlIconAccent = ContextCompat.getColor(this, R.color.control_icon_accent_dark);
            colorBadgeBg = ContextCompat.getColor(this, R.color.badge_bg_dark);
            colorBadgeText = ContextCompat.getColor(this, R.color.badge_text_dark);

            colorPresetBtnBg = ContextCompat.getColor(this, R.color.preset_btn_bg_dark);
            colorPresetBtnStroke = ContextCompat.getColor(this, R.color.preset_btn_stroke_dark);
            colorPresetBtnText = ContextCompat.getColor(this, R.color.preset_btn_text_dark);
        }
    }

    private void initViews() {
        rootLayout = findViewById(R.id.rootLayout);
        centerControlBar = findViewById(R.id.centerControlBar);

        cards[0] = findViewById(R.id.cardBottom);
        cards[1] = findViewById(R.id.cardTop);

        tvTimes[0] = findViewById(R.id.tvTimeBottom);
        tvTimes[1] = findViewById(R.id.tvTimeTop);

        tvMoves[0] = findViewById(R.id.tvMovesBottom);
        tvMoves[1] = findViewById(R.id.tvMovesTop);

        tvPlayers[0] = findViewById(R.id.tvPlayerBottom);
        tvPlayers[1] = findViewById(R.id.tvPlayerTop);

        tvStatuses[0] = findViewById(R.id.tvStatusBottom);
        tvStatuses[1] = findViewById(R.id.tvStatusTop);

        tvIncrements[0] = findViewById(R.id.tvIncrementBottom);
        tvIncrements[1] = findViewById(R.id.tvIncrementTop);

        cards[0].setOnClickListener(v -> handleTap(0, v));
        cards[1].setOnClickListener(v -> handleTap(1, v));

        btnReset = findViewById(R.id.btnReset);
        btnTheme = findViewById(R.id.btnTheme);
        btnPlayPause = findViewById(R.id.btnPlayPause);
        btnTimePreset = findViewById(R.id.btnTimePreset);
        btnSound = findViewById(R.id.btnSound);

        btnPlayPause.setOnClickListener(v -> togglePlayPause());
        btnReset.setOnClickListener(v -> confirmReset());
        btnTheme.setOnClickListener(v -> toggleThemeMode());
        btnTimePreset.setOnClickListener(v -> showTimePresetDialog());
        btnSound.setOnClickListener(v -> toggleSound());
    }

    private void toggleThemeMode() {
        isLightMode = !isLightMode;
        getSharedPreferences(PREFS_NAME, MODE_PRIVATE)
                .edit()
                .putBoolean(KEY_LIGHT_MODE, isLightMode)
                .apply();

        btnTheme.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP);
        loadColors();
        applyThemeMode();
        updateUI();
    }

    private void applyThemeMode() {
        if (rootLayout != null) {
            rootLayout.setBackgroundColor(colorClockBg);
        }

        getWindow().setStatusBarColor(colorClockBg);
        getWindow().setNavigationBarColor(colorClockBg);

        WindowInsetsControllerCompat insetsController = WindowCompat.getInsetsController(getWindow(), getWindow().getDecorView());
        if (insetsController != null) {
            insetsController.setAppearanceLightStatusBars(isLightMode);
            insetsController.setAppearanceLightNavigationBars(isLightMode);
        }

        if (centerControlBar != null) {
            GradientDrawable barDrawable = new GradientDrawable();
            barDrawable.setShape(GradientDrawable.RECTANGLE);
            barDrawable.setCornerRadius(dpToPx(32));
            barDrawable.setColor(colorControlBarBg);
            barDrawable.setStroke(dpToPx(1.5f), colorControlBarStroke);
            centerControlBar.setBackground(barDrawable);
        }

        if (btnReset != null) btnReset.setImageTintList(ColorStateList.valueOf(colorControlIcon));
        if (btnTheme != null) {
            btnTheme.setImageResource(isLightMode ? R.drawable.ic_dark_mode : R.drawable.ic_light_mode);
            btnTheme.setImageTintList(ColorStateList.valueOf(colorControlIcon));
        }
        if (btnPlayPause != null) btnPlayPause.setImageTintList(ColorStateList.valueOf(colorControlIconAccent));
        if (btnSound != null) btnSound.setImageTintList(ColorStateList.valueOf(colorControlIcon));

        if (btnTimePreset != null) {
            btnTimePreset.setBackgroundTintList(ColorStateList.valueOf(colorPresetBtnBg));
            btnTimePreset.setStrokeColor(ColorStateList.valueOf(colorPresetBtnStroke));
            btnTimePreset.setTextColor(colorPresetBtnText);
            btnTimePreset.setIconTint(ColorStateList.valueOf(colorPresetBtnText));
        }

        for (int p = 0; p < 2; p++) {
            if (tvMoves[p] != null) {
                GradientDrawable badgeDrawable = new GradientDrawable();
                badgeDrawable.setShape(GradientDrawable.RECTANGLE);
                badgeDrawable.setCornerRadius(dpToPx(12));
                badgeDrawable.setColor(colorBadgeBg);
                badgeDrawable.setStroke(dpToPx(1), colorPresetBtnStroke);
                tvMoves[p].setBackground(badgeDrawable);
                tvMoves[p].setTextColor(colorBadgeText);
            }
        }
    }

    private int dpToPx(float dp) {
        return Math.round(dp * getResources().getDisplayMetrics().density);
    }

    private void initAudio() {
        try {
            toneGenerator = new ToneGenerator(AudioManager.STREAM_MUSIC, 85);
        } catch (Exception ignored) {
            toneGenerator = null;
        }
    }

    private void handleTap(int player, View view) {
        long now = SystemClock.elapsedRealtime();

        if (active == -1 && moves[0] + moves[1] == 0) {
            moves[player]++;
            active = 1 - player;
            isPaused = false;
            lastTimestamp = now;
            provideFeedback(view);
            handler.removeCallbacks(tick);
            handler.post(tick);
            updateUI();
            return;
        }

        if (isPaused) {
            return;
        }

        if (active == player) {
            time[player] -= (now - lastTimestamp);
            int inc = presets[selectedPresetIndex].incrementSeconds;
            if (inc > 0) {
                time[player] += (inc * 1000L);
            }
            moves[player]++;
            active = 1 - player;
            lastTimestamp = now;
            provideFeedback(view);
            updateUI();
        }
    }

    private void togglePlayPause() {
        if (active == -1 && !isPaused) return;

        if (isPaused) {
            isPaused = false;
            lastTimestamp = SystemClock.elapsedRealtime();
            handler.removeCallbacks(tick);
            handler.post(tick);
        } else {
            isPaused = true;
            handler.removeCallbacks(tick);
        }
        updateUI();
    }

    private void confirmReset() {
        if (moves[0] + moves[1] > 0 && (time[0] > 0 && time[1] > 0)) {
            new MaterialAlertDialogBuilder(this)
                    .setTitle(R.string.dialog_reset_title)
                    .setMessage(R.string.dialog_reset_message)
                    .setPositiveButton(R.string.btn_reset, (dialog, which) -> resetGame(selectedPresetIndex))
                    .setNegativeButton(R.string.btn_cancel, null)
                    .show();
        } else {
            resetGame(selectedPresetIndex);
        }
    }

    private void resetGame(int presetIndex) {
        handler.removeCallbacks(tick);
        selectedPresetIndex = presetIndex;
        TimeControl tc = presets[presetIndex];
        time[0] = tc.timeMillis;
        time[1] = tc.timeMillis;
        moves[0] = 0;
        moves[1] = 0;
        active = -1;
        isPaused = false;

        btnTimePreset.setText(tc.shortLabel);
        String incText = tc.incrementSeconds > 0 ? "+" + tc.incrementSeconds + "s" : "+0s";
        tvIncrements[0].setText(incText);
        tvIncrements[1].setText(incText);

        updateUI();
    }

    private void showTimePresetDialog() {
        String[] options = new String[presets.length];
        for (int i = 0; i < presets.length; i++) {
            options[i] = presets[i].label;
        }

        new MaterialAlertDialogBuilder(this)
                .setTitle(R.string.time_preset_dialog_title)
                .setSingleChoiceItems(options, selectedPresetIndex, (dialog, which) -> {
                    resetGame(which);
                    dialog.dismiss();
                })
                .setNegativeButton(R.string.btn_cancel, null)
                .show();
    }

    private void toggleSound() {
        soundEnabled = !soundEnabled;
        btnSound.setImageResource(soundEnabled ? R.drawable.ic_volume_up : R.drawable.ic_volume_off);
        btnSound.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP);
    }

    private void provideFeedback(View view) {
        view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP);
        if (soundEnabled && toneGenerator != null) {
            try {
                toneGenerator.startTone(ToneGenerator.TONE_PROP_BEEP, 40);
            } catch (Exception ignored) {
            }
        }
    }

    private void playTimeoutAlert() {
        if (soundEnabled && toneGenerator != null) {
            try {
                toneGenerator.startTone(ToneGenerator.TONE_PROP_BEEP2, 400);
            } catch (Exception ignored) {
            }
        }
    }

    private String formatTime(long ms) {
        if (ms <= 0) return "00:00";

        if (ms < 10000) {
            long seconds = ms / 1000;
            long tenths = (ms % 1000) / 100;
            return String.format(Locale.getDefault(), "0:0%d.%d", seconds, tenths);
        }

        long totalSeconds = (ms + 999) / 1000;
        long minutes = totalSeconds / 60;
        long seconds = totalSeconds % 60;
        return String.format(Locale.getDefault(), "%02d:%02d", minutes, seconds);
    }

    private void updateUI() {
        btnPlayPause.setImageResource(isPaused ? R.drawable.ic_play : R.drawable.ic_pause);

        for (int p = 0; p < 2; p++) {
            tvTimes[p].setText(formatTime(time[p]));
            String unit = moves[p] == 1 ? getString(R.string.move_singular) : getString(R.string.move_plural);
            tvMoves[p].setText(getString(R.string.moves_format, moves[p], unit));

            if (time[p] == 0 && (moves[0] + moves[1] > 0)) {
                cards[p].setCardBackgroundColor(colorFlaggedBg);
                cards[p].setStrokeColor(colorFlaggedStroke);
                tvTimes[p].setTextColor(colorTextFlaggedPrimary);
                tvPlayers[p].setTextColor(colorTextFlaggedAccent);
                tvStatuses[p].setTextColor(colorTextFlaggedAccent);
                tvStatuses[p].setText(R.string.status_flagged);
            } else if (p == active && !isPaused) {
                cards[p].setCardBackgroundColor(colorActiveBg);
                cards[p].setStrokeColor(colorActiveStroke);
                tvTimes[p].setTextColor(colorTextActivePrimary);
                tvPlayers[p].setTextColor(colorTextActiveAccent);
                tvStatuses[p].setTextColor(colorTextActiveAccent);
                tvStatuses[p].setText(R.string.status_active);
            } else {
                cards[p].setCardBackgroundColor(colorInactiveBg);
                cards[p].setStrokeColor(colorInactiveStroke);
                tvTimes[p].setTextColor(colorTextInactivePrimary);
                tvPlayers[p].setTextColor(colorTextInactiveSecondary);
                tvStatuses[p].setTextColor(colorTextInactiveSecondary);

                if (isPaused) {
                    tvStatuses[p].setText(R.string.status_paused);
                } else if (active == -1 && moves[0] + moves[1] == 0) {
                    tvStatuses[p].setText(R.string.status_tap_to_start);
                } else {
                    tvStatuses[p].setText(R.string.status_waiting);
                }
            }
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        handler.removeCallbacks(tick);
        if (toneGenerator != null) {
            toneGenerator.release();
            toneGenerator = null;
        }
    }
}
