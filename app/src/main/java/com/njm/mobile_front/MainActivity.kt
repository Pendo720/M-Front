package com.njm.mobile_front

import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Build
import android.os.Bundle
import android.view.View
import android.view.WindowInsets
import android.view.WindowManager
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import java.util.*

class MainActivity : AppCompatActivity() {
    private lateinit var mSensorMan : SensorManager
    val sensorType: Int = Sensor.TYPE_PROXIMITY
    private lateinit var txtView: TextView
    private lateinit var postBtn: Button

    private val mListener: SensorEventListener = object : SensorEventListener {
        @RequiresApi(api = Build.VERSION_CODES.N)
        override fun onSensorChanged(event: SensorEvent) {
            if (event.sensor.type == sensorType) {
                /* update the display to show the new values */
                if (event.values[0] == 0F) {
                    txtView.text = generateReg()
                    postBtn.visibility = View.VISIBLE
                }
            }
        }

        override fun onAccuracyChanged(sensor: Sensor, accuracy: Int) {}
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        @Suppress("DEPRECATION")
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            window.insetsController?.hide(WindowInsets.Type.statusBars())
        } else {
            window.setFlags(
                WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN
            )
        }

        setContentView(R.layout.activity_main)
        mSensorMan = (getSystemService(android.content.Context.SENSOR_SERVICE) as SensorManager?)!!
        txtView = findViewById(R.id.textView)
        postBtn = findViewById(R.id.postText)
        postBtn.setOnClickListener { processEvent() }
        postBtn.visibility = View.GONE
    }

    override fun onPause() {
        super.onPause()
        mSensorMan.unregisterListener(mListener)
    }

    override fun onResume() {
        super.onResume()
        mSensorMan.registerListener(mListener, mSensorMan.getDefaultSensor(sensorType), SensorManager.SENSOR_DELAY_GAME)
    }

    private fun generateReg(): String? {
        val random = Random()
        var toReturn: String? = ""
        for (t in 0..5) {
            if (t == 2) {
                toReturn += String.format(Locale.UK, "%02d ", Math.abs(random.nextInt() % 20))
            } else {
                var c = '\n'
                while (!Character.isAlphabetic(c.toInt())) {
                    c = (51 + random.nextInt(70) % 50).toChar()
                    if (Character.isAlphabetic(c.toInt())) {
                        toReturn += Character.toUpperCase(c)
                        random.setSeed(Date().time)
                        break
                    }
                }
            }
        }
        return toReturn
    }

    private fun processEvent(){
        Toast.makeText(this, txtView.getText(), Toast.LENGTH_LONG).show()
        txtView.setText("")
        postBtn.visibility = View.GONE
    }
}