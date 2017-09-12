package com.deselmo.android.tambourine

import android.Manifest
import android.annotation.SuppressLint
import android.content.*
import android.content.pm.PackageManager
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.hardware.SensorManager
import android.hardware.SensorEvent
import android.hardware.Sensor
import android.hardware.SensorEventListener
import android.media.MediaPlayer
import android.support.v4.app.ActivityCompat
import android.support.v4.content.LocalBroadcastManager
import android.graphics.PorterDuff
import android.graphics.Rect
import android.media.AudioManager
import android.preference.PreferenceManager
import android.support.v4.content.ContextCompat
import android.view.*

import kotlinx.android.synthetic.main.activity_tambourine.*


class TambourineActivity : AppCompatActivity(), SensorEventListener,
        SharedPreferences.OnSharedPreferenceChangeListener {
    companion object {
        val UPDATE_UI_FILTER: String = "com.deselmo.android.TambourineActivity.UPDATE_UI_FILTER"
        val ACTION: String = "ACTION"
        val START_RECORDING: String = "START_RECORDING"
        val STOP_RECORDING: String = "STOP_RECORDING"

        val UPDATE_RECORDING_STATE: String = "UPDATE_RECORDING_STATE"
        val UPDATE_RECORDING_STATE_VALUE: String = "UPDATE_RECORDING_STATE_VALUE"
    }


    private var audioManager: AudioManager? = null

    private var sensorManager: SensorManager? = null
    private var accelerometer: Sensor? = null
    private var soundLogic: SoundLogic? = null

    private var menuToolbar: Menu? = null

    private var permissionToRecordAccepted = false
    private val permissions = arrayOf(Manifest.permission.RECORD_AUDIO)
    private val REQUEST_RECORD_AUDIO_PERMISSION = 200


    private val broadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            when(intent.extras[ACTION]) {
                START_RECORDING -> startRecordingUpdateUI()

                STOP_RECORDING -> stopRecordingUpdateUI()

                UPDATE_RECORDING_STATE -> {
                    when(intent.extras[UPDATE_RECORDING_STATE_VALUE]) {
                        true -> startRecordingUpdateUI()
                        false -> stopRecordingUpdateUI()
                    }
                }
            }
        }
    }

    override fun onRequestPermissionsResult
            (requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        when (requestCode) {
            REQUEST_RECORD_AUDIO_PERMISSION -> {
                if(grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    permissionToRecordAccepted = true
                    startRecordingSendIntent()
                }
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        ThemeChange.apply(this)
        setContentView(R.layout.activity_tambourine)
        setSupportActionBar(toolbar)

        audioManager = getSystemService(Context.AUDIO_SERVICE) as AudioManager

        sensorManager = getSystemService(SENSOR_SERVICE) as SensorManager?
        accelerometer = sensorManager?.getDefaultSensor(Sensor.TYPE_LINEAR_ACCELERATION)
        soundLogic = SoundLogic(this)

        LocalBroadcastManager.getInstance(this)
                .registerReceiver(broadcastReceiver, IntentFilter(UPDATE_UI_FILTER))

        PreferenceManager.getDefaultSharedPreferences(this)
                .registerOnSharedPreferenceChangeListener(this)

        onSharedPreferenceChanged(PreferenceManager.getDefaultSharedPreferences(this),
                "button_color")

        soundButton.setOnTouchListener { view: View, motionEvent ->
            when(motionEvent.actionMasked) {
                MotionEvent.ACTION_DOWN -> {
                    sensorManager?.registerListener(this, accelerometer,
                            SensorManager.SENSOR_DELAY_FASTEST)
                }

                MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                    sensorManager?.unregisterListener (this, accelerometer)
                }

                MotionEvent.ACTION_MOVE -> {
                    val rect = Rect(view.left, view.top, view.right, view.bottom)
                    if(!rect.contains(view.left + motionEvent.x.toInt(),
                            view.top + motionEvent.y.toInt())){
                        sensorManager?.unregisterListener (this, accelerometer)
                    }
                }
            }
            false
        }
    }

    override fun onResume() {
        super.onResume()
    }

    override fun onPause() {
        super.onPause()

        sensorManager?.unregisterListener (this, accelerometer)
    }


    override fun onDestroy() {
        LocalBroadcastManager.getInstance(this)
                .unregisterReceiver(broadcastReceiver)

        PreferenceManager.getDefaultSharedPreferences(this)
                .unregisterOnSharedPreferenceChangeListener(this)

        super.onDestroy()
    }


    @SuppressLint("PrivateResource")
    override fun onSharedPreferenceChanged(sharedPreferences: SharedPreferences, key: String) {
        println(key)
        if(key == "button_color") {
            val color = when(sharedPreferences.getString("button_color", "theme")) {
                "red" -> R.color.red
                "green" -> R.color.green
                "blue" -> R.color.blue
                "yellow" -> R.color.yellow
                "theme" -> when(sharedPreferences.getString("theme", "light")) {
                    "light" -> R.color.button_material_light
                    "dark" -> R.color.button_material_dark
                    else -> null
                }
                else -> null
            }

            if(color != null) {
                soundButton.background.setColorFilter(ContextCompat
                        .getColor(this, color), PorterDuff.Mode.MULTIPLY)
            }
        }
    }


    override fun onKeyDown(keyCode: Int, event: KeyEvent): Boolean {
        return when (keyCode) {
            KeyEvent.KEYCODE_VOLUME_UP -> {
                audioManager?.adjustStreamVolume(AudioManager.STREAM_MUSIC,
                        AudioManager.ADJUST_RAISE, AudioManager.FLAG_SHOW_UI)
                true
            }
            KeyEvent.KEYCODE_VOLUME_DOWN -> {
                audioManager?.adjustStreamVolume(AudioManager.STREAM_MUSIC,
                        AudioManager.ADJUST_LOWER, AudioManager.FLAG_SHOW_UI)
                true
            }
            else -> super.onKeyDown(keyCode, event)
        }
    }

    override fun onBackPressed() { moveTaskToBack(true) }


    override fun onSaveInstanceState(bundle: Bundle) {
        super.onSaveInstanceState(bundle)
    }

    override fun onRestoreInstanceState(bundle: Bundle) {
        super.onSaveInstanceState(bundle)
    }


    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.menu_tambourine, menu)
        menuToolbar = menu

        return true
    }

    override fun onPrepareOptionsMenu(menu: Menu): Boolean {
        startService(Intent(this, MediaRecorderService::class.java)
                .setAction(MediaRecorderService.GET_RECORDING_STATE))

        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_start_record -> {
                if(permissionToRecordAccepted) {
                    startRecordingSendIntent()
                } else {
                    ActivityCompat.requestPermissions(this, permissions,
                            REQUEST_RECORD_AUDIO_PERMISSION)
                }
                true
            }

            R.id.action_stop_record -> {
                stopRecordingSendIntent()
                true
            }

            R.id.action_play -> {
                startActivity(Intent(this,
                        PlayActivity::class.java))
                true
            }

            R.id.action_settings -> {
                startActivity(Intent(this,
                        SettingsActivity::class.java))
                true
            }

            else -> super.onOptionsItemSelected(item)
        }
    }


    override fun onSensorChanged(event : SensorEvent) {
        if(event.sensor.type == Sensor.TYPE_LINEAR_ACCELERATION) {
            val eventValues = event.values

            val accelerometerModule = Math.sqrt((
                    eventValues[0] * eventValues[0] +
                            eventValues[1] * eventValues[1] +
                            eventValues[2] * eventValues[2]).toDouble()).toFloat()

            soundLogic?.add(accelerometerModule)


            val result = soundLogic?.checkHit()
            if(result != "") {
                println(result)
                var mediaPlayer: MediaPlayer? = MediaPlayer()
                mediaPlayer?.setDataSource(
                        assets.openFd(result).fileDescriptor,
                        assets.openFd(result).startOffset,
                        assets.openFd(result).length)
                mediaPlayer?.setOnPreparedListener {
                    mediaPlayer?.start()
                }
                mediaPlayer?.setOnCompletionListener {
                    mediaPlayer?.release()
                    mediaPlayer = null
                }
                mediaPlayer?.prepareAsync()
            }
        }
    }

    override fun onAccuracyChanged(sensor : Sensor, accuracy : Int) {}


    private fun startRecordingUpdateUI() {
        menuToolbar?.findItem(R.id.action_start_record)?.isVisible = false
        menuToolbar?.findItem(R.id.action_stop_record)?.isVisible = true
    }
    private fun startRecordingSendIntent() {
        startService(Intent(this, MediaRecorderService::class.java)
                .setAction(MediaRecorderService.START))
    }

    private fun stopRecordingUpdateUI() {
        menuToolbar?.findItem(R.id.action_start_record)?.isVisible = true
        menuToolbar?.findItem(R.id.action_stop_record)?.isVisible = false
    }
    private fun stopRecordingSendIntent() {
        startService(Intent(this, MediaRecorderService::class.java)
                .setAction(MediaRecorderService.STOP))
    }
}
