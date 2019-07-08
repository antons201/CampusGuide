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
import android.util.Log
import com.google.gson.Gson
import org.json.JSONArray
import org.json.JSONObject


class BusesService : Service() {
    private var thread: Thread? = null

    val array = arrayListOf<Bus>()

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Thread(Runnable {
            while (!Thread.currentThread().isInterrupted) {
                try {
                    val response = NetworkUtil.get(URL, null, null, false)
                    val data = JSONArray(response.responseBody)
                    for (i in 0 until data.length()) {
                        val item = data.getJSONObject(i)
                        val location = Location(LocationManager.GPS_PROVIDER)
                        location.latitude = item.getDouble("latitude")
                        location.longitude = item.getDouble("longitude")
                        val title = item.getString("id")
                        val type = item.getString("type")
                        if (type != "shuttle") continue
                        var flag = false
                        for (bus in array) {
                            if (bus.title.equals(title)) {
                                bus.location = location
                                flag = true
                                break
                            }
                        }
                        if (flag) continue
                        array.add(Bus(title, location, 0.0))
                    }
                    val notification = Intent("BUSES_UPDATE")
                    notification.putParcelableArrayListExtra("buses", array)
                    sendBroadcast(notification)
                    //Thread.sleep(3000)
                } catch (e: Exception) {
                    Log.i("ERR", e.message)
                }
            }
        }).start()
        return START_STICKY
    }

    /*override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Thread(Runnable {
            while (!Thread.currentThread().isInterrupted) {
                try {
                    val response = NetworkUtil.get(URL, null, null, false)
                    response.responseBody = response.responseBody.replace("\"", "")
                    response.responseBody = response.responseBody.replace("\\", "")
                    response.responseBody = "[${response.responseBody}]"
                    val data = JSONArray(response.responseBody)
                    //val array = arrayListOf<Bus>()
                    for (i in 0 until data.length()) {
                        val item = data.getJSONObject(i)
                        val location = Location(LocationManager.GPS_PROVIDER)
                        val size = item.getJSONArray("coords").length()
                        if (size > 0) {
                            val temp = item.getJSONArray("coords")[size - 1]
                            //val long = ((temp as  JSONObject).getDouble("lon1") / 100)
                            //val lat = temp.getDouble("lat1") / 100
                            //location.longitude = long.toInt() + ((long % 1) * 100 / 60)
                            //location.latitude = lat.toInt() + ((lat % 1) * 100 / 60)
                            location.longitude = (temp as JSONObject).getDouble("lon1")
                            location.latitude = temp.getDouble("lat1")
                            val title = item.getString("imei")
                            Log.i("imei", title)
                            Log.i("long", location.longitude.toString())
                            Log.i("lat", location.latitude.toString())


                            var flag = false
                            for (bus in array) {
                                if (bus.title.equals(title)) {
                                    bus.location = location
                                    flag = true
                                    break
                                }
                            }
                            if (flag) continue
                            array.add(Bus(title, location, 0.0))
                        }
                    }
                    val notification = Intent("BUSES_UPDATE")
                    notification.putParcelableArrayListExtra("buses", array)
                    sendBroadcast(notification)
                    //Thread.sleep(3000)
                } catch (e: Exception) {
                    Log.i("ERR", e.message)
                }
//              пробегаемся по всем координатам
//                try {
//                    var response = NetworkUtil.get(URL, null, null, false)
//                    response.responseBody = response.responseBody.replace("\"", "")
//                    response.responseBody = response.responseBody.replace("\\", "")
//                    response.responseBody = "[${response.responseBody}]"
//                    val data = JSONArray(response.responseBody)
//                    //val array = arrayListOf<Bus>()
//
//                    for (i in 0 until data.length()) {
//                        val item = data.getJSONObject(i)
//                        val location = Location(LocationManager.GPS_PROVIDER)
//                        val size = item.getJSONArray("coords").length()
//                        if(size > 0) {
//                            for (j in 0 until size) {
//
//                                val temp = item.getJSONArray("coords")[j]
//                                val long = ((temp as JSONObject).getDouble("lon1") / 100)
//                                val lat = temp.getDouble("lat1") / 100
//
//
//                                location.longitude = long.toInt() + ((long % 1) * 100 / 60)
//                                location.latitude = lat.toInt() + ((lat % 1) * 100 / 60)
//
//                                val title = item.getInt("imei").toString()
//                                Log.i("imei", title)
//                                Log.i("long", long.toString())
//                                Log.i("lat", lat.toString())
//
//                                var q = 0
//                                var size2 = array.size
//                                while (q != size2) {
//                                    if (array[q].title.equals(title)) {
//                                        array.removeAt(q)
//                                    }
//                                }
//                                array.add(Bus(title, location, 0.0))
//                                Thread.sleep(10)
//                                val notification = Intent("BUSES_UPDATE")
//                                notification.putParcelableArrayListExtra("buses", array)
//                                sendBroadcast(notification)
//                            }
//                        }
//                    }
//
//                    //Thread.sleep(3000)
//                } catch (e: Exception) {
//                    Log.i("ERR", e.message)
//                }
            }
        }).start()
        return START_STICKY
    }*/

    override fun onDestroy() {
        super.onDestroy()
        thread?.interrupt()
    }

    override fun onBind(p0: Intent?): IBinder? {
        return null
    }

    companion object {
        //const val URL = "http://194.213.97.46:8118/campus-gid/common"
        const val URL = "https://dvfu.dewish.ru/map/api/"
    }

}

class
BusesOverlay(context: Context, map: MapViewOverlays) : Overlay(context, map) {
    var buses: MutableList<Bus> = arrayListOf()
    var items: MutableList<OverlayItem> = arrayListOf()
    val marker = BitmapFactory.decodeResource(mContext.resources, R.drawable.ic_bus_grey600_18dp)


    fun defaultColor() {
        for (item in items)
            item.marker = marker
    }


    fun selectBus(envelope: GeoEnvelope): Bus? {
        for (i in 0 until items.size) {
            val coordinates = items[i].getCoordinates(GeoConstants.CRS_WEB_MERCATOR)
            if (envelope.contains(coordinates)) {
                val marker = marker.copy(Bitmap.Config.ARGB_8888, true)
                items[i].marker = applyColorFilter(marker)
                mMapViewOverlays.postInvalidate()
                return buses[i]
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
