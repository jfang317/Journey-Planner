package com.example.jfang317.androidmidtermproject;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.net.Uri;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.text.Html;
import android.util.Log;

import com.google.android.gms.maps.model.LatLng;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;


public class MainActivity extends AppCompatActivity {

    // Google direction API url & key
    String dir_api = "https://maps.googleapis.com/maps/api/directions/json?";
    String geo_api = "https://maps.googleapis.com/maps/api/geocode/json?";
    String key = "AIzaSyBtOZUrmoncqHCQBiwmU6FTvD1ULPKZNYc";

    // List of route data to display
    ArrayList<Map<String, Object>> route_list = new ArrayList<>();
    Map<String, Object> overall = new HashMap<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        sendRequest();

    }

    private void sendRequest() {
        // Setup request parameters
        //String origin = "24.784931,120.991764";
        String origin = "台南車站";
        //String destination = "24.800164,120.979457";
        String destination = "台南市永康區三民街18巷";
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

            // Get start/end location data
            LatLng start_location = new LatLng(legs.getJSONObject("start_location").optDouble("lat"),
                    legs.getJSONObject("start_location").optDouble("lng"));
            LatLng end_location = new LatLng(legs.getJSONObject("end_location").optDouble("lat"),
                    legs.getJSONObject("end_location").optDouble("lng"));
            String start_address = legs.getString("start_address");
            String end_address = legs.getString("end_address");
            String duration = legs.getJSONObject("duration").getString("text");

            overall.put("start_address", start_address);
            overall.put("end_address", end_address);
            overall.put("duration", duration);

            Log.v("JSON", start_location.toString());
            Log.v("JSON", end_location.toString());
            Log.v("JSON", start_address);
            Log.v("JSON", end_address);

            if ((steps.length() == 1) &&
                    (steps.getJSONObject(0).getString("travel_mode").equals("WALKING"))) {
                // Ubike all the way QQ
            }
            for (int i = 0; i < steps.length(); i++) {
                if (steps.getJSONObject(i).getString("travel_mode").equals("WALKING")) {
                    if (steps.getJSONObject(i).has("steps")) {
                        JSONArray step_index = steps.getJSONObject(i).getJSONArray("steps");
                        for (int j = 0; j < step_index.length(); j++) {
                            Map<String, Object> index = new HashMap<>();

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

                            String travel_mode = step_index
                                    .getJSONObject(j)
                                    .getString("travel_mode");
                            index.put("travel_mode", travel_mode);

                            route_list.add(index);
                            Log.v("LOOP", Integer.toString(j));
                        }
                    } else {
                        Map<String, Object> index = new HashMap<>();

                        String html_instructions = steps
                                .getJSONObject(i)
                                .getString("html_instructions");
                        html_instructions = Html.fromHtml(html_instructions).toString();
                        html_instructions = html_instructions
                                .replaceAll("(?m)^[ \t]*\r?\n", "")
                                .replaceAll("開", "走");
                        index.put("html_instructions", html_instructions);

                        String travel_mode = steps.getJSONObject(i).getString("travel_mode");
                        index.put("travel_mode", travel_mode);

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
                            StringBuffer instructions = new StringBuffer();
                            instructions.append(departure_time)
                                    .append(short_name)
                                    .append(System.getProperty("line.separator"))
                                    .append(departure_stop)
                                    .append("到")
                                    .append(arrival_stop);
                            String html_instructions = instructions.toString();
                            index.put("html_instructions", html_instructions);

                            String travel_mode = transit_details
                                    .getJSONObject("line")
                                    .getJSONObject("vehicle")
                                    .getString("type");
                            index.put("travel_mode", travel_mode);

                            route_list.add(index);
                        }
                    } else {
                        Map<String, Object> index = new HashMap<>();
                        JSONObject transit_details = steps
                                .getJSONObject(i)
                                .getJSONObject("transit_details");

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
                        StringBuffer instructions = new StringBuffer();
                        instructions.append(departure_time)
                                .append(short_name)
                                .append(System.getProperty("line.separator"))
                                .append(departure_stop)
                                .append("到")
                                .append(arrival_stop);
                        String html_instructions = instructions.toString();
                        index.put("html_instructions", html_instructions);

                        String travel_mode = transit_details
                                .getJSONObject("line")
                                .getJSONObject("vehicle")
                                .getString("type");
                        index.put("travel_mode", travel_mode);

                        route_list.add(index);
                    }
                }
            }
            printList();
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
        String language = "zh-TW";
        Uri.Builder builder;
        builder = Uri.parse(geo_api).buildUpon();
        builder.appendQueryParameter("place_id", start)
                .appendQueryParameter("language", language)
                .appendQueryParameter("key", key);
        String start_url = builder.build().toString();
        Log.d("URL",start_url);
        new JSONParser().getJSONFromUrl(start_url,new responseListener() {
            @Override
            public void onResponseComplete(String response) {
                try {
                    LatLng start_location = new LatLng(
                            new JSONObject(response)
                                    .getJSONArray("results")
                                    .getJSONObject(0)
                                    .getJSONObject("geometry")
                                    .getJSONObject("location")
                                    .optDouble("lat"),
                            new JSONObject(response)
                                    .getJSONArray("results")
                                    .getJSONObject(0)
                                    .getJSONObject("geometry")
                                    .getJSONObject("location")
                                    .optDouble("lng")
                    );
                    String start_address = new JSONObject(response)
                            .getJSONArray("results")
                            .getJSONObject(0)
                            .getString("formatted_address");
                    Log.d("start_address", start_address);
                    overall.put("start_address", start_address);
                } catch (JSONException k) {
                    Log.v("JSON", "2");
                    errorMsg("Error", "unexpected response data");
                }
            }
        });
        builder = Uri.parse(geo_api).buildUpon();
        builder.appendQueryParameter("place_id", end)
                .appendQueryParameter("language", language)
                .appendQueryParameter("key", key);
        String end_url = builder.build().toString();
        Log.d("URL",end_url);
        new JSONParser().getJSONFromUrl(end_url,new responseListener() {
            @Override
            public void onResponseComplete(String response) {
                try {
                    LatLng end_location = new LatLng(
                            new JSONObject(response)
                                    .getJSONArray("results")
                                    .getJSONObject(0)
                                    .getJSONObject("geometry")
                                    .getJSONObject("location")
                                    .optDouble("lat"),
                            new JSONObject(response)
                                    .getJSONArray("results")
                                    .getJSONObject(0)
                                    .getJSONObject("geometry")
                                    .getJSONObject("location")
                                    .optDouble("lng")
                    );
                    String end_address = new JSONObject(response)
                            .getJSONArray("results")
                            .getJSONObject(0)
                            .getString("formatted_address");
                    Log.d("end_address", end_address);
                    overall.put("end_address", end_address);
                    printList();
                } catch (JSONException k) {
                    Log.v("JSON", "3");
                    errorMsg("Error","unexpected response data");
                }
            }
        });

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
        for (int i = 0; i < route_list.size(); i++) {
            for (Map.Entry entry : route_list.get(i).entrySet()) {
                Log.v("DATA", entry.getValue().toString());
            }
        }
    }














}
