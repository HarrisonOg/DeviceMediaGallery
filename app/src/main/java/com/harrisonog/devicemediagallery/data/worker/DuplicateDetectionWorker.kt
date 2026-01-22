package com.harrisonog.devicemediagallery.data.worker

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.ForegroundInfo
import androidx.work.WorkerParameters
import com.harrisonog.devicemediagallery.R
import com.harrisonog.devicemediagallery.data.repository.DuplicateRepository
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject

@HiltWorker
class DuplicateDetectionWorker @AssistedInject constructor(
    @Assisted private val context: Context,
    @Assisted params: WorkerParameters,
    private val duplicateRepository: DuplicateRepository
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        return try {
            setForeground(createForegroundInfo())

            val result = duplicateRepository.detectDuplicates()

            result.fold(
                onSuccess = { duplicateCount ->
                    showCompletionNotification(duplicateCount)
                    Result.success()
                },
                onFailure = {
                    Result.failure()
                }
            )
        } catch (e: Exception) {
            Result.failure()
        }
    }

    private fun createForegroundInfo(): ForegroundInfo {
        createNotificationChannel()

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setContentTitle("Scanning for duplicates")
            .setContentText("Analyzing your media files...")
            .setSmallIcon(R.drawable.ic_notification)
            .setOngoing(true)
            .setProgress(0, 0, true)
            .build()

        return ForegroundInfo(NOTIFICATION_ID, notification)
    }

    private fun showCompletionNotification(duplicateCount: Int) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val hasPermission = ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
            if (!hasPermission) return
        }

        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        val message = if (duplicateCount > 0) {
            "Found $duplicateCount duplicate files"
        } else {
            "No duplicates found"
        }

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setContentTitle("Duplicate scan complete")
            .setContentText(message)
            .setSmallIcon(R.drawable.ic_notification)
            .setAutoCancel(true)
            .build()

        notificationManager.notify(COMPLETION_NOTIFICATION_ID, notification)
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Duplicate Detection",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Notifications for duplicate detection progress"
            }

            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }

    companion object {
        const val CHANNEL_ID = "duplicate_detection"
        const val NOTIFICATION_ID = 1
        const val COMPLETION_NOTIFICATION_ID = 2
        const val WORK_NAME = "duplicate_detection_work"
    }
}
