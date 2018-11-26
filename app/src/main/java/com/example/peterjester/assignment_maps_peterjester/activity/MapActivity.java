package com.example.peterjester.assignment_maps_peterjester.activity;

import android.app.Activity;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.util.Log;

import com.example.peterjester.assignment_maps_peterjester.R;
import com.example.peterjester.assignment_maps_peterjester.broadcast.BroadcastReceiverMap;
import com.example.peterjester.assignment_maps_peterjester.model.MapLocation;
import com.google.android.gms.maps.CameraUpdate;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapFragment;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;

import java.util.ArrayList;


public class MapActivity extends Activity implements OnMapReadyCallback {

    private final String LOG_MAP = "GOOGLE_MAPS";

    // Google Maps
    private LatLng currentLatLng;
    private MapFragment mapFragment;
    private Marker currentMapMarker;

    // Broadcast Receiver
    private IntentFilter intentFilter = null;
    private BroadcastReceiverMap broadcastReceiverMap = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.maps_activity);

        mapFragment = (MapFragment) getFragmentManager().findFragmentById(R.id.google_map);
        mapFragment.getMapAsync(this);

        // Instantiating a new IntentFilter to support BroadcastReceivers
        intentFilter = new IntentFilter("mc.sweng888.psu.edu.newmapsexample.action.NEW_MAP_LOCATION_BROADCAST");
        broadcastReceiverMap = new BroadcastReceiverMap();

    }

    @Override
    protected void onStart() {
        super.onStart();
        // Register the Broadcast Receiver.
        registerReceiver(broadcastReceiverMap, intentFilter);
    }

    @Override
    protected void onStop() {
        // Unregister the Broadcast Receiver
        unregisterReceiver(broadcastReceiverMap);
        super.onStop();



    }

    // Step 1 - Set up initial configuration for the map.
    @Override
    public void onMapReady(GoogleMap googleMap) {

        Intent intent = getIntent();
        Double latiude = intent.getDoubleExtra("LATITUDE", Double.NaN);
        Double longitude = intent.getDoubleExtra("LONGITUDE", Double.NaN);
        String location = intent.getStringExtra("LOCATION");

        // Set initial positionning (Latitude / longitude)
        currentLatLng = new LatLng(latiude, longitude);

        googleMap.addMarker(new MarkerOptions()
                .position(currentLatLng)
                .title(location)
        );

        // Set the camera focus on the current LatLtn object, and other map properties.
        mapCameraConfiguration(googleMap);
        useMapClickListener(googleMap);
        useMarkerClickListener(googleMap);
    }

    /** Step 2 - Set a few properties for the map when it is ready to be displayed.
       Zoom position varies from 2 to 21.
       Camera position implements a builder pattern, which allows to customize the view.
      Bearing - screen rotation ( the angulation needs to be defined ).
      Tilt - screen inclination ( the angulation needs to be defined ).
    **/
    private void mapCameraConfiguration(GoogleMap googleMap){

        CameraPosition cameraPosition = new CameraPosition.Builder()
                .target(currentLatLng)
                .zoom(14)
                .bearing(0)
                .build();

        // Camera that makes reference to the maps view
        CameraUpdate cameraUpdate = CameraUpdateFactory.newCameraPosition(cameraPosition);

        googleMap.animateCamera(cameraUpdate, 3000, new GoogleMap.CancelableCallback() {

            @Override
            public void onFinish() {
                Log.i(LOG_MAP, "googleMap.animateCamera:onFinish is active");
            }

            @Override
            public void onCancel() {
                Log.i(LOG_MAP, "googleMap.animateCamera:onCancel is active");
            }});
    }

    /** Step 3 - Reusable code
     This method is called everytime the use wants to place a new marker on the map. **/
    private void createCustomMapMarkers(GoogleMap googleMap, LatLng latlng, String title, String snippet){

        MarkerOptions markerOptions = new MarkerOptions();
        markerOptions.position(latlng) // coordinates
                .title(title) // location name
                .snippet(snippet); // location description

        // Update the global variable (currentMapMarker)
        currentMapMarker = googleMap.addMarker(markerOptions);
    }

    // Step 4 - Define a new marker based on a Map click (uses onMapClickListener)
    private void useMapClickListener(final GoogleMap googleMap){

        googleMap.setOnMapClickListener(new GoogleMap.OnMapClickListener() {

            @Override
            public void onMapClick(LatLng latltn) {
                Log.i(LOG_MAP, "setOnMapClickListener");

                if(currentMapMarker != null){
                    // Remove current marker from the map.
                    currentMapMarker.remove();
                }
                // The current marker is updated with the new position based on the click.
                createCustomMapMarkers(
                        googleMap,
                        new LatLng(latltn.latitude, latltn.longitude),
                        "New Marker",
                        "Listener onMapClick - new position"
                                +"lat: "+latltn.latitude
                                +" lng: "+ latltn.longitude);
            }
        });
    }

    // Step 5 - Use OnMarkerClickListener for displaying information about the MapLocation
    private void useMarkerClickListener(GoogleMap googleMap){
        googleMap.setOnMarkerClickListener(new GoogleMap.OnMarkerClickListener() {

            // If FALSE, when the map should have the standard behavior (based on the android framework)
            // When the marker is clicked, it wil focus / centralize on the specific point on the map
            // and show the InfoWindow. IF TRUE, a new behavior needs to be specified in the source code.
            // However, you are not required to change the behavior for this method.
            @Override
            public boolean onMarkerClick(Marker marker) {
                Log.i(LOG_MAP, "setOnMarkerClickListener");

                return false;
            }
        });
    }

    public void createMarkersFromFirebase(GoogleMap googleMap){
        // FIXME Call loadData() to gather all MapLocation instances from firebase.
        // FIXME Call createCustomMapMarkers for each MapLocation in the Collection
    }

    private ArrayList<MapLocation> loadData(){

        // FIXME Method should create/return a new Collection with all MapLocation available on firebase.

        ArrayList<MapLocation> mapLocations = new ArrayList<>();

        mapLocations.add(new MapLocation("New York","City never sleeps", String.valueOf(39.953348), String.valueOf(-75.163353)));
        mapLocations.add(new MapLocation("Paris","City of lights", String.valueOf(48.856788), String.valueOf(2.351077)));
        mapLocations.add(new MapLocation("Las Vegas","City of dreams", String.valueOf(36.167114), String.valueOf(-115.149334)));
        mapLocations.add(new MapLocation("Tokyo","City of technology", String.valueOf(35.689506), String.valueOf(139.691700)));

       return mapLocations;
    }

}
