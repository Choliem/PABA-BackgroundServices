package com.example.backgroundservices

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.provider.CalendarContract
import android.widget.Button
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import java.util.Calendar

class MainActivity : AppCompatActivity() {

    private val permissionsToRequest = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        arrayOf(
            Manifest.permission.ACTIVITY_RECOGNITION,
            Manifest.permission.POST_NOTIFICATIONS,
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION
        )
    } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
        arrayOf(
            Manifest.permission.ACTIVITY_RECOGNITION,
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION
        )
    } else {
        arrayOf(
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION
        )
    }

    private lateinit var statusTextView: TextView
    private lateinit var btnStart: Button
    private lateinit var btnExercise: Button
    private var startExerciseAfterPermission = false

    companion object {
        const val ACTION_SERVICE_STOPPED_JUMP =
            "com.example.backgroundservices.ACTION_SERVICE_STOPPED_JUMP"

        const val ACTION_EXERCISE_FINISHED =
            "com.example.backgroundservices.ACTION_EXERCISE_FINISHED"

        const val EXTRA_JUMP_COUNT = "jump_count"

        const val REQUEST_CODE_PERMISSIONS = 101
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)

        statusTextView = findViewById(R.id.statusTextView)
        btnStart = findViewById(R.id.btnStart)
        btnExercise = findViewById(R.id.btnExercise)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        btnStart.setOnClickListener {
            startExerciseAfterPermission = false

            if (hasAllPermissions()) {
                startTrackerService()
            } else {
                ActivityCompat.requestPermissions(
                    this,
                    permissionsToRequest,
                    REQUEST_CODE_PERMISSIONS
                )
            }
        }

        btnExercise.setOnClickListener {
            startExerciseAfterPermission = true

            if (hasAllPermissions()) {
                startExerciseService()
            } else {
                ActivityCompat.requestPermissions(
                    this,
                    permissionsToRequest,
                    REQUEST_CODE_PERMISSIONS
                )
            }
        }

        handleIntent(intent)
    }

    private fun hasAllPermissions(): Boolean {
        return permissionsToRequest.all {
            checkSelfPermission(it) == PackageManager.PERMISSION_GRANTED
        }
    }

    private fun startTrackerService() {
        val serviceIntent = Intent(this, TrackerService::class.java)
        startForegroundService(serviceIntent)
        statusTextView.text = "Background service sedang berjalan...."
    }

    private fun startExerciseService() {
        val serviceIntent = Intent(this, TrackerService::class.java)
        serviceIntent.putExtra(TrackerService.EXTRA_EXERCISE_MODE, true)
        startForegroundService(serviceIntent)
        finish()
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        if (requestCode == REQUEST_CODE_PERMISSIONS) {
            var allPermissionsGranted = true

            for (result in grantResults) {
                if (result != PackageManager.PERMISSION_GRANTED) {
                    allPermissionsGranted = false
                    break
                }
            }

            if (allPermissionsGranted) {
                if (startExerciseAfterPermission) {
                    startExerciseService()
                } else {
                    val serviceIntent = Intent(this, TrackerService::class.java)
                    startForegroundService(serviceIntent)
                    finish()
                }
            } else {
                statusTextView.text = "Izin ditolak, service tidak dapat dijalankan."
            }
        }
    }

    private fun handleIntent(intent: Intent?) {
        if (intent?.action == ACTION_SERVICE_STOPPED_JUMP) {
            statusTextView.text =
                "Background services sudah berhenti karena Anda melompat"
        }

        if (intent?.action == ACTION_EXERCISE_FINISHED) {
            val jumpCount = intent.getIntExtra(EXTRA_JUMP_COUNT, 0)

            statusTextView.text =
                "Latihan selesai\nJumlah lompatan: $jumpCount"

            createCalendarEvent(jumpCount)
        }
    }

    private fun createCalendarEvent(jumpCount: Int) {
        val startTime = Calendar.getInstance()
        val endTime = Calendar.getInstance()
        endTime.add(Calendar.HOUR, 1)

        val calendarIntent = Intent(Intent.ACTION_INSERT).apply {
            data = CalendarContract.Events.CONTENT_URI
            putExtra(CalendarContract.Events.TITLE, "Latihan Hari Ini")
            putExtra(
                CalendarContract.Events.DESCRIPTION,
                "Jumlah lompatan: $jumpCount"
            )
            putExtra(
                CalendarContract.EXTRA_EVENT_BEGIN_TIME,
                startTime.timeInMillis
            )
            putExtra(
                CalendarContract.EXTRA_EVENT_END_TIME,
                endTime.timeInMillis
            )
        }

        if (calendarIntent.resolveActivity(packageManager) != null) {
            startActivity(calendarIntent)
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleIntent(intent)
    }
}