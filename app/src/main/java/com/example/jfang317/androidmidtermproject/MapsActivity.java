package com.example.jfang317.androidmidtermproject;

import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.support.v4.app.FragmentActivity;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.PolylineOptions;
import com.google.maps.android.PolyUtil;

import java.util.ArrayList;
import java.util.List;

import static com.example.jfang317.androidmidtermproject.R.id.map;

public class MapsActivity extends FragmentActivity implements OnMapReadyCallback {

    private GoogleMap mMap;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_maps);
        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(map);
        mapFragment.getMapAsync(this);
    }

    // Define map
    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;
        showRoad();
    }

    // Draw polyline
    private void showRoad() {
        // Get the parameters from MainActivity
        Intent intent = getIntent();
        ArrayList<String> polyline = intent.getStringArrayListExtra("polyline");
        ArrayList<String> travel_mode = intent.getStringArrayListExtra("travel_mode");

        // Get the encoded format polyline
        List<LatLng> middlePath = PolyUtil.decode(polyline.get(polyline.size()/2));

        // Move the map to the center of route by obtaining the middle-way (Lat,Lng)
        LatLng center = new LatLng(intent.getDoubleExtra("Lat",
                middlePath.get(middlePath.size()/2).latitude),
                intent.getDoubleExtra("Lng",
                middlePath.get(middlePath.size()/2).longitude));
        mMap.moveCamera(CameraUpdateFactory.newLatLng(center));

        // Zoom in the camera
        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(center, 15));
        for (int i = 0; i < polyline.size(); i++) {

            // Decode and draw polyline
            List<LatLng> decodedPath = PolyUtil.decode(polyline.get(i));
            if (travel_mode.get(i).equals("WALKING")) {
                mMap.addPolyline(new PolylineOptions()
                        .addAll(decodedPath)
                        .width(8)
                        .color(Color.RED));
            } else if (travel_mode.get(i).equals("BIKE")) {
                mMap.addPolyline(new PolylineOptions()
                        .addAll(decodedPath)
                        .width(8)
                        .color(Color.rgb(156, 39, 176)));
            } else if (travel_mode.get(i).equals("BUS")) {
                mMap.addPolyline(new PolylineOptions()
                        .addAll(decodedPath)
                        .width(8)
                        .color(Color.GREEN));
            } else if (travel_mode.get(i).equals("HEAVY_RAIL")) {
                mMap.addPolyline(new PolylineOptions()
                        .addAll(decodedPath)
                        .width(8)
                        .color(Color.BLUE));
            } else {
                mMap.addPolyline(new PolylineOptions()
                        .addAll(decodedPath)
                        .width(8)
                        .color(Color.BLACK));
            }
        }
    }
}
