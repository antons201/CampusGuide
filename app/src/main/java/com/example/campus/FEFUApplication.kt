package com.example.campus

import android.net.Uri
import android.os.Handler
import com.nextgis.maplib.util.NGException
import com.nextgis.maplibui.GISApplication
import com.nextgis.maplibui.mapui.RemoteTMSLayerUI
import java.io.IOException

class FEFUApplication : GISApplication() {
    override fun onCreate() {
        super.onCreate()
        if (mMap.getLayerByName("OSM") == null) {
            addLayer()
        }
    }

     fun addLayer() {
        val layer = RemoteTMSLayerUI(this, mMap.createLayerStorage())
        layer.url = "http://{a,b,c}.tile.openstreetmap.org/{z}/{x}/{y}.png"
        layer.tmsType = 2
        layer.minZoom = 2f
        layer.maxZoom = 19f
        layer.name = "OSM"
        layer.isVisible = true
        mMap.addLayer(layer)
        mMap.moveLayer( 0, layer)
        mMap.save()


         Handler().post {
             try {
                 layer.fillFromZip(Uri.parse("android.resource://" + packageName + "/" + R.raw.mapnik), null)
             } catch (e: IOException) {
                 e.printStackTrace()
             } catch (e: NGException) {
                 e.printStackTrace()
             } catch (e: RuntimeException) {
                 e.printStackTrace()
             }
         }

     }

    override fun getAuthority(): String {
        return AUTHORITY
    }

    override fun showSettings(setting: String?) {
    }

    override fun sendEvent(category: String?, action: String?, label: String?) {
    }

    override fun sendScreen(name: String?) {
    }

    override fun getAccountsType(): String {
        return ACCOUNT_TYPE
    }

    companion object {
        const val ACCOUNT_TYPE = "fefu_account"
        const val AUTHORITY = "com.nextgis.demo.provider"
        const val OSM = "osm_layer"
    }
}