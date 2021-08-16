package com.example.hereapplication

import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.hereapplication.Util.PermissionsRequester
import com.example.hereapplication.databinding.ActivityMainBinding
import com.here.sdk.mapviewlite.MapStyle


class MainActivity : AppCompatActivity() {
    lateinit var viewBinding: ActivityMainBinding
    private val TAG = MainActivity::class.java.simpleName
    private lateinit var permissionRequester: PermissionsRequester
    private lateinit var routingClass: RoutingClass
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        viewBinding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(viewBinding.root)
        viewBinding.mapView.onCreate(savedInstanceState)
        handleAndroidPermissions()
        viewBinding.findPlaces.setOnClickListener {
            if (isValidLatLng(
                    viewBinding.etSourceLat.text.toString(),
                    viewBinding.etSourceLong.text.toString(),
                    viewBinding.etDestLat.text.toString(),
                    viewBinding.etDestLong.text.toString()

                )
            ) {
                routingClass.calculateRestaurantsCount(
                    viewBinding.etSourceLat.text.toString().toDouble(),
                    viewBinding.etSourceLong.text.toString().toDouble(),
                    viewBinding.etDestLat.text.toString().toDouble(),
                    viewBinding.etDestLong.text.toString().toDouble()
                )
            } else {
                Toast.makeText(this, getString(R.string.invalidCoordinates), Toast.LENGTH_SHORT)
                    .show()
            }
        }
    }

    private fun isValidLatLng(
        sourceLat: String,
        sourceLong: String,
        destLat: String,
        destLong: String
    ): Boolean {
        if (sourceLat.isEmpty() || sourceLat.toDouble() < -90 || sourceLat.toDouble() > 90 || destLat.isEmpty() || destLat.toDouble() < -90 || destLat.toDouble() > 90) {
            return false
        } else if (sourceLong.isEmpty() || sourceLong.toDouble() < -180 || sourceLong.toDouble() > 180 || destLong.isEmpty() || destLong.toDouble() < -180 || destLong.toDouble() > 180) {
            return false
        }
        return true
    }

    private fun handleAndroidPermissions() {
        permissionRequester = PermissionsRequester(this)
        permissionRequester.request(object : PermissionsRequester.ResultListener {
            override fun permissionsGranted() {
                loadMap()
            }

            override fun permissionsDenied() {
                Log.e(TAG, getString(R.string.permissionDenied));
            }
        })
    }

    private fun loadMap() {
        viewBinding.mapView?.let { mapInstance ->
            viewBinding.mapView.mapScene.loadScene(
                MapStyle.NORMAL_DAY
            ) { errorCode ->
                if (errorCode == null) {
                    routingClass = RoutingClass(this, mapInstance)
                }
            }
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        permissionRequester.onRequestPermissionsResult(requestCode, grantResults)
    }

    override fun onPause() {
        super.onPause()
        viewBinding.mapView.onPause()

    }

    override fun onResume() {
        super.onResume()
        viewBinding.mapView.onResume()
    }

    override fun onDestroy() {
        super.onDestroy()
        viewBinding.mapView.onDestroy()
    }
}