package com.app.savior.my.savior;

/**
 * Created by evpru on 4/17/2017.
 */

//Referred from: https://sourcey.com/beautiful-android-login-and-signup-screens-with-material-design/
//https://github.com/sourcey/materiallogindemo

import android.app.ProgressDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;

import butterknife.ButterKnife;
import butterknife.InjectView;

public class SignupActivity extends AppCompatActivity {
    private static final String TAG = "SignupActivity";

    EditText input_age;
    EditText input_phone;
    RadioGroup genderRB;

    @InjectView(R.id.input_name) EditText _nameText;
    @InjectView(R.id.input_email) EditText _emailText;
    @InjectView(R.id.input_password) EditText _passwordText;
    @InjectView(R.id.btn_signup) Button _signupButton;
    @InjectView(R.id.link_login) TextView _loginLink;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_signup);
        ButterKnife.inject(this);

        input_age = (EditText) findViewById(R.id.input_age);
        genderRB = (RadioGroup) findViewById(R.id.genderRB);
        input_phone = (EditText) findViewById(R.id.input_phone);


        _signupButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                signup();
            }
        });

        _loginLink.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Finish the registration screen and return to the Login activity
                finish();
            }
        });
    }

    public void signup() {
        Log.d(TAG, "Signup");

        if (!validate()) {
            onSignupFailed();
            return;
        }

        _signupButton.setEnabled(false);

        final ProgressDialog progressDialog = new ProgressDialog(SignupActivity.this);
        progressDialog.setIndeterminate(true);
        progressDialog.setMessage("Creating Account...");
        progressDialog.show();

        String name = _nameText.getText().toString();
        String email = _emailText.getText().toString();
        String password = _passwordText.getText().toString();

        String age = input_age.getText().toString();
        String phone = input_phone.getText().toString();
        int genderID = genderRB.getCheckedRadioButtonId();
        String gender = ((RadioButton) findViewById(genderID)).getText().toString();


//        SharedPreferences prefs = getSharedPreferences("Savior_UserDetails", MODE_PRIVATE);
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
        SharedPreferences.Editor editor = prefs.edit();
        editor.putString("User_Name", name);
        editor.putString("User_Email",email);
        editor.putString("User_PWD",password);
        editor.putString("User_Age",age);
        editor.putString("User_Sex", gender);
        editor.putString("User_Phone",phone);
        editor.putBoolean("LoggedIN",true);
        editor.putBoolean("Alert_Flag",false);
        editor.putBoolean("User_Updated",false);
        editor.commit();

        String sqlInsertQuery = "INSERT INTO users(name, emailid, password, age, sex, contactno, activeflag, " +
                "alertflag, threshold, emergencycontactonename, emergencycontactoneno, emergencycontactoneemail) VALUES " + "(\'" +name+"\',\'"+
                email+"\',\'"+ password+"\',"+ age+",\'"+ gender.charAt(0)+"\',"+ phone+","+ "\'TRUE\'"+","+
                "\'FALSE\'"+","+ "100"+",\'\',0,\'\');";
        updatedUserInfoDB(sqlInsertQuery);

        // TODO: Implement your own signup logic here.

        new android.os.Handler().postDelayed(
                new Runnable() {
                    public void run() {
                        // On complete call either onSignupSuccess or onSignupFailed
                        // depending on success
                        progressDialog.dismiss();
                        onSignupSuccess();
                        // onSignupFailed();
                    }
                }, 2000);
    }


    public void onSignupSuccess() {
        _signupButton.setEnabled(true);
        setResult(RESULT_OK, null);
        Toast.makeText(getBaseContext(), "Signup Success", Toast.LENGTH_LONG).show();
        Intent intent = new Intent(getApplicationContext(),MapsActivity.class);
        startActivity(intent);
    }

    public void updatedUserInfoDB(final String insertQuery)
    {
        AsyncTask asyncTask = new AsyncTask()
        {
            @Override
            protected Object doInBackground(Object[] params) {
                try
                {
                    Connection conn = DriverManager.getConnection("jdbc:postgresql://192.168.0.21:5432/savior?sslmode=require", "postgres", "postgres");
                    Statement st = conn.createStatement();
                    st.executeQuery(insertQuery);
                    conn.commit();
                } catch (SQLException e)
                {
                    Log.d("MapsActivity","Signup update to DB failed: " + e.getMessage());
                    Log.d("MapsActivity","Signup update to DB failed:query: " + insertQuery);
                    e.printStackTrace();
                }
                return null;
            }
        };
        asyncTask.execute();
    }

    @Override
    protected void onStart() {
        super.onStart();
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
        boolean loggedIn = prefs.getBoolean("LoggedIN",false);

        if(loggedIn)
        {
            launchMapsActivity();
        }
    }

    public void launchMapsActivity()
    {
        Intent intent = new Intent(getApplicationContext(), MapsActivity.class);
        startActivity(intent);
    }

    public void onSignupFailed() {
        Toast.makeText(getBaseContext(), "Signup failed", Toast.LENGTH_LONG).show();

        _signupButton.setEnabled(true);
    }

    public boolean validate() {
        boolean valid = true;

        String name = _nameText.getText().toString();
        String email = _emailText.getText().toString();
        String password = _passwordText.getText().toString();

        String age = input_age.getText().toString();
        int genderID = genderRB.getCheckedRadioButtonId();
        String phone = input_phone.getText().toString();

        if (name.isEmpty() || name.length() < 3) {
            _nameText.setError("at least 3 characters");
            valid = false;
        } else {
            _nameText.setError(null);
        }

        if (email.isEmpty() || !android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            _emailText.setError("enter a valid email address");
            valid = false;
        } else {
            _emailText.setError(null);
        }

        if (password.isEmpty() || password.length() < 4 || password.length() > 10) {
            _passwordText.setError("between 4 and 10 alphanumeric characters");
            valid = false;
        } else {
            _passwordText.setError(null);
        }

        if(age.length() == 0)
        {
            input_age.setError("enter age");
            valid = false;
        }

        if(phone.length() != 10)
        {
            input_phone.setError("enter 10 digit number");
            valid = false;
        }

        if(genderID == -1)
        {
            valid = false;
        }


        return valid;
    }
}