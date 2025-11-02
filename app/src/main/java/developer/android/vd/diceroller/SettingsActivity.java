package developer.android.vd.diceroller;

import static android.content.ContentValues.TAG;

import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.ColorDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.RatingBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.android.gms.ads.AdError;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.FullScreenContentCallback;
import com.google.android.gms.ads.LoadAdError;
import com.google.android.gms.ads.rewarded.RewardedAd;
import com.google.android.gms.ads.rewarded.RewardedAdLoadCallback;

import java.util.Objects;
import java.util.Timer;
import java.util.TimerTask;

import yuku.ambilwarna.AmbilWarnaDialog;

public class SettingsActivity extends AppCompatActivity {
    private static final int COOL_DOWN_TIME = 8 * 60 * 60 * 1000;
    private RewardedAd rewardedAd;
    private Button removeAdsBtn;
    private TextView showTotal, hideTotal;

    private SharedPreferences sharedPreferences;
    private Timer timer;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_settings);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        initializeVariables();
        handleToggle();
        initializeAds();
    }

    private void initializeVariables() {
        Button otherAppsBtn = findViewById(R.id.bt_other_apps);
        Button colorPickerBtn = findViewById(R.id.bt_color_picker);

        showTotal = findViewById(R.id.tv_show_total);
        hideTotal = findViewById(R.id.tv_hide_total);
        removeAdsBtn = findViewById(R.id.bt_pause_ads);

        colorPickerBtn.setOnClickListener(this::colorPicker);
        removeAdsBtn.setOnClickListener(this::pauseAds);
        otherAppsBtn.setOnClickListener(this::otherApps);

        findViewById(R.id.ib_back).setOnClickListener(view -> finish());

        ((RatingBar) findViewById(R.id.rr_ratings)).setOnRatingBarChangeListener((ratingBar, rating, fromUser) -> {
            if (fromUser) {
                openPlayStore();
            }
        });

        sharedPreferences = getSharedPreferences(getString(R.string.app_name), MODE_PRIVATE);
    }


    public void colorPicker(View view) {
        int selectedBackgroundColor = sharedPreferences.getInt(getString(R.string.selected_background), Color.parseColor("#33b5e5"));

        AmbilWarnaDialog ambilWarnaDialog = new AmbilWarnaDialog(this, selectedBackgroundColor, new AmbilWarnaDialog.OnAmbilWarnaListener() {
            @Override
            public void onCancel(AmbilWarnaDialog dialog) {

            }

            @Override
            public void onOk(AmbilWarnaDialog dialog, int color) {
                SharedPreferences.Editor bgcEdit = sharedPreferences.edit();
                bgcEdit.putInt(getString(R.string.selected_background), color);
                bgcEdit.apply();
                closeWithTransition();
            }
        });
        ambilWarnaDialog.show();
    }

    private void handleToggle() {
        toggleButton();

        showTotal.setOnClickListener(view -> setTotalVisibility(false));
        hideTotal.setOnClickListener(view -> setTotalVisibility(true));
    }

    private void toggleButton() {
        boolean toggleSwitch = sharedPreferences.getBoolean(getString(R.string.total_visibility), true);
        showTotal.setVisibility(toggleSwitch ? View.VISIBLE : View.GONE);
        hideTotal.setVisibility(toggleSwitch ? View.GONE : View.VISIBLE);
    }

    private void setTotalVisibility(boolean visibility) {
        SharedPreferences.Editor toggleEdit = sharedPreferences.edit();
        toggleEdit.putBoolean(getString(R.string.total_visibility), visibility);
        toggleEdit.apply();
        toggleButton();
    }

    private void openPlayStore() {
        Uri uri = Uri.parse("market://details?id=" + getPackageName());
        Intent goToMarket = new Intent(Intent.ACTION_VIEW, uri);
        goToMarket.addFlags(Intent.FLAG_ACTIVITY_NO_HISTORY |
                Intent.FLAG_ACTIVITY_NEW_DOCUMENT |
                Intent.FLAG_ACTIVITY_MULTIPLE_TASK);
        try {
            startActivity(goToMarket);
        } catch (Exception e) {
            Uri webUri = Uri.parse("https://play.google.com/store/apps/details?id=" + getPackageName());
            startActivity(new Intent(Intent.ACTION_VIEW, webUri));
        }
    }

    public void otherApps(View view) {
        Uri uri = Uri.parse("market://search?q=pub:Vaibhav+Deshmukh");
        Intent goToMarket = new Intent(Intent.ACTION_VIEW, uri);
        goToMarket.addFlags(Intent.FLAG_ACTIVITY_NO_HISTORY |
                Intent.FLAG_ACTIVITY_NEW_DOCUMENT |
                Intent.FLAG_ACTIVITY_MULTIPLE_TASK);
        try {
            startActivity(goToMarket);
        } catch (Exception e) {
            Uri webUri = Uri.parse("https://play.google.com/store/search?q=pub:Vaibhav+Deshmukh");
            startActivity(new Intent(Intent.ACTION_VIEW, webUri));
        }
    }

    private void closeWithTransition() {
        finish();
        overridePendingTransition(R.anim.animate_zoom_enter, R.anim.animate_zoom_exit);
    }

    private void initializeAds() {
        AdRequest adRequest = new AdRequest.Builder().build();
        RewardedAd.load(this, getString(R.string.rewarded_ad_unit_id),
                adRequest, new RewardedAdLoadCallback() {
                    @Override
                    public void onAdFailedToLoad(@NonNull LoadAdError loadAdError) {
                        // Handle the error.
                        Log.d(TAG, loadAdError.toString());
                        rewardedAd = null;
                    }

                    @Override
                    public void onAdLoaded(@NonNull RewardedAd ad) {
                        rewardedAd = ad;
                        Log.d(TAG, "Ad was loaded.");
                    }
                });
    }

    public void pauseAds(View view) {
        Dialog dialog = new Dialog(this);
        dialog.setContentView(R.layout.ads_confirmation_dialog);
        Objects.requireNonNull(dialog.getWindow()).setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        Objects.requireNonNull(dialog.getWindow()).setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);

        Button ok = dialog.findViewById(R.id.bt_ok_ads_dialog);
        ok.setOnClickListener(view1 -> {
            dialog.cancel();
            showRewardedVideoAd();
        });

        Button cancel = dialog.findViewById(R.id.bt_cancel_ads_dialog);
        cancel.setOnClickListener(view1 -> dialog.cancel());
        dialog.show();
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
        float timeRemainingHrs = (timerCutOff - System.currentTimeMillis()) / (float) (60 * 60 * 1000);
        if (timeRemainingHrs > 0) {
            removeAdsBtn.setBackgroundColor(Color.GRAY);
            removeAdsBtn.setEnabled(false);

            if (timeRemainingHrs > 1) {
                removeAdsBtn.setText(getString(R.string.no_ads_hrs, timeRemainingHrs));
            } else {
                int minutes = (int) (timeRemainingHrs * 60);
                removeAdsBtn.setText(getString(R.string.no_ads_mins, minutes));
            }
        } else {
            removeAdsBtn.setBackgroundColor(Color.BLACK);
            removeAdsBtn.setEnabled(true);
            removeAdsBtn.setText(getString(R.string.remove_ads));
        }
    }

    private void showRewardedVideoAd() {
        if (rewardedAd == null) {
            Toast.makeText(this, "Rewarded video isn't ready, please check internet connection", Toast.LENGTH_LONG).show();
        } else {
            rewardedAd.setFullScreenContentCallback(new FullScreenContentCallback() {
                @Override
                public void onAdClicked() {
                    // Called when a click is recorded for an ad.
                    Log.d(TAG, "Ad was clicked.");
                }

                @Override
                public void onAdDismissedFullScreenContent() {
                    // Called when ad is dismissed.
                    // Set the ad reference to null so you don't show the ad a second time.
                    Log.d(TAG, "Ad dismissed fullscreen content.");
                    rewardedAd = null;
                }

                @Override
                public void onAdFailedToShowFullScreenContent(@NonNull AdError adError) {
                    // Called when ad fails to show.
                    Log.e(TAG, "Ad failed to show fullscreen content.");
                    rewardedAd = null;
                }

                @Override
                public void onAdImpression() {
                    // Called when an impression is recorded for an ad.
                    Log.d(TAG, "Ad recorded an impression.");
                }

                @Override
                public void onAdShowedFullScreenContent() {
                    // Called when ad is shown.
                    Log.d(TAG, "Ad showed fullscreen content.");
                }
            });

            rewardedAd.show(this, rewardItem -> {
                // Handle the reward.
                Log.d(TAG, "The user earned the reward.");
                SharedPreferences.Editor editor = sharedPreferences.edit();
                editor.putLong(getString(R.string.pause_ads_cut_off_time), System.currentTimeMillis() + COOL_DOWN_TIME);
                editor.apply();
            });
        }
    }

    private void resetTimer() {
        if (timer != null) {
            timer.cancel();
            timer = null;
        }
    }

    @Override
    protected void onStart() {
        super.onStart();
        startTimerForAds();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        resetTimer();
    }
}