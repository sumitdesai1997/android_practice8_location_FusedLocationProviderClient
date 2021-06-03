package com.example.android_practice8_location_fusedlocationproviderclient;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.fragment.app.DialogFragment;

import android.Manifest;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GoogleApiAvailability;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.tasks.OnSuccessListener;

import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    // 2nd step - go to build.gradle file and add the dependency for the play-services-location to use FusedLocationProviderClient

    // 3rd step - declaring fusedLocationProviderClient, locationRequest and locationCallback
    FusedLocationProviderClient fusedLocationProviderClient;
    LocationRequest locationRequest;
    LocationCallback locationCallback;

    List<String> permissionsToRequest;
    List<String> permissions = new ArrayList<>();
    List<String> permissionsRejected = new ArrayList<>();

    private static final String TAG = "MainActivity";
    private static final int REQUEST_CODE = 1;

    private static final int UPDATE_INTERVAL = 5000; // 5 seconds
    private static final int FASTEST_INTERVAL = 3000; // 3 seconds

    TextView locationTV;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        locationTV = findViewById(R.id.locationTV);

        // 4th step - instantiate the fusedLocationProviderClient
        fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(this);

        // 5th step -
        // 5.1 add all services, that we need to use, to permissions
        // 5.2 filter all services from permissions and add those services inside permissionsToRequest who needs to be requested for permission (because some services from permissions may not need to be requested as they may have been already granted permission)
        permissions.add(Manifest.permission.ACCESS_FINE_LOCATION);
        permissions.add(Manifest.permission.ACCESS_COARSE_LOCATION);

        permissionsToRequest = permissionsToRequest(permissions);
        if (permissionsToRequest.size() > 0) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                requestPermissions(permissionsToRequest.toArray(new String[permissionsToRequest.size()]), REQUEST_CODE);
            }
        }
    }

    private List<String> permissionsToRequest(List<String> permissions) {
        ArrayList<String> results = new ArrayList<>();

        for (String perm : permissions) {
            if (!hasPermission(perm)) { // if the service is not already granted the permission by default, we may need to ask for permission for that service
                results.add(perm);
            }
        }
        return results;
    }

    private boolean hasPermission(String perm) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            return checkSelfPermission(perm) == PackageManager.PERMISSION_GRANTED;
        }
        return false;
    }

    // 6th step - once the UI of screen is loaded completely, we need to check the availability of the service and if service is available then find location and update location
    @Override
    protected void onPostResume() {
        super.onPostResume();

        int resultCode = GoogleApiAvailability.getInstance().isGooglePlayServicesAvailable(this);

        // check if the services is available or not
        if (resultCode != ConnectionResult.SUCCESS) {
            Dialog errorDialog = GoogleApiAvailability.getInstance().getErrorDialog(this, resultCode, REQUEST_CODE, new DialogInterface.OnCancelListener() {
                @Override
                public void onCancel(DialogInterface dialog) {
                    Toast.makeText(MainActivity.this, "No service is available", Toast.LENGTH_SHORT).show();
                }
            });
            errorDialog.show();
        } else {
            Log.i(TAG, "onPostResume");
            findLocation();
        }
    }

    private void findLocation() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED &&
                ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            return;
        }

        // show the last available location and start updating location if user starts movement
        fusedLocationProviderClient.getLastLocation().addOnSuccessListener(this, new OnSuccessListener<Location>() {
            @Override
            public void onSuccess(Location location) {
                if (location != null) {
                    locationTV.setText(String.format("Lat: %s, Lng: %s", location.getLatitude(), location.getLongitude()));
                }
            }
        });

        startUpdateLocation();
    }

    private void startUpdateLocation() {
        Log.d(TAG, "startUpdateLocation: ");
        locationRequest = LocationRequest.create();
        locationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
        locationRequest.setInterval(UPDATE_INTERVAL);
        locationRequest.setFastestInterval(FASTEST_INTERVAL);

        locationCallback = new LocationCallback() {
            @Override
            public void onLocationResult(@NonNull LocationResult locationResult) {
                super.onLocationResult(locationResult);

                if (locationResult != null) {
                    Location location = locationResult.getLastLocation();
                    locationTV.setText(String.format("Lat: %s, Lng: %s", location.getLatitude(), location.getLongitude()));
                }
            }
        };
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED &&
                ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            return;
        }

        // now as we have location request and location callback, we need to use them together for location update
        fusedLocationProviderClient.requestLocationUpdates(locationRequest, locationCallback, null);
    }

    // 7th step - after granting or denying permissions, the following method will be executed
    // total 7 steps
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if(requestCode == REQUEST_CODE){
            for(String perm: permissions){
                if(!hasPermission(perm)){ // if user denied for giving permission for the service we will add that service to permissionsRejected. so that we can show the message to user regarding the need of that service to use the application
                    permissionsRejected.add(perm);
                }
            }

            if(permissionsRejected.size() > 0){
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    if(shouldShowRequestPermissionRationale(permissionsRejected.get(0))){
                        new AlertDialog.Builder(MainActivity.this).
                                setMessage("The location permission is mandatory").
                                setPositiveButton("Ok", new DialogInterface.OnClickListener(){ // on click of ok, user will be asked one more time for permission for that service
                                    @Override
                                    public void onClick(DialogInterface dialog, int which) {
                                        requestPermissions(permissionsRejected.toArray(new String[permissionsRejected.size()]), REQUEST_CODE);
                                    }
                                }).setNegativeButton("Cancel", null).
                                create().
                                show();
                    }
                }
            }
        }
    }
}
