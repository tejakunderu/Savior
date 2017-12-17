package com.app.savior.my.savior;

import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.widget.EditText;
import android.widget.RadioButton;
import android.widget.RadioGroup;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;

/**
 * Created by evpru on 3/24/2017.
 */

public class UserSettings extends AppCompatActivity
{

    EditText user_Name;
    EditText user_Age;
    RadioGroup user_Sex;
    EditText user_Phone;
    EditText emerg_Name;
    EditText emerg_Phone;
    EditText emerg_Email;


    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_user_settings);

        user_Name = (EditText) findViewById(R.id.user_name);
        user_Age = (EditText) findViewById(R.id.user_age);
        user_Phone = (EditText) findViewById(R.id.user_phone);
        emerg_Name = (EditText) findViewById(R.id.emerg_name);
        emerg_Phone = (EditText) findViewById(R.id.emerg_phone);
        emerg_Email = (EditText) findViewById(R.id.emerg_email);

        populateData();
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        String userName = user_Name.getText().toString();
        String userAge = user_Age.getText().toString();
        String userPhone = user_Phone.getText().toString();
        String emergName = emerg_Name.getText().toString();
        String emergPhone = emerg_Phone.getText().toString();
        String emergEmail = emerg_Email.getText().toString();

        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
        SharedPreferences.Editor editor = prefs.edit();
        String user_email = prefs.getString("User_Email","");
        editor.putString("User_Name", userName);
        editor.putString("User_Age",userAge);
        editor.putString("User_Phone",userPhone);
        editor.putString("Emerg_Name",emergName);
        editor.putString("Emerg_Phone",emergPhone);
        editor.putString("Emerg_Email",emergEmail);
        editor.commit();

        String sqlQuery = "UPDATE users SET emergencycontactonename=\'"+emergName+"\', " +
                "emergencycontactoneno="+emergPhone+", emergencycontactoneemail=\'"+emergEmail+"\' WHERE emailid =\'"+ user_email+ "\';";
        updateEmergencyContactDB(sqlQuery);
    }

    public void updateEmergencyContactDB(final String sqlQuery)
    {
        AsyncTask asyncTask = new AsyncTask()
        {
            @Override
            protected Object doInBackground(Object[] params) {
                try
                {
                    Connection conn = DriverManager.getConnection("jdbc:postgresql://192.168.0.21:5432/savior?sslmode=require", "postgres", "postgres");
                    Statement st = conn.createStatement();
                    st.executeQuery(sqlQuery);
                    conn.commit();
                } catch (SQLException e)
                {
                    Log.d("MapsActivity","EmergencyContacts update to DB failed:query: " + sqlQuery);
                    Log.d("MapsActivity","EmergencyContacts update to DB failed: " + e.getMessage());
                    e.printStackTrace();
                }
                return null;
            }
        };
        asyncTask.execute();
    }

    public void populateData()
    {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
        String user_Name = prefs.getString("User_Name","null");
        String user_Age = prefs.getString("User_Age","null");
        String user_Sex = prefs.getString("User_Sex","null");
        String user_Phone = prefs.getString("User_Phone","null");
        String emerg_Name = prefs.getString("Emerg_Name","null");
        String emerg_Phone = prefs.getString("Emerg_Phone","null");
        String emerg_Email = prefs.getString("Emerg_Email","null");

        this.user_Name.setText(user_Name);
        this.user_Age.setText(user_Age);
        this.user_Phone.setText(user_Phone);
        if(user_Sex.equalsIgnoreCase("Male"))
        {
            ((RadioButton)findViewById(R.id.user_male)).setChecked(true);
        }
        else if(user_Sex.equalsIgnoreCase("Female"))
        {
            ((RadioButton)findViewById(R.id.user_female)).setChecked(true);
        }

        if(!emerg_Email.equalsIgnoreCase("null"))
            this.emerg_Email.setText(emerg_Email);
        if(!emerg_Name.equalsIgnoreCase("null"))
            this.emerg_Name.setText(emerg_Name);
        if(!emerg_Phone.equalsIgnoreCase("null"))
            this.emerg_Phone.setText(emerg_Phone);
    }
}
