package com.a500.sweng.sickness_locator;

import android.content.Context;
import android.content.pm.PackageManager;
import android.location.Criteria;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.content.Intent;

import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;
import android.location.Address;
import android.location.Geocoder;

import com.a500.sweng.sickness_locator.models.SicknessEntry;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.io.IOException;
import java.util.List;

/**
 * Android activity that displays a google map. The map displays all map markers for sickness entries
 * in the firebase database. The map defaults to show the users current location, if the user allows
 * permission. The map will display the entire United States as a default if permission is not given
 * to access the users's current position.
 *
 */
public class MapsActivity extends AppCompatActivity implements OnMapReadyCallback {
    private GoogleMap mMap;
    // Reference to the firebase databse
    private DatabaseReference mDatabaseSicknessEntries;

    // Default latitude for the map
    private double mLatitude = 38.3887885;

    // Default longitude for the map
    private double mLongitude = -93.5316315;

    // The default zoom on the map
    private float mZoom = 4.25f;

    // The minimum zoom the map is allowed to render
    private float mMaxZoom = 14.0f;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_maps);

        if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{android.Manifest.permission.ACCESS_COARSE_LOCATION}, 11);
            return;
        }

        // Try to get the users current location
        LocationManager locationmanager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        Criteria cri = new Criteria();
        cri.setAccuracy(Criteria.ACCURACY_COARSE);
        String provider = locationmanager.getBestProvider(cri, false);

        if (provider != null & !provider.equals("")) {
            LocationListener locationChangeListener = new LocationListener() {
                public void onLocationChanged(Location l) {
                    if (l != null) {
                        mLatitude = l.getLatitude();
                        mLongitude = l.getLongitude();
                        mZoom = mMaxZoom;
                    }
                }
                public void onProviderEnabled(String p) {}
                public void onProviderDisabled(String p) {}
                public void onStatusChanged(String p, int status, Bundle extras) {}
            };

            Location location = locationmanager.getLastKnownLocation(provider);
            locationmanager.requestLocationUpdates(provider, 2000, 1, locationChangeListener);
            if (location != null) {
                mLatitude = location.getLatitude();
                mLongitude = location.getLongitude();
                mZoom = mMaxZoom;
            }
        }

        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);
    }

    /**
     * Displays the menu.
     */
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.menu_options, menu);
        return true;
    }

    /**
     * Changes screen view.
     * This is triggered by the user selecting an item on the menu. Will change the view to whichever
     * option the user selects.
     */
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.menu_entry:
                startActivity(new Intent(this, SicknessEntryActivity.class));
                return true;
            case R.id.menu_map:
                startActivity(new Intent(this, MapsActivity.class));
                return true;
            case R.id.menu_reports:
                Intent intent = new Intent(this, ReportsActivity.class);
                startActivity(intent);
                return true;
            case R.id.menu_settings:
                startActivity(new Intent(this, UserSettingsActivity.class));
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
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
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;
        mMap.setMaxZoomPreference(mMaxZoom);

        mDatabaseSicknessEntries = FirebaseDatabase.getInstance().getReference("sicknessEntries");
        mDatabaseSicknessEntries.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                for (DataSnapshot postSnapshot : dataSnapshot.getChildren()) {
                    SicknessEntry entry = postSnapshot.getValue(SicknessEntry.class);

                    LatLng position = new LatLng(entry.getLatitude(), entry.getLongitude());
                    mMap.addMarker(new MarkerOptions()
                        .position(position)
                        .icon(BitmapDescriptorFactory.defaultMarker(entry.getMarkerColor()))
                        .title(entry.getType() + ": " + entry.getSickness())
                        .snippet("Sickness Information will be display here. Website link will also be here.")
                    );
                }
            }

            @Override
            public void onCancelled(DatabaseError error) {}
        });

        // Move the camera to either the default location, or the users current location
        LatLng position = new LatLng(mLatitude, mLongitude);
        mMap.moveCamera(CameraUpdateFactory.zoomTo(mZoom));
        mMap.moveCamera(CameraUpdateFactory.newLatLng(position));
    }

    /**
     * Performs a search based on input from the user. The method uses google location services to
     * decode the string and return a latitude/longitude. The map will be updated and have the camera
     * move to the location that the user searched for.
     */
    public void onMapSearch(View view) {
        EditText locationSearch = (EditText) findViewById(R.id.editText);
        String location = locationSearch.getText().toString();
        List<Address> addressList = null;

        if (location != null || !location.equals("")) {
            Geocoder geocoder = new Geocoder(this);
            try {
                addressList = geocoder.getFromLocationName(location, 1);

            } catch (IOException e) {
                e.printStackTrace();
            }
            Address address = addressList.get(0);
            LatLng latLng = new LatLng(address.getLatitude(), address.getLongitude());

            // Move map camera to the location searched for
            mMap.animateCamera(CameraUpdateFactory.newLatLng(latLng));
        }
    }
}
