package com.example.jfang317.androidmidtermproject;

import android.Manifest;
import android.app.AlertDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.database.SQLException;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.text.Html;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;

import com.google.android.gms.maps.model.LatLng;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import static android.location.Location.distanceBetween;


public class MainActivity extends AppCompatActivity {

    // layout components
    private Button btnGetLocation1 = null;
    private Button btnGetLocation2 = null;
    private Button btnSearch = null;
    private Button btnshowmap= null;
    private EditText editLocation1 = null;
    private EditText editLocation2 = null;
    private String location_txt;
    private String start_txt;
    private String end_txt;
    private ListView mListView;

    // For receiving GPS data
    private BroadcastReceiver broadcastReceiver;

    // Check if "使用目前位置" button pressed
    private boolean flag1 = false;
    private boolean flag2 = false;

    // Google direction API url & key
    String dir_api = "https://maps.googleapis.com/maps/api/directions/json?";
    private String key = "AIzaSyBtOZUrmoncqHCQBiwmU6FTvD1ULPKZNYc";

    // List of route data to display
    protected ArrayList<Map<String, Object>> route_list = new ArrayList<>(); // Raw data from Google direction api
    protected ArrayList<Map<String, Object>> steps_list = new ArrayList<>(); // Store data for specific travel mode
    protected ArrayList<Map<String, Object>> ubike_list = new ArrayList<>(); // Route for Ubike
    protected ArrayList<Map<String, Object>> final_list = new ArrayList<>(); // Optimized result by adding ubike station database
    protected Map<String, Object> overall = new HashMap<>(); // Raw overall traveling data
    protected ArrayList<String> route_polyline = new ArrayList<>(); // polyline set of raw data
    protected ArrayList<String> ubike_polyline = new ArrayList<>(); // polyline set of ubike route
    protected ArrayList<String> final_polyline = new ArrayList<>(); // polyline set of final optimized route

    // Cursor for ubike database
    Cursor c = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Specify ID for layout components
        editLocation1 = (EditText) findViewById(R.id.editText1);
        btnGetLocation1 = (Button) findViewById(R.id.button);
        editLocation2 = (EditText) findViewById(R.id.editText2);
        btnGetLocation2 = (Button) findViewById(R.id.button2);
        btnshowmap = (Button) findViewById(R.id.button4);
        mListView = (ListView) findViewById(R.id.list);
        mListView.setVisibility(View.INVISIBLE);

