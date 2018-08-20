package com.project.android.test;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
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

import com.mapbox.mapboxsdk.annotations.Icon;
import com.mapbox.mapboxsdk.annotations.IconFactory;
import com.mapbox.mapboxsdk.annotations.Marker;
import com.mapbox.mapboxsdk.annotations.MarkerOptions;
import com.mapbox.mapboxsdk.annotations.MarkerView;
import com.mapbox.mapboxsdk.annotations.MarkerViewOptions;
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
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.ExecutionException;

//import com.google.android.gms.location.LocationListener;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;



public class MainActivity extends AppCompatActivity {

    Cursor cursor = null;
    Cursor cursorCoord = null;
    Cursor cursorDest = null;
    Graph graph = new Graph();


    // private GeoJsonSource indoorBuildingSource;
    private List<Point> boundingBox;
    private List<List<Point>> boundingBoxList;
    private View levelButtons;
    private MapView mapView;
    private MapboxMap map;
    private MarkerOptions mMarker;
    private MarkerOptions mMarkerUsr;
    private MarkerViewOptions options;
    private MarkerView marker_inter;
    private LatLng mLatLng;
    WifiManager wifi;
    private  Button startNavigation;
    private Button btnGetLocation;
    private EditText dest;
    final String[] destinationMap = {""};
    List<ScanResult> results;
    int size = 0;
    private Icon icon;
    private Icon iconUsr;
    public int run =0;

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
                ArrayList<String> nodePathName = new ArrayList<>();
               getDestination_Route destination = new getDestination_Route();
               dest = findViewById(R.id.txtdestination);
               destination.execute(dest.getText().toString()).toString();

                String[] columns = {"nodeid","nodecoordinates","building"};
                HashMap<String,Node> nodeArrayList = new HashMap<>();


                DatabaseHelper myDbHelper = new DatabaseHelper(MainActivity.this);
                try {
                    myDbHelper.createDataBase();
                } catch (IOException ioe) {
                    throw new Error("Unable to create database");
                }
                myDbHelper.openDataBase();
                cursor = myDbHelper.query("node", columns, null, null, null, null, null);


                //Form the graph

                for (int i = 0; i < cursor.getCount(); i++) {

                    cursor.moveToPosition(i);
                    //cursor.getString(1).toString();
                    String nodeId = cursor.getString(0).toString();
                    Node node = new Node(nodeId);
                    nodeArrayList.put(nodeId,node);
                    //if(!nodeId.equals("CSG000"))
                      //  nodeArrayList.put(nodeId,node);

                }
                for (int i = 0; i < cursor.getCount(); i++) {
                    cursor.moveToPosition(i);
                    String nodeId = cursor.getString(0).toString();
                    String connectedNodes = cursor.getString(1).toString();
                    String[] connectedNodesArray = connectedNodes.split(",");
                    for(String nodeid:connectedNodesArray){
                        if (!nodeid.equals("")) {
                            String splitNode = nodeid.split("-")[1];
                            Node connectNode = nodeArrayList.get(splitNode);
                            if( nodeArrayList.get(nodeId)!=null)
                                nodeArrayList.get(nodeId).addDestination(connectNode, 5);
                        }
                    }


                }

                for(Node nodeInArray: nodeArrayList.values()){
                    graph.addNode(nodeInArray);

                }

                graph = algorithm.calculateShortestPathFromSource(graph, nodeArrayList.get("CSG001"));
                //cursorCoord = myDbHelper.query("coordinate", columnsCoordintate, null, null, null, null, null);
                cursorDest = myDbHelper.rawQuery("SELECT * FROM coordinate WHERE location='"+dest.getText().toString()+"C'");
                cursorDest.moveToFirst();
                String destNode = cursorDest.getString(0).toString();


                Iterator<Node> itr = graph.getNodes().iterator();
                while(itr.hasNext())
                {
                    Node curNode = itr.next();
                    String name = curNode.getName();
                    if(name.equalsIgnoreCase(destNode))
                    {
                        Iterator<Node> itrPath = curNode.getShortestPath().iterator();
                        while(itrPath.hasNext())
                        {
                            Node nodePath = itrPath.next();
                            String nodesInPath = nodePath.getName().toString();
                            nodePathName.add(nodesInPath);
                        }


                    }


                }
                String NodePath = "(";
                Iterator<String> nameNodeInPath = nodePathName.iterator();
                while(nameNodeInPath.hasNext())
                {
                    NodePath =NodePath+ "'"+ nameNodeInPath.next()+"'" +",";

                }
                NodePath = NodePath.substring(0,NodePath.length()-1);
                NodePath = NodePath+")";
                cursorCoord = myDbHelper.rawQuery("SELECT * FROM coordinate WHERE nodeid IN"+NodePath);

                for (int i = 0; i < cursorCoord.getCount(); i++) {
                    cursorCoord.moveToPosition(i);
                    String point = cursorCoord.getString(1).toString();
                    String[] latitude = point.split(",");
                    addMarker(Double.parseDouble(latitude[0]),Double.parseDouble(latitude[1]));



                }





            }
        });

        btnGetLocation.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                String routerDetails = getRouters();
               // map.removeMarker(mMarkerUsr);



                //Begin from here
                FetchPredictedLocation predictedLocation = new FetchPredictedLocation();
                predictedLocation.execute(routerDetails);
               // predictedLocation.execute(routerDetails);
               // predictedLocation.execute(routerDetails);

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
        IconFactory iconFactory = IconFactory.getInstance(this);
        icon = iconFactory.fromResource(R.drawable.purple_marker);
        iconUsr = iconFactory.fromResource(R.drawable.usr_icon);





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
            addDestination(lat, lon);


        }

    }

    private void addDestination(double lat, double lon) {

        mLatLng = new LatLng(lat,lon);
        mMarker = new MarkerOptions()
                .position(mLatLng)
                .title("Location")
                .snippet("Welcome to you");
        map.addMarker(mMarker);
    }

    private void addMarker(double lat, double lon) {

        mLatLng = new LatLng(lat,lon);
        mMarker = new MarkerOptions()
                .position(mLatLng)
                .title("Location")
                .setIcon(icon)
                .snippet("Welcome to you");
        map.addMarker(mMarker);
    }
    private void addCurrentMarker(double lat, double lon) {
        mLatLng = new LatLng(lat, lon);
            //map.removeMarker(marker_inter);
        mMarker = new MarkerOptions()
                .position(mLatLng)
                .title("Location")
                .setIcon(iconUsr)
                .snippet("Welcome to you");

       // map.addMarker(mMarker);

       // Marker updateMarker = new Marker(mMarker);


        /*marker_inter = map.addMarker(new MarkerViewOptions()
                .position(mLatLng)
                .title("currentLocation")
                .snippet("You are here!")
                .icon(iconUsr));*/

            map.addMarker(mMarker);

            //map.removeMarker(mMarker.getMarker());
            map.updateMarker(mMarker.getMarker());

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

            String predictedLocation = "";

            try {
                URL url = new URL("http://ucdgps.ucd.ie");
                //URL url = new URL("http://10.0.2.2:5000/");


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
                String[] latitude = predictedLocation.split(",");
                addCurrentMarker(Double.parseDouble(latitude[0]),Double.parseDouble(latitude[1]));


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

        return jsonRouter;
    }








}
