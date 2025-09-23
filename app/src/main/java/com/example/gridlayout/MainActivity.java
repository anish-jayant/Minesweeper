package com.example.gridlayout;

import androidx.appcompat.app.AppCompatActivity;
import androidx.gridlayout.widget.GridLayout;

import android.content.res.Resources;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.Random;

public class MainActivity extends AppCompatActivity {

    private static final int COLUMN_COUNT = 10;
    private static final int BOMB_COUNT = 5;
    private static final String TAG = "Minesweeper";

    private ArrayList<TextView> cell_tvs;
    private ArrayList<Integer> bombIndices;

    private TextView statusText;
    private TextView flagCounter;
    private TextView timerText;
    private Button restartButton;
    private GridLayout grid;

    private Handler timerHandler;
    private Runnable timerRunnable;
    private int secondsElapsed;
    private boolean gameOver;

    private int revealedSafeSquares;
    private int correctlyFlaggedBombs;
    private int remainingFlags;

    private int dpToPixel(int dp) {
        float density = Resources.getSystem().getDisplayMetrics().density;
        return Math.round(dp * density);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        setTitle("CSCI 310 Minesweeper: Anish Jayant");

        statusText = findViewById(R.id.statusText);
        flagCounter = findViewById(R.id.flagCounter);
        timerText = findViewById(R.id.timerText);
        restartButton = findViewById(R.id.restartButton);
        grid = findViewById(R.id.gridLayout01);

        restartButton.setOnClickListener(v -> resetGame());

        setupBoard();
    }

    private void setupBoard() {
        cell_tvs = new ArrayList<>();
        grid.removeAllViews();
        grid.setRowCount(COLUMN_COUNT);
        grid.setColumnCount(COLUMN_COUNT);

        gameOver = false;
        revealedSafeSquares = 0;
        correctlyFlaggedBombs = 0;
        remainingFlags = BOMB_COUNT;

        bombIndices = new ArrayList<>();
        Random rand = new Random();
        while (bombIndices.size() < BOMB_COUNT) {
            int idx = rand.nextInt(COLUMN_COUNT * COLUMN_COUNT);
            if (!bombIndices.contains(idx)) {
                bombIndices.add(idx);
            }
        }

        for (int idx : bombIndices) {
            int row = idx / COLUMN_COUNT;
            int col = idx % COLUMN_COUNT;
            Log.d(TAG, "Bomb at: (" + row + "," + col + ")");
        }

        for (int i = 0; i < COLUMN_COUNT; i++) {
            for (int j = 0; j < COLUMN_COUNT; j++) {
                TextView tv = new TextView(this);
                tv.setTextColor(Color.GRAY);
                tv.setBackgroundColor(Color.parseColor("#32CD32")); // lime green
                tv.setTextAlignment(TextView.TEXT_ALIGNMENT_CENTER);
                tv.setTextSize(16);
                tv.setOnClickListener(this::onClickTV);

                GridLayout.LayoutParams lp =
                        new GridLayout.LayoutParams(GridLayout.spec(i, 1f), GridLayout.spec(j, 1f));
                lp.width = 0;
                lp.height = 0;
                lp.setMargins(dpToPixel(1), dpToPixel(1), dpToPixel(1), dpToPixel(1));

                grid.addView(tv, lp);
                cell_tvs.add(tv);
            }
        }

        statusText.setVisibility(View.GONE);
        restartButton.setVisibility(View.GONE);

        flagCounter.setText("🚩 " + remainingFlags);
        timerText.setText("⏱ 0s");

        secondsElapsed = 0;
        timerHandler = new Handler(Looper.getMainLooper());
        timerRunnable = new Runnable() {
            @Override
            public void run() {
                if (!gameOver) {
                    secondsElapsed++;
                    timerText.setText("⏱ " + secondsElapsed + "s");
                    timerHandler.postDelayed(this, 1000);
                }
            }
        };
        timerHandler.postDelayed(timerRunnable, 1000);
    }

    private int findIndexOfCellTextView(TextView tv) {
        for (int n = 0; n < cell_tvs.size(); n++) {
            if (cell_tvs.get(n) == tv) return n;
        }
        return -1;
    }

    public void onClickTV(View view) {
        if (gameOver) return;

        TextView tv = (TextView) view;
        int n = findIndexOfCellTextView(tv);
        int row = n / COLUMN_COUNT;
        int col = n % COLUMN_COUNT;

        Object tag = tv.getTag();
        if ("revealed".equals(tag)) return;

        int state = (tag instanceof Integer) ? (Integer) tag : 0;

        if (state == 0) {
            if (remainingFlags > 0
                    && tv.getBackground() instanceof ColorDrawable
                    && ((ColorDrawable) tv.getBackground()).getColor() == Color.parseColor("#32CD32")) {
                tv.setText("🚩");
                tv.setTextColor(Color.RED);
                tv.setTag(1); // flagged
                remainingFlags--;
                flagCounter.setText("🚩 " + remainingFlags);

                if (bombIndices.contains(n)) {
                    correctlyFlaggedBombs++;
                }
                checkWinCondition();
            }
            return;
        }

        if (state == 1) {
            tv.setText("");
            remainingFlags++;
            if (remainingFlags > BOMB_COUNT) remainingFlags = BOMB_COUNT;
            flagCounter.setText("🚩 " + remainingFlags);

            boolean wasCorrectFlag = bombIndices.contains(n);
            if (wasCorrectFlag && correctlyFlaggedBombs > 0) {
                correctlyFlaggedBombs--;
            }

            tv.setTag(2);

            if (wasCorrectFlag) {

                tv.setText("💣");
                tv.setTextColor(Color.WHITE);
                tv.setBackgroundColor(Color.RED);

                for (int idx : bombIndices) {
                    TextView bombCell = cell_tvs.get(idx);
                    bombCell.setText("💣");
                    bombCell.setTextColor(Color.WHITE);
                    bombCell.setBackgroundColor(Color.BLACK);
                }

                gameOver = true;
                statusText.setText("YOU LOSE (" + secondsElapsed + "s)");
                statusText.setTextColor(Color.RED);
                statusText.setVisibility(View.VISIBLE);
                restartButton.setVisibility(View.VISIBLE);
            } else {

                revealSquare(row, col);
                checkWinCondition();
            }
            return;
        }

    }


    private void revealSquare(int row, int col) {
        int idx = row * COLUMN_COUNT + col;
        TextView tv = cell_tvs.get(idx);

        if (tv.getTag() != null && tv.getTag().equals("revealed")) {
            return;
        }

        int neighborBombs = countNeighborBombs(row, col);

        tv.setBackgroundColor(Color.LTGRAY);
        if (neighborBombs > 0) {
            tv.setText(String.valueOf(neighborBombs));
            tv.setTextColor(getNumberColor(neighborBombs));
        } else {
            tv.setText("");
        }

        tv.setTag("revealed");
        revealedSafeSquares++;

        if (neighborBombs == 0) {
            revealEmptyNeighbors(row, col);
        }
    }

    private void revealEmptyNeighbors(int row, int col) {
        int[] dRow = {-1, -1, -1, 0, 0, 1, 1, 1};
        int[] dCol = {-1, 0, 1, -1, 1, -1, 0, 1};

        for (int k = 0; k < 8; k++) {
            int newRow = row + dRow[k];
            int newCol = col + dCol[k];

            if (newRow >= 0 && newRow < COLUMN_COUNT &&
                    newCol >= 0 && newCol < COLUMN_COUNT) {
                int idx = newRow * COLUMN_COUNT + newCol;
                if (!bombIndices.contains(idx)) {
                    revealSquare(newRow, newCol);
                }
            }
        }
    }

    private int countNeighborBombs(int row, int col) {
        int count = 0;
        int[] dRow = {-1, -1, -1, 0, 0, 1, 1, 1};
        int[] dCol = {-1, 0, 1, -1, 1, -1, 0, 1};

        for (int k = 0; k < 8; k++) {
            int newRow = row + dRow[k];
            int newCol = col + dCol[k];

            if (newRow >= 0 && newRow < COLUMN_COUNT &&
                    newCol >= 0 && newCol < COLUMN_COUNT) {
                int neighborIndex = newRow * COLUMN_COUNT + newCol;
                if (bombIndices.contains(neighborIndex)) {
                    count++;
                }
            }
        }
        return count;
    }

    private int getNumberColor(int num) {
        switch (num) {
            case 1: return Color.BLUE;
            case 2: return Color.parseColor("#228B22");
            case 3: return Color.RED;
            case 4: return Color.rgb(0, 0, 139);
            case 5: return Color.rgb(139, 69, 19);
            case 6: return Color.CYAN;
            case 7: return Color.BLACK;
            case 8: return Color.DKGRAY;
            default: return Color.BLACK;
        }
    }

    private void checkWinCondition() {
        int totalSafeSquares = COLUMN_COUNT * COLUMN_COUNT - BOMB_COUNT;

        if (revealedSafeSquares == totalSafeSquares || correctlyFlaggedBombs == BOMB_COUNT) {
            gameOver = true;

            for (int idx : bombIndices) {
                TextView bombCell = cell_tvs.get(idx);
                bombCell.setText("🚩");
                bombCell.setTextColor(Color.RED);
                bombCell.setBackgroundColor(Color.LTGRAY);
            }

            statusText.setText("YOU WIN (" + secondsElapsed + "s)");
            statusText.setTextColor(Color.parseColor("#4CAF50"));
            statusText.setVisibility(View.VISIBLE);
            restartButton.setVisibility(View.VISIBLE);
        }
    }

    private void resetGame() {
        setupBoard();
    }
}
