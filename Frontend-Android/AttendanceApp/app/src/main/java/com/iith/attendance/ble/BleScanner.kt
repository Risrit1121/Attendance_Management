package com.iith.attendance.ble

import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothManager
import android.bluetooth.le.BluetoothLeScanner
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanFilter
import android.bluetooth.le.ScanResult
import android.bluetooth.le.ScanSettings
import android.content.Context
import android.os.ParcelUuid
import com.iith.attendance.data.models.BeaconResult
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.util.UUID

/**
 * Android BLE scanner that finds iBeacon advertisements matching the
 * same UUID used by the ESP32 firmware:
 *   550e8400-e29b-41d4-a716-446655440000
 *
 * iBeacon packets are Apple manufacturer-specific data (company ID 0x004C).
 * We parse the raw advertisement payload to extract UUID, major, minor.
 */
object BleScanner {

    // Must match ESP32 firmware UUID
    const val BEACON_UUID = "550e8400-e29b-41d4-a716-446655440000"

    // Apple company ID bytes (little-endian in Android BLE APIs)
    private val APPLE_COMPANY_ID = byteArrayOf(0x4C.toByte(), 0x00.toByte())

    /**
     * Emits [BeaconResult] objects for every matching iBeacon advertisement
     * until the calling coroutine scope is cancelled.
     *
     * Call from a coroutine that has BLUETOOTH_SCAN + ACCESS_FINE_LOCATION granted.
     */
    fun scan(context: Context): Flow<BeaconResult> = callbackFlow {
        val btManager = context.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
        val btAdapter: BluetoothAdapter = btManager.adapter
        val scanner: BluetoothLeScanner = btAdapter.bluetoothLeScanner

        val settings = ScanSettings.Builder()
            .setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY)
            .build()

        // Filter by manufacturer data would be most efficient, but broad scan
        // with manual UUID filtering is more compatible across Android versions.
        val callback = object : ScanCallback() {
            override fun onScanResult(callbackType: Int, result: ScanResult) {
                parseIBeacon(result)?.let { trySend(it) }
            }

            override fun onBatchScanResults(results: List<ScanResult>) {
                results.forEach { r -> parseIBeacon(r)?.let { trySend(it) } }
            }

            override fun onScanFailed(errorCode: Int) {
                close(Exception("BLE scan failed with code $errorCode"))
            }
        }

        scanner.startScan(emptyList<ScanFilter>(), settings, callback)

        awaitClose { scanner.stopScan(callback) }
    }

    // ── iBeacon payload parser ────────────────────────────────────────────────

    /**
     * iBeacon manufacturer data layout (after the 2-byte company ID):
     *   Byte 0    : 0x02 (subtype)
     *   Byte 1    : 0x15 (length = 21 bytes follow)
     *   Bytes 2–17: UUID (big-endian)
     *   Bytes 18–19: Major (big-endian uint16)
     *   Bytes 20–21: Minor (big-endian uint16)
     *   Byte 22   : TX Power
     */
    private fun parseIBeacon(result: ScanResult): BeaconResult? {
        val record = result.scanRecord ?: return null
        // getManufacturerSpecificData(0x004C) → returns bytes AFTER the company ID
        val appleData = record.getManufacturerSpecificData(0x004C) ?: return null

        // Must be at least: subtype(1) + length(1) + UUID(16) + major(2) + minor(2) + tx(1) = 23
        if (appleData.size < 23) return null
        if (appleData[0] != 0x02.toByte() || appleData[1] != 0x15.toByte()) return null

        // Parse UUID (bytes 2–17, big-endian)
        val uuidBytes = appleData.copyOfRange(2, 18)
        val buf = ByteBuffer.wrap(uuidBytes)
        val msb = buf.long
        val lsb = buf.long
        val uuid = UUID(msb, lsb).toString()

        // Only proceed if UUID matches our beacon
        if (!uuid.equals(BEACON_UUID, ignoreCase = true)) return null

        val major = ByteBuffer.wrap(appleData, 18, 2).order(ByteOrder.BIG_ENDIAN).short.toInt() and 0xFFFF
        val minor = ByteBuffer.wrap(appleData, 20, 2).order(ByteOrder.BIG_ENDIAN).short.toInt() and 0xFFFF

        return BeaconResult(
            major = major,
            minor = minor,
            rssi  = result.rssi,
            uuid  = uuid
        )
    }
}
