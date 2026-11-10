package com.example.chessclock;

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
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import java.util.Locale;

public class MainActivity extends AppCompatActivity {

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

    private ToneGenerator toneGenerator;
    private final Handler handler = new Handler(Looper.getMainLooper());

    // Views
    private final MaterialCardView[] cards = new MaterialCardView[2];
    private final TextView[] tvTimes = new TextView[2];
    private final TextView[] tvMoves = new TextView[2];
    private final TextView[] tvPlayers = new TextView[2];
    private final TextView[] tvStatuses = new TextView[2];
    private final TextView[] tvIncrements = new TextView[2];

    private ImageButton btnPlayPause;
    private MaterialButton btnTimePreset;
    private ImageButton btnSound;

    // Colors
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
            // Faster tick rate (30ms) for smooth tenths-of-a-second display in low time scrambles
            long delay = time[active] < 10000 ? 30 : 100;
            handler.postDelayed(this, delay);
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Keep screen awake during chess matches
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        loadColors();
        initViews();
        initAudio();
        resetGame(selectedPresetIndex);
    }

    private void loadColors() {
        colorInactiveBg = ContextCompat.getColor(this, R.color.card_inactive_bg);
        colorInactiveStroke = ContextCompat.getColor(this, R.color.card_inactive_stroke);
        colorTextInactivePrimary = ContextCompat.getColor(this, R.color.text_inactive_primary);
        colorTextInactiveSecondary = ContextCompat.getColor(this, R.color.text_inactive_secondary);

        colorActiveBg = ContextCompat.getColor(this, R.color.card_active_bg);
        colorActiveStroke = ContextCompat.getColor(this, R.color.card_active_stroke);
        colorTextActivePrimary = ContextCompat.getColor(this, R.color.text_active_primary);
        colorTextActiveAccent = ContextCompat.getColor(this, R.color.text_active_accent);

        colorFlaggedBg = ContextCompat.getColor(this, R.color.card_flagged_bg);
        colorFlaggedStroke = ContextCompat.getColor(this, R.color.card_flagged_stroke);
        colorTextFlaggedPrimary = ContextCompat.getColor(this, R.color.text_flagged_primary);
        colorTextFlaggedAccent = ContextCompat.getColor(this, R.color.text_flagged_accent);
    }

    private void initViews() {
        // Player 0 = Bottom (White), Player 1 = Top (Black)
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

        btnPlayPause = findViewById(R.id.btnPlayPause);
        btnTimePreset = findViewById(R.id.btnTimePreset);
        btnSound = findViewById(R.id.btnSound);

        ImageButton btnReset = findViewById(R.id.btnReset);

        btnPlayPause.setOnClickListener(v -> togglePlayPause());
        btnReset.setOnClickListener(v -> confirmReset());
        btnTimePreset.setOnClickListener(v -> showTimePresetDialog());
        btnSound.setOnClickListener(v -> toggleSound());
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

        // If game has not started yet
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

        // If game is paused, ignore card tap until resumed
        if (isPaused) {
            return;
        }

        // Only the player whose turn it is can tap to switch
        if (active == player) {
            time[player] -= (now - lastTimestamp);
            // Apply Fischer increment if configured
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
        // Can't pause if game has not started or already finished
        if (active == -1 && !isPaused) return;

        if (isPaused) {
            // Resume
            isPaused = false;
            lastTimestamp = SystemClock.elapsedRealtime();
            handler.removeCallbacks(tick);
            handler.post(tick);
        } else {
            // Pause
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

        // Under 10 seconds: show tenths of a second for high-stakes scramble
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
        // Play / Pause Icon
        btnPlayPause.setImageResource(isPaused ? R.drawable.ic_play : R.drawable.ic_pause);

        for (int p = 0; p < 2; p++) {
            tvTimes[p].setText(formatTime(time[p]));
            String unit = moves[p] == 1 ? getString(R.string.move_singular) : getString(R.string.move_plural);
            tvMoves[p].setText(getString(R.string.moves_format, moves[p], unit));

            // Check if player timed out
            if (time[p] == 0 && (moves[0] + moves[1] > 0)) {
                cards[p].setCardBackgroundColor(colorFlaggedBg);
                cards[p].setStrokeColor(colorFlaggedStroke);
                tvTimes[p].setTextColor(colorTextFlaggedPrimary);
                tvPlayers[p].setTextColor(colorTextFlaggedAccent);
                tvStatuses[p].setTextColor(colorTextFlaggedAccent);
                tvStatuses[p].setText(R.string.status_flagged);
            } else if (p == active && !isPaused) {
                // Active player
                cards[p].setCardBackgroundColor(colorActiveBg);
                cards[p].setStrokeColor(colorActiveStroke);
                tvTimes[p].setTextColor(colorTextActivePrimary);
                tvPlayers[p].setTextColor(colorTextActiveAccent);
                tvStatuses[p].setTextColor(colorTextActiveAccent);
                tvStatuses[p].setText(R.string.status_active);
            } else {
                // Waiting / Inactive player
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
