package com.example.campus

import android.accounts.AccountManager
import android.content.Context
import android.graphics.Canvas
import android.graphics.PointF
import com.nextgis.maplib.datasource.Feature
import com.nextgis.maplib.datasource.GeoGeometry
import com.nextgis.maplib.datasource.GeoMultiPoint
import com.nextgis.maplib.datasource.GeoPoint
import com.nextgis.maplib.map.MapDrawable
import com.nextgis.maplib.util.Constants
import com.nextgis.maplib.util.GeoConstants
import com.nextgis.maplibui.api.DrawItem
import com.nextgis.maplibui.api.Overlay
import com.nextgis.maplibui.api.VertexStyle
import com.nextgis.maplibui.fragment.NGWSettingsFragment
import com.nextgis.maplibui.mapui.MapViewOverlays
import com.nextgis.maplibui.util.ControlHelper
import android.Manifest
import android.annotation.SuppressLint
import android.content.*
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.content.res.ColorStateList
import android.graphics.Color
import android.location.LocationManager
import android.os.Build
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.preference.PreferenceManager
import android.support.design.widget.FloatingActionButton
import android.support.v4.app.ActivityCompat
import android.support.v4.content.ContextCompat
import android.support.v7.app.AlertDialog
import android.util.Log
import android.util.Log.*
import android.view.Menu
import android.view.MenuItem
import android.view.MotionEvent
import android.view.View
import android.widget.*
import com.nextgis.maplib.api.IGISApplication
import com.nextgis.maplib.api.ILayerView
import com.nextgis.maplib.datasource.GeoEnvelope
import com.nextgis.maplib.map.Layer
import com.nextgis.maplib.map.VectorLayer
import com.nextgis.maplibui.GISApplication
import com.nextgis.maplibui.api.MapViewEventListener
import com.nextgis.maplibui.mapui.NGWVectorLayerUI
import com.nextgis.maplibui.overlay.CurrentLocationOverlay
import com.nextgis.maplibui.util.ConstantsUI
import kotlin.random.Random

class MainActivity : AppCompatActivity(), MapViewEventListener {
    private lateinit var selectedOverlay: SelectFeatureOverlay

    var mapView: MapViewOverlays? = null
    var preferences: SharedPreferences? = null
    var overlay: BusesOverlay? = null
    var authorized = false

    override fun onLayersReordered() {}

    override fun onLayerDrawFinished(id: Int, percent: Float) {}

