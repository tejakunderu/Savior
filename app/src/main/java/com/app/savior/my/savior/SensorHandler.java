package com.app.savior.my.savior;

import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.media.Ringtone;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.IBinder;
import android.preference.PreferenceManager;
import android.support.v7.app.NotificationCompat;
import android.util.Log;


/**
 * Created by evpru on 4/21/2017.
 */

public class SensorHandler extends Service implements SensorEventListener{

    private SensorManager accelManage;
    private Sensor senseAccel;
    Ringtone ring;

    float accelValuesX[] = new float[128];
    float accelValuesY[] = new float[128];
    float accelValuesZ[] = new float[128];
    int index = 0;
    int k=0;
    Bundle b;
    @Override
    public void onSensorChanged(SensorEvent sensorEvent) {
        // TODO Auto-generated method stub
        Sensor mySensor = sensorEvent.sensor;

        if (mySensor.getType() == Sensor.TYPE_ACCELEROMETER) {
            index++;
            accelValuesX[index] = sensorEvent.values[0];
            accelValuesY[index] = sensorEvent.values[1];
            accelValuesZ[index] = sensorEvent.values[2];
            if(index >= 127){
                index = 0;
                accelManage.unregisterListener(this);
                callFallRecognition();
                accelManage.registerListener(this, senseAccel, SensorManager.SENSOR_DELAY_NORMAL);
            }
        }
    }


    public void callFallRecognition(){
        float prev = 0;
        float curr = 0;
        prev = 10;
        for(int i=11;i<128;i++){
            curr = accelValuesZ[i];
            if(Math.abs(prev - curr) > 10 )
            {
                Log.d("SensorHandler","fall detected");
                SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
                boolean fall_detected = prefs.getBoolean("Fall_Detected",false);
//                if(!fall_detected)
//                {
                SharedPreferences.Editor editor = prefs.edit();
                editor.putBoolean("Fall_Detected", true);
                editor.commit();
                boolean alert_flag = prefs.getBoolean("Alert_Flag",false);
                if(alert_flag)
                {
                    editor.putBoolean("Emergency_Activated",true);
                    editor.commit();
                    sendNotification();
                }
//                }
            }
        }
    }

    public void sendNotification()
    {
        NotificationCompat.Builder mBuilder =
                (android.support.v7.app.NotificationCompat.Builder) new NotificationCompat.Builder(this)
                        .setSmallIcon(R.mipmap.savior)
                        .setContentTitle("ALERT!!!")
                        .setContentText("Fall has been detected. Enter password to Stop Emergency Services.");
        Intent resultIntent = new Intent(getBaseContext(), FallCheckVerification.class);


        PendingIntent resultPendingIntent = PendingIntent.getActivity(getBaseContext(), 1, resultIntent, PendingIntent.FLAG_UPDATE_CURRENT);
        mBuilder.setContentIntent(resultPendingIntent);

        try {
            Uri notification = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
            Ringtone ring = RingtoneManager.getRingtone(getBaseContext(), notification);
            ring.play();
        } catch (Exception e) {
            e.printStackTrace();
        }

        NotificationManager mNotificationManager = (NotificationManager) getSystemService(FallCheckVerification.NOTIFICATION_SERVICE);
        mNotificationManager.notify(123, mBuilder.build());
    }


    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {
        // TODO Auto-generated method stub

    }

    @Override
    public void onCreate(){
//        Toast.makeText(this, "Service Started", Toast.LENGTH_LONG).show();
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
        SharedPreferences.Editor editor = prefs.edit();
        editor.putBoolean("Fall_Detected", false);
        editor.commit();

        accelManage = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        senseAccel = accelManage.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        accelManage.registerListener(this, senseAccel, SensorManager.SENSOR_DELAY_NORMAL);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId)
    {
//        Toast.makeText(SensorHandler.this, "Starting Sensor Service", Toast.LENGTH_LONG).show();
        // We want this service to continue running until it is explicitly
        // stopped, so return sticky.
        //k = 0;
        return START_STICKY;
    }

    @Override
    public IBinder onBind(Intent intent) {
        // TODO Auto-generated method stub

        return null;
    }

}
