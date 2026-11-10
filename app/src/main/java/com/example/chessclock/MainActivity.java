package com.example.chessclock;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;
import android.widget.Button;
import androidx.appcompat.app.AppCompatActivity;

public class MainActivity extends AppCompatActivity {

    private final long[] time = {180000, 180000}; // 3 minutes each, in milliseconds
    private final int[] moves = {0, 0};
    private int active = -1;   // whose clock is running: 0, 1, or -1 for none
    private long last;
    private final Button[] btn = new Button[2];
    private final Handler handler = new Handler(Looper.getMainLooper());

    private final Runnable tick = new Runnable() {
        @Override
        public void run() {
            if (active < 0) return;
            long now = SystemClock.elapsedRealtime();
            time[active] -= now - last;
            last = now;
            if (time[active] <= 0) {
                time[active] = 0;
                active = -1;          // time is up
                update();
                return;
            }
            update();
            handler.postDelayed(this, 50);
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        btn[0] = findViewById(R.id.btnBottom);
        btn[1] = findViewById(R.id.btnTop);
        btn[0].setOnClickListener(v -> tap(0));
        btn[1].setOnClickListener(v -> tap(1));
        update();
    }

    private void tap(int p) {
        long now = SystemClock.elapsedRealtime();
        if (active == -1 && moves[0] + moves[1] == 0) {
            // first move starts the game
            moves[p]++;
            active = 1 - p;
            last = now;
            handler.post(tick);
        } else if (active == p) {
            time[p] -= now - last;
            last = now;
            moves[p]++;
            active = 1 - p;
        }
        update();
    }

    private String fmt(long ms) {
        long total = (ms + 999) / 1000;
        return (total / 60) + ":" + String.format("%02d", total % 60);
    }

    private void update() {
        for (int p = 0; p < 2; p++) {
            btn[p].setText(fmt(time[p]) + "\nMoves: " + moves[p]);
        }
    }
}