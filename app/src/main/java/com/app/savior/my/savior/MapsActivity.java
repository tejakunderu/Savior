package com.app.savior.my.savior;

import android.Manifest;
import android.app.NotificationManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.location.LocationManager;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.NavigationView;
import android.support.v4.content.ContextCompat;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.telephony.SmsManager;
import android.text.InputType;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;

import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;

import java.util.ArrayList;

public class MapsActivity extends AppCompatActivity implements OnMapReadyCallback, GoogleMap.OnMyLocationButtonClickListener,
        GoogleMap.OnCameraMoveStartedListener, NavigationView.OnNavigationItemSelectedListener,DrawerLayout.DrawerListener
{

    private GoogleMap mMap;
    LocationRequest mLocationRequest;
    LocationManager locationManager;
    FloatingActionButton emergSwitch;
    DrawerLayout drawer;
    String emerg_Phone;
    SmsManager smsManager;
    private DataUpdateReceiver dataUpdateReceiver;

    public static boolean emergActivated = false;
    private String password = "";
    private boolean followCurrentLocation = true;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_maps);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);

        drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
//        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
//                this, drawer, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close);
        drawer.setDrawerListener(this);
//        toggle.syncState();

        emergSwitch = (FloatingActionButton) findViewById(R.id.emergSwitch);

        smsManager = SmsManager.getDefault();

        NavigationView navigationView = (NavigationView) findViewById(R.id.nav_view);
        navigationView.setNavigationItemSelectedListener(this);
    }

    public void turnOffService(View v)
    {
        Intent intent = new Intent(getBaseContext(),MyLocationService.class);
        stopService(intent);
    }

    @Override
    public void onDrawerSlide(View drawerView, float offset){
        emergSwitch.setAlpha(1-offset);
    }

    @Override
    public void onDrawerOpened(View drawerView)
    {
        NavigationView nV = (NavigationView) findViewById(R.id.nav_view);
        nV.bringToFront();
        nV.requestLayout();
    }

    @Override
    public void onDrawerClosed(View drawerView)
    {
    }

    @Override
    public void onDrawerStateChanged(int newState) {
        int i = 0;
    }

    public void checkGPSOn()
    {
        locationManager = (LocationManager)getApplicationContext().getSystemService(Context.LOCATION_SERVICE);

        boolean gpsOn = locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER);

        if(!gpsOn)
        {
            AlertDialog.Builder builder = new AlertDialog.Builder(MapsActivity.this);
            builder.setTitle("Turn on GPS!");
            builder.setMessage("Please turn on the GPS and open the app again!");
            builder.setCancelable(false);
            builder.setPositiveButton("Ok", new DialogInterface.OnClickListener()
            {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    closeApp();
                }
            });

            AlertDialog alert = builder.create();
            alert.show();
        }
        else
        {
            Log.d("MapActivity","Before starting service of MapsActivity");
            Intent intent = new Intent(getBaseContext(),MyLocationService.class);
            startService(intent);

            Intent accIntent = new Intent(getBaseContext(),SensorHandler.class);
            startService(accIntent);
        }
    }

    @Override
    public boolean onMyLocationButtonClick() {
        followCurrentLocation = true;
        return false;
    }

    @Override
    public void onCameraMoveStarted(int reason)
    {
        Log.d("MapActivity","Inside onCameraMoveStarted of MapsActivity");
        if(reason == GoogleMap.OnCameraMoveStartedListener.REASON_GESTURE)
            followCurrentLocation = false;
    }

    private class DataUpdateReceiver extends BroadcastReceiver
    {
        @Override
        public void onReceive(Context context, Intent intent) {
            SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
            boolean emerg = prefs.getBoolean("Emergency_Activated",false);

            if(emerg == false)
                emergSwitch.setBackgroundTintList(ColorStateList.valueOf(Color.parseColor("#F44336")));
            else
                emergSwitch.setBackgroundTintList(ColorStateList.valueOf(Color.parseColor("#4CAF50")));

            Log.d("MapActivity","Inside onReceive of BroadcastReceiver of MapsActivity: " + intent.getAction());
            if (intent.getAction().equals("MyLocationService")) {
                String location = intent.getStringExtra("MyLocation");

                String[] tokens = location.split(",");
                LatLng latLng = new LatLng(Double.valueOf(tokens[0]), Double.valueOf(tokens[1]));
                if(followCurrentLocation)
                    mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(latLng, 15));
//                Toast.makeText(getBaseContext(), "Location to Maps Activity: " + location, Toast.LENGTH_SHORT).show();
                // Do stuff - maybe update my view based on the changed DB contents
            }
            else if(intent.getAction().equals("CrimeDataService"))
            {
                ArrayList<String> arrayList = intent.getStringArrayListExtra("Crime_Data");
                if(arrayList.size() > 0)
                {
                    addmarkers(arrayList);
                }
            }
        }
    }

    public void addmarkers(ArrayList<String> arrayList)
    {
        for(String crime_data : arrayList)
        {
            String[] tokens = crime_data.split("_");
            LatLng latLng1 = new LatLng(Double.valueOf(tokens[0]), Double.valueOf(tokens[1]));
            MarkerOptions markerOptions = new MarkerOptions();
            markerOptions.position(latLng1);
            markerOptions.title(tokens[2]);
            markerOptions.icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_RED));
            mMap.addMarker(markerOptions);
        }
    }

    @Override
    protected void onStart() {
        super.onStart();
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
        emerg_Phone = prefs.getString("Emerg_Phone","null");
    }

    @Override
    protected void onDestroy() {
//        if (mGoogleApiClient != null) {
//            LocationServices.FusedLocationApi.removeLocationUpdates(mGoogleApiClient, this);
//        }
        super.onDestroy();
    }

    @Override
    protected void onStop() {
        super.onStop();
    }

    public void emergActivate(View v)
    {
        if(emergActivated)
        {
            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setTitle("DeActivate Emergency Service!");
            final EditText input = new EditText(this);
            input.setHint("Enter Password");
            input.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
            builder.setView(input);

            builder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    password = input.getText().toString();
                    SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
                    String pwd = prefs.getString("User_PWD","null");;
                    if(password.equals(pwd))
                    {
                        emergActivated = false;
                        SharedPreferences.Editor editor = prefs.edit();
                        editor.putBoolean("User_Updated", true);
                        editor.commit();
                        NotificationManager notificationManager = (NotificationManager)getSystemService(Context.NOTIFICATION_SERVICE);
                        notificationManager.cancel(123);
                        emergSwitch.setBackgroundTintList(ColorStateList.valueOf(Color.parseColor("#F44336")));
//                        Toast.makeText(getBaseContext(),"Emergency Service de-activated.\n",Toast.LENGTH_LONG).show();
                    }
                }
            });

            builder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    dialog.cancel();
