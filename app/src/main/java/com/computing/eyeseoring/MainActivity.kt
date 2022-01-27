package com.computing.eyeseoring

import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener2
import android.hardware.SensorManager
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.LayoutInflater
import android.webkit.WebChromeClient
import android.webkit.WebViewClient
import com.computing.eyeseoring.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity(), SensorEventListener2 {
    companion object {
        val WEB_CLIENT_URL = "http://10.0.2.2:63343/tensorflow_sample/index.html"
    }

    private var sensorManger: SensorManager? = null
    private lateinit var binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(LayoutInflater.from(this))
        setContentView(binding.root)

        binding.webView.apply {
            webChromeClient = WebChromeClient()
            webViewClient = WebViewClient()
            loadUrl(WEB_CLIENT_URL)
            settings.javaScriptEnabled = true
        }
        sensorManger = getSystemService(SENSOR_SERVICE) as? SensorManager
    }

    override fun onResume() {
        super.onResume()
        sensorManger?.apply {
            getSensorList(Sensor.TYPE_LIGHT).let { sensorsList ->
                if (sensorsList.isNotEmpty()) {
                    val sensor = sensorsList[0]
                    this.registerListener(this@MainActivity, sensor, SensorManager.SENSOR_DELAY_UI)
                }
            }
        }
    }

    override fun onFlushCompleted(p0: Sensor?) { /*Do nothing*/
    }

    override fun onAccuracyChanged(p0: Sensor?, p1: Int) { /*Do nothing*/
    }

    override fun onSensorChanged(event: SensorEvent?) {
        event?.let {
            if (event.sensor.type == Sensor.TYPE_LIGHT) {
                val brightness = event.values[0]

                when {
                    brightness < 150 -> {
                        binding.mainText.text = "暗いよ"
                    }
                }
            }
        }
    }

    override fun onStop() {
        super.onStop()
        sensorManger?.unregisterListener(this)
    }


}