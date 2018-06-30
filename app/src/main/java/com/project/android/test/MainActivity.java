package com.project.android.test;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.view.animation.AlphaAnimation;
import android.widget.Button;

import com.mapbox.geojson.Point;
import com.mapbox.mapboxsdk.Mapbox;
import com.mapbox.mapboxsdk.maps.MapView;
import com.mapbox.mapboxsdk.maps.MapboxMap;
import com.mapbox.mapboxsdk.maps.OnMapReadyCallback;

import java.util.ArrayList;
import java.util.List;

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

        mapView.getMapAsync(new OnMapReadyCallback() {

            @Override

            public void onMapReady(final MapboxMap mapboxMap) {

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








}
