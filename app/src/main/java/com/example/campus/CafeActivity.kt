package com.example.campus

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.support.design.widget.BottomSheetDialog
import android.support.design.widget.FloatingActionButton
import android.support.v7.app.AppCompatActivity
import android.view.View
import android.widget.TextView
import com.nextgis.maplib.api.IGISApplication
import com.nextgis.maplib.map.MapDrawable
import com.nextgis.maplib.map.VectorLayer


class CafeActivity : AppCompatActivity() {
    private var phone: String = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_cafe)

        val layerId = intent.getIntExtra("layer_id", -1)
        val featureId = intent.getLongExtra("feature_id", -1)
        val app = application as? IGISApplication

        ((app?.map as MapDrawable?)?.getLayerById(layerId) as? VectorLayer)?.let {
            it.getFeature(featureId)?.let { feature ->
                title = feature.getFieldValueAsString("title")
                phone = feature.getFieldValueAsString("phone")
                supportActionBar?.subtitle = feature.getFieldValueAsString("category")
                findViewById<TextView>(R.id.description).text = feature.getFieldValueAsString("description")
                findViewById<TextView>(R.id.menu).text = feature.getFieldValueAsString("menu")
            }
        }

        findViewById<FloatingActionButton>(R.id.fab).setOnClickListener { dialog() }
    }

    private fun dialog() {
        val view = layoutInflater.inflate(R.layout.action_sheet, null)
        val dialog = BottomSheetDialog(this)
        dialog.setContentView(view)
        val review = view.findViewById<TextView>(R.id.review)
        val call = view.findViewById<TextView>(R.id.call)
        val route = view.findViewById<TextView>(R.id.route)
        val order = view.findViewById<TextView>(R.id.order)
        review.setOnClickListener {
            showForm(3)
            dialog.dismiss()
        }
        if (phone.isBlank())
            call.visibility = View.GONE
        call.setOnClickListener {
            val callIntent = Intent(Intent.ACTION_DIAL)
            callIntent.data = Uri.parse("tel:$phone")
            startActivity(callIntent)
            dialog.dismiss()
        }
        route.setOnClickListener {
            dialog.dismiss()
        }
        order.setOnClickListener {
            showForm(4)
            dialog.dismiss()
        }
        dialog.show()
    }

    private fun showForm(id: Int) {


    }
}

