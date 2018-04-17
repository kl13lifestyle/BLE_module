package com.example.juggernaut.ble_module;

import android.graphics.Color;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.widget.ImageView;

import com.andrognito.patternlockview.PatternLockView;
import com.andrognito.patternlockview.listener.PatternLockViewListener;
import com.andrognito.patternlockview.utils.PatternLockUtils;
import com.example.juggernaut.ble_module.BLEUtils.BluetoothServerInstance;

import java.util.List;

public class MainActivity extends AppCompatActivity {

    static MainActivity mn;
    private static final String TAG = "Module";
    private int state = 1;

    private PatternLockView patternLockView;
    private static ImageView whatsapp;
    private static ImageView message;
    private static ImageView call;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        BluetoothServerInstance.setContext(MainActivity.this);
        mn=MainActivity.this;
        BluetoothServerInstance.initialize_bluetooth();
        BluetoothServerInstance.initServer();
        initialize_screen();
    }

    private void initialize_screen() {
        patternLockView =(PatternLockView)findViewById(R.id.patternlock);
        whatsapp=(ImageView)findViewById(R.id.whatsapp);
        message=(ImageView)findViewById(R.id.message);
        call=(ImageView)findViewById(R.id.call);
        patternLockView.addPatternLockListener(patternListener);
    }
    PatternLockViewListener patternListener = new PatternLockViewListener() {
        @Override
        public void onStarted() {
            Log.d(getClass().getName(), "Pattern drawing started");
        }

        @Override
        public void onProgress(List<PatternLockView.Dot> progressPattern) {
            Log.d(getClass().getName(), "Pattern progress: " +
                    PatternLockUtils.patternToString(patternLockView, progressPattern));
        }

        @Override
        public void onComplete(List<PatternLockView.Dot> pattern) {
            Log.d(getClass().getName(), "Pattern complete: " +
                    PatternLockUtils.patternToString(patternLockView, pattern));
            BluetoothServerInstance.setValue(PatternLockUtils.patternToString(patternLockView, pattern).getBytes());
        }

        @Override
        public void onCleared() {
            Log.d(getClass().getName(), "Pattern has been cleared");
        }
    };

    public static void notificationType(byte value) {
        switch (value) {
            case 00:
                mn.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {whatsapp.setColorFilter(Color.argb(255, 255, 255, 255));}});
                    break;
            case 01:mn.runOnUiThread(new Runnable() {
                @Override
                public void run() {message.setColorFilter(Color.argb(255, 255, 255, 255));}});
                break;
            case 02:mn.runOnUiThread(new Runnable() {
                @Override
                public void run() {call.setColorFilter(Color.argb(255, 255, 255, 255));}});
                break;
            default:
                mn.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        whatsapp.setColorFilter(Color.argb(255, 0, 0, 0));
                        message.setColorFilter(Color.argb(255, 0, 0, 0));
                        call.setColorFilter(Color.argb(255, 0, 0, 0));
                    }});
                Log.i(TAG, "notificationType: ");
                break;
            }
        }



    @Override
    protected void onResume() {
        super.onResume();
        BluetoothServerInstance.startAdvertising();
    }
}