    override fun onSingleTapUp(event: MotionEvent?) {
        event?.let {
            val tolerance = resources.displayMetrics.density * ConstantsUI.TOLERANCE_DP.toDouble()
            val dMinX = event.x - tolerance
            val dMaxX = event.x + tolerance
            val dMinY = event.y - tolerance
            val dMaxY = event.y + tolerance
            val envelope = GeoEnvelope(dMinX, dMaxX, dMinY, dMaxY)
            val mapEnv = mapView?.screenToMap(envelope) ?: return


            val types = GeoConstants.GTPointCheck
            mapView?.getVectorLayersByType(types)?.let { layers ->
                var items: List<Long>? = null
                var selectedLayer: NGWVectorLayerUI? = null
                for (layer in layers) {
                    if (!layer.isValid || layer is ILayerView && !layer.isVisible)
                        continue

                    items = (layer as NGWVectorLayerUI).query(mapEnv)
                    if (!items.isEmpty()) {
                        selectedLayer = layer
                        break
                    }
                }

                selectedLayer?.let {
                    for (i in items!!.indices) {
                        val feature = selectedLayer.getFeature(items[i])
                        feature?.let {
                            if (selectedLayer.name == SignInActivity.LAYERS[2].second) {
                                openCafe(selectedLayer.id, feature.id)
                                return
                            }
                            it.geometry?.let { selectedOverlay.feature = feature }

                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                                val cat = feature.getFieldValueAsInteger("category_id")
                                when (cat) {
                                    1 -> findViewById<ImageView>(R.id.avatar).imageTintList =
                                        ColorStateList.valueOf(Color.LTGRAY)
                                    2 -> findViewById<ImageView>(R.id.avatar).imageTintList =
                                        ColorStateList.valueOf(Color.GRAY)
                                    3 -> findViewById<ImageView>(R.id.avatar).imageTintList =
                                        ColorStateList.valueOf(Color.MAGENTA)
                                    else -> findViewById<ImageView>(R.id.avatar).imageTintList =
                                        ColorStateList.valueOf(Color.RED)
                                }
                            }

                            findViewById<View>(R.id.people).visibility = View.GONE
                            findViewById<TextView>(R.id.title).text = feature.getFieldValueAsString("title")
                            findViewById<TextView>(R.id.category).text = feature.getFieldValueAsString("category")
                            findViewById<TextView>(R.id.description).text = feature.getFieldValueAsString("description")
                        }
                    }

                    selectedOverlay.feature?.let {
                        findViewById<View>(R.id.info).visibility = View.VISIBLE
                    }
                }
            }


            if (overlay!!.isVisible) {
                overlay!!.selectBus(mapEnv)?.let {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                        findViewById<ImageView>(R.id.avatar).imageTintList = ColorStateList.valueOf(Color.BLUE)
                    }

                    findViewById<View>(R.id.people).visibility = View.VISIBLE
                    findViewById<ProgressBar>(R.id.people).progress = Random(System.currentTimeMillis()).nextInt(0, 100)
                    findViewById<TextView>(R.id.title).text = getString(R.string.bus)
                    findViewById<TextView>(R.id.category).text = ""
                    findViewById<TextView>(R.id.description).text = getString(R.string.bus_desc)
                    findViewById<View>(R.id.info).visibility = View.VISIBLE

                    mapView?.let { map ->
                        it.getCoordinates(GeoConstants.CRS_WEB_MERCATOR)?.let { location ->
                            val center = location.copy() as GeoPoint
                            center.y -= 1000
                            map.setZoomAndCenter(map.zoomLevel, center)
                        }
                    }
                    return
                }
            }


        }
    }



    override fun onLayerAdded(id: Int) {}


    override fun onLayerDeleted(id: Int) {}


    override fun onLayerChanged(id: Int) {}


    override fun onExtentChanged(zoom: Float, center: GeoPoint?) {}


    override fun onLayerDrawStarted() {

    }
    override fun onLongPress(event: MotionEvent?) {}


    override fun panStart(e: MotionEvent?) {}


    override fun panMoveTo(e: MotionEvent?) {}


    override fun panStop() {}




    override fun onCreate(savedInstanceState: Bundle?) {

        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        val app = application as GISApplication
        val map = app.map as MapDrawable
        mapView = MapViewOverlays(this, map)
        selectedOverlay = SelectFeatureOverlay(this, mapView!!)
        mapView?.addOverlay(selectedOverlay)

        overlay = BusesOverlay ( this, mapView!!)
        mapView!!.addOverlay(overlay)
        val locationOverlay = CurrentLocationOverlay(this, mapView!!)
        mapView!!.addOverlay(locationOverlay)
        locationOverlay.startShowingCurrentLocation()

        findViewById<FrameLayout>(R.id.map).addView(mapView)
        findViewById<FloatingActionButton>(R.id.location).setOnClickListener { locatePosition() }
        findViewById<FloatingActionButton>(R.id.zoomIn).setOnClickListener { mapView!!.zoomIn() }
        findViewById<FloatingActionButton>(R.id.zoomOut).setOnClickListener { mapView!!.zoomOut() }
        findViewById<ImageButton>(R.id.close).setOnClickListener {
            findViewById<View>(R.id.info).visibility = View.GONE
            overlay?.defaultColor()
            selectedOverlay.feature = null

        }

        preferences = PreferenceManager.getDefaultSharedPreferences(this)

/*        authorized = preferences!!.getBoolean("authorized", false)
        if (!authorized) {
            val cafe = SignInActivity.LAYERS[2].second
            (mapView?.map?.getLayerByName(cafe) as? VectorLayer)?.let {
                it.isVisible = false
            }
            overlay!!.setVisibility(false)
        }*/


        setCenter()
        if (!preferences!!.getBoolean("signed", false)) {
            val intent = Intent(this, SignInActivity::class.java)
            startActivity(intent)
            finish()
        }
        sync()
    }



        override fun onStart() {
            super.onStart()
            mapView?.addListener(this)
        }


    private fun sync() {
        AccountManager.get(this)?.let { manager ->
            (application as? IGISApplication)?.let { app ->
                manager.getAccountsByType(app.accountsType).firstOrNull()?.let { account ->
                    val syncEnabled = NGWSettingsFragment.isAccountSyncEnabled(account, app.authority)
                    if (syncEnabled) {
                        val settings = Bundle()
                        settings.putBoolean(ContentResolver.SYNC_EXTRAS_MANUAL, true)
                        settings.putBoolean(ContentResolver.SYNC_EXTRAS_EXPEDITED, true)
                        ContentResolver.requestSync(account, app.authority, settings)
                    }
                }
            }
        }
    }

    override fun onStop() {

        super.onStop()
        mapView?.let {
            val point = it.mapCenter
            preferences?.edit()?.putFloat("zoom", it.zoomLevel)
                ?.putFloat("scroll_x", point.x.toFloat())
                ?.putFloat("scroll_y", point.y.toFloat())
                ?.apply()
        }
        mapView?.removeListener(this)
    }



    private fun setCenter() {
        mapView?.let {
            val mapZoom = preferences?.getFloat("zoom", 16f)
            val x = preferences?.getFloat("scroll_x", 14682143.82f)
            val y = preferences?.getFloat("scroll_y", 5316524.04f)
            val mapScrollX = x?.toDouble() ?: 0.0
            val mapScrollY = y?.toDouble() ?: 0.0
            it.setZoomAndCenter(mapZoom ?: it.minZoom, GeoPoint(mapScrollX,mapScrollY))
        }
    }



    private fun openCafe(layerId: Int, featureId: Long) {
        val intent = Intent(this, CafeActivity::class.java)
        intent.putExtra("layer_id", layerId)
        intent.putExtra("feature_id", featureId)
        startActivity(intent)
    }


    fun locatePosition() {
        val permission = Manifest.permission.ACCESS_FINE_LOCATION
        if (!isPermissionGranted(permission)) {
            val permissions = arrayOf(permission)
            ActivityCompat.requestPermissions(this, permissions, 3)
        } else {
            setLocation()
        }
    }

    @SuppressLint("MissingPermission")
    fun setLocation() {
        val manager = getSystemService(Context.LOCATION_SERVICE) as LocationManager
        val location = manager.getLastKnownLocation(LocationManager.GPS_PROVIDER)
        if (location != null) {
            val point = GeoPoint()
            point.setCoordinates(location.longitude, location.latitude)
            point.crs = GeoConstants.CRS_WGS84

            if (point.project(GeoConstants.CRS_WEB_MERCATOR)) {
                mapView?.panTo(point)
            }
        } else {
            Toast.makeText(this, R.string.error_no_location, Toast.LENGTH_SHORT).show()
        }
    }

    fun isPermissionGranted(permission: String): Boolean {
        return ContextCompat.checkSelfPermission(this, permission) == PackageManager.PERMISSION_GRANTED
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        when (requestCode) {
            3 -> locatePosition()
            else -> super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        }
    }


    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.main_menu, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem?): Boolean {
        return when(item?.itemId) {
            R.id.action_signout -> {
                val preferences = PreferenceManager.getDefaultSharedPreferences(this)
                preferences.edit().remove("signed").remove("authorized").apply()
                val app = application as? IGISApplication
                app?.getAccount(SignInActivity.AUTHORITY)?.let { app.removeAccount(it) }
                (app?.map as MapDrawable).delete()
                signin()
                true
            }
            R.id.action_layers -> {
                val layers = arrayOf("Магазины и автоматы", "Кафе и рестораны", "Автобусы")
                val checked = BooleanArray(layers.size)

                val app = application as? IGISApplication
                val map = app?.map as MapDrawable?

                val shops = map?.getLayerByName(SignInActivity.LAYERS[0].second) as Layer
                val vending = map.getLayerByName(SignInActivity.LAYERS[1].second) as Layer
                shops.let { checked[0] = it.isVisible }
                val cafe = map.getLayerByName(SignInActivity.LAYERS[2].second) as Layer
                cafe.let { checked[1] = it.isVisible }
                overlay?.let { checked[2] = it.isVisible }

                val builder = AlertDialog.Builder(this)
                builder.setTitle(R.string.track_list)
                    .setMultiChoiceItems(layers, checked) { _, which, selected ->
                        checked[which] = selected
                    }
                    .setPositiveButton(R.string.ok) { _, _ ->
                        shops.let { it.isVisible = checked[0] }
                        vending.let { it.isVisible = checked[0] }
                        cafe.let { it.isVisible = checked[1] }
                        overlay?.setVisibility(checked[2])
                    }



                if (!authorized) {
                    builder.setNegativeButton(R.string.login, null)
                }
                val dialog = builder.create()
                dialog.show()
                if (!authorized)
                    dialog.listView.isEnabled = false

                true
            }
            else -> super.onOptionsItemSelected(item)
        }


    }





    class SelectFeatureOverlay(context: Context, map: MapViewOverlays) : Overlay(context, map) {
        private var items: MutableList<DrawItem> = arrayListOf()
        private var selectedItem: DrawItem? = null
        var feature: Feature? = null
            set(value) {
                field = value
                mMapViewOverlays.postInvalidate()
            }

        init {
            val outlineColor = ControlHelper.getColor(mContext, R.attr.colorAccent)
            val fillColor = ControlHelper.getColor(mContext, R.attr.colorPrimary)
            val vertexStyle = VertexStyle(mContext, 255, fillColor, 5f, 2.6f,
                fillColor, 5f, 2.6f, outlineColor, 6f, 3f)
            DrawItem.setVertexStyle(vertexStyle)
        }

        override fun draw(canvas: Canvas?, mapDrawable: MapDrawable?) {
            feature?.let {
                fillDrawItems(it.geometry)

                canvas?.let {
                    for (item in items) {
                        val isSelected = selectedItem === item
                        drawItem(item, canvas, isSelected)
                    }
                }
            }
        }

        override fun drawOnPanning(canvas: Canvas?, currentMouseOffset: PointF?) {
            canvas?.let {
                for (item in items) {
                    val isSelected = selectedItem === item
                    val newItem = item.pan(currentMouseOffset)
                    if (isSelected) {
                        newItem.setSelectedRing(selectedItem!!.selectedRingId)
                        newItem.setSelectedPoint(selectedItem!!.selectedPointId)
                    }

                    drawItem(newItem, canvas, isSelected)
                }
            }
        }

        override fun drawOnZooming(canvas: Canvas?, currentFocusLocation: PointF?, scale: Float) {
            canvas?.let {
                for (item in items) {
                    val isSelected = selectedItem === item
                    val newItem = item.zoom(currentFocusLocation, scale)

                    if (isSelected) {
                        newItem.setSelectedRing(selectedItem!!.selectedRingId)
                        newItem.setSelectedPoint(selectedItem!!.selectedPointId)
                    }

                    drawItem(newItem, canvas, isSelected)
                }
            }
        }

        private fun fillDrawItems(geom: GeoGeometry?) {
            val lastItemsCount = items.size
            val lastSelectedItemPosition = items.indexOf(selectedItem)
            val lastSelectedItem = selectedItem
            items.clear()

            if (null == geom) {
                return
            }

            val geoPoints = arrayOfNulls<GeoPoint>(1)
            when (geom.type) {
                GeoConstants.GTPoint -> {
                    geoPoints[0] = geom as GeoPoint?
                    selectedItem = DrawItem(DrawItem.TYPE_VERTEX, mMapViewOverlays.map.mapToScreen(geoPoints))
                    items.add(selectedItem!!)
                }
                GeoConstants.GTMultiPoint -> {
                    val geoMultiPoint = geom as GeoMultiPoint?
                    for (i in 0 until geoMultiPoint!!.size()) {
                        geoPoints[0] = geoMultiPoint.get(i)
                        selectedItem = DrawItem(DrawItem.TYPE_VERTEX, mMapViewOverlays.map.mapToScreen(geoPoints))
                        items.add(selectedItem!!)
                    }
                }
                GeoConstants.GTLineString -> {
                }
                GeoConstants.GTMultiLineString -> {
                }
                GeoConstants.GTPolygon -> {
                }
                GeoConstants.GTMultiPolygon -> {
                }
                GeoConstants.GTGeometryCollection -> {
                }
                else -> {
                }
            }

            if (items.size == lastItemsCount && lastSelectedItem != null && lastSelectedItemPosition != Constants.NOT_FOUND) {
                selectedItem = items[lastSelectedItemPosition]
                selectedItem!!.setSelectedRing(lastSelectedItem.selectedRingId)
                selectedItem!!.setSelectedPoint(lastSelectedItem.selectedPointId)
            } else {
                selectedItem = items[0]
            }
        }

        private fun drawItem(drawItem: DrawItem, canvas: Canvas, isSelected: Boolean) {
            when (feature?.geometry?.type) {
                GeoConstants.GTPoint, GeoConstants.GTMultiPoint -> drawItem.drawPoints(canvas, isSelected)
                GeoConstants.GTLineString, GeoConstants.GTMultiLineString, GeoConstants.GTPolygon, GeoConstants.GTMultiPolygon -> {
                }
                else -> {}
            }
        }
    }


    private fun signin() {
        val intent = Intent(this, SignInActivity::class.java)
        startActivity(intent)
        finish()
    }

}