        // Ask for permission of GPS for this app, enable it
        if(!runtime_permissions()) enable_gps();
    }

    // onclick button "搜尋"
    public void search(View view) {
        // Clear out all the global variable
        route_list.clear();
        steps_list.clear();
        ubike_list.clear();
        final_list.clear();
        route_polyline.clear();
        ubike_polyline.clear();
        final_polyline.clear();
        overall.clear();

        // Get string from text box
        start_txt = editLocation1.getText().toString();
        end_txt = editLocation2.getText().toString();

        // Hide the soft keyboard
        if (view != null) {
            InputMethodManager imm = (InputMethodManager)getSystemService(Context.INPUT_METHOD_SERVICE);
            imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
        }

        // Start the whole process
        sendRequest(start_txt, end_txt);
    }

    // onclick button "地圖"
    public void showmap(View view) {
        // Declare MapsActivity.class
        Intent intent = new Intent(this, MapsActivity.class);

        // Parameters to pass over to MapsActivity
        intent.putStringArrayListExtra("polyline", final_polyline);
        ArrayList<String> travel_mode = new ArrayList<>();
        for (int i = 0; i < final_list.size(); i++) {
            travel_mode.add(final_list.get(i).get("travel_mode").toString());
        }
        intent.putStringArrayListExtra("travel_mode", travel_mode);
        Bundle bundle = new Bundle();
        LatLng start = (LatLng) overall.get("start_location");
        LatLng end = (LatLng) overall.get("end_location");
        bundle.putDouble("Lat", (start.latitude + end.latitude) / 2);
        bundle.putDouble("Lng", (start.longitude + end.longitude) / 2);

        // Start MapsActivity
        startActivity(intent);
    }

    // onclick button top "使用目前位置"
    public void buttonClick1(View view) {
        flag1 = true;
        // Show GPS text on text box
        editLocation1.setText(location_txt);
    }

    // onclick button bottom "使用目前位置"
    public void buttonClick2(View view) {
        flag2 = true;
        // Show GPS text on text box
        editLocation2.setText(location_txt);
    }

    // Results are showed in self-constructed layout
    private class MyAdapter extends BaseAdapter {

        // Define size for adapter layout
        @Override
        public int getCount() {
            return final_list.size();
        }

        @Override
        public Object getItem(int position) {
            return null;
        }

        @Override
        public long getItemId(int position) {
            return 0;
        }

        // Show the results in adapter
        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            View v = convertView;
            Holder holder;
            if (v == null) {
                v = LayoutInflater.from(getApplicationContext()).inflate(R.layout.adapter, null);
                holder = new Holder();
                holder.image = (ImageView) v.findViewById(R.id.image);
                holder.text = (TextView) v.findViewById(R.id.text);
                holder.text1 = (TextView) v.findViewById(R.id.text1);
                v.setTag(holder);
            } else {
                holder = (Holder) v.getTag();
            }

            // Set image
            if (final_list.get(position).get("travel_mode").equals("WALKING")) {
                holder.image.setImageResource(R.drawable.walk);
            } else if (final_list.get(position).get("travel_mode").equals("BUS")) {
                holder.image.setImageResource(R.drawable.bus);
            } else if (final_list.get(position).get("travel_mode").equals("HIGH_SPEED_TRAIN")) {
                holder.image.setImageResource(R.drawable.speedtrain);
            } else if (final_list.get(position).get("travel_mode").equals("HEAVY_RAIL")) {
                holder.image.setImageResource(R.drawable.train);
            } else if (final_list.get(position).get("travel_mode").equals("BIKE")) {
                holder.image.setImageResource(R.drawable.bike);
            }

            // Set text
            holder.text1.setText(final_list.get(position).get("distance").toString());
            holder.text.setText(final_list.get(position).get("html_instructions").toString());
            return v;
        }

        class Holder{
            ImageView image;
            TextView text;
            TextView text1;
        }
    }

    // Get / Store GPS data
    @Override
    protected void onResume() {
        super.onResume();
        if(broadcastReceiver == null){
            broadcastReceiver = new BroadcastReceiver() {
                @Override
                public void onReceive(Context context, Intent intent) {
                    location_txt = intent.getExtras().get("coordinates").toString();
                }
            };
        }
        registerReceiver(broadcastReceiver,new IntentFilter("location_update"));
    }

    // Unregister GPS receiver
    @Override
    protected void onDestroy() {
        super.onDestroy();
        if(broadcastReceiver != null){
            unregisterReceiver(broadcastReceiver);
        }
    }

    // Start GPS service
    private void enable_gps() {
        Intent i =new Intent(getApplicationContext(),GPS_Service.class);
        startService(i);
    }

    // Ask for GPS permissions and build apk version
    private boolean runtime_permissions() {
        if(Build.VERSION.SDK_INT >= 23 && ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED){

            requestPermissions(new String[]{Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION},100);

            return true;
        }
        return false;
    }

    // Check for GPS permissions
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if(requestCode == 100){
            if( grantResults[0] == PackageManager.PERMISSION_GRANTED && grantResults[1] == PackageManager.PERMISSION_GRANTED){
                enable_gps();
            }else {
                runtime_permissions();
            }
        }
    }

    // Form the main request for Google directions api
    private void sendRequest(String origin, String destination) {
        // Setup request parameters
        String travel_mode = "transit";
        String departure_time = "now";
        String language = "zh-TW";

        // Build request url
        Uri.Builder builder = Uri.parse(dir_api).buildUpon();
        builder.appendQueryParameter("origin", origin)
                .appendQueryParameter("destination", destination)
                .appendQueryParameter("departure_time", departure_time)
                .appendQueryParameter("mode", travel_mode)
                .appendQueryParameter("language", language)
                .appendQueryParameter("key", key);
        String url = builder.build().toString();
        Log.d("URL",url);

        // Send request to target url
        new JSONParser().getJSONFromUrl(url,new responseListener() {
            @Override
            public void onResponseComplete(String response) {
                parseJSON(response);
            }
        });
    }

    // Parse the responding JSON string
    private void parseJSON(String data) {
        try {
            // Create JSON Objects
            JSONObject directions = new JSONObject(data);
            JSONObject legs = directions
                    .getJSONArray("routes")
                    .getJSONObject(0)
                    .getJSONArray("legs")
                    .getJSONObject(0);
            JSONArray steps = legs.getJSONArray("steps");

            // Get and store start / end location / address data & total duration,
            LatLng start_location = new LatLng(legs.getJSONObject("start_location").optDouble("lat"),
                    legs.getJSONObject("start_location").optDouble("lng"));
            LatLng end_location = new LatLng(legs.getJSONObject("end_location").optDouble("lat"),
                    legs.getJSONObject("end_location").optDouble("lng"));
            String start_address = legs.getString("start_address");
            String end_address = legs.getString("end_address");

            // store summary data
            overall.put("start_address", start_address);
            overall.put("end_address", end_address);
            overall.put("start_location", start_location);
            overall.put("end_location", end_location);

            /*
            Log.v("JSON", start_location.toString());
            Log.v("JSON", end_location.toString());
            Log.v("JSON", start_address);
            Log.v("JSON", end_address);
            */

            // Parse the main route data
            for (int i = 0; i < steps.length(); i++) {
                // Construct info. for steps_list first
                Map<String, Object> steps_info = new HashMap<>();

                // Get the duration for specific travel mode
                int time = steps.getJSONObject(i).getJSONObject("duration").optInt("value");

                // Get the start / end for specific travel mode
                LatLng start = new LatLng(
                        steps.getJSONObject(i).getJSONObject("start_location").optDouble("lat"),
                        steps.getJSONObject(i).getJSONObject("start_location").optDouble("lng"));
                LatLng end = new LatLng(
                        steps.getJSONObject(i).getJSONObject("end_location").optDouble("lat"),
                        steps.getJSONObject(i).getJSONObject("end_location").optDouble("lng"));

                // Count the instruction quantity
                int count;
                if (steps.getJSONObject(i).has("steps")) {
                    count = steps.getJSONObject(i).getJSONArray("steps").length();
                } else count = 1;

                // Form by adding constructed info. into step_list
                steps_info.put("duration", time);
                steps_info.put("start_location", start);
                steps_info.put("end_location", end);
                steps_info.put("count", count);
                steps_list.add(steps_info);

                // Handling different responding cases of JSON
                // If current travel_mode is walking
                if (steps.getJSONObject(i).getString("travel_mode").equals("WALKING")) {
                    // If current travel mode has several instructions
                    if (steps.getJSONObject(i).has("steps")) {
                        JSONArray step_index = steps.getJSONObject(i).getJSONArray("steps");
                        for (int j = 0; j < step_index.length(); j++) {
                            Map<String, Object> index = new HashMap<>();

                            // Get the distance for current instruction
                            String distance = step_index
                                    .getJSONObject(j)
                                    .getJSONObject("distance")
                                    .getString("text");
                            index.put("distance", distance);

                            // Get / Form html instructions
                            if (step_index.getJSONObject(j).has("html_instructions")) {
                                String html_instructions = step_index
                                        .getJSONObject(j)
                                        .getString("html_instructions");
                                html_instructions = Html.fromHtml(html_instructions).toString();
                                html_instructions = html_instructions
                                        .replaceAll("(?m)^[ \t]*\r?\n", "")
                                        .replaceAll("開", "走");
                                index.put("html_instructions", html_instructions);
                            } else {
                                String html_instructions = steps
                                        .getJSONObject(i)
                                        .getString("html_instructions");
                                html_instructions = Html.fromHtml(html_instructions).toString();
                                html_instructions = html_instructions
                                        .replaceAll("(?m)^[ \t]*\r?\n", "")
                                        .replaceAll("開", "走");
                                index.put("html_instructions", html_instructions);
                            }

                            // Get travel mode for current instruction
                            String travel_mode = step_index
                                    .getJSONObject(j)
                                    .getString("travel_mode");
                            index.put("travel_mode", travel_mode);

                            // Get polyline for current instruction
                            String points = step_index
                                    .getJSONObject(j)
                                    .getJSONObject("polyline")
                                    .getString("points");

                            // Form by adding constructed info. into route_polyline / route_list
                            route_polyline.add(points);
                            route_list.add(index);
                        }
                    } else {
                        Map<String, Object> index = new HashMap<>();

                        // Get the distance for current instruction
                        String distance = steps
                                .getJSONObject(i)
                                .getJSONObject("distance")
                                .getString("text");
                        index.put("distance", distance);

                        // Get html instructions
                        String html_instructions = steps
                                .getJSONObject(i)
                                .getString("html_instructions");
                        html_instructions = Html.fromHtml(html_instructions).toString();
                        html_instructions = html_instructions
                                .replaceAll("(?m)^[ \t]*\r?\n", "")
                                .replaceAll("開", "走");
                        index.put("html_instructions", html_instructions);

                        // Get travel mode for current instruction
                        String travel_mode = steps.getJSONObject(i).getString("travel_mode");
                        index.put("travel_mode", travel_mode);

                        // Get polyline for current instruction
                        String points = steps
                                .getJSONObject(i)
                                .getJSONObject("polyline")
                                .getString("points");

                        // Form by adding constructed info. into route_polyline / route_list
                        route_polyline.add(points);
                        route_list.add(index);
                    }
                    // If current travel_mode is public transport
                } else if (steps.getJSONObject(i).getString("travel_mode").equals("TRANSIT")) {
                    // If current travel_mode has several instructions
                    if (steps.getJSONObject(i).has("steps")) {
                        JSONArray step_index = steps.getJSONObject(i).getJSONArray("steps");
                        for (int j = 0; j < step_index.length(); j++) {
                            Map<String, Object> index = new HashMap<>();
                            JSONObject transit_details = step_index
                                    .getJSONObject(j)
                                    .getJSONObject("transit_details");

                            // Get the distance for current instruction
                            String distance = step_index
                                    .getJSONObject(j)
                                    .getJSONObject("distance")
                                    .getString("text");
                            index.put("distance", distance);

                            // Construct self-defined html instructions
                            String departure_time = transit_details
                                    .getJSONObject("departure_time")
                                    .getString("text");
                            String short_name = transit_details
                                    .getJSONObject("line")
                                    .getString("short_name");
                            String departure_stop = transit_details
                                    .getJSONObject("departure_stop")
                                    .getString("name");
                            String arrival_stop = transit_details
                                    .getJSONObject("arrival_stop")
                                    .getString("name");
                            StringBuilder instructions = new StringBuilder();
                            instructions.append(departure_time)
                                    .append(short_name)
                                    .append(System.getProperty("line.separator"))
                                    .append(departure_stop)
                                    .append("到")
                                    .append(arrival_stop);
                            String html_instructions = instructions.toString();
                            index.put("html_instructions", html_instructions);

                            // Get transit type for current instruction
                            String travel_mode = transit_details
                                    .getJSONObject("line")
                                    .getJSONObject("vehicle")
                                    .getString("type");
                            index.put("travel_mode", travel_mode);

                            // Get polyline for current instruction
                            String points = step_index
                                    .getJSONObject(j)
                                    .getJSONObject("polyline")
                                    .getString("points");

                            // Form by adding constructed info. into route_polyline / route_list
                            route_polyline.add(points);
                            route_list.add(index);
                        }
                    } else {
                        Map<String, Object> index = new HashMap<>();
                        JSONObject transit_details = steps
                                .getJSONObject(i)
                                .getJSONObject("transit_details");

                        // Get the distance for current instruction
                        String distance = steps
                                .getJSONObject(i)
                                .getJSONObject("distance")
                                .getString("text");
                        index.put("distance", distance);

                        // Construct self-defined html instructions
                        String departure_time = transit_details
                                .getJSONObject("departure_time")
                                .getString("text");
                        String short_name = transit_details
                                .getJSONObject("line")
                                .getString("short_name");
                        String departure_stop = transit_details
                                .getJSONObject("departure_stop")
                                .getString("name");
                        String arrival_stop = transit_details
                                .getJSONObject("arrival_stop")
                                .getString("name");
                        StringBuilder instructions = new StringBuilder();
                        instructions.append(departure_time)
                                .append(short_name)
                                .append(System.getProperty("line.separator"))
                                .append(departure_stop)
                                .append("到")
                                .append(arrival_stop);
                        String html_instructions = instructions.toString();
                        index.put("html_instructions", html_instructions);

                        // Get transit type for current instruction
                        String travel_mode = transit_details
                                .getJSONObject("line")
                                .getJSONObject("vehicle")
                                .getString("type");
                        index.put("travel_mode", travel_mode);

                        // Get polyline for current instruction
                        String points = steps
                                .getJSONObject(i)
                                .getJSONObject("polyline")
                                .getString("points");

                        // Form by adding constructed info. into route_polyline / route_list
                        route_polyline.add(points);
                        route_list.add(index);
                    }
                }
            }/*
            for (int i = 0; i < steps_list.size(); i++) {
                for (Map.Entry entry : steps_list.get(i).entrySet()) {
                    Log.v("YO", entry.getValue().toString());
                }
            }
            for (int i = 0; i < route_list.size(); i++) {
                for (Map.Entry entry : route_list.get(i).entrySet()) {
                    Log.v("YO", entry.getValue().toString());
                }
            }*/
            // Begin optimization by adding ubike stations into route
            ubikePartOpt();
        } catch (JSONException e) {
            try {
                // If responding JSON has a "ZERO_RESULT" state,
                // it should still contain the encoded place id data within request text location.
                // ( Notice that if request location is in (Lat,Lng) format, it will not encode )
                // Resend the request on walking mode by suggestion with place id and (Lat,Lng)
                if (flag1 && flag2) {
                    zeroResults(start_txt, end_txt);
                } else if (!flag1 && flag2) {
                    String start_id = new JSONObject(data)
                            .getJSONArray("geocoded_waypoints")
                            .getJSONObject(0)
                            .getString("place_id");
                    start_id = "place_id:" + start_id;
                    zeroResults(start_id, end_txt);
                } else if (flag1 && !flag2) {
                    String end_id = new JSONObject(data)
                            .getJSONArray("geocoded_waypoints")
                            .getJSONObject(1)
                            .getString("place_id");
                    end_id = "place_id:" + end_id;
                    zeroResults(start_txt, end_id);
                } else {
                    String start_id = new JSONObject(data)
                            .getJSONArray("geocoded_waypoints")
                            .getJSONObject(0)
                            .getString("place_id");
                    String end_id = new JSONObject(data)
                            .getJSONArray("geocoded_waypoints")
                            .getJSONObject(1)
                            .getString("place_id");
                    start_id = "place_id:" + start_id;
                    end_id = "place_id:" + end_id;
                    zeroResults(start_id, end_id);
                }
            } catch (JSONException j) {
                Log.v("JSON", "1");
                errorMsg("Error", "unexpected response data");
            }
        }

    }

    // Resend request on walkijng mode
    private void zeroResults(String start, String end) {

        Uri.Builder builder = Uri.parse(dir_api).buildUpon();
        builder.appendQueryParameter("origin", start)
                .appendQueryParameter("destination", end)
                .appendQueryParameter("mode", "walking")
                .appendQueryParameter("language", "zh-TW")
                .appendQueryParameter("key", key);
        String url = builder.build().toString();
        Log.d("URL",url);

        new JSONParser().getJSONFromUrl(url,new responseListener() {
            @Override
            public void onResponseComplete(String response) {
                parseJSON(response);
            }
        });
    }

    // Search for the nearest ubike station
    private String getUbikeGps(LatLng location) {
        DatabaseHelper myDbHelper = new DatabaseHelper(MainActivity.this);
        // if have db use it
        try {
            myDbHelper.createDataBase();
        } catch (IOException ioe) {
            throw new Error("Unable to create database");
        }
        try {
            myDbHelper.openDataBase();
        } catch (SQLException sqle) {
            throw sqle;
        }
        //query the db from beginning.
        c = myDbHelper.query("YoubileInfo", null, null, null, null, null, null);
        String resultLongitude = null, resultLatitude = null;
        float minDist = 10000000;
        // go to the first item.
        if (c.moveToFirst()) {
            //get the whole db items.
            do {
                //Using gps data to get what i want.
                String ubikeLon = c.getString(1);
                String ubikeLat = c.getString(2);

                Double lonDouble = location.longitude;
                Double latDouble = location.latitude;
                Double ubikeLongitude = Double.parseDouble(ubikeLon);
                Double ubikeLatitude = Double.parseDouble(ubikeLat);
                float[] result = new float[1];

                distanceBetween(latDouble, lonDouble, ubikeLatitude, ubikeLongitude, result);

                if (result[0] < minDist) {
                    minDist = result[0];
                    resultLatitude = ubikeLat;
                    resultLongitude = ubikeLon;
                }
            } while (c.moveToNext());
        }
        return (resultLatitude + "," + resultLongitude);
    }

    // For a specific travel mode, find out if if can be rearranged by ubike
    private void ubikePartOpt() {
        // Form start / end location string
        LatLng start_location = (LatLng) steps_list.get(0).get("start_location");
        LatLng end_location = (LatLng) steps_list.get(0).get("end_location");
        String start = Double.toString(start_location.latitude)
                + "," + Double.toString(start_location.longitude);
        String end = Double.toString(end_location.latitude)
                + "," + Double.toString(end_location.longitude);

        // Fetch nearest ubike station according to start / end location
        String ubike_start = getUbikeGps(start_location);
        String ubike_end = getUbikeGps(end_location);

        // If both ubike station are in the same location -> no use of optimization
        if (ubike_start.equals(ubike_end)) {
            for (int i = 0; i < (Integer) steps_list.get(0).get("count"); i++) {
                // Since steps_list has recorded the number of instructions of each travel modes,
                // we can know that the first n list are for this travel mode.
                // In order to get these info, I copied the first element into final then deleted it.
                // By doing this in the number of loops,
                // each time I get the first element is actually the next from the original list.
                final_list.add(route_list.get(0));
                final_polyline.add(route_polyline.get(0));
                route_list.remove(0);
                route_polyline.remove(0);
            }
            // Remove data of this travel mode when finished updating final_list
            steps_list.remove(0);

            // If no more travel mode to optimize, prepare to show the result
            // Else do the function again until steps_list is empty
            if (steps_list.size() == 0) {
                printList();
                return;
            } else {
                ubikePartOpt();
                return;
            }
        }

        // There are three requests required:
        // 1. Walking from start location to ubike station
        // 2. Cycling between ubike stations
        // 3. Walking from ubike station to end location
        // Below are the url constructors
        Uri.Builder builder1 = Uri.parse(dir_api).buildUpon();
        builder1.appendQueryParameter("origin", start)
                .appendQueryParameter("destination", ubike_start)
                .appendQueryParameter("mode", "walking")
                .appendQueryParameter("language", "zh-TW")
                .appendQueryParameter("key", key);
        String url1 = builder1.build().toString();
        Log.d("URL",url1);

        Uri.Builder builder2 = Uri.parse(dir_api).buildUpon();
        builder2.appendQueryParameter("origin", ubike_start)
                .appendQueryParameter("destination", ubike_end)
                .appendQueryParameter("mode", "walking")
                .appendQueryParameter("language", "zh-TW")
                .appendQueryParameter("key", key);
        String url2 = builder2.build().toString();
        Log.d("URL",url2);

        Uri.Builder builder3 = Uri.parse(dir_api).buildUpon();
        builder3.appendQueryParameter("origin", ubike_end)
                .appendQueryParameter("destination", end)
                .appendQueryParameter("mode", "walking")
                .appendQueryParameter("language", "zh-TW")
                .appendQueryParameter("key", key);
        String url3 = builder3.build().toString();
        Log.d("URL",url3);

        String[] url = new String[3];
        url[0] = url1;
        url[1] = url2;
        url[2] = url3;

        // Start sending requests, in order to keep the result, requests are synced
        walkingRequest1(url);
    }

    // First request, mostly same as parsJSON()
    private void walkingRequest1(final String[] url) {
        new JSONParser().getJSONFromUrl(url[0],new responseListener() {
            @Override
            public void onResponseComplete(String response) {
                try {
                    JSONObject directions = new JSONObject(response);
                    JSONObject legs = directions
                            .getJSONArray("routes")
                            .getJSONObject(0)
                            .getJSONArray("legs")
                            .getJSONObject(0);
                    JSONArray steps = legs.getJSONArray("steps");

                    // Store the duration data
                    int duration = legs.getJSONObject("duration").optInt("value");

                    for (int j = 0; j < steps.length(); j++) {
                        Map<String, Object> index = new HashMap<>();

                        // Get/Form html instructions
                        if (steps.getJSONObject(j).has("html_instructions")) {
                            String distance = steps
                                    .getJSONObject(j)
                                    .getJSONObject("distance")
                                    .getString("text");
                            index.put("distance", distance);

                            String html_instructions = steps
                                    .getJSONObject(j)
                                    .getString("html_instructions");
                            html_instructions = Html.fromHtml(html_instructions).toString();
                            html_instructions = html_instructions
                                    .replaceAll("(?m)^[ \t]*\r?\n", "")
                                    .replaceAll("開", "走");
                            index.put("html_instructions", html_instructions);
                        }

                        // Get travel mode
                        String travel_mode = steps
                                .getJSONObject(j)
                                .getString("travel_mode");
                        index.put("travel_mode", travel_mode);

                        // Get polyline
                        String points = steps
                                .getJSONObject(j)
                                .getJSONObject("polyline")
                                .getString("points");
                        ubike_polyline.add(points);
                        ubike_list.add(index);
                    }

                    // Send duration value to next request
                    walkingRequest2(url, duration);
                } catch (JSONException e) {
                    Log.v("JSON", "2");
                    errorMsg("Error", "unexpected response data");
                }
            }
        });
    }

    // Second request, mostly same as parsJSON()
    private void walkingRequest2(final String[] url, final int time) {
        new JSONParser().getJSONFromUrl(url[1],new responseListener() {
            @Override
            public void onResponseComplete(String response) {
                try {
                    JSONObject directions = new JSONObject(response);
                    JSONObject legs = directions
                            .getJSONArray("routes")
                            .getJSONObject(0)
                            .getJSONArray("legs")
                            .getJSONObject(0);
                    JSONArray steps = legs.getJSONArray("steps");

                    // Assume that it is 3x faster then walking when cycling, add duration
                    int duration = time +
                            legs.getJSONObject("duration").optInt("value") / 3;

                    for (int j = 0; j < steps.length(); j++) {
                        Map<String, Object> index = new HashMap<>();
                        // Get/Form html instructions
                        if (steps.getJSONObject(j).has("html_instructions")) {
                            String distance = steps
                                    .getJSONObject(j)
                                    .getJSONObject("distance")
                                    .getString("text");
                            index.put("distance", distance);
                            String html_instructions = steps
                                    .getJSONObject(j)
                                    .getString("html_instructions");
                            html_instructions = Html.fromHtml(html_instructions).toString();
                            html_instructions = html_instructions
                                    .replaceAll("(?m)^[ \t]*\r?\n", "")
                                    .replaceAll("開", "騎")
                                    .replaceAll("走", "騎");
                            index.put("html_instructions", html_instructions);
                        } /*else {
                            String html_instructions = steps
                                    .getJSONObject(0)
                                    .getString("html_instructions");
                            html_instructions = Html.fromHtml(html_instructions).toString();
                            html_instructions = html_instructions
                                    .replaceAll("(?m)^[ \t]*\r?\n", "")
                                    .replaceAll("開", "騎")
                                    .replaceAll("走", "騎");
                            index.put("html_instructions", html_instructions);
                        }*/
                        index.put("travel_mode", "BIKE");
                        // Get polyline
                        String points = steps
                                .getJSONObject(j)
                                .getJSONObject("polyline")
                                .getString("points");
                        ubike_polyline.add(points);
                        ubike_list.add(index);
                    }
                    walkingRequest3(url, duration);
                } catch (JSONException e) {
                    Log.v("JSON", "3");
                    errorMsg("Error", "unexpected response data");
                }
            }
        });
    }

    // Third request, mostly same as parsJSON()
    private void walkingRequest3(final String[] url, final int time) {
        new JSONParser().getJSONFromUrl(url[2],new responseListener() {
            @Override
            public void onResponseComplete(String response) {
                try {
                    JSONObject directions = new JSONObject(response);
                    JSONObject legs = directions
                            .getJSONArray("routes")
                            .getJSONObject(0)
                            .getJSONArray("legs")
                            .getJSONObject(0);
                    JSONArray steps = legs.getJSONArray("steps");

                    int duration = time +
                            legs.getJSONObject("duration").optInt("value");

                    for (int j = 0; j < steps.length(); j++) {
                        Map<String, Object> index = new HashMap<>();
                        // Get/Form html instructions
                        if (steps.getJSONObject(j).has("html_instructions")) {
                            String distance = steps
                                    .getJSONObject(j)
                                    .getJSONObject("distance")
                                    .getString("text");
                            index.put("distance", distance);
                            String html_instructions = steps
                                    .getJSONObject(j)
                                    .getString("html_instructions");
                            html_instructions = Html.fromHtml(html_instructions).toString();
                            html_instructions = html_instructions
                                    .replaceAll("(?m)^[ \t]*\r?\n", "")
                                    .replaceAll("開", "走");
                            index.put("html_instructions", html_instructions);
                        } /*else {
                            String html_instructions = steps
                                    .getJSONObject(0)
                                    .getString("html_instructions");
                            html_instructions = Html.fromHtml(html_instructions).toString();
                            html_instructions = html_instructions
                                    .replaceAll("(?m)^[ \t]*\r?\n", "")
                                    .replaceAll("開", "走");
                            index.put("html_instructions", html_instructions);
                        }*/
                        // Get travel mode
                        String travel_mode = steps
                                .getJSONObject(j)
                                .getString("travel_mode");
                        index.put("travel_mode", travel_mode);
                        // Get polyline
                        String points = steps
                                .getJSONObject(j)
                                .getJSONObject("polyline")
                                .getString("points");
                        ubike_polyline.add(points);
                        ubike_list.add(index);
                    }
                    // Pass duration in order to decide which traveling method is better
                    routeGen(duration);
                } catch (JSONException e) {
                    Log.v("JSON", "4");
                    errorMsg("Error", "unexpected response data");
                }
            }
        });
    }

    private void routeGen(int time) {
        // If riding ubike takes less time then current travel mode,
        // update the final_list with ubike instructions
        if (time < (Integer) steps_list.get(0).get("duration")) {
            final_list.addAll(ubike_list);
            final_polyline.addAll(ubike_polyline);
            // remove the original data of current travel mode
            for (int i = 0; i < (Integer) steps_list.get(0).get("count"); i++) {
                route_list.remove(0);
                route_polyline.remove(0);
            }
        } else {
            for (int i = 0; i < (Integer) steps_list.get(0).get("count"); i++) {
                final_list.add(route_list.get(0));
                final_polyline.add(route_polyline.get(0));
                route_list.remove(0);
                route_polyline.remove(0);
            }
        }
        // Remove data of this travel mode when finished updating final_list
        steps_list.remove(0);

        // If no more travel mode to optimize, prepare to show the result
        // Else do the function again until steps_list is empty
        if (steps_list.size() == 0){
            printList();
            return;
        }
        else {
            ubikePartOpt();
            return;
        }
    }

    // Show dialog when error occurs
    private void errorMsg(String title, String content) {
        new AlertDialog.Builder(this)
                .setTitle(title)
                .setMessage(content)
                .setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.cancel();
                        finish();
                    }
                })
                .setIcon(android.R.drawable.ic_dialog_alert)
                .show();
    }

    protected void printList() {
        /*
        for (Map.Entry entry : overall.entrySet()) {
            Log.v("DATA", entry.getValue().toString());
        }
        for (int i = 0; i < final_list.size(); i++) {
            for (Map.Entry entry : final_list.get(i).entrySet()) {
                Log.v("DATA", entry.getValue().toString());
            }
        }
        for (int i = 0; i < final_polyline.size(); i++) {
            Log.v("DATA", final_polyline.get(i));
        }*/
        // Reset flag when finished
        flag1 = false; flag2 = false;
        // Show adapter
        mListView.setAdapter(new MyAdapter());
        mListView.setVisibility(View.VISIBLE);
    }

}
