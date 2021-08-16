package com.example.hereapplication

import android.content.Context
import android.util.Log
import android.widget.Toast
import com.example.hereapplication.Util.PolyUtil
import com.here.sdk.core.GeoCircle
import com.here.sdk.core.GeoCoordinates
import com.here.sdk.core.GeoPolyline
import com.here.sdk.core.LanguageCode
import com.here.sdk.core.errors.InstantiationErrorException
import com.here.sdk.mapviewlite.*
import com.here.sdk.routing.*
import com.here.sdk.search.*
import kotlinx.coroutines.*


class RoutingClass() {
    constructor(context: Context, mapView: MapViewLite) : this() {
        this.context = context
        this.mapView = mapView
        val camera = mapView.camera
        camera.target = GeoCoordinates(28.5272803, 77.0689)
        camera.zoomLevel = 12.0
        try {
            routingEngine = RoutingEngine()
            searchEngine = SearchEngine()
        } catch (e: Exception) {
            throw  RuntimeException("Initialization of RoutingEngine failed: " + e.message.toString());
        }
    }

    private var routingEngine: RoutingEngine? = null
    private var searchEngine: SearchEngine? = null
    private var startGeoCoordinates: GeoCoordinates? = null
    private var destinationGeoCoordinates: GeoCoordinates? = null
    private var context: Context? = null
    private lateinit var mapView: MapViewLite
    private var validPlacesList = ArrayList<Place>()
    private var invalidEligiblePlacesList = ArrayList<Place>()

    fun calculateRestaurantsCount(
        sourceLat: Double,
        sourceLong: Double,
        destinationLat: Double,
        destinationLong: Double
    ) {
        startGeoCoordinates = GeoCoordinates(sourceLat, sourceLong)
        destinationGeoCoordinates =
            GeoCoordinates(destinationLat, destinationLong)
        startGeoCoordinates?.let { startGeoCoordinate ->
            destinationGeoCoordinates?.let { endGeoCoordinate ->
                val startWayPoint = Waypoint(startGeoCoordinate)
                val destinationWayPoint = Waypoint(endGeoCoordinate)
                val wayPoints: List<Waypoint> =
                    ArrayList(listOf(startWayPoint, destinationWayPoint))
                routingEngine?.calculateRoute(
                    wayPoints,
                    CarOptions()
                ) { routingError, routes ->
                    if (routingError == null) {
                        val route = routes?.get(0)
                        route?.let {
                            GlobalScope.launch {
                                findRestaurants(it)
                            }
                            val geoBox = it.boundingBox
                            val camera = mapView.camera.calculateEnclosingCameraUpdate(
                                geoBox,
                                Padding(50f, 50f, 50f, 50f)
                            )
                            mapView.camera.updateCamera(camera)
                            showRouteOnMap(route)
                        }

                    } else {
                        Toast.makeText(
                            context,
                            "${context?.getString(R.string.routeError)} $routingError",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }
            }
        }
    }

    private fun showRouteOnMap(route: Route) {
        val routeGeoPolyline: GeoPolyline = try {
            GeoPolyline(route.polyline)
        } catch (e: InstantiationErrorException) {
            // It should never happen that the route polyline contains less than two vertices.
            return
        }
        val mapPolylineStyle = MapPolylineStyle()
        mapPolylineStyle.setColor(0x00908AA0, PixelFormat.RGBA_8888)
        mapPolylineStyle.widthInPixels = 10.0
        val routeMapPolyline = MapPolyline(routeGeoPolyline, mapPolylineStyle)
        mapView.mapScene.addMapPolyline(routeMapPolyline)
    }

    private fun runSearchQuery(
        previousCoordinate: GeoCoordinates,
        mutableListOfPolyLines: MutableList<GeoCoordinates>
    ) {
        val categoryList: MutableList<PlaceCategory> = ArrayList()
        categoryList.add(PlaceCategory(PlaceCategory.EAT_AND_DRINK_RESTAURANT))
        val categoryQuery =
            CategoryQuery(
                categoryList,
                previousCoordinate,
                GeoCircle(previousCoordinate, 0.5 * 1000.0)
            )
        val maxItems = 100
        val searchOptions = SearchOptions(LanguageCode.EN_US, maxItems)
        searchEngine?.search(
            categoryQuery, searchOptions,
            SearchCallback { searchError, list ->
                if (searchError != null) {
                    return@SearchCallback
                }

                // If error is null, list is guaranteed to be not empty.
                list?.let { searchedResult ->
                    for (searchResult in searchedResult) {
                        //50m tolerance
                        if (PolyUtil.isLocationOnEdgeOrPath(
                                searchResult.geoCoordinates,
                                mutableListOfPolyLines, false, false,
                                50.0
                            )
                        ) {
                            validPlacesList.add(searchResult)
                            searchResult.geoCoordinates?.let { coordinates ->
                                CoroutineScope(Dispatchers.IO).launch {
                                    addMarker(coordinates)
                                }
                            }
                        } else {
                            invalidEligiblePlacesList.add(searchResult)
                        }
                    }
                }
            })
    }

    private suspend fun findRestaurants(route: Route) {
        route.polyline.let {
            val polyLineSize = it.size
            var measuredDistance = 1
            var previousCoordinate = it[0]
            while (measuredDistance < polyLineSize) {
                val currentCoordinate = it[measuredDistance]
                val distance = currentCoordinate.distanceTo(previousCoordinate)
                // in km's
                if (distance > 1 * 1000) {
                    runSearchQuery(previousCoordinate, it)
                    delay(50)
                    previousCoordinate = currentCoordinate
                }
                measuredDistance++
            }
            runSearchQuery(it[it.size - 1], it)
        }

    }

    private fun addMarker(geoCoordinates: GeoCoordinates) {
        val mapImage = MapImageFactory.fromResource(context?.resources, R.drawable.marker)
        val mapMarker = MapMarker(geoCoordinates)
        mapMarker.addImage(mapImage, MapMarkerImageStyle())
        mapView.mapScene.addMapMarker(mapMarker)
    }

}