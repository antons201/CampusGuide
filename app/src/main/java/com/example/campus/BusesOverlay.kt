package com.example.campus


import android.content.Context
import android.graphics.*
import com.nextgis.maplib.datasource.GeoEnvelope
import com.nextgis.maplib.map.MapDrawable
import com.nextgis.maplib.util.GeoConstants
import com.nextgis.maplib.util.NetworkUtil
import com.nextgis.maplibui.api.Overlay
import com.nextgis.maplibui.api.OverlayItem
import com.nextgis.maplibui.mapui.MapViewOverlays
import android.app.Service
import android.content.Intent
import android.location.Location
import android.location.LocationManager
import android.os.IBinder
import org.json.JSONArray

class BusesOverlay(context: Context, map: MapViewOverlays) : Overlay(context, map) {
    private var items: MutableList<OverlayItem> = arrayListOf()
    private val marker = BitmapFactory.decodeResource(mContext.resources, R.drawable.ic_bus_grey600_18dp)


    fun defaultColor() {
        for (item in items)
            item.marker = marker
    }



    fun selectBus(envelope: GeoEnvelope): OverlayItem? {
        for (i in 0 until items.size) {
            val coordinates = items[i].getCoordinates(GeoConstants.CRS_WEB_MERCATOR)
            if (envelope.contains(coordinates)) {
                val marker = marker.copy(Bitmap.Config.ARGB_8888, true)
                items[i].marker = applyColorFilter(marker)
                mMapViewOverlays.postInvalidate()
                return items[i]
            }
        }
        return null
    }

    private fun applyColorFilter(marker: Bitmap): Bitmap {
        val canvas = Canvas(marker)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG)
        val filter = PorterDuffColorFilter(Color.BLUE, PorterDuff.Mode.SRC_ATOP)
        paint.colorFilter = filter
        canvas.drawBitmap(marker, 0f, 0f, paint)
        return marker
    }



    class BusesService : Service() {
        private var thread: Thread? = null

        override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
            Thread(Runnable {
                while (!Thread.currentThread().isInterrupted) {
                    try {
                        val response = NetworkUtil.get(URL, null, null, false)
                        val data = JSONArray(response.responseBody)
                        val array = arrayListOf<Bus>()
                        for (i in 0 until data.length()) {
                            val item = data.getJSONObject(i)
                            val location = Location(LocationManager.GPS_PROVIDER)
                            location.longitude = item.getDouble("lon")
                            location.latitude = item.getDouble("lat")
                            val title = item.getString("title")
                            val congestion = item.getDouble("congestion")
                            array.add(Bus(title, location, congestion))
                        }
                        val notification = Intent("BUSES_UPDATE")
                        notification.putParcelableArrayListExtra("buses", array)
                        sendBroadcast(notification)
                        Thread.sleep(3000)
                    } catch (e: InterruptedException) {
                    }
                }
            }).start()
            return START_STICKY
        }

        override fun onDestroy() {
            super.onDestroy()
            thread?.interrupt()
        }

        override fun onBind(p0: Intent?): IBinder? {
            return null
        }

        companion object {
            const val URL = "http://ms.4ert.com/buses"
        }

    }
    override fun draw(canvas: Canvas?, mapDrawable: MapDrawable?) {
        for (i in 0 until items.size) {
            drawOverlayItem(canvas, items[i])
        }
    }


    override fun drawOnPanning(canvas: Canvas?, currentMouseOffset: PointF?) {
        for (item in items) {
            drawOnPanning(canvas, currentMouseOffset, item)
        }
    }

    override fun drawOnZooming(canvas: Canvas?, currentFocusLocation: PointF?, scale: Float) {
        for (item in items) {
            drawOnZooming(canvas, currentFocusLocation, scale, item, false)
        }
    }

}
