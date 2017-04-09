package com.example.jfang317.androidmidtermproject;

import android.Manifest;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.database.SQLException;
import android.location.Criteria;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.text.Html;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.maps.model.LatLng;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import static android.location.Location.distanceBetween;


public class MainActivity extends AppCompatActivity
        implements LocationListener{

    // layout components
    private Button btnGetLocation1 = null;
    private Button btnGetLocation2 = null;
    private Button btnSearch = null;
    private Button btnshowmap= null;
    private EditText editLocation1 = null;
    private EditText editLocation2 = null;

    private static final int REQUEST_FINE_LOCATION_PERMISSION = 102;
    private LocationManager lms;
    private Location location;
    private LocationListener listener;
    private String bestProvider = LocationManager.NETWORK_PROVIDER;
    private Boolean getService = false;

    private String longitude_txt;
    private String latitude_txt;
    private String start_txt;
    private String end_txt;

    private ListView mListView;

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

        editLocation1 = (EditText) findViewById(R.id.editText1);
        btnGetLocation1 = (Button) findViewById(R.id.button);
        editLocation2 = (EditText) findViewById(R.id.editText2);
        btnGetLocation2 = (Button) findViewById(R.id.button2);
        btnshowmap = (Button) findViewById(R.id.button4);
        mListView = (ListView) findViewById(R.id.list);
        mListView.setVisibility(View.INVISIBLE);

    }

    public void search(View view) {
        route_list.clear();
        steps_list.clear();
        ubike_list.clear();
        final_list.clear();
        route_polyline.clear();
        ubike_polyline.clear();
        final_polyline.clear();
        overall.clear();
        start_txt = editLocation1.getText().toString();
        end_txt = editLocation2.getText().toString();

        sendRequest(start_txt, end_txt);

    }

    public void buttonClick1(View view) {
        LocationManager status = (LocationManager) (this.getSystemService(Context.LOCATION_SERVICE));
        if (status.isProviderEnabled(LocationManager.GPS_PROVIDER) || status.isProviderEnabled(LocationManager.NETWORK_PROVIDER)) {
            //如果GPS或網路定位開啟，呼叫locationServiceInitial()更新位置
            locationServiceInitial();
        } else {
            Toast.makeText(this, "請開啟定位服務", Toast.LENGTH_LONG).show();
            getService = true; //確認開啟定位服務
            startActivity(new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS)); //開啟設定頁面
        }
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            // TODO: Consider calling
            //    ActivityCompat#requestPermissions
            // here to request the missing permissions, and then overriding
            //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
            //                                          int[] grantResults)
            // to handle the case where the user grants the permission. See the documentation
            // for ActivityCompat#requestPermissions for more details.
            return;
        }
        location = lms.getLastKnownLocation(bestProvider);
        if (location == null) location = lms.getLastKnownLocation("network");
        if (location == null) location = lms.getLastKnownLocation("gps");

        getLocation(location);
        editLocation1.setText(latitude_txt+","+longitude_txt);
    }

    public void showmap(View view){
        Intent intent = new Intent(this, MapsActivity.class);
        intent.putStringArrayListExtra("polyline", final_polyline);
        Bundle bundle = new Bundle();
        LatLng start = (LatLng) overall.get("start_location");
        LatLng end = (LatLng) overall.get("end_location");
        bundle.putDouble("Lat", (start.latitude + end.latitude) / 2);
        bundle.putDouble("Lng", (start.longitude + end.longitude) / 2);
        startActivity(intent);
    }

    public void buttonClick2(View view) {
        LocationManager status = (LocationManager) (this.getSystemService(Context.LOCATION_SERVICE));
        if (status.isProviderEnabled(LocationManager.GPS_PROVIDER) || status.isProviderEnabled(LocationManager.NETWORK_PROVIDER)) {
            //如果GPS或網路定位開啟，呼叫locationServiceInitial()更新位置
            locationServiceInitial();
        } else {
            Toast.makeText(this, "請開啟定位服務", Toast.LENGTH_LONG).show();
            getService = true; //確認開啟定位服務
            startActivity(new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS)); //開啟設定頁面
        }
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            // TODO: Consider calling
            //    ActivityCompat#requestPermissions
            // here to request the missing permissions, and then overriding
            //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
            //                                          int[] grantResults)
            // to handle the case where the user grants the permission. See the documentation
            // for ActivityCompat#requestPermissions for more details.
            return;
        }
        location = lms.getLastKnownLocation(bestProvider);
        if(location == null)location = lms.getLastKnownLocation("network");
        if(location == null)location = lms.getLastKnownLocation("gps");

        getLocation(location);
        editLocation2.setText(latitude_txt+","+longitude_txt);
    }

    private void locationServiceInitial() {
        lms = (LocationManager) getSystemService(LOCATION_SERVICE); //取得系統定位服務
        Criteria criteria = new Criteria();  //資訊提供者選取標準
        bestProvider = lms.getBestProvider(criteria, true);    //選擇精準度最高的提供者
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            // TODO: Consider calling
            //    ActivityCompat#requestPermissions
            // here to request the missing permissions, and then overriding
            //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
            //                                          int[] grantResults)
            // to handle the case where the user grants the permission. See the documentation
            // for ActivityCompat#requestPermissions for more details.
            requestLocationPermission();
            //return;
        }
        location = lms.getLastKnownLocation(bestProvider);
        if(location == null)location = lms.getLastKnownLocation("network");
        if(location == null)location = lms.getLastKnownLocation("gps");

        getLocation(location);
    }

    private void requestLocationPermission() {
        // 如果裝置版本是6.0（包含）以上
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            // 取得授權狀態，參數是請求授權的名稱
            int hasPermission = checkSelfPermission(
                    Manifest.permission.ACCESS_FINE_LOCATION);

            // 如果未授權
            if (hasPermission != PackageManager.PERMISSION_GRANTED) {
                // 請求授權
                //     第一個參數是請求授權的名稱
                //     第二個參數是請求代碼
                requestPermissions(
                        new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                        REQUEST_FINE_LOCATION_PERMISSION);
            }
            else {
                // 啟動地圖與定位元件

            }
        }
    }

    private void getLocation(Location location) {
        if(location != null) {
            Double longitude = location.getLongitude();   //取得經度
            Double latitude = location.getLatitude();     //取得緯度
            longitude_txt = String.valueOf(longitude);
            latitude_txt = String.valueOf(latitude);
        }
        else {
            Toast.makeText(this, "無法定位座標", Toast.LENGTH_LONG).show();
        }
    }

    @Override
    public void onLocationChanged(Location location) {  //當地點改變時
        // TODO 自動產生的方法 Stub
        getLocation(location);
    }

    @Override
    public void onProviderDisabled(String arg0) {//當GPS或網路定位功能關閉時
        // TODO 自動產生的方法 Stub
        //Toast.makeText(this, "請開啟gps或3G網路", Toast.LENGTH_LONG).show();
    }

    @Override
    public void onProviderEnabled(String arg0) { //當GPS或網路定位功能開啟
        // TODO 自動產生的方法 Stub
    }

    @Override
    public void onStatusChanged(String arg0, int arg1, Bundle arg2) { //定位狀態改變
        // TODO 自動產生的方法 Stub
    }

    @Override
    protected void onResume() {
        // TODO Auto-generated method stub
        super.onResume();
        if (getService) {
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                // TODO: Consider calling
                //    ActivityCompat#requestPermissions
                // here to request the missing permissions, and then overriding
                //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
                //                                          int[] grantResults)
                // to handle the case where the user grants the permission. See the documentation
                // for ActivityCompat#requestPermissions for more details.
                requestLocationPermission();
                return;
            }
            lms.requestLocationUpdates(bestProvider, 1000, 1, this);
            //服務提供者、更新頻率60000毫秒=1分鐘、最短距離、地點改變時呼叫物件
        }
    }

    @Override
    protected void onPause() {
        // TODO Auto-generated method stub
        super.onPause();
        if(getService) {
            //lms.removeUpdates(this);	//離開頁面時停止更新
        }
    }

    protected void onDestroy() {
        lms.removeUpdates(listener);
        lms.setTestProviderEnabled(bestProvider, false);
        super.onDestroy();
    }

    private class MyAdapter extends BaseAdapter {

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

    private void sendRequest(String origin, String destination) {
        // Setup request parameters
        //String origin = "24.784931,120.991764";
        //String origin = "台南車站";
        origin = "300新竹市東區新源街138號";
        //String destination = "24.800164,120.979457";
        //tring destination = "300台灣新竹市東區大學路68號";
        destination = "300新竹市東區大學路200號";
        String travel_mode = "transit";
        //String departure_time = "now";
        String departure_time = "1491771600";
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
            overall.put("start_location", start_location);
            overall.put("end_location", end_location);

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
                            index.put("distance", distance);
                            // Get/Form html instructions
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
                        }
                    } else {
                        Map<String, Object> index = new HashMap<>();

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
                            index.put("distance", distance);

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
                        index.put("distance", distance);

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
            ubikePartOpt();
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

    private void ubikeFullOpt() {

    }

    private void ubikePartOpt() {
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
                            index.put("distance", distance);

                            String html_instructions = steps
                                    .getJSONObject(j)
                                    .getString("html_instructions");
                            html_instructions = Html.fromHtml(html_instructions).toString();
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
                            index.put("distance", distance);
                            String html_instructions = steps
                                    .getJSONObject(j)
                                    .getString("html_instructions");
                            html_instructions = Html.fromHtml(html_instructions).toString();
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
            }
        }
        for (int i = 0; i < (Integer) steps_list.get(0).get("count"); i++) {
            route_list.remove(0);
            route_polyline.remove(0);
        }
        steps_list.remove(0);
        for (int i = 0; i < steps_list.size(); i++) {
            for (Map.Entry entry : steps_list.get(i).entrySet()) {
                Log.v("DATA", entry.getValue().toString());
            }
        }
        Log.v("DATA", "~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~");
        if (steps_list.size() == 0) printList();
        else ubikePartOpt();
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
        mListView.setAdapter(new MyAdapter());
        mListView.setVisibility(View.VISIBLE);
    }

}