//                    Toast.makeText(getBaseContext(),"Emergency Service Deactivated.",Toast.LENGTH_LONG).show();
                }
            });
            builder.show();
            emergActivated = false;
        }
        else
        {
            emergActivated = true;
            emergSwitch.setBackgroundTintList(ColorStateList.valueOf(Color.parseColor("#4CAF50")));
//            Toast.makeText(getBaseContext(),"Emergency Service Activated.\n",Toast.LENGTH_LONG).show();
        }
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
        SharedPreferences.Editor editor = prefs.edit();
        editor.putBoolean("Emergency_Activated", emergActivated);
        editor.commit();
    }


    /**
     * Manipulates the map once available.
     * This callback is triggered when the map is ready to be used.
     * This is where we can add markers or lines, add listeners or move the camera. In this case,
     * we just add a marker near Sydney, Australia.
     * If Google Play services is not installed on the device, the user will be prompted to install
     * it inside the SupportMapFragment. This method will only be triggered once the user has
     * installed Google Play services and returned to the app.
     */
    @Override
    public void onMapReady(GoogleMap googleMap)
    {
        Log.d("MapActivity","Inside onMapReady of MapsActivity");
        mMap = googleMap;
        mMap.setOnMyLocationButtonClickListener(this);
        mMap.setOnCameraMoveStartedListener(this);

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED)
        {
            mMap.setMyLocationEnabled(true);
            checkGPSOn();
        }
    }


    @Override
    public void onBackPressed()
    {
        if(drawer.isDrawerOpen(GravityCompat.START))
            drawer.closeDrawer(GravityCompat.START);
        else
            closeApp();
    }

    public void closeApp()
    {
        Intent intent = new Intent(Intent.ACTION_MAIN);
        intent.addCategory(Intent.CATEGORY_HOME);
        intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        startActivity(intent);
        finish();
    }


    @Override
    protected void onPause() {
        super.onPause();
        if (dataUpdateReceiver != null)
            unregisterReceiver(dataUpdateReceiver);
    }

    @Override
    protected void onResume() {
        super.onResume();
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
        emergActivated = prefs.getBoolean("Emergency_Activated",false);

        if(emergActivated)
            emergSwitch.setBackgroundTintList(ColorStateList.valueOf(Color.parseColor("#4CAF50")));

        if (dataUpdateReceiver == null)
            dataUpdateReceiver = new DataUpdateReceiver();
        registerReceiver(dataUpdateReceiver, new IntentFilter("MyLocationService"));
        registerReceiver(dataUpdateReceiver, new IntentFilter("CrimeDataService"));
    }

    @SuppressWarnings("StatementWithEmptyBody")
    @Override
    public boolean onNavigationItemSelected(MenuItem item) {
        // Handle navigation view item clicks here.
        int id = item.getItemId();

        if (id == R.id.userSettings)
        {
            Intent intent = new Intent(this,UserSettings.class);
            startActivity(intent);
        }
        else if (id == R.id.appSettings)
        {
            Intent intent = new Intent(this,AppSettings.class);
            startActivity(intent);
        }

        drawer.closeDrawer(GravityCompat.START);
        return false;
    }
}
