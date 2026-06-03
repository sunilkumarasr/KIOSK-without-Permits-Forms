package com.provizit.kioskcheckin.activities.Meetings;

import static android.view.View.GONE;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkRequest;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.view.animation.AnimationSet;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;
import androidx.activity.OnBackPressedCallback;
import androidx.appcompat.app.AppCompatActivity;
import com.bumptech.glide.Glide;
import com.provizit.kioskcheckin.R;
import com.provizit.kioskcheckin.config.ConnectionReceiver;
import com.provizit.kioskcheckin.config.Preferences;
import com.provizit.kioskcheckin.logins.VisitorLoginActivity;
import com.provizit.kioskcheckin.services.Conversions;

public class MeetingCheckInDoneActivity extends AppCompatActivity implements View.OnClickListener{

    //internet connection
    BroadcastReceiver broadcastReceiver;
    RelativeLayout relativeinternet;
    RelativeLayout relativeui;

    ImageView backimage;
    ImageView companylogo;
    Button btnOk;

    Handler handler = new Handler(Looper.getMainLooper());

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_meeting_check_in_done);

        SharedPreferences sharedPreferences = getSharedPreferences("MyPrefs", Context.MODE_PRIVATE);
        String VersionName = sharedPreferences.getString("VersionName", "1.0");
        TextView txtVersion = findViewById(R.id.txtVersion);
        txtVersion.setText(VersionName);


        backimage = findViewById(R.id.backimage);
        companylogo = findViewById(R.id.company_logo);
        btnOk = findViewById(R.id.btnOk);



        //internet connection
        relativeinternet = findViewById(R.id.relative_internet);
        relativeui = findViewById(R.id.relative_ui);
        broadcastReceiver = new ConnectionReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                if (isConnecteds(context)) {
                    relativeinternet.setVisibility(GONE);
                    relativeui.setVisibility(View.VISIBLE);
                } else {
                    relativeinternet.setVisibility(View.VISIBLE);
                    relativeui.setVisibility(GONE);
                }
            }
        };
        registerNetworkBroadcast();

        // Company logo
        String companyLogo = Preferences.loadStringValue(getApplicationContext(), Preferences.company_Logo, "");

        if (!companyLogo.equalsIgnoreCase("")) {
            // Load the company logo if it's available
            Glide.with(MeetingCheckInDoneActivity.this)
                    .load(companyLogo)
                    .into(companylogo);
        } else {

            companylogo.setVisibility(View.GONE);
        }

        btnOk.setOnClickListener(this);
        backimage.setOnClickListener(this);


        // Register the back press behavior using OnBackPressedDispatcher
        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                handleBackPressed(); // Call your custom back pressed logic
            }
        });

    }

    //disable auto click action after scann
    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_ENTER) {
            return true;
        }
        if (keyCode == KeyEvent.KEYCODE_DEL) {
            return true;
        }
        disableTriggeredItems();
        return super.onKeyDown(keyCode, event);
    }

    //usb scanner
    @Override
    public boolean dispatchKeyEvent(KeyEvent event) {
        if (event.getAction() == KeyEvent.ACTION_DOWN) {
            switch (event.getKeyCode()) {
                case KeyEvent.KEYCODE_ENTER:
                    return true;
                case KeyEvent.KEYCODE_BACK:
                    Intent intent = new Intent(getApplicationContext(), VisitorLoginActivity.class);
                    startActivity(intent);
                    return true;
                case KeyEvent.KEYCODE_DEL:
                    Log.d("KeyEvent", "DEL in dispatchKeyEvent");
                    break;
                default:
                    char keyChar = (char) event.getUnicodeChar();
                    if (Character.isLetterOrDigit(keyChar)) {
                        return true;
                    }
            }
        }
        return super.dispatchKeyEvent(event);
    }

    private void disableTriggeredItems() {
        btnOk.setFocusable(false);
        btnOk.setFocusableInTouchMode(false);
        backimage.setFocusable(false);
        backimage.setFocusableInTouchMode(false);
        handler.post(myRunnable);
    }
    Runnable myRunnable = () -> {
        finish();
        // Remove deprecated calls to overridePendingTransition
        startActivity(getIntent());
        // The second call to overridePendingTransition is also removed.

        // Set focusable properties for buttons and image
        btnOk.setFocusable(true);
        btnOk.setFocusableInTouchMode(true);
        backimage.setFocusable(true);
        backimage.setFocusableInTouchMode(true);
    };


    @Override
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.btnOk:
                AnimationSet animation1 = Conversions.animation();
                view.startAnimation(animation1);
                Intent intent = new Intent(getApplicationContext(), VisitorLoginActivity.class);
                startActivity(intent);
                break;
            case R.id.backimage:
                AnimationSet animation = Conversions.animation();
                view.startAnimation(animation);
                Intent intent1 = new Intent(getApplicationContext(), VisitorLoginActivity.class);
                startActivity(intent1);
                break;
            default:
                break;
        }
    }


    protected void registerNetworkBroadcast() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            ConnectivityManager connectivityManager = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
            NetworkRequest.Builder builder = new NetworkRequest.Builder();
            connectivityManager.registerNetworkCallback(builder.build(), new ConnectivityManager.NetworkCallback() {
                @Override
                public void onAvailable(Network network) {
                    // Handle network available
                }

                @Override
                public void onLost(Network network) {
                    // Handle network lost
                }
            });
        }
    }

    // Custom back pressed method
    private void handleBackPressed() {
        Intent intent = new Intent(getApplicationContext(), VisitorLoginActivity.class);
        startActivity(intent);
    }

}