package developer.android.vd.diceroller;

import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.res.ResourcesCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdView;
import com.google.android.gms.ads.MobileAds;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Random;
import java.util.Timer;
import java.util.TimerTask;

public class MainActivity extends AppCompatActivity {
    private RelativeLayout mainRl;
    private ImageView dice1, dice2, dice3, dice4, dice5, dice6, dice7, dice8, dice9;
    private TextView total, number;
    private AdView adView;

    private ArrayList<ImageView> validDices, allDiceList;
    private final Handler handler = new Handler(Looper.getMainLooper());
    private Timer timer;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.rl_main_container), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        initializeViews();
        initializeAd();

        Button rollButton = findViewById(R.id.btn_roll);
        rollButton.setOnClickListener(this::roll);

        allDiceList = new ArrayList<>(Arrays.asList(dice1, dice2, dice3, dice4, dice5, dice6, dice7, dice8, dice9));
        validDices = new ArrayList<>();
        validDices.add(dice5);//when app starts
    }

    private void initializeViews() {
        mainRl = findViewById(R.id.rl_main_container);
        number = findViewById(R.id.tv_no_dice);
        dice1 = findViewById(R.id.iv_dice1);
        dice2 = findViewById(R.id.iv_dice2);
        dice3 = findViewById(R.id.iv_dice3);
        dice4 = findViewById(R.id.iv_dice4);
        dice5 = findViewById(R.id.iv_dice5);
        dice6 = findViewById(R.id.iv_dice6);
        dice7 = findViewById(R.id.iv_dice7);
        dice8 = findViewById(R.id.iv_dice8);
        dice9 = findViewById(R.id.iv_dice9);
        total = findViewById(R.id.tv_total);

        findViewById(R.id.fb_settings).setOnClickListener(view -> startActivity(new Intent(getApplicationContext(), SettingsActivity.class)));
        findViewById(R.id.ib_minus).setOnClickListener(this::minusOne);
        findViewById(R.id.ib_plus).setOnClickListener(this::plusOne);
    }

    private void initializeAd() {
        MobileAds.initialize(this, initializationStatus -> {
        });
        AdRequest adRequest = new AdRequest.Builder().build();
        adView = findViewById(R.id.adView);
        adView.loadAd(adRequest);
    }

    private void setDiceVisibility(List<ImageView> visibleList) {
        for (ImageView iView : allDiceList) {
            iView.setVisibility(View.INVISIBLE);
        }
        for (ImageView iView : visibleList) {
            validDices.add(iView);
            iView.setVisibility(View.VISIBLE);
        }
    }

    private void resetDices(ArrayList<ImageView> diceList) {
        for (ImageView dice : diceList) {
            dice.setImageDrawable(ResourcesCompat.getDrawable(getResources(), R.drawable.dice_none_solid, null));
        }
    }

    public void roll(View view) {
        for (ImageView dice : validDices) {
            animateDice(dice);
        }

        handler.postDelayed(() -> {
            for (ImageView dice : validDices) {
                dice.clearAnimation(); // Stop the animation
                int randomInt = new Random().nextInt(6) + 1;
                setViewForDice(dice, randomInt);
            }

            int sum = 0;
            for (ImageView dice : validDices) {
                int randomInt = new Random().nextInt(6) + 1;
                sum += randomInt;
                setViewForDice(dice, randomInt);
            }
            String totalText = "TOTAL : " + sum;
            total.setText(totalText);
        }, 1000); // Delay for the duration of the animation
    }

    private void animateDice(ImageView dice) {
        int randomMultiplierX = new Random().nextInt(2) * 2 - 1;//-1 or 1
        int randomMultiplierY = new Random().nextInt(2) * 2 - 1;//-1 or 1
        int randomMultiplierZ = new Random().nextInt(2) * 2 - 1;//-1 or 1
        ObjectAnimator rotateX = ObjectAnimator.ofFloat(dice, "rotationX", 0f, randomMultiplierX * 1800f);
        ObjectAnimator rotateY = ObjectAnimator.ofFloat(dice, "rotationY", 0f, randomMultiplierY * 1800f);
        ObjectAnimator rotateZ = ObjectAnimator.ofFloat(dice, "rotation", 0f, randomMultiplierZ * 1800f);
        rotateX.setDuration(1000);
        rotateY.setDuration(1000);
        rotateZ.setDuration(1000);

        AnimatorSet animatorSet = new AnimatorSet();
        animatorSet.playTogether(rotateX, rotateY, rotateZ);
        animatorSet.start();
    }


    private void setViewForDice(ImageView diceView, int number) {
        switch (number) {
            case 1:
                diceView.setImageDrawable(ResourcesCompat.getDrawable(getResources(), R.drawable.dice_one_solid, null));
                break;
            case 2:
                diceView.setImageDrawable(ResourcesCompat.getDrawable(getResources(), R.drawable.dice_two_solid, null));
                break;
            case 3:
                diceView.setImageDrawable(ResourcesCompat.getDrawable(getResources(), R.drawable.dice_three_solid, null));
                break;
            case 4:
                diceView.setImageDrawable(ResourcesCompat.getDrawable(getResources(), R.drawable.dice_four_solid, null));
                break;
            case 5:
                diceView.setImageDrawable(ResourcesCompat.getDrawable(getResources(), R.drawable.dice_five_solid, null));
                break;
            case 6:
                diceView.setImageDrawable(ResourcesCompat.getDrawable(getResources(), R.drawable.dice_six_solid, null));
                break;
        }
    }

    public void plusOne(View view) {
        int count = Integer.parseInt(number.getText().toString());
        if (count < 9) {
            count++;
            rearrangeDice(count);
            number.setText(String.valueOf(count));
        }
    }

    public void minusOne(View view) {
        int count = Integer.parseInt(number.getText().toString());
        if (count > 1) {
            count--;
            rearrangeDice(count);
            number.setText(String.valueOf(count));
        }
    }

    private void rearrangeDice(int number) {
        validDices.clear();
        switch (number) {
            case 2:
                setDiceVisibility(Arrays.asList(dice4, dice6));
                break;
            case 3:
                setDiceVisibility(Arrays.asList(dice1, dice5, dice9));
                break;
            case 4:
                setDiceVisibility(Arrays.asList(dice1, dice3, dice7, dice9));
                break;
            case 5:
                setDiceVisibility(Arrays.asList(dice1, dice3, dice5, dice7, dice9));
                break;
            case 6:
                setDiceVisibility(Arrays.asList(dice1, dice3, dice4, dice6, dice7, dice9));
                break;
            case 7:
                setDiceVisibility(Arrays.asList(dice1, dice3, dice4, dice5, dice6, dice7, dice9));
                break;
            case 8:
                setDiceVisibility(Arrays.asList(dice1, dice2, dice3, dice4, dice6, dice7, dice8, dice9));
                break;
            case 9:
                setDiceVisibility(Arrays.asList(dice1, dice2, dice3, dice4, dice5, dice6, dice7, dice8, dice9));
                break;
            default:
                setDiceVisibility(Collections.singletonList(dice5));
                break;
        }
        resetDices(validDices);
        total.setText(getResources().getString(R.string.total));
    }


    @Override
    protected void onStart() {
        super.onStart();
        startTimerForAds();

        SharedPreferences sharedPreferences = getSharedPreferences(getString(R.string.app_name), MODE_PRIVATE);

        boolean totalVisibility = sharedPreferences.getBoolean(getString(R.string.total_visibility), true);
        total.setVisibility(totalVisibility ? View.VISIBLE : View.INVISIBLE);

        int selectedBackgroundColor = sharedPreferences.getInt(getString(R.string.selected_background), Color.parseColor("#33B5E5"));
        mainRl.setBackgroundColor(selectedBackgroundColor);
    }

    private void startTimerForAds() {
        resetTimer();
        timer = new Timer();
        timer.schedule(new TimerTask() {
            @Override
            public void run() {
                new Handler(Looper.getMainLooper()).post(() -> updateTimeForAds());
            }
        }, 0, 1000); // Update every second
    }

    private void updateTimeForAds() {
        SharedPreferences sharedPreferences = getSharedPreferences(getString(R.string.app_name), Context.MODE_PRIVATE);
        long timerCutOff = sharedPreferences.getLong(getString(R.string.pause_ads_cut_off_time), System.currentTimeMillis());
        if (timerCutOff > System.currentTimeMillis()) {
            adView.setVisibility(View.GONE);
        } else {
            adView.setVisibility(View.VISIBLE);
        }
    }

    private void resetTimer() {
        if (timer != null) {
            timer.cancel();
            timer = null;
        }
    }
}