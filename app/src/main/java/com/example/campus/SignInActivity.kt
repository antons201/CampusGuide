package com.example.campus



import android.Manifest
import android.app.ProgressDialog
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import android.preference.PreferenceManager
import android.support.v4.app.ActivityCompat
import android.support.v4.content.ContextCompat
import android.support.v7.app.AppCompatActivity
import android.widget.Button
import android.widget.Toast
import com.nextgis.maplib.api.IGISApplication
import com.nextgis.maplib.display.FieldStyleRule
import com.nextgis.maplib.display.RuleFeatureRenderer
import com.nextgis.maplib.display.SimpleFeatureRenderer
import com.nextgis.maplib.display.SimpleMarkerStyle
import com.nextgis.maplib.map.MapDrawable
import com.nextgis.maplib.map.NGWVectorLayer
import com.nextgis.maplib.map.VectorLayer
import com.nextgis.maplib.util.Constants
import com.nextgis.maplib.util.PermissionUtil
import com.nextgis.maplibui.fragment.NGWSettingsFragment
import com.nextgis.maplibui.service.LayerFillService

class SignInActivity : AppCompatActivity() {
    private var receiver: BroadcastReceiver? = null
    private var total = LAYERS.size
    private var dialog: ProgressDialog? = null
    private var authorized = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_signin)

        findViewById<Button>(R.id.skip).setOnClickListener { load() }
        findViewById<Button>(R.id.signin).setOnClickListener { load(true) }
    }


    private fun signin() {
        val preferences = PreferenceManager.getDefaultSharedPreferences(this)
        preferences.edit().putBoolean("authorized", authorized).apply()
        preferences.edit().putBoolean("signed", true).apply()
        val intent = Intent(this, MainActivity::class.java)
        startActivity(intent)
        finish()
    }


    private fun requestPermission() {
        val permissions = arrayOf(Manifest.permission.GET_ACCOUNTS, Manifest.permission.WRITE_SYNC_SETTINGS)
        ActivityCompat.requestPermissions(this, permissions, PERMISSIONS_CODE)
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        var granted = requestCode == PERMISSIONS_CODE
        for (result in grantResults)
            if (result != PackageManager.PERMISSION_GRANTED)
                granted = false

        if (granted)
            load(authorized)
        else
            Toast.makeText(this, R.string.error_auth, Toast.LENGTH_SHORT).show()
    }

    private fun load(authorized: Boolean = false) {
        this.authorized = authorized


        if (!PermissionUtil.hasPermission(this, Manifest.permission.WRITE_SYNC_SETTINGS)
            || !PermissionUtil.hasPermission(this, Manifest.permission.GET_ACCOUNTS)
        ) {
            requestPermission()
            return
        }

        dialog = ProgressDialog(this)
        dialog?.isIndeterminate = true
        dialog?.setCancelable(false)
        dialog?.setMessage(getString(R.string.message_loading))
        dialog?.show()

        val fullUrl = FULL_URL
        val accountName = AUTHORITY
        val app = application as? IGISApplication
        app?.addAccount(accountName, fullUrl, "student", "student1" , "ngw")?.let {
            if (!it) {
                Toast.makeText(this, R.string.error_auth, Toast.LENGTH_SHORT).show()
                app.getAccount(accountName)?.let { account -> app.removeAccount(account) }
                dialog?.dismiss()
                return
            } else {
                app.getAccount(accountName)?.let { account ->
                    NGWSettingsFragment.setAccountSyncEnabled(account, app.authority, true)
                }
                layers()
            }
        }
    }
    private fun layers() {
        receiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context, intent: Intent) {
                if (intent.action?.equals(LayerFillService.ACTION_STOP) == true) {
                    Toast.makeText(this@SignInActivity, R.string.canceled, Toast.LENGTH_SHORT).show()
                    clear()
                    return
                }

                val serviceStatus = intent.getShortExtra(LayerFillService.KEY_STATUS, 0)
                when (serviceStatus) {
                    LayerFillService.STATUS_STOP -> {
                        total--
                        if (total <= 0)
                            style()
                    }
                }
            }
        }

        val intentFilter = IntentFilter(LayerFillService.ACTION_UPDATE)
        intentFilter.addAction(LayerFillService.ACTION_STOP)
        registerReceiver(receiver, intentFilter)

        val intent = Intent(this, LayerFillService::class.java)
        intent.action = LayerFillService.ACTION_ADD_TASK

        val app = application as? IGISApplication
        val map = app?.map as MapDrawable?
        val accountName = AUTHORITY

        for (layer in LAYERS) {
            val uri = Uri.parse(Uri.decode(layer.first))
            val id = uri.lastPathSegment?.toLongOrNull()
            intent.putExtra(LayerFillService.KEY_REMOTE_ID, id)
            intent.putExtra(LayerFillService.KEY_ACCOUNT, accountName)
            intent.putExtra(LayerFillService.KEY_NAME, layer.second)
            intent.putExtra(LayerFillService.KEY_LAYER_GROUP_ID, map?.id)
            intent.putExtra(LayerFillService.KEY_INPUT_TYPE, LayerFillService.NGW_LAYER)
            intent.putExtra(LayerFillService.KEY_URI, uri)
            ContextCompat.startForegroundService(this, intent)
        }


    }
    private fun clear() {
            val app = application as? FEFUApplication
            (app?.map as MapDrawable).delete()
            app.addLayer()
            total = LAYERS.size
        }

        override fun onDestroy() {
            super.onDestroy()
            try {
                receiver?.let { unregisterReceiver(it) }
            } catch (e: Exception) {
            }
        }

    private fun style() {

        val style = SimpleMarkerStyle.MarkerStyleCircle
        val cafeStyle = SimpleMarkerStyle(Color.GREEN, Color.BLACK, 6f, style)
        val app = application as? FEFUApplication
        val map = app?.map as MapDrawable
        val cafe = map.getLayerByName(LAYERS[2].second) as NGWVectorLayer
        cafe.syncType = Constants.SYNC_ALL
        app.addLayer()
        cafe.renderer = SimpleFeatureRenderer(cafe, cafeStyle)
        cafe.save()
        val vending = map.getLayerByName(LAYERS[1].second) as NGWVectorLayer
        vending.syncType = Constants.SYNC_ALL
        vending.save()
        val services = map.getLayerByName(LAYERS[3].second) as NGWVectorLayer
        services.syncType = Constants.SYNC_ALL
        services.save()
        val drugstore = map.getLayerByName(LAYERS[4].second) as NGWVectorLayer
        drugstore.syncType = Constants.SYNC_ALL
        drugstore.save()
        val amenities = map.getLayerByName(LAYERS[5].second) as NGWVectorLayer
        amenities.syncType = Constants.SYNC_ALL
        amenities.save()
        val shop = map.getLayerByName(LAYERS[0].second) as NGWVectorLayer
        shop.syncType = Constants.SYNC_ALL
//        val shopStyle = FieldStyleRule(shop)
//        shopStyle.key = "category_id"
//        val groceryStyle = SimpleMarkerStyle(Color.LTGRAY, Color.BLACK, 5f, style)
//        shopStyle.setStyle("1", groceryStyle)
//        val supermarketStyle = SimpleMarkerStyle(Color.GRAY, Color.BLACK, 5f, style)
//        shopStyle.setStyle("2", supermarketStyle)
//        val pharmacyStyle = SimpleMarkerStyle(Color.MAGENTA, Color.BLACK, 5f, style)
//        shopStyle.setStyle("3", pharmacyStyle)
//        shop.renderer = RuleFeatureRenderer(shop, shopStyle, groceryStyle)
        shop.save()

        signin()

    }
    companion object {

        const val AUTHORITY = "194.213.97.46:8080"
        const val FULL_URL = "http://$AUTHORITY"
        const val PERMISSIONS_CODE = 47


        const val INSTANCE = "http://${AUTHORITY}/resource/"
        val LAYERS = arrayListOf(
            Pair("$INSTANCE/38", "Магазины"),
            Pair("$INSTANCE/4", "Вендинговые автоматы"),
            Pair("$INSTANCE/23", "Кафе и рестораны"),
            Pair("$INSTANCE/7", "Сервисы"),
            Pair("$INSTANCE/41", "Аптеки"),
            Pair("$INSTANCE/48", "Услуги")

        )

    }

}
