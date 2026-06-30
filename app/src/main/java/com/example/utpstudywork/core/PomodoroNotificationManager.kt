package com.example.utpstudywork.core

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.example.utpstudywork.domain.model.Note
import com.example.utpstudywork.domain.model.NotificationType
import com.example.utpstudywork.domain.model.SessionType

object NotificationChannels {
    const val POMODORO = "pomodoro_channel"
    const val TIMER = "timer_channel"

    fun createAll(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            manager.createNotificationChannel(
                NotificationChannel(POMODORO, "Pomodoro", NotificationManager.IMPORTANCE_DEFAULT)
                    .apply { description = "Notificaciones de inicio de sesión" }
            )
            manager.createNotificationChannel(
                NotificationChannel(TIMER, "Temporizador", NotificationManager.IMPORTANCE_LOW)
                    .apply {
                        description = "Temporizador activo del Pomodoro"
                        setShowBadge(false)
                    }
            )
        }
    }
}

class PomodoroNotificationManager(private val context: Context) {

    private val notifManager = NotificationManagerCompat.from(context)

    companion object {
        private const val TIMER_NOTIF_ID = 1000
        private const val NOTE_NOTIF_BASE_ID = 2000
        private const val NOTE_ASSIGNED_BASE_ID = NOTE_NOTIF_BASE_ID + 500
    }

    // Notificación persistente del temporizador — se actualiza cada segundo
    fun updateTimerNotification(remainingSec: Int, sessionType: SessionType, activeNotes: List<Note>) {
        val sessionLabel = if (sessionType == SessionType.WORK) "Trabajo" else "Estudio"
        val timeStr = remainingSec.formatTime()
        val noteTitles = activeNotes.joinToString(" · ") { it.title }.ifBlank { "Sin notas activas" }

        val notification = NotificationCompat.Builder(context, NotificationChannels.TIMER)
            .setSmallIcon(android.R.drawable.ic_media_play)
            .setContentTitle("Pomodoro · $sessionLabel  $timeStr")
            .setContentText(noteTitles)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setOngoing(true)
            .setSilent(true)
            .setOnlyAlertOnce(true)
            .build()

        try {
            notifManager.notify(TIMER_NOTIF_ID, notification)
        } catch (_: SecurityException) {}
    }

    fun cancelTimerNotification() {
        notifManager.cancel(TIMER_NOTIF_ID)
    }

    // Notificaciones puntuales al iniciar la sesión para notas con config activada
    fun triggerForSession(sessionType: SessionType, activeNotes: List<Note>) {
        activeNotes.forEachIndexed { index, note ->
            if (shouldNotify(note, sessionType)) {
                showNoteNotification(note, sessionType, NOTE_NOTIF_BASE_ID + index)
            }
        }
    }

    private fun shouldNotify(note: Note, session: SessionType): Boolean {
        val config = note.notification ?: return false
        return when (config.type) {
            NotificationType.DEFAULT -> note.category == session
            NotificationType.CUSTOM -> config.filterCategory == null || config.filterCategory == session
        }
    }

    private fun showNoteNotification(note: Note, session: SessionType, notifId: Int) {
        val config = note.notification ?: return
        val sessionLabel = if (session == SessionType.WORK) "Trabajo" else "Estudio"
        val message = when (config.type) {
            NotificationType.DEFAULT -> "Sesión de $sessionLabel activa: ${note.title}"
            NotificationType.CUSTOM -> config.customMessage.ifBlank { "Sesión de $sessionLabel: ${note.title}" }
        }
        val taskSummary = note.tasks.count { !it.isDone }
            .takeIf { it > 0 }?.let { " · $it tarea(s) pendiente(s)" } ?: ""

        try {
            notifManager.notify(
                notifId,
                NotificationCompat.Builder(context, NotificationChannels.POMODORO)
                    .setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
                    .setContentTitle(note.title)
                    .setContentText(message + taskSummary)
                    .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                    .setAutoCancel(true)
                    .build()
            )
        } catch (_: SecurityException) {}
    }

    fun notifyAssignedToToday(note: Note) {
        val notifId = NOTE_ASSIGNED_BASE_ID + note.id.hashCode()
        try {
            notifManager.notify(
                notifId,
                NotificationCompat.Builder(context, NotificationChannels.POMODORO)
                    .setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
                    .setContentTitle("Nota asignada al día de hoy: ${note.title}")
                    .setContentText("Esta nota estará activa durante el día de hoy")
                    .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                    .setAutoCancel(true)
                    .build()
            )
        } catch (_: SecurityException) {}
    }

    private fun Int.formatTime(): String {
        val m = this / 60
        val s = this % 60
        return String.format("%02d:%02d", m, s)
    }
}
