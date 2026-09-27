package com.example.medtracker.notifications

import android.app.*
import android.content.*
import android.os.Build
import androidx.core.app.NotificationCompat
import com.example.medtracker.MainActivity
import com.example.medtracker.R
import com.example.medtracker.domain.ScheduledDose

interface ReminderScheduler { fun schedule(dose: ScheduledDose, medication:String, doseText:String); fun cancel(key:String) }
class AlarmReminderScheduler(private val context:Context):ReminderScheduler {
    private val alarms=context.getSystemService(AlarmManager::class.java)
    override fun schedule(dose:ScheduledDose,medication:String,doseText:String){
        val intent=Intent(context,ReminderReceiver::class.java).putExtra("title",medication).putExtra("text","${dose.label} — $doseText").putExtra("key",dose.key)
        val pending=PendingIntent.getBroadcast(context,dose.key.hashCode(),intent,PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        alarms.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP,dose.scheduledAt.toEpochMilli(),pending)
    }
    override fun cancel(key:String){PendingIntent.getBroadcast(context,key.hashCode(),Intent(context,ReminderReceiver::class.java),PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE)?.let(alarms::cancel)}
}
class ReminderReceiver:BroadcastReceiver(){override fun onReceive(context:Context,intent:Intent){
    val channel="dose_reminders"; val manager=context.getSystemService(NotificationManager::class.java)
    if(Build.VERSION.SDK_INT>=26) manager.createNotificationChannel(NotificationChannel(channel,"Rappels de prises",NotificationManager.IMPORTANCE_HIGH))
    val open=PendingIntent.getActivity(context,0,Intent(context,MainActivity::class.java),PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
    val notification=NotificationCompat.Builder(context,channel).setSmallIcon(android.R.drawable.ic_lock_idle_alarm).setContentTitle(intent.getStringExtra("title")?:"Rappel").setContentText(intent.getStringExtra("text")?:"Prise planifiée").setContentIntent(open).setAutoCancel(true).build()
    manager.notify((intent.getStringExtra("key")?:"reminder").hashCode(),notification)
}}
class BootReceiver:BroadcastReceiver(){override fun onReceive(context:Context,intent:Intent){
    // MainActivity reschedules the current local plan. Data remains available even without permission.
    context.getSharedPreferences("reminders",Context.MODE_PRIVATE).edit().putBoolean("reschedule",true).apply()
}}
