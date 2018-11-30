package com.example.peterjester.assignment_maps_peterjester.activity;

import android.app.Activity;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.Log;
import android.widget.Toast;

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
import com.google.firebase.database.ChildEventListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.util.ArrayList;


public class MapActivity extends Activity implements OnMapReadyCallback {

    private final String LOG_MAP = "GOOGLE_MAPS";

    // Google Maps
    private LatLng currentLatLng;
    private MapFragment mapFragment;
    private Marker currentMapMarker;
    private GoogleMap maps;

    // Broadcast Receiver
    private IntentFilter intentFilter = null;
    private BroadcastReceiverMap broadcastReceiverMap = null;

    FirebaseDatabase database = null;
    DatabaseReference myRef = null;

    ArrayList<MapLocation> mapLocations = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.maps_activity);

        mapFragment = (MapFragment) getFragmentManager().findFragmentById(R.id.google_map);
        mapFragment.getMapAsync(this);

        // Instantiating a new IntentFilter to support BroadcastReceivers
        intentFilter = new IntentFilter("com.example.peterjester.assignment_maps_peterjester.NEW_MAP_LOCATION_BROADCAST");
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

        this.maps = googleMap;

        Intent intent = getIntent();
        Double latiude = intent.getDoubleExtra("LATITUDE", Double.NaN);
        Double longitude = intent.getDoubleExtra("LONGITUDE", Double.NaN);
        String location = intent.getStringExtra("LOCATION");

        // Set initial positioning (Latitude / longitude)
        currentLatLng = new LatLng(latiude, longitude);

        maps.addMarker(new MarkerOptions()
                .position(currentLatLng)
                .title(location)
        );

        // Set the camera focus on the current LatLtn object, and other map properties.
        mapCameraConfiguration(maps);
        useMapClickListener(maps);
        useMarkerClickListener(maps);
        useMapLongClickListener(maps);
        userMapCameraMoveLister(maps);

        createMarkersFromFirebase(maps);
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
        // Call loadData() to gather all MapLocation instances from firebase.
        firebaseloadData();

        // Call createCustomMapMarkers for each MapLocation in the Collection
        for(MapLocation location : mapLocations) {
            LatLng latLong = new LatLng(Double.valueOf(location.getLatitude()), Double.valueOf(location.getLongitude()));
            createCustomMapMarkers(maps, latLong, location.getDescription(),location.getTitle() );
            triggerBroadcastMessageFromFirebase(latLong, location.getDescription());
        }
    }

    private ArrayList<MapLocation> firebaseloadData(){

        // FIXME Method should create/return a new Collection with all MapLocation available on firebase.

        database = FirebaseDatabase.getInstance();
        myRef = database.getReference("places");


        // Read from the database
        myRef.addChildEventListener(new ChildEventListener() {

            @Override
            public void onChildAdded(@NonNull DataSnapshot dataSnapshot, @Nullable String s) {
                Log.d("hello database", "onChildAdded: ");
//                This method is called once with the initial value and again
//                 whenever data at this location is updated.
                Log.d("Number of elements in database", "onDataChange: " + dataSnapshot.getChildrenCount());

                MapLocation location = dataSnapshot.getValue(MapLocation.class);

                    Log.d("Reading location", "Value is: " + location.getDescription());
                    Log.d("Reading latitude", "Value is: " + location.getLatitude());
                    Log.d("Reading longitude", "Value is: " + location.getLongitude());

            }

            @Override
            public void onChildChanged(@NonNull DataSnapshot dataSnapshot, @Nullable String s) {


            }

            @Override
            public void onChildRemoved(@NonNull DataSnapshot dataSnapshot) {
                Log.d("hello database", "onChildRemoved: ");

            }

            @Override
            public void onChildMoved(@NonNull DataSnapshot dataSnapshot, @Nullable String s) {
                Log.d("hello database", "onChildRemoved: ");

            }

            @Override
            public void onCancelled(DatabaseError error) {
                // Failed to read value
                Log.w("On cancelled", "Failed to read value.", error.toException());
            }

        });



        mapLocations.add(new MapLocation("New York","City never sleeps", String.valueOf(39.953348), String.valueOf(-75.163353)));
        mapLocations.add(new MapLocation("Paris","City of lights", String.valueOf(48.856788), String.valueOf(2.351077)));
        mapLocations.add(new MapLocation("Las Vegas","City of dreams", String.valueOf(36.167114), String.valueOf(-115.149334)));
        mapLocations.add(new MapLocation("Tokyo","City of technology", String.valueOf(35.689506), String.valueOf(139.691700)));

       return mapLocations;
    }

    private void useMapLongClickListener(GoogleMap googleMap)
    {
        googleMap.setOnMapLongClickListener(new GoogleMap.OnMapLongClickListener() {

            @Override
            public void onMapLongClick(LatLng latLng) {
                Toast.makeText(MapActivity.this, "You just pressed the map for a long time",
                        Toast.LENGTH_SHORT).show();
            }
        });

    }

    private void userMapCameraMoveLister(GoogleMap googleMap)
    {
        googleMap.setOnCameraMoveStartedListener(new GoogleMap.OnCameraMoveStartedListener() {
            @Override
            public void onCameraMoveStarted(int i) {
                Toast.makeText(MapActivity.this, "The camera is moving.",
                        Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void triggerBroadcastMessageFromFirebase(LatLng latLng, String description) {
        // Broadcast Receiver
        Intent explicitIntent = new Intent(this, BroadcastReceiverMap.class);

        explicitIntent.putExtra("LATITUDE", latLng.latitude);
        explicitIntent.putExtra("LONGITUDE", latLng.longitude);
        explicitIntent.putExtra("LOCATION", description);

        sendBroadcast(explicitIntent);
    }

}
