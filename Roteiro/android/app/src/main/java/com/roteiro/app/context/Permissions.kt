package com.roteiro.app.context

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.PowerManager
import android.provider.Settings
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat

/** Estado das permissões que liberam cada tipo de lembrete. */
data class PermissionState(
    val location: Boolean,
    val backgroundLocation: Boolean,
    val notifications: Boolean,
    val batteryUnrestricted: Boolean,
) {
    /** Chegada e saída só funcionam com o app fechado se a localização for "o tempo todo". */
    val canDetectInBackground get() = location && backgroundLocation
}

object Permissions {
    fun granted(context: Context, permission: String) =
        ContextCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED

    fun hasLocation(context: Context) = granted(context, Manifest.permission.ACCESS_FINE_LOCATION)

    fun hasBackgroundLocation(context: Context) =
        Build.VERSION.SDK_INT < Build.VERSION_CODES.Q || granted(context, Manifest.permission.ACCESS_BACKGROUND_LOCATION)

    fun hasNotifications(context: Context) = NotificationManagerCompat.from(context).areNotificationsEnabled()

    fun isBatteryUnrestricted(context: Context): Boolean {
        val pm = context.getSystemService(Context.POWER_SERVICE) as PowerManager
        return pm.isIgnoringBatteryOptimizations(context.packageName)
    }

    fun state(context: Context) = PermissionState(
        location = hasLocation(context),
        backgroundLocation = hasBackgroundLocation(context),
        notifications = hasNotifications(context),
        batteryUnrestricted = isBatteryUnrestricted(context),
    )

    /** Permissões do primeiro pedido (a localização em segundo plano vem depois, sozinha). */
    val foregroundLocation = arrayOf(
        Manifest.permission.ACCESS_FINE_LOCATION,
        Manifest.permission.ACCESS_COARSE_LOCATION,
    )

    fun appSettingsIntent(context: Context) =
        Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.fromParts("package", context.packageName, null))
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)

    fun batterySettingsIntent() =
        Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)

    fun notificationSettingsIntent(context: Context) =
        Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS)
            .putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
}
