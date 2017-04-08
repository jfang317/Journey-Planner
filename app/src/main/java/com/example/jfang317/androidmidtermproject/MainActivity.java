package com.example.jfang317.androidmidtermproject;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.database.Cursor;
import android.database.SQLException;
import android.net.Uri;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.text.Html;
import android.util.Log;

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

    // Google direction API url & key
    String dir_api = "https://maps.googleapis.com/maps/api/directions/json?";
    private String key = "AIzaSyBtOZUrmoncqHCQBiwmU6FTvD1ULPKZNYc";

    // List of route data to display
    protected ArrayList<Map<String, Object>> route_list = new ArrayList<>(); // Raw data from Google direction api
    protected ArrayList<Map<String, Object>> steps_list = new ArrayList<>(); // Store data for specific travel mode
    protected ArrayList<Map<String, Object>> ubike_list = new ArrayList<>(); // Route for Ubike
    protected ArrayList<Map<String, Object>> final_list = new ArrayList<>(); // Optimized by adding ubike station database
    protected Map<String, Object> overall = new HashMap<>();
    protected ArrayList<String> route_polyline = new ArrayList<>();
    protected ArrayList<String> ubike_polyline = new ArrayList<>();
    protected ArrayList<String> final_polyline = new ArrayList<>();

    Cursor c = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        sendRequest();
    }

    private void sendRequest() {
        // Setup request parameters
        //String origin = "24.784931,120.991764";
        //String origin = "台南車站";
        String origin = "24.800305,120.994376";
        //String destination = "24.800164,120.979457";
        String destination = "300台灣新竹市東區大學路68號";
        //String destination = "24.789403,120.999980";
        String travel_mode = "transit";
        String departure_time = "now";
        String language = "zh-TW";

        // Build request url
        Uri.Builder builder = Uri.parse(dir_api).buildUpon();
        builder.appendQueryParameter("origin", origin)
                .appendQueryParameter("destination", destination)
                .appendQueryParameter("mode", travel_mode)
                .appendQueryParameter("departure_time", departure_time)
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

            // Get and store start/end location data & total duration,
            LatLng start_location = new LatLng(legs.getJSONObject("start_location").optDouble("lat"),
                    legs.getJSONObject("start_location").optDouble("lng"));
            LatLng end_location = new LatLng(legs.getJSONObject("end_location").optDouble("lat"),
                    legs.getJSONObject("end_location").optDouble("lng"));
            String start_address = legs.getString("start_address");
            String end_address = legs.getString("end_address");
            String duration = legs.getJSONObject("duration").getString("text");

            // store summary data
            overall.put("start_address", start_address);
            overall.put("end_address", end_address);
            overall.put("duration", duration);

            Log.v("JSON", start_location.toString());
            Log.v("JSON", end_location.toString());
            Log.v("JSON", start_address);
            Log.v("JSON", end_address);

            // Handling different responding cases of JSON
            for (int i = 0; i < steps.length(); i++) {
                Map<String, Object> steps_info = new HashMap<>();

                int time = steps.getJSONObject(i).getJSONObject("duration").optInt("value");

                LatLng start = new LatLng(
                        steps.getJSONObject(i).getJSONObject("start_location").optDouble("lat"),
                        steps.getJSONObject(i).getJSONObject("start_location").optDouble("lng"));
                LatLng end = new LatLng(
                        steps.getJSONObject(i).getJSONObject("end_location").optDouble("lat"),
                        steps.getJSONObject(i).getJSONObject("end_location").optDouble("lng"));

                int count;
                if (steps.getJSONObject(i).has("steps")) {
                    count = steps.getJSONObject(i).getJSONArray("steps").length();
                } else count = 1;

                steps_info.put("duration", time);
                steps_info.put("start_location", start);
                steps_info.put("end_location", end);
                steps_info.put("count", count);
                steps_list.add(steps_info);

                if (steps.getJSONObject(i).getString("travel_mode").equals("WALKING")) {
                    if (steps.getJSONObject(i).has("steps")) {
                        JSONArray step_index = steps.getJSONObject(i).getJSONArray("steps");
                        for (int j = 0; j < step_index.length(); j++) {
                            Map<String, Object> index = new HashMap<>();

                            String distance = step_index
                                    .getJSONObject(j)
                                    .getJSONObject("distance")
                                    .getString("text");
                            // Get/Form html instructions
                            if (step_index.getJSONObject(j).has("html_instructions")) {
                                String html_instructions = step_index
                                        .getJSONObject(j)
                                        .getString("html_instructions");
                                html_instructions = Html.fromHtml(html_instructions).toString();
                                html_instructions = html_instructions
                                        .replaceAll("(?m)^[ \t]*\r?\n", "")
                                        .replaceAll("開", "走");
                                html_instructions = html_instructions
                                        .concat(System.getProperty("line.separator"))
                                        .concat(distance);
                                index.put("html_instructions", html_instructions);
                            } else {
                                String html_instructions = steps
                                        .getJSONObject(i)
                                        .getString("html_instructions");
                                html_instructions = html_instructions
                                        .replaceAll("(?m)^[ \t]*\r?\n", "")
                                        .replaceAll("開", "走");
                                html_instructions = Html.fromHtml(html_instructions).toString();
                                index.put("html_instructions", html_instructions);
                            }

                            // Get travel mode
                            String travel_mode = step_index
                                    .getJSONObject(j)
                                    .getString("travel_mode");
                            index.put("travel_mode", travel_mode);

                            // Get polyline
                            String points = step_index
                                    .getJSONObject(j)
                                    .getJSONObject("polyline")
                                    .getString("points");
                            route_polyline.add(points);

                            route_list.add(index);
                            //Log.v("LOOP", Integer.toString(j));
                        }
                    } else {
                        Map<String, Object> index = new HashMap<>();

                        String distance = steps
                                .getJSONObject(i)
                                .getJSONObject("distance")
                                .getString("text");

                        // Get html instructions
                        String html_instructions = steps
                                .getJSONObject(i)
                                .getString("html_instructions");
                        html_instructions = Html.fromHtml(html_instructions).toString();
                        html_instructions = html_instructions
                                .concat(System.getProperty("line.separator"))
                                .concat(distance);
                        html_instructions = html_instructions
                                .replaceAll("(?m)^[ \t]*\r?\n", "")
                                .replaceAll("開", "走");
                        index.put("html_instructions", html_instructions);

                        // Get travel mode
                        String travel_mode = steps.getJSONObject(i).getString("travel_mode");
                        index.put("travel_mode", travel_mode);

                        // Get polyline
                        String points = steps
                                .getJSONObject(i)
                                .getJSONObject("polyline")
                                .getString("points");
                        route_polyline.add(points);

                        route_list.add(index);
                    }
                } else if (steps.getJSONObject(i).getString("travel_mode").equals("TRANSIT")) {
                    if (steps.getJSONObject(i).has("steps")) {
                        JSONArray step_index = steps.getJSONObject(i).getJSONArray("steps");
                        for (int j = 0; j < step_index.length(); j++) {
                            Map<String, Object> index = new HashMap<>();
                            JSONObject transit_details = step_index
                                    .getJSONObject(j)
                                    .getJSONObject("transit_details");

                            String distance = steps
                                    .getJSONObject(j)
                                    .getJSONObject("distance")
                                    .getString("text");

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
                            html_instructions = html_instructions
                                    .concat(System.getProperty("line.separator"))
                                    .concat(distance);
                            index.put("html_instructions", html_instructions);

                            String travel_mode = transit_details
                                    .getJSONObject("line")
                                    .getJSONObject("vehicle")
                                    .getString("type");
                            index.put("travel_mode", travel_mode);

                            String points = step_index
                                    .getJSONObject(j)
                                    .getJSONObject("polyline")
                                    .getString("points");
                            route_polyline.add(points);

                            route_list.add(index);
                        }
                    } else {
                        Map<String, Object> index = new HashMap<>();
                        JSONObject transit_details = steps
                                .getJSONObject(i)
                                .getJSONObject("transit_details");

                        String distance = steps
                                .getJSONObject(i)
                                .getJSONObject("distance")
                                .getString("text");

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
                        html_instructions = html_instructions
                                .concat(System.getProperty("line.separator"))
                                .concat(distance);
                        index.put("html_instructions", html_instructions);

                        String travel_mode = transit_details
                                .getJSONObject("line")
                                .getJSONObject("vehicle")
                                .getString("type");
                        index.put("travel_mode", travel_mode);

                        String points = steps
                                .getJSONObject(i)
                                .getJSONObject("polyline")
                                .getString("points");
                        route_polyline.add(points);

                        route_list.add(index);
                    }
                }
            }
            ubikeOpt();
        } catch (JSONException e) {
            try {
                String start_id = new JSONObject(data)
                        .getJSONArray("geocoded_waypoints")
                        .getJSONObject(0)
                        .getString("place_id");
                String end_id = new JSONObject(data)
                        .getJSONArray("geocoded_waypoints")
                        .getJSONObject(1)
                        .getString("place_id");
                zeroResults(start_id, end_id);
            } catch (JSONException j) {
                Log.v("JSON", "1");
                errorMsg("Error", "unexpected response data");
            }
        }

    }

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


    private String getUbikeGps(LatLng location) {
        DatabaseHelper myDbHelper = new DatabaseHelper(MainActivity.this);
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
        c = myDbHelper.query("YoubileInfo", null, null, null, null, null, null);
        String resultLongitude = null, resultLatitude = null;
        float minDist = 10000000;
        if (c.moveToFirst()) {
            do {
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

    private void ubikeOpt() {
        LatLng start_location = (LatLng) steps_list.get(0).get("start_location");
        LatLng end_location = (LatLng) steps_list.get(0).get("end_location");

        String start = Double.toString(start_location.latitude)
                + "," + Double.toString(start_location.longitude);
        String end = Double.toString(end_location.latitude)
                + "," + Double.toString(end_location.longitude);

        String ubike_start = getUbikeGps(start_location);
        String ubike_end = getUbikeGps(end_location);

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

        walkingRequest1(url);

        //Log.v("DATA", Double.toString(start.longitude));
        //if (steps_list.size() == 0) printList();
    }

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

                    int duration = legs.getJSONObject("duration").optInt("value");

                    for (int j = 0; j < steps.length(); j++) {
                        Map<String, Object> index = new HashMap<>();

                        // Get/Form html instructions
                        if (steps.getJSONObject(j).has("html_instructions")) {
                            String distance = steps
                                    .getJSONObject(j)
                                    .getJSONObject("distance")
                                    .getString("text");

                            String html_instructions = steps
                                    .getJSONObject(j)
                                    .getString("html_instructions");
                            html_instructions = Html.fromHtml(html_instructions).toString();
                            html_instructions = html_instructions
                                    .concat(System.getProperty("line.separator"))
                                    .concat(distance);
                            html_instructions = html_instructions
                                    .replaceAll("(?m)^[ \t]*\r?\n", "")
                                    .replaceAll("開", "走");
                            index.put("html_instructions", html_instructions);
                        } else {
                            String html_instructions = steps
                                    .getJSONObject(0)
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
                    walkingRequest2(url, duration);
                } catch (JSONException e) {
                    Log.v("JSON", "2");
                    errorMsg("Error", "unexpected response data");
                }
            }
        });
    }

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

                    int duration = time +
                            legs.getJSONObject("duration").optInt("value") / 3;
                    Log.v("YO", Integer.toString(duration));

                    for (int j = 0; j < steps.length(); j++) {
                        Map<String, Object> index = new HashMap<>();
                        // Get/Form html instructions
                        if (steps.getJSONObject(j).has("html_instructions")) {
                            String distance = steps
                                    .getJSONObject(j)
                                    .getJSONObject("distance")
                                    .getString("text");
                            String html_instructions = steps
                                    .getJSONObject(j)
                                    .getString("html_instructions");
                            html_instructions = Html.fromHtml(html_instructions).toString();
                            html_instructions = html_instructions
                                    .concat(System.getProperty("line.separator"))
                                    .concat(distance);
                            html_instructions = html_instructions
                                    .replaceAll("(?m)^[ \t]*\r?\n", "")
                                    .replaceAll("開", "騎")
                                    .replaceAll("走", "騎");
                            index.put("html_instructions", html_instructions);
                        } else {
                            String html_instructions = steps
                                    .getJSONObject(0)
                                    .getString("html_instructions");
                            html_instructions = Html.fromHtml(html_instructions).toString();
                            html_instructions = html_instructions
                                    .replaceAll("(?m)^[ \t]*\r?\n", "")
                                    .replaceAll("開", "騎")
                                    .replaceAll("走", "騎");
                            index.put("html_instructions", html_instructions);
                        }
                        index.put("travel_mode", "BIKE");
                        // Get polyline
                        String points = steps
                                .getJSONObject(j)
                                .getJSONObject("polyline")
                                .getString("points");
                        ubike_polyline.add(points);
                        ubike_list.add(index);
                        //Log.v("LOOP", Integer.toString(j));
                    }
                    walkingRequest3(url, duration);
                } catch (JSONException e) {
                    Log.v("JSON", "3");
                    errorMsg("Error", "unexpected response data");
                }
            }
        });
    }

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
                            String html_instructions = steps
                                    .getJSONObject(j)
                                    .getString("html_instructions");
                            html_instructions = Html.fromHtml(html_instructions).toString();
                            html_instructions = html_instructions
                                    .concat(System.getProperty("line.separator"))
                                    .concat(distance);
                            html_instructions = html_instructions
                                    .replaceAll("(?m)^[ \t]*\r?\n", "")
                                    .replaceAll("開", "走");
                            index.put("html_instructions", html_instructions);
                        } else {
                            String html_instructions = steps
                                    .getJSONObject(0)
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
                        //Log.v("LOOP", Integer.toString(j));
                    }
                    routeGen(duration);
                } catch (JSONException e) {
                    Log.v("JSON", "4");
                    errorMsg("Error", "unexpected response data");
                }
            }
        });
    }

    private void routeGen(int time) {
        if (time < (Integer) steps_list.get(0).get("duration")) {
            final_list.addAll(ubike_list);
            final_polyline.addAll(ubike_polyline);
        } else {
            for (int i = 0; i < (Integer) steps_list.get(0).get("count"); i++) {
                final_list.add(route_list.get(0));
                final_polyline.add(route_polyline.get(0));
                route_list.remove(0);
                route_polyline.remove(0);
            }
        }
        steps_list.remove(0);
        if (steps_list.size() == 0) printList();
        else ubikeOpt();
    }

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
        //Log.v("DATA", Integer.toString(route_list.size()));
        //Log.v("DATA", "HEY");
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
        }
    }

}
