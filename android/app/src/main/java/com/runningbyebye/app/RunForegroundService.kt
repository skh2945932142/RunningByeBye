package com.runningbyebye.app

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import android.os.PowerManager
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import java.io.File
import java.util.concurrent.Executors

class RunForegroundService : Service() {
    private val worker = Executors.newSingleThreadExecutor()

    @Volatile
    private var runner: mobile.Runner? = null

    @Volatile
    private var currentOpenID: String? = null

    @Volatile
    private var stopRequested = false

    private var wakeLock: PowerManager.WakeLock? = null

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START -> startRunFromIntent(intent)
            ACTION_STOP -> stopCurrentRun()
        }
        return START_NOT_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        releaseWakeLock()
        worker.shutdownNow()
        super.onDestroy()
    }

    private fun startRunFromIntent(intent: Intent) {
        val fieldName = intent.getStringExtra(EXTRA_FIELD_NAME).orEmpty()
        startForegroundCompat(buildNotification("跑步启动中", fieldName, true))

        if (currentOpenID != null) {
            updateNotification("跑步已在进行中", fieldName, true)
            return
        }

        val openID = intent.getStringExtra(EXTRA_OPEN_ID).orEmpty()
        val fieldCode = intent.getStringExtra(EXTRA_FIELD_CODE).orEmpty()
        val pace = intent.getStringExtra(EXTRA_PACE).orEmpty()
        val interval = intent.getStringExtra(EXTRA_INTERVAL).orEmpty()
        val dataDirPath = intent.getStringExtra(EXTRA_DATA_DIR).orEmpty()
        val pointsDirPath = intent.getStringExtra(EXTRA_POINTS_DIR).orEmpty()

        if (openID.isEmpty() || fieldCode.isEmpty() || dataDirPath.isEmpty() || pointsDirPath.isEmpty()) {
            val message = "启动参数不完整"
            RunServiceState.update(RunStatus.Failed(message))
            updateNotification("启动失败", message, false)
            cleanupAndStop()
            return
        }

        currentOpenID = openID
        stopRequested = false
        acquireWakeLock()
        RunServiceState.update(RunStatus.Running(fieldName))
        updateNotification("跑步中", fieldName, true)

        worker.execute {
            try {
                val dataDir = File(dataDirPath)
                dataDir.mkdirs()
                PointAssetCopier.copy(
                    pointsDir = File(dataDir, "points"),
                    listAssets = { path -> assets.list(path) },
                    openAsset = { path -> assets.open(path) },
                )

                val serviceRunner = mobile.Mobile.newRunner(dataDir.absolutePath)
                runner = serviceRunner
                serviceRunner.startRun(
                    openID,
                    fieldCode,
                    pace,
                    interval,
                    pointsDirPath,
                    serviceCallback(fieldName),
                )
            } catch (e: Exception) {
                val message = e.message ?: e.toString()
                RunServiceState.update(RunStatus.Failed(message))
                updateNotification("启动失败", message, false)
                cleanupAndStop()
            }
        }
    }

    private fun stopCurrentRun() {
        if (stopRequested) {
            return
        }

        val openID = currentOpenID
        stopRequested = true
        updateNotification("正在停止跑步", "正在结束并提交跑步记录", true)

        if (openID == null) {
            RunServiceState.update(RunStatus.Stopped())
            cleanupAndStop()
            return
        }

        val activeRunner = runner
        if (activeRunner == null) {
            RunServiceState.update(RunStatus.Stopped())
            cleanupAndStop()
            return
        }

        activeRunner.stopRun(openID)
    }

    private fun serviceCallback(fieldName: String) = object : mobile.RunCallback {
        override fun onProgress(submitted: Int, total: Int, mileage: Double) {
            val percent = if (total > 0) submitted * 100 / total else 0
            RunServiceState.update(RunStatus.Progress(submitted, total, mileage))
            updateNotification(
                "跑步中 $percent%",
                "$fieldName · ${String.format("%.3f", mileage)} km · $submitted/$total",
                true,
            )
        }

        override fun onCompleted(mileage: Double) {
            if (stopRequested) {
                RunServiceState.update(RunStatus.Stopped())
                updateNotification("跑步已停止", "记录已提交", false)
            } else {
                RunServiceState.update(RunStatus.Completed(mileage))
                updateNotification("跑步完成", "${String.format("%.3f", mileage)} km", false)
            }
            cleanupAndStop()
        }

        override fun onFailed(errMsg: String) {
            RunServiceState.update(RunStatus.Failed(errMsg))
            updateNotification("跑步失败", errMsg, false)
            cleanupAndStop()
        }
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
            return
        }

        val channel = NotificationChannel(
            CHANNEL_ID,
            getString(R.string.run_notification_channel_name),
            NotificationManager.IMPORTANCE_LOW,
        ).apply {
            description = getString(R.string.run_notification_channel_description)
            setShowBadge(false)
        }
        val manager = getSystemService(NotificationManager::class.java)
        manager.createNotificationChannel(channel)
    }

    private fun buildNotification(title: String, text: String, ongoing: Boolean): Notification {
        val contentIntent = PendingIntent.getActivity(
            this,
            0,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        val stopIntent = PendingIntent.getService(
            this,
            1,
            Intent(this, RunForegroundService::class.java).setAction(ACTION_STOP),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

        val builder = NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_run_notification)
            .setContentTitle(title)
            .setContentText(text)
            .setContentIntent(contentIntent)
            .setOngoing(ongoing)
            .setOnlyAlertOnce(true)
            .setCategory(NotificationCompat.CATEGORY_PROGRESS)
            .setPriority(NotificationCompat.PRIORITY_LOW)

        if (ongoing) {
            builder.addAction(R.drawable.ic_run_notification, "停止跑步", stopIntent)
        }

        return builder.build()
    }

    private fun startForegroundCompat(notification: Notification) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(
                NOTIFICATION_ID,
                notification,
                ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC,
            )
        } else {
            startForeground(NOTIFICATION_ID, notification)
        }
    }

    private fun updateNotification(title: String, text: String, ongoing: Boolean) {
        val manager = getSystemService(NotificationManager::class.java)
        try {
            manager.notify(NOTIFICATION_ID, buildNotification(title, text, ongoing))
        } catch (_: SecurityException) {
            // Android 13+ may suppress notification updates if notification permission is denied.
        }
    }

    private fun acquireWakeLock() {
        if (wakeLock?.isHeld == true) {
            return
        }

        val powerManager = getSystemService(POWER_SERVICE) as PowerManager
        wakeLock = powerManager.newWakeLock(
            PowerManager.PARTIAL_WAKE_LOCK,
            "$packageName:RunUpload",
        ).apply {
            setReferenceCounted(false)
            acquire()
        }
    }

    private fun releaseWakeLock() {
        val lock = wakeLock
        if (lock?.isHeld == true) {
            lock.release()
        }
        wakeLock = null
    }

    private fun cleanupAndStop() {
        releaseWakeLock()
        currentOpenID = null
        runner = null
        stopRequested = false
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    companion object {
        private const val CHANNEL_ID = "runningbyebye_run"
        private const val NOTIFICATION_ID = 1001

        private const val ACTION_START = "com.runningbyebye.app.action.START_RUN"
        private const val ACTION_STOP = "com.runningbyebye.app.action.STOP_RUN"

        private const val EXTRA_OPEN_ID = "open_id"
        private const val EXTRA_FIELD_CODE = "field_code"
        private const val EXTRA_FIELD_NAME = "field_name"
        private const val EXTRA_PACE = "pace"
        private const val EXTRA_INTERVAL = "interval"
        private const val EXTRA_DATA_DIR = "data_dir"
        private const val EXTRA_POINTS_DIR = "points_dir"

        fun startRun(
            context: Context,
            openID: String,
            fieldCode: String,
            fieldName: String,
            pace: String,
            interval: String,
            dataDir: String,
            pointsDir: String,
        ) {
            val intent = Intent(context, RunForegroundService::class.java).apply {
                action = ACTION_START
                putExtra(EXTRA_OPEN_ID, openID)
                putExtra(EXTRA_FIELD_CODE, fieldCode)
                putExtra(EXTRA_FIELD_NAME, fieldName)
                putExtra(EXTRA_PACE, pace)
                putExtra(EXTRA_INTERVAL, interval)
                putExtra(EXTRA_DATA_DIR, dataDir)
                putExtra(EXTRA_POINTS_DIR, pointsDir)
            }
            ContextCompat.startForegroundService(context, intent)
        }

        fun stopRun(context: Context) {
            val intent = Intent(context, RunForegroundService::class.java).apply {
                action = ACTION_STOP
            }
            context.startService(intent)
        }
    }
}
