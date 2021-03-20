package com.njm.mobile_front

import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.View
import android.view.WindowInsets
import android.view.WindowManager
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.res.ResourcesCompat
import androidx.lifecycle.ViewModelProvider
import com.njm.mobile_front.ui.FacadeModelView
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject
import java.io.IOException
import java.util.*

class MainActivity : AppCompatActivity() {
    val LOCAL_URL = "http://192.168.1.108:8080"
    val LOCAL_POST_URL = "$LOCAL_URL/create"

    private lateinit var mSensorMan : SensorManager
    val sensorType: Int = Sensor.TYPE_PROXIMITY
    private lateinit var txtView: TextView
    private lateinit var postBtn: Button

    var mData: String? = null;
    var mFacade: FacadeModelView? = null
    private var mClient: OkHttpClient? = null

    private val mListener: SensorEventListener = object : SensorEventListener {
        @RequiresApi(api = Build.VERSION_CODES.N)
        override fun onSensorChanged(event: SensorEvent) {
            if (event.sensor.type == sensorType) {
                /* update the ui to show the changes */
                if (event.values[0] == 0F) {
                    mData = generateReg()
                    mFacade?.getFacade()?.postValue(mData)
                    postBtn.visibility = View.VISIBLE
                }
            }
        }

        override fun onAccuracyChanged(sensor: Sensor, accuracy: Int) {}
    }

    @RequiresApi(Build.VERSION_CODES.N)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Objects.requireNonNull(supportActionBar)!!.setDisplayHomeAsUpEnabled(true)
        supportActionBar!!.setHomeAsUpIndicator(ResourcesCompat.getDrawable(resources, R.mipmap.ic_launcher, null))
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
        mFacade = ViewModelProvider(this)[FacadeModelView::class.java]
        this.mFacade!!.getFacade().observe(
            this,
            {
                this.mData?.let { it1 -> this.txtView.text = it1 }
            })
        this.mFacade!!.getFacade().postValue(mData)
        mClient = OkHttpClient()
    }

    override fun onPause() {
        super.onPause()
        mSensorMan.unregisterListener(mListener)
    }

    override fun onResume() {
        super.onResume()
        mSensorMan.registerListener(
                mListener,
                mSensorMan.getDefaultSensor(sensorType),
                SensorManager.SENSOR_DELAY_GAME
        )
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

    @RequiresApi(Build.VERSION_CODES.N)
    private fun processEvent(){
        var txt = txtView.getText().toString()
        Thread {
            try {
                postMessage(txt)
            } catch (e: IOException) {
                e.printStackTrace()
            } catch (e: JSONException) {
                e.printStackTrace()
            }
        }.start()
    }

    @RequiresApi(api = Build.VERSION_CODES.N)
    @Throws(IOException::class, JSONException::class)
    private fun requestItem() {
        val request: Request = Request.Builder()
            .url(LOCAL_URL + if (0 > 0) "/get/" + 0 else "/all")
            .build()
        mClient?.newCall(request)?.execute().use { response ->
            val toReturn: String = Objects.requireNonNull(response?.body)!!.string()
            handleResponse("requestAll", toReturn)
        }
    }

    @RequiresApi(api = Build.VERSION_CODES.N)
    @Throws(IOException::class, JSONException::class)
    private fun postMessage(txt: String) {
        val body: RequestBody = txt.toRequestBody("application/text; charset=utf-8".toMediaType())
        val request: Request = Request.Builder()
            .url(LOCAL_POST_URL)
            .post(body)
            .build()
        mClient?.newCall(request)!!.execute().use { response ->
            Objects.requireNonNull(response.body)?.let {
                handleResponse(
                        "postMessage",
                        it.string()
                )
            }
        }
    }

    @RequiresApi(api = Build.VERSION_CODES.N)
    @Throws(JSONException::class)
    private fun handleResponse(src: String, response: String) {
        val builder = StringBuilder()
        val all: MutableList<String?> = ArrayList()
        if (src == "requestAll") {
            val items = JSONArray(response)[0] as JSONArray
            for (c in 0 until items.length()) {
                all.add(items[c].toString())
            }
            // ensure the last inserted item is brought to the front of the list
            Collections.reverse(all)
            builder.append(all.stream().reduce(
                    ""
            ) { s: String?, t: String? ->
                """
                $s
                $t
                """.trimIndent()
            })
        }
        if (src == "requestItem") {
            builder.append(JSONObject(response).toString()).append('\n')
        }
        if (src == "postMessage") {
            builder.append(response)
        }

        runOnUiThread(Runnable {
            mData = ""
            mFacade?.getFacade()?.postValue(mData)
            postBtn.visibility = View.GONE
            Toast.makeText(this, response, Toast.LENGTH_LONG).show()
        });

        Log.i(resources.getString(R.string.app_name), "processResponse: $builder")
    }
}