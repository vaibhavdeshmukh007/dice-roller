package developer.android.vd.diceroller;

import android.graphics.Typeface;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdView;
import com.google.android.gms.ads.MobileAds;
import com.google.android.gms.ads.initialization.InitializationStatus;
import com.google.android.gms.ads.initialization.OnInitializationCompleteListener;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Random;

public class MainActivity extends AppCompatActivity {

    private ImageView dice1, dice2, dice3, dice4, dice5, dice6, dice7, dice8, dice9;
    private TextView total, number;
    private ArrayList<ImageView> validDices, allDiceList;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

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
        Button rollButton = findViewById(R.id.btn_roll);

        Typeface stylishFont = Typeface.createFromAsset(getAssets(), "fonts/Aladin-Regular.ttf");
        rollButton.setTypeface(stylishFont);

        allDiceList = new ArrayList<>(Arrays.asList(dice1, dice2, dice3, dice4, dice5, dice6, dice7, dice8, dice9));

        validDices = new ArrayList<>();
        validDices.add(dice5);//when app starts

        MobileAds.initialize(this, initializationStatus -> {});

        AdView adView = findViewById(R.id.adView);
        AdRequest adRequest = new AdRequest.Builder().build();
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
            dice.setImageDrawable(getResources().getDrawable(R.drawable.dice_none_svg));
        }
    }

    public void roll(View view) {
        UpdateUI runner = new UpdateUI();
        runner.execute();
    }

    private void setView(int digit, ArrayList<ImageView> diceList) {
        for (ImageView dice : diceList) {
            setViewForDice(dice, digit);
        }
    }

    private void setViewForDice(ImageView dice, int number) {
        switch (number) {
            case 1:
                dice.setImageDrawable(getResources().getDrawable(R.drawable.dice_one_svg));
                break;
            case 2:
                dice.setImageDrawable(getResources().getDrawable(R.drawable.dice_two_svg));
                break;
            case 3:
                dice.setImageDrawable(getResources().getDrawable(R.drawable.dice_three_svg));
                break;
            case 4:
                dice.setImageDrawable(getResources().getDrawable(R.drawable.dice_four_svg));
                break;
            case 5:
                dice.setImageDrawable(getResources().getDrawable(R.drawable.dice_five_svg));
                break;
            case 6:
                dice.setImageDrawable(getResources().getDrawable(R.drawable.dice_six_svg));
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
            case 1:
                setDiceVisibility(Collections.singletonList(dice5));
                break;
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

    private class UpdateUI extends AsyncTask<String, Integer, String> {
        private String resp = "";

        @Override
        protected void onPreExecute() {
        }

        @Override
        protected String doInBackground(String... params) {
            try {
                Random randomGenerator = new Random();
                for (int idx = 1; idx <= 10; ++idx) {
                    int randomInt = randomGenerator.nextInt(6) + 1;
                    Thread.sleep(10);
                    publishProgress(randomInt); // Calls onProgressUpdate()
                }
            } catch (Exception e) {
                e.printStackTrace();
                resp = e.getMessage();
            }
            return resp;
        }

        @Override
        protected void onProgressUpdate(Integer... number) {
            setView(number[0], validDices);
        }

        @Override
        protected void onPostExecute(String result) {
            int sum = 0;
            for (ImageView dice : validDices) {
                Random randomGenerator = new Random();
                int randomInt = randomGenerator.nextInt(6) + 1;
                sum += randomInt;
                setViewForDice(dice, randomInt);
            }
            String totalText = "TOTAL : " + sum;
            total.setText(totalText);
        }
    }
}