package com.lance.steadysteps

import android.content.Context
import android.content.Intent
import androidx.health.connect.client.HealthConnectClient
import androidx.health.connect.client.PermissionController
import androidx.health.connect.client.permission.HealthPermission
import androidx.health.connect.client.records.StepsRecord
import androidx.health.connect.client.request.AggregateRequest
import androidx.health.connect.client.time.TimeRangeFilter
import kotlinx.coroutines.runBlocking
import org.json.JSONObject
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

object Steps {
    val PERM: String = HealthPermission.getReadPermission(StepsRecord::class)

    /** Returns JSON like {"state":"ok","steps":1234}. state: ok, perm, unavailable. */
    fun read(ctx: Context): String {
        val o = JSONObject()
        try {
            if (HealthConnectClient.getSdkStatus(ctx) != HealthConnectClient.SDK_AVAILABLE) {
                o.put("state", "unavailable"); o.put("steps", 0); return o.toString()
            }
            val client = HealthConnectClient.getOrCreate(ctx)
            val granted = runBlocking { client.permissionController.getGrantedPermissions() }
            if (!granted.contains(PERM)) {
                o.put("state", "perm"); o.put("steps", 0); return o.toString()
            }
            val zone = ZoneId.systemDefault()
            val start = LocalDate.now(zone).atStartOfDay(zone).toInstant()
            val res = runBlocking {
                client.aggregate(
                    AggregateRequest(
                        metrics = setOf(StepsRecord.COUNT_TOTAL),
                        timeRangeFilter = TimeRangeFilter.between(start, Instant.now())
                    )
                )
            }
            o.put("state", "ok")
            o.put("steps", res[StepsRecord.COUNT_TOTAL] ?: 0L)
        } catch (e: Exception) {
            o.put("state", "unavailable"); o.put("steps", 0)
        }
        return o.toString()
    }

    fun permissionIntent(ctx: Context): Intent =
        PermissionController.createRequestPermissionResultContract().createIntent(ctx, setOf(PERM))
}
