package com.project.android.test;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.location.Location;
import android.net.Uri;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.view.animation.AlphaAnimation;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import com.google.gson.Gson;
import com.mapbox.geojson.Point;
import com.mapbox.mapboxsdk.Mapbox;

import com.mapbox.mapboxsdk.annotations.MarkerOptions;
import com.mapbox.mapboxsdk.geometry.LatLng;
import com.mapbox.mapboxsdk.maps.MapView;
import com.mapbox.mapboxsdk.maps.MapboxMap;
import com.mapbox.mapboxsdk.maps.OnMapReadyCallback;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.ExecutionException;

//import com.google.android.gms.location.LocationListener;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

//import com.mapbox.mapboxandroiddemo.R;
//import com.mapbox.turf.TurfJoins; ???



/**

 * Display an indoor map of a building with toggles to switch between floor levels

 */

public class MainActivity extends AppCompatActivity {

   // private GeoJsonSource indoorBuildingSource;
    private List<Point> boundingBox;
    private List<List<Point>> boundingBoxList;
    private View levelButtons;
    private MapView mapView;
    private MapboxMap map;
    private MarkerOptions mMarker;
    private LatLng mLatLng;
    WifiManager wifi;
    private  Button startNavigation;
    private Button btnGetLocation;
    private EditText dest;
    final String[] destinationMap = {""};
    List<ScanResult> results;
    int size = 0;
    //private ArrayList<String> arraylist = new ArrayList<>();
    String ITEM_KEY = "key";

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);


        // Mapbox access token is configured here. This needs to be called either in your application
        // object or in the same activity which contains the mapview.
        Mapbox.getInstance(this, "pk.eyJ1IjoibHZ5YXcxMjI1IiwiYSI6ImNqaTdkZWFtYzA5YXYza3F1ZnJ3cXdxaWwifQ.lNkrhdsRg3l98fr1dXRsUw");
        // This contains the MapView in XML and needs to be called after the access token is configured.
        setContentView(R.layout.activity_main);
        mapView = findViewById(R.id.mapView);
        mapView.onCreate(savedInstanceState);

        startNavigation = findViewById(R.id.start_navigation);
        btnGetLocation = findViewById(R.id.get_location);
        wifi = (WifiManager) getApplicationContext().getSystemService(Context.WIFI_SERVICE);
        if (wifi.isWifiEnabled() == false)
        {
            Toast.makeText(getApplicationContext(), "wifi is disabled..making it enabled", Toast.LENGTH_LONG).show();
            wifi.setWifiEnabled(true);
        }
        registerReceiver(new BroadcastReceiver()
        {
            @Override
            public void onReceive(Context c, Intent intent)
            {

                results = wifi.getScanResults();
                size = results.size();
            }
        }, new IntentFilter(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION));


        startNavigation.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
               getDestination_Route destination = new getDestination_Route();
               dest = findViewById(R.id.txtdestination);


              destination.execute(dest.getText().toString()).toString();



            }
        });

        btnGetLocation.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                String routerDetails = getRouters();

                //Begin from here
                FetchPredictedLocation predictedLocation = new FetchPredictedLocation();
                predictedLocation.execute(routerDetails);



            }
        });



        mapView.getMapAsync(new OnMapReadyCallback() {

            @Override
            public void onMapReady(MapboxMap mapboxMap) {
                map = mapboxMap;
                levelButtons = findViewById(R.id.floor_level_buttons);
                boundingBox = new ArrayList<>();
                boundingBox.add(Point.fromLngLat(53.309460, -6.22470));
                boundingBox.add(Point.fromLngLat(53.309460, -6.22327));
                boundingBox.add(Point.fromLngLat(53.309150, -6.22327));
                boundingBox.add(Point.fromLngLat(53.309150, -6.22470));

                boundingBoxList = new ArrayList<>();
                boundingBoxList.add(boundingBox);
                mapboxMap.addOnCameraMoveListener(new MapboxMap.OnCameraMoveListener() {

                    @Override
                    public void onCameraMove() {
                        if (mapboxMap.getCameraPosition().zoom > 16) {
                                if (levelButtons.getVisibility() != View.VISIBLE) {
                                    showLevelButton();
                                }
                            else {
                                if (levelButtons.getVisibility() == View.VISIBLE) {
                                    hideLevelButton();
                                }
                            }

                        } else if (levelButtons.getVisibility() == View.VISIBLE) {
                            hideLevelButton();
                        }
                    }
                });
               //there is no geojsonsouce we use stylr url replace it.
               // indoorBuildingSource = new GeoJsonSource("indoor-building", loadJsonFromAsset("white_house_lvl_0.geojson"));
               // mapboxMap.addSource(indoorBuildingSource);
                // show the ground floor  since we know zoom levels in range
                map.setStyleUrl("mapbox://styles/lvyaw1225/cji7dfzx01e8v2sqygbr4xrto");
                //addMarker();


            }
        });
        Button buttonSecondLevel = findViewById(R.id.second_level_button);
        buttonSecondLevel.setOnClickListener( new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                map.setStyleUrl("mapbox://styles/lvyaw1225/cjisvh0ft2jyj2rp39losekqd");
            }
        });
        Button buttonFirstLevel = findViewById(R.id.first_level_button);
        buttonFirstLevel.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                map.setStyleUrl("mapbox://styles/lvyaw1225/cjisvanza56tv2rqo11ij53qn");
            }
        });

        Button buttonGroundLevel = findViewById(R.id.ground_level_button);
        buttonGroundLevel.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                map.setStyleUrl("mapbox://styles/lvyaw1225/cji7dfzx01e8v2sqygbr4xrto");
            }
        });

       // arraylist = getRouters();


    }

    public class getDestination_Route extends AsyncTask<String, Void, String>{

        @Override
        protected String doInBackground(String... params) {
            HttpURLConnection urlConnection = null;
            BufferedReader reader = null;
            String Url = "https://search-indoor-navigation-r2fy6yafkgybvrrhsgpcdev62e.us-east-2.es.amazonaws.com/fingerprint/routers/_search?q="+params[0]+"C&size=1";

            String destinationLocation = "";
            Uri uri = Uri.parse(Url).buildUpon().build();
            try {
                URL url = new URL(Url);
                //create and open request to Amazon API
                urlConnection = (HttpURLConnection) url.openConnection();
                urlConnection.setRequestMethod("GET");
                urlConnection.connect();

                InputStream inputStream = urlConnection.getInputStream();
                destinationLocation = readStream(inputStream);
                try {
                    JSONObject result = new JSONObject(destinationLocation);
                    destinationLocation = "";
                    String lat = result.getJSONObject("hits").getJSONArray("hits").getJSONObject(0).getJSONObject("_source").get("latitude1").toString();
                    String lon = result.getJSONObject("hits").getJSONArray("hits").getJSONObject(0).getJSONObject("_source").get("longitude1").toString();

                    destinationLocation = lat + "|" + lon;



                } catch (JSONException e) {
                    e.printStackTrace();
                }


            } catch (IOException e) {
                e.printStackTrace();
                return null;
            }



            return destinationLocation;
        }

        @Override
        protected void onPostExecute(String result) {

            destinationMap[0] = result;
            String[] position = destinationMap[0].split("\\|");
            double lat = Double.parseDouble(position[0]);
            double lon = Double.parseDouble(position[1]);
            addMarker(lat, lon);


        }

    }



    private void addMarker(double lat, double lon) {
        mLatLng = new LatLng(lat,lon);
        mMarker = new MarkerOptions()
                .position(mLatLng)
                .title("Location")
                .snippet("Welcome to you");
        map.addMarker(mMarker);
    }



    @Override
    protected void onResume() {
        super.onResume();
        mapView.onResume();
    }



    @Override
    protected void onStart() {
        super.onStart();
        mapView.onStart();
    }



    @Override
    protected void onStop() {
        super.onStop();
        mapView.onStop();
    }



    @Override
    protected void onPause() {
        super.onPause();
        mapView.onPause();
    }



    @Override

    protected void onDestroy() {
        super.onDestroy();
        mapView.onDestroy();
    }



    @Override

    public void onLowMemory() {
        super.onLowMemory();
        mapView.onLowMemory();
    }



    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        mapView.onSaveInstanceState(outState);
    }



    private void hideLevelButton() {
        // When the user moves away from our bounding box region or zooms out far enough the floor level
        // buttons are faded out and hidden.
        AlphaAnimation animation = new AlphaAnimation(1.0f, 0.0f);
        animation.setDuration(500);
        levelButtons.startAnimation(animation);
        levelButtons.setVisibility(View.GONE);
    }



    private void showLevelButton() {
        // When the user moves inside our bounding box region or zooms in to a high enough zoom level,
        // the floor level buttons are faded out and hidden.
        AlphaAnimation animation = new AlphaAnimation(0.0f, 1.0f);
        animation.setDuration(500);
        levelButtons.startAnimation(animation);
        levelButtons.setVisibility(View.VISIBLE);
    }

    public class FetchPredictedLocation extends AsyncTask<String, Void, String[]>{
        @Override
        protected String[] doInBackground(String... params) {

            HttpURLConnection urlConnection = null;
            BufferedReader reader = null;
            DataOutputStream writer;
            String WifiJson = "{\"ID\":\"test\",\"Email\":\"test@gmail.com\",\"Pwd\":\"test\"}";



            String predictedLocation = "";
           // Uri uri = Uri.parse("http://127.0.0.1:5000/api").buildUpon().build();
            try {
                URL url = new URL("http://10.0.2.2:5000/api");
                //create and open request to google API
                urlConnection = (HttpURLConnection) url.openConnection();
                urlConnection.setRequestMethod("GET");
                urlConnection.setRequestProperty("Content-Type", "application/json");
                urlConnection.setDoInput(true);
                urlConnection.setDoOutput(true);

                OutputStream stream = urlConnection.getOutputStream();
                writer = new DataOutputStream(stream);
                writer.writeBytes(params[0]);
                writer.flush();
                writer.close();
                stream.close();


                urlConnection.connect();

                InputStream inputStream = urlConnection.getInputStream();
                predictedLocation = readStream(inputStream);





            } catch (IOException e) {
                e.printStackTrace();
                return null;
            }



            return new String[0];
        }

    }

    public String readStream(InputStream in) {
        BufferedReader reader = null;
        StringBuffer data = new StringBuffer("");
        try {
            reader = new BufferedReader(new InputStreamReader(in));
            String line = "";
            while ((line = reader.readLine()) != null) {
                data.append(line);
            }
        } catch (IOException e) {
            Log.e("Log", "IOException");
        } finally {
            if (reader != null) {
                try {
                    reader.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
        return data.toString();
    }

    public String getRouters(){

       // arraylist.clear();
        wifi.startScan();
        results = wifi.getScanResults();
        size = results.size();
        String routerDetails;
        //JSONArray jsonArray = new JSONArray(results);
        String jsonRouter = new Gson().toJson(results);
       /* try {
            size = size - 1;
            while (size >= 0) {

               routerDetails = results.get(size).BSSID;

               // arraylist.add(item);
                size--;

            }
        } catch (Exception e) {
        }*/
        String staticRouterDetails = "[{\"BSSID\":\"c0:7b:bc:f0:b4:b0\",\"SSID\":\"UCD Wireless\",\"anqpDomainId\":0,\"blackListTimestamp\":0,\"capabilities\":\"[ESS]\",\"centerFreq0\":0,\"centerFreq1\":0,\"channelWidth\":0,\"distanceCm\":-1,\"distanceSdCm\":-1,\"flags\":0,\"frequency\":2462,\"hessid\":0,\"informationElements\":[{\"bytes\":[85,67,68,32,87,105,114,101,108,101,115,115],\"id\":0},{\"bytes\":[24,-92,48,72,96,108],\"id\":1},{\"bytes\":[11],\"id\":3},{\"bytes\":[73,69,32,1,13,20],\"id\":7},{\"bytes\":[4,0,64,-115,91],\"id\":11},{\"bytes\":[0],\"id\":42},{\"bytes\":[-84,17,27,-1,-1,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0],\"id\":45},{\"bytes\":[11,0,5,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0],\"id\":61},{\"bytes\":[0,16,0,0,0,64],\"id\":127},{\"bytes\":[3,0,-113,0,15,0,-1,3,89,0,108,119,98,115,45,99,115,99,105,45,48,48,56,0,0,0,4,0,0,66],\"id\":133},{\"bytes\":[0,64,-106,0,16,0],\"id\":150},{\"bytes\":[0,80,-14,2,1,1,-128,0,3,-92,0,0,39,-92,0,0,66,67,94,0,98,50,47,0],\"id\":221},{\"bytes\":[0,64,-106,1,1,4],\"id\":221},{\"bytes\":[0,64,-106,3,5],\"id\":221},{\"bytes\":[0,64,-106,11,9],\"id\":221},{\"bytes\":[0,64,-106,20,0],\"id\":221},{\"bytes\":[0,-96,-58,0,1,0,0,71,1,0,0,76,90,-57,-114],\"id\":221}],\"is80211McRTTResponder\":false,\"level\":-50,\"numConnection\":0,\"numIpConfigFailures\":0,\"numUsage\":0,\"operatorFriendlyName\":\"\",\"seen\":1532017662292,\"timestamp\":761788447329,\"untrusted\":false,\"venueName\":\"\",\"wifiSsid\":{\"octets\":{\"buf\":[85,67,68,32,87,105,114,101,108,101,115,115,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0],\"count\":12}}},{\"BSSID\":\"c0:7b:bc:f0:b4:b1\",\"SSID\":\"eduroam\",\"anqpDomainId\":0,\"blackListTimestamp\":0,\"capabilities\":\"[WPA2-EAP-CCMP][ESS]\",\"centerFreq0\":0,\"centerFreq1\":0,\"channelWidth\":0,\"distanceCm\":-1,\"distanceSdCm\":-1,\"flags\":0,\"frequency\":2462,\"hessid\":0,\"informationElements\":[{\"bytes\":[101,100,117,114,111,97,109],\"id\":0},{\"bytes\":[24,-92,48,72,96,108],\"id\":1},{\"bytes\":[11],\"id\":3},{\"bytes\":[73,69,32,1,13,20],\"id\":7},{\"bytes\":[4,0,64,-115,91],\"id\":11},{\"bytes\":[0],\"id\":42},{\"bytes\":[-84,17,27,-1,-1,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0],\"id\":45},{\"bytes\":[1,0,0,15,-84,4,1,0,0,15,-84,4,1,0,0,15,-84,1,40,0],\"id\":48},{\"bytes\":[11,0,5,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0],\"id\":61},{\"bytes\":[0,16,0,0,0,64],\"id\":127},{\"bytes\":[3,0,-113,0,15,0,-1,3,89,0,108,119,98,115,45,99,115,99,105,45,48,48,56,0,0,0,4,0,0,66],\"id\":133},{\"bytes\":[0,64,-106,0,16,0],\"id\":150},{\"bytes\":[0,80,-14,2,1,1,-128,0,3,-92,0,0,39,-92,0,0,66,67,94,0,98,50,47,0],\"id\":221},{\"bytes\":[0,64,-106,1,1,4],\"id\":221},{\"bytes\":[0,64,-106,3,5],\"id\":221},{\"bytes\":[0,64,-106,11,9],\"id\":221},{\"bytes\":[0,64,-106,20,1],\"id\":221},{\"bytes\":[0,-96,-58,0,1,0,0,71,1,0,0,76,-126,-58,-114],\"id\":221}],\"is80211McRTTResponder\":false,\"level\":-50,\"numConnection\":0,\"numIpConfigFailures\":0,\"numUsage\":0,\"operatorFriendlyName\":\"\",\"seen\":1532017662294,\"timestamp\":761788447217,\"untrusted\":false,\"venueName\":\"\",\"wifiSsid\":{\"octets\":{\"buf\":[101,100,117,114,111,97,109,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0],\"count\":7}}},{\"BSSID\":\"c0:7b:bc:f0:b4:be\",\"SSID\":\"eduroam\",\"anqpDomainId\":0,\"blackListTimestamp\":0,\"capabilities\":\"[WPA2-EAP-CCMP][ESS]\",\"centerFreq0\":0,\"centerFreq1\":0,\"channelWidth\":0,\"distanceCm\":-1,\"distanceSdCm\":-1,\"flags\":0,\"frequency\":5180,\"hessid\":0,\"informationElements\":[{\"bytes\":[101,100,117,114,111,97,109],\"id\":0},{\"bytes\":[24,-92,48,72,96,108],\"id\":1},{\"bytes\":[73,69,32,36,8,23,100,5,23,-124,3,30],\"id\":7},{\"bytes\":[8,0,18,-115,91],\"id\":11},{\"bytes\":[-84,17,27,-1,-1,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0],\"id\":45},{\"bytes\":[1,0,0,15,-84,4,1,0,0,15,-84,4,1,0,0,15,-84,1,40,0],\"id\":48},{\"bytes\":[36,8,4,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0],\"id\":61},{\"bytes\":[0,16,0,0,0,64],\"id\":127},{\"bytes\":[1,0,-113,0,15,0,-1,3,89,0,108,119,98,115,45,99,115,99,105,45,48,48,56,0,0,0,8,0,0,65],\"id\":133},{\"bytes\":[0,64,-106,0,18,0],\"id\":150},{\"bytes\":[0,80,-14,2,1,1,-128,0,3,-92,0,0,39,-92,0,0,66,67,94,0,98,50,47,0],\"id\":221},{\"bytes\":[0,64,-106,1,1,4],\"id\":221},{\"bytes\":[0,64,-106,3,5],\"id\":221},{\"bytes\":[0,64,-106,11,9],\"id\":221},{\"bytes\":[0,64,-106,20,1],\"id\":221},{\"bytes\":[0,-96,-58,0,1,0,0,39,1,0,0,113,-51,43,-20],\"id\":221}],\"is80211McRTTResponder\":false,\"level\":-57,\"numConnection\":0,\"numIpConfigFailures\":0,\"numUsage\":0,\"operatorFriendlyName\":\"\",\"seen\":1532017662292,\"timestamp\":761788447417,\"untrusted\":false,\"venueName\":\"\",\"wifiSsid\":{\"octets\":{\"buf\":[101,100,117,114,111,97,109,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0],\"count\":7}}},{\"BSSID\":\"c0:7b:bc:f0:b4:bf\",\"SSID\":\"UCD Wireless\",\"anqpDomainId\":0,\"blackListTimestamp\":0,\"capabilities\":\"[ESS]\",\"centerFreq0\":0,\"centerFreq1\":0,\"channelWidth\":0,\"distanceCm\":-1,\"distanceSdCm\":-1,\"flags\":0,\"frequency\":5180,\"hessid\":0,\"informationElements\":[{\"bytes\":[85,67,68,32,87,105,114,101,108,101,115,115],\"id\":0},{\"bytes\":[24,-92,48,72,96,108],\"id\":1},{\"bytes\":[73,69,32,36,8,23,100,5,23,-124,3,30],\"id\":7},{\"bytes\":[8,0,18,-115,91],\"id\":11},{\"bytes\":[-84,17,27,-1,-1,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0],\"id\":45},{\"bytes\":[36,8,4,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0],\"id\":61},{\"bytes\":[0,16,0,0,0,64],\"id\":127},{\"bytes\":[1,0,-113,0,15,0,-1,3,89,0,108,119,98,115,45,99,115,99,105,45,48,48,56,0,0,0,8,0,0,65],\"id\":133},{\"bytes\":[0,64,-106,0,18,0],\"id\":150},{\"bytes\":[0,80,-14,2,1,1,-128,0,3,-92,0,0,39,-92,0,0,66,67,94,0,98,50,47,0],\"id\":221},{\"bytes\":[0,64,-106,1,1,4],\"id\":221},{\"bytes\":[0,64,-106,3,5],\"id\":221},{\"bytes\":[0,64,-106,11,9],\"id\":221},{\"bytes\":[0,64,-106,20,0],\"id\":221},{\"bytes\":[0,-96,-58,0,1,0,0,39,1,0,0,113,-91,44,-20],\"id\":221}],\"is80211McRTTResponder\":false,\"level\":-57,\"numConnection\":0,\"numIpConfigFailures\":0,\"numUsage\":0,\"operatorFriendlyName\":\"\",\"seen\":1532017662293,\"timestamp\":761788447376,\"untrusted\":false,\"venueName\":\"\",\"wifiSsid\":{\"octets\":{\"buf\":[85,67,68,32,87,105,114,101,108,101,115,115,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0],\"count\":12}}},{\"BSSID\":\"e6:6a:6a:65:1a:51\",\"SSID\":\"DIRECT-ZeDESKTOP-7H8N5QTmsHw\",\"anqpDomainId\":0,\"blackListTimestamp\":0,\"capabilities\":\"[WPA2-PSK-CCMP][ESS][WPS]\",\"centerFreq0\":5190,\"centerFreq1\":0,\"channelWidth\":1,\"distanceCm\":-1,\"distanceSdCm\":-1,\"flags\":0,\"frequency\":5180,\"hessid\":0,\"informationElements\":[{\"bytes\":[68,73,82,69,67,84,45,90,101,68,69,83,75,84,79,80,45,55,72,56,78,53,81,84,109,115,72,119],\"id\":0},{\"bytes\":[-116,18,-104,36,-80,72,96,108],\"id\":1},{\"bytes\":[36],\"id\":3},{\"bytes\":[1,0,0,15,-84,4,1,0,0,15,-84,4,1,0,0,15,-84,2,0,0],\"id\":48},{\"bytes\":[-17,9,27,-1,-1,0,0,0,0,0,0,0,0,0,0,-128,0,0,0,0,0,0,0,0,0,0],\"id\":45},{\"bytes\":[36,5,1,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0],\"id\":61},{\"bytes\":[20,0,10,0,-56,0,-56,0,20,0,5,0,25,0],\"id\":74},{\"bytes\":[5,0,0,0,0,0,0,64],\"id\":127},{\"bytes\":[0,80,-14,2,1,1,-128,0,3,-92,0,0,39,-92,0,0,66,67,94,0,98,50,47,0],\"id\":221},{\"bytes\":[0,3,127,1,1,0,0,-1,127],\"id\":221},{\"bytes\":[0,80,-14,4,16,74,0,1,16,16,68,0,1,2,16,59,0,1,0,16,71,0,16,-13,13,122,-67,3,-40,67,-67,-75,89,20,53,100,-118,23,-68,16,33,0,9,77,105,99,114,111,115,111,102,116,16,35,0,13,73,110,115,112,105,114,111,110,32,53,51,55,57,16,36,0,10,49,48,46,48,46,49,55,49,51,52,16,66,0,1,48,16,84,0,8,0,7,0,80,-14,0,0,0,16,17,0,3,84,101,106,16,8,0,2,0,8,16,73,0,6,0,55,42,0,1,32,16,73,0,23,0,1,55,16,6,0,16,-32,-2,-6,81,-118,86,79,36,-105,-66,26,-111,-68,88,115,-33,16,73,0,15,0,1,55,32,1,0,1,5,32,2,0,3,84,101,106],\"id\":221},{\"bytes\":[80,111,-102,10,0,0,6,0,17,28,68,0,6],\"id\":221},{\"bytes\":[0,-96,-58,0,1,0,0,39,1,0,0,-15,103,87,120],\"id\":221}],\"is80211McRTTResponder\":false,\"level\":-65,\"numConnection\":0,\"numIpConfigFailures\":0,\"numUsage\":0,\"operatorFriendlyName\":\"\",\"seen\":1532017662295,\"timestamp\":761788447459,\"untrusted\":false,\"venueName\":\"\",\"wifiSsid\":{\"octets\":{\"buf\":[68,73,82,69,67,84,45,90,101,68,69,83,75,84,79,80,45,55,72,56,78,53,81,84,109,115,72,119,0,0,0,0],\"count\":28}}},{\"BSSID\":\"60:38:e0:bd:89:31\",\"SSID\":\"\",\"anqpDomainId\":0,\"blackListTimestamp\":0,\"capabilities\":\"[WPA2-PSK-CCMP][ESS]\",\"centerFreq0\":2422,\"centerFreq1\":0,\"channelWidth\":1,\"distanceCm\":-1,\"distanceSdCm\":-1,\"flags\":0,\"frequency\":2412,\"hessid\":0,\"informationElements\":[{\"bytes\":[],\"id\":0},{\"bytes\":[-126,-124,-117,-106,12,18,24,36],\"id\":1},{\"bytes\":[1],\"id\":3},{\"bytes\":[0,3,0,0],\"id\":5},{\"bytes\":[0],\"id\":42},{\"bytes\":[48,72,96,108],\"id\":50},{\"bytes\":[1,0,0,15,-84,4,1,0,0,15,-84,4,1,0,0,15,-84,2,0,0],\"id\":48},{\"bytes\":[111,8,23,-1,-1,-1,0,1,0,0,0,0,0,0,0,0,0,0,0,0,0,31,-1,7,24,0],\"id\":45},{\"bytes\":[1,5,4,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0],\"id\":61},{\"bytes\":[11,36,40,44,48,52,56,60,64,100,104,108,112,116,120,124,-128,-124,-120,-116],\"id\":51},{\"bytes\":[20,0,10,0,-76,0,-56,0,20,0,5,0,25,0],\"id\":74},{\"bytes\":[1,0,0,0,0,0,0,0],\"id\":127},{\"bytes\":[50,121,-117,51,-22,-1,0,0,-22,-1,0,0],\"id\":191},{\"bytes\":[0,0,0,-64,-1],\"id\":192},{\"bytes\":[0,80,67,3,0,0],\"id\":221},{\"bytes\":[0,80,-14,2,1,1,-128,0,3,-92,0,0,39,-92,0,0,66,67,94,0,98,50,47,0],\"id\":221},{\"bytes\":[0,-96,-58,0,1,0,0,-103,1,0,0,69,-44,-87,-20],\"id\":221}],\"is80211McRTTResponder\":false,\"level\":-74,\"numConnection\":0,\"numIpConfigFailures\":0,\"numUsage\":0,\"operatorFriendlyName\":\"\",\"seen\":1532017662295,\"timestamp\":761788447498,\"untrusted\":false,\"venueName\":\"\",\"wifiSsid\":{\"octets\":{\"buf\":[0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0],\"count\":0}}},{\"BSSID\":\"c0:7b:bc:36:ec:d1\",\"SSID\":\"eduroam\",\"anqpDomainId\":0,\"blackListTimestamp\":0,\"capabilities\":\"[WPA2-EAP-CCMP][ESS]\",\"centerFreq0\":0,\"centerFreq1\":0,\"channelWidth\":0,\"distanceCm\":-1,\"distanceSdCm\":-1,\"flags\":0,\"frequency\":2412,\"hessid\":0,\"informationElements\":[{\"bytes\":[101,100,117,114,111,97,109],\"id\":0},{\"bytes\":[24,-92,48,72,96,108],\"id\":1},{\"bytes\":[1],\"id\":3},{\"bytes\":[73,69,32,1,13,20],\"id\":7},{\"bytes\":[1,0,33,-115,91],\"id\":11},{\"bytes\":[0],\"id\":42},{\"bytes\":[-84,17,27,-1,-1,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0],\"id\":45},{\"bytes\":[1,0,0,15,-84,4,1,0,0,15,-84,4,1,0,0,15,-84,1,40,0],\"id\":48},{\"bytes\":[1,8,4,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0],\"id\":61},{\"bytes\":[0,16,0,0,0,64],\"id\":127},{\"bytes\":[2,0,-113,0,15,0,-1,3,89,0,108,119,98,115,45,99,115,99,105,45,48,48,55,0,0,0,1,0,0,66],\"id\":133},{\"bytes\":[0,64,-106,0,16,0],\"id\":150},{\"bytes\":[0,80,-14,2,1,1,-128,0,3,-92,0,0,39,-92,0,0,66,67,94,0,98,50,47,0],\"id\":221},{\"bytes\":[0,64,-106,1,1,4],\"id\":221},{\"bytes\":[0,64,-106,3,5],\"id\":221},{\"bytes\":[0,64,-106,11,9],\"id\":221},{\"bytes\":[0,64,-106,20,1],\"id\":221},{\"bytes\":[0,-96,-58,0,1,0,0,-80,1,0,0,-14,-122,-94,39],\"id\":221}],\"is80211McRTTResponder\":false,\"level\":-76,\"numConnection\":0,\"numIpConfigFailures\":0,\"numUsage\":0,\"operatorFriendlyName\":\"\",\"seen\":1532017662293,\"timestamp\":761788447533,\"untrusted\":false,\"venueName\":\"\",\"wifiSsid\":{\"octets\":{\"buf\":[101,100,117,114,111,97,109,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0],\"count\":7}}},{\"BSSID\":\"c0:7b:bc:36:ec:d0\",\"SSID\":\"UCD Wireless\",\"anqpDomainId\":0,\"blackListTimestamp\":0,\"capabilities\":\"[ESS]\",\"centerFreq0\":0,\"centerFreq1\":0,\"channelWidth\":0,\"distanceCm\":-1,\"distanceSdCm\":-1,\"flags\":0,\"frequency\":2412,\"hessid\":0,\"informationElements\":[{\"bytes\":[85,67,68,32,87,105,114,101,108,101,115,115],\"id\":0},{\"bytes\":[24,-92,48,72,96,108],\"id\":1},{\"bytes\":[1],\"id\":3},{\"bytes\":[73,69,32,1,13,20],\"id\":7},{\"bytes\":[1,0,33,-115,91],\"id\":11},{\"bytes\":[0],\"id\":42},{\"bytes\":[-84,17,27,-1,-1,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0],\"id\":45},{\"bytes\":[1,8,4,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0],\"id\":61},{\"bytes\":[0,16,0,0,0,64],\"id\":127},{\"bytes\":[2,0,-113,0,15,0,-1,3,89,0,108,119,98,115,45,99,115,99,105,45,48,48,55,0,0,0,1,0,0,66],\"id\":133},{\"bytes\":[0,64,-106,0,16,0],\"id\":150},{\"bytes\":[0,80,-14,2,1,1,-128,0,3,-92,0,0,39,-92,0,0,66,67,94,0,98,50,47,0],\"id\":221},{\"bytes\":[0,64,-106,1,1,4],\"id\":221},{\"bytes\":[0,64,-106,3,5],\"id\":221},{\"bytes\":[0,64,-106,11,9],\"id\":221},{\"bytes\":[0,64,-106,20,0],\"id\":221},{\"bytes\":[0,-96,-58,0,1,0,0,-80,1,0,0,-14,94,-93,39],\"id\":221}],\"is80211McRTTResponder\":false,\"level\":-77,\"numConnection\":0,\"numIpConfigFailures\":0,\"numUsage\":0,\"operatorFriendlyName\":\"\",\"seen\":1532017662294,\"timestamp\":761788447569,\"untrusted\":false,\"venueName\":\"\",\"wifiSsid\":{\"octets\":{\"buf\":[85,67,68,32,87,105,114,101,108,101,115,115,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0],\"count\":12}}}]";


        return staticRouterDetails;
    }








}
