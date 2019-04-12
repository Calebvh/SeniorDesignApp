package com.example.caleb.seniordesignapp;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.os.IBinder;
import android.util.Log;

public class AlarmService extends Service {

    private static final String TAG = AlarmService.class.getName();
    MediaPlayer AlarmMediaPlayer;
    private long startTime;
    private long currentTime;
    private long timeElapsed;

    @Override
    public void onStart(Intent intent, int startId){
        super.onStart(intent, startId);


        Log.d(TAG,"AlarmService: Starting Sound");


        AudioManager audioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
        audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, 20, 0);

        AlarmMediaPlayer = MediaPlayer.create(this, R.raw.alarm_sound_2);
        AlarmMediaPlayer.start();

    }


    @Override
    public void onDestroy(){
        super.onDestroy();

        Log.d(TAG,"AlarmService: Stopping Sound");
        AlarmMediaPlayer.stop();
    }

    @Override
    public IBinder onBind(Intent arg0) {

        return null;
    }

}