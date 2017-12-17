package com.app.savior.my.savior;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.widget.EditText;

/**
 * Created by evpru on 4/20/2017.
 */

public class AppSettings extends AppCompatActivity
{
    EditText threshold;
    SharedPreferences prefs;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.app_settings);
        threshold = (EditText) findViewById(R.id.threshold);

        prefs = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
        String threshold_prefs = prefs.getString("Threshold","null");

        if(!threshold_prefs.equals("null"))
        {
            threshold.setText(threshold_prefs);
        }
    }

    @Override
    protected void onStop() {
        super.onStop();

        String thresholdValue = threshold.getText().toString();

        if(null == thresholdValue || thresholdValue.equals(""))
        {
            thresholdValue = "100";
        }


        SharedPreferences.Editor editor = prefs.edit();
        editor.putString("Threshold", thresholdValue);
        editor.commit();

    }
}
