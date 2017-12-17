package com.app.savior.my.savior;

import android.Manifest;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.location.Location;
import android.media.Ringtone;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.IBinder;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.NotificationCompat;
import android.telephony.SmsManager;
import android.util.Log;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;

/**
 * Created by evpru on 4/20/2017.
 */

public class MyLocationService extends Service implements GoogleApiClient.ConnectionCallbacks,
        GoogleApiClient.OnConnectionFailedListener, LocationListener
{
    GoogleApiClient mGoogleApiClient;
    Location mLastLocation;
    LocationRequest mLocationRequest;
    String crimeDataCount = "0";
    Connection conn;
    Statement st;

    static boolean emergency_activated = false;
    String mapsURL = "http://maps.google.com/maps?q=";
    String url = "jdbc:postgresql://192.168.0.21:5432/savior?sslmode=require";



    @Override
    public void onCreate()
    {
        super.onCreate();
        Log.d("MapActivity","Inside onCreate of Location Service");
        try
        {
            Class.forName("org.postgresql.Driver");
        }
        catch (ClassNotFoundException e)
        {
            Log.d("MapActivity","Postgresql Driver not found.");
        }

        AsyncTask asyncTask = new AsyncTask()
        {
            @Override
            protected Object doInBackground(Object[] params) {
                try
                {
                    conn = DriverManager.getConnection(url,"postgres","postgres");
                    st = conn.createStatement();
                } catch (SQLException e)
                {
                    Log.d("MapActivity","Postgresql connection failed.");
                }
                return null;
            }
        };
        asyncTask.execute();
        connectGoogleApiClient();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId)
    {
        Log.d("MapActivity","Inside onStartCommand of Location Service");
        return START_STICKY;
    }

    public void connectGoogleApiClient()
    {
        Log.d("MapActivity","Inside connectGoogleApiClient of Location Service");
        if (mGoogleApiClient == null) {
            mGoogleApiClient = new GoogleApiClient.Builder(this)
                    .addConnectionCallbacks(this)
                    .addOnConnectionFailedListener(this)
                    .addApi(LocationServices.API)
                    .build();
            mGoogleApiClient.connect();
        }
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onLocationChanged(Location location) {
        mLastLocation = location;

        String location_string = mLastLocation.getLatitude() + "," + mLastLocation.getLongitude();
        Log.d("MapActivity","Latitude: " + location_string);

        Intent intent = new Intent("MyLocationService");
        sendMarkersToUI();
        checkIfNearCrimeLocation();
        updateCurrentLocationToDB(location_string);
        intent.putExtra("MyLocation",location_string);
        sendBroadcast(intent);
        String mapsLink = mapsURL+mLastLocation.getLatitude() + "," + mLastLocation.getLongitude();
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
        emergency_activated = prefs.getBoolean("Emergency_Activated",false);
        String user_name = prefs.getString("User_Name","");
        String emerg_Phone = prefs.getString("Emerg_Phone","null");

        if(emergency_activated)
        {
            SmsManager smsManager = SmsManager.getDefault();
            smsManager.sendTextMessage(emerg_Phone, null, "Your friend, " + user_name +
                    "might be in trouble!" +" Present location is " + mapsLink, null, null);
            Log.d("MapActivity","Emergency Activated");
        }
    }

    public void checkIfNearCrimeLocation()
    {
        if(null != conn)
        {
            AsyncTask asyncTask = new AsyncTask()
            {
                @Override
                protected Object doInBackground(Object[] params) {
                    try
                    {
                        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
                        String user_email = prefs.getString("User_Email","");
                        boolean previous_alert = prefs.getBoolean("Alert_Flag",false);
                        SharedPreferences.Editor editor = prefs.edit();
                        ResultSet rs = st.executeQuery("select alertflag from users where emailid=\'" + user_email+"\'");
                        rs.next();
                        boolean alertFlag = rs.getBoolean("alertflag");
                        editor.putBoolean("Alert_Flag",alertFlag);
                        boolean user_updated = prefs.getBoolean("User_Updated",false);
                        editor.commit();
                        if((alertFlag == true && user_updated == false) || previous_alert != alertFlag)
                        {
                            String message = "You are in Crime Zone. Please be careful.";
                            if(alertFlag == false)
                            {
                                message = "You are out of Crime Zone. Enjoy.";
                                editor.putBoolean("User_Updated",false);
                                editor.commit();
                            }

                            NotificationCompat.Builder mBuilder =
                                    (android.support.v7.app.NotificationCompat.Builder) new NotificationCompat.Builder(getBaseContext())
                                            .setSmallIcon(R.mipmap.savior)
                                            .setContentTitle("ALERT!!!")
                                            .setContentText(message);
                            mBuilder.setAutoCancel(true);

                            try {
                                Uri notification = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
                                Ringtone ring = RingtoneManager.getRingtone(getBaseContext(), notification);
                                ring.play();
                            } catch (Exception e) {
                                e.printStackTrace();
                            }

                            NotificationManager mNotificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
                            mNotificationManager.notify(234, mBuilder.build());
                        }
                    } catch (SQLException e)
                    {
                        Log.d("MapsActivity","Inside CheckIfNearCrimeLocation: " + e.getMessage());
                        e.printStackTrace();
                    }
                    return null;
                }
            };
            asyncTask.execute();
        }
    }

    public void sendMarkersToUI()
    {
        if(null != conn)
        {
            AsyncTask asyncTask = new AsyncTask()
            {
                @Override
                protected Object doInBackground(Object[] params) {
                    try
                    {
                        ArrayList<String> arrayList = new ArrayList<>();
                        ResultSet rs = st.executeQuery("select count(*) from crimedata");
                        String newCount = "";
                        rs.next();
                        newCount = rs.getString("count");
//                        if (!newCount.equals(crimeDataCount))
//                        {
                        crimeDataCount = newCount;
                        ResultSet rsdata = st.executeQuery("select * from crimedata");
                        while (rsdata.next())
                        {
                            arrayList.add(rsdata.getString("latitude") + "_" + rsdata.getString("longitude") + "_" + rsdata.getString("address"));
                        }
//                        }
                        Intent crimeIntent = new Intent("CrimeDataService");
                        crimeIntent.putExtra("Crime_Data",arrayList);
                        sendBroadcast(crimeIntent);
                    } catch (SQLException e)
                    {
                        e.printStackTrace();
                    }
                    return null;
                }
            };
            asyncTask.execute();
        }
    }

    public void updateCurrentLocationToDB(final String currentLocation)
    {
        if(null != conn)
        {
            AsyncTask asyncTask = new AsyncTask()
            {
                @Override
                protected Object doInBackground(Object[] params) {
                    try
                    {
                        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
                        String user_email = prefs.getString("User_Email","");
                        ResultSet rs = st.executeQuery("SELECT userid,sex FROM users WHERE emailid=\'"+ user_email+"\'");
                        rs.next();
                        String[] tokens = currentLocation.split(",");
                        int userid = rs.getInt("userid");
                        String sex = rs.getString("sex");
                        st.executeQuery("INSERT INTO "+sex.toLowerCase()+userid+ "(latitude,longitude) VALUES ("
                                + Double.valueOf(tokens[0])+","+Double.valueOf(tokens[1])+");");
                        conn.commit();
                    } catch (SQLException e)
                    {
                        e.printStackTrace();
                    }
                    return null;
                }
            };
            asyncTask.execute();
        }

    }




    @Override
    public void onConnected(@Nullable Bundle bundle)
    {
        Log.d("MapActivity","Inside onConnected of Location Service");
        mLocationRequest = new LocationRequest();
        mLocationRequest.setInterval(5000);
        mLocationRequest.setFastestInterval(3000);
        mLocationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED)
        {
            LocationServices.FusedLocationApi.requestLocationUpdates(mGoogleApiClient, mLocationRequest, this);
        }
    }

    @Override
    public void onConnectionSuspended(int i) {

    }

    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {

    }
}
