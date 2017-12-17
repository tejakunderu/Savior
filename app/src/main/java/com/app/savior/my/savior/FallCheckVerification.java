package com.app.savior.my.savior;

import android.app.NotificationManager;
import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.annotation.Nullable;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.text.InputType;
import android.widget.EditText;
import android.widget.Toast;

/**
 * Created by evpru on 4/21/2017.
 */

public class FallCheckVerification extends AppCompatActivity
{
    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.dialog_fallcheckverification);
        checkPassword();
    }

    public void checkPassword()
    {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Alert!!!");

        final EditText input = new EditText(this);
        input.setHint("Enter password");
        input.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
        builder.setView(input);

        builder.setPositiveButton("Verify", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                String m_Text = input.getText().toString();

                SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
                String pwd = prefs.getString("User_PWD","null");
                boolean emerg_active = true;
                if(m_Text.equals(pwd))
                {
                    NotificationManager notificationManager = (NotificationManager)getSystemService(Context.NOTIFICATION_SERVICE);
                    notificationManager.cancel(123);
                    emerg_active = false;
                    Toast.makeText(getBaseContext(),"Emergency Service de-activated.\n",Toast.LENGTH_LONG).show();
                }
                else
                {
                    Toast.makeText(getBaseContext(),"Wrong Password. Emergency Service activated.\n",Toast.LENGTH_LONG).show();
                }
                SharedPreferences.Editor editor = prefs.edit();
                editor.putBoolean("Emergency_Activated", emerg_active);
                editor.commit();
                finish();
            }
        });
        builder.show();
    }
}
