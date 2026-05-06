package com.iith.attendanceapp

import android.annotation.SuppressLint
import android.bluetooth.BluetoothManager
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanRecord
import android.bluetooth.le.ScanResult
import android.bluetooth.le.ScanSettings
import android.content.Context
import android.os.Handler
import android.os.Looper
import android.util.Log

private const val TAG = "BleScanner"

data class BleBeaconResult(
    val uuid: String,
    val major: Int,
    val minor: Int,
    val avgRssi: Double
)

// Parsed iBeacon fields from a single advertisement packet
private data class IBeacon(val uuid: String, val major: Int, val minor: Int)

// Parse iBeacon from ScanRecord using getManufacturerSpecificData(0x004C)
// This is the reliable API — same approach used by nRF Connect and the working debugger app
private fun parseIBeacon(scanRecord: ScanRecord?): IBeacon? {
    if (scanRecord == null) return null

    // getManufacturerSpecificData(0x004C) returns ONLY Apple's manufacturer data bytes,
    // with the 2-byte company ID already stripped — so index 0 is the iBeacon type byte
    val appleData = scanRecord.getManufacturerSpecificData(0x004C) ?: return null

    Log.v(TAG, "Apple mfr data (${appleData.size} bytes): ${appleData.toHex()}")

    // iBeacon structure (after company ID):
    //   [0]    = 0x02  iBeacon type
    //   [1]    = 0x15  length (21 bytes follow)
    //   [2–17] = 16-byte proximity UUID
    //   [18–19]= major (big-endian)
    //   [20–21]= minor (big-endian)
    //   [22]   = TX power
    if (appleData.size < 23) return null
    if ((appleData[0].toInt() and 0xFF) != 0x02) return null
    if ((appleData[1].toInt() and 0xFF) != 0x15) return null

    val uuidBytes = appleData.copyOfRange(2, 18)
    val uuid  = bytesToUuid(uuidBytes)
    val major = ((appleData[18].toInt() and 0xFF) shl 8) or (appleData[19].toInt() and 0xFF)
    val minor = ((appleData[20].toInt() and 0xFF) shl 8) or (appleData[21].toInt() and 0xFF)

    return IBeacon(uuid, major, minor)
}

private fun bytesToUuid(bytes: ByteArray): String {
    val hex = bytes.joinToString("") { "%02x".format(it) }
    return "${hex.substring(0,8)}-${hex.substring(8,12)}-${hex.substring(12,16)}-${hex.substring(16,20)}-${hex.substring(20)}"
}

private fun ByteArray.toHex() = joinToString(" ") { "%02x".format(it) }

@SuppressLint("MissingPermission")
fun startBleScan(
    context: Context,
    targetUuid: String,
    expectedMajor: String? = null,
    onResult: (List<BleBeaconResult>) -> Unit
) {
    val bluetoothManager = context.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
    val adapter = bluetoothManager.adapter

    if (adapter == null || !adapter.isEnabled) {
        Log.e(TAG, "Bluetooth is off or unavailable")
        onResult(emptyList())
        return
    }

    val scanner = adapter.bluetoothLeScanner ?: run {
        Log.e(TAG, "bluetoothLeScanner is null")
        onResult(emptyList())
        return
    }

    // key = "major-minor", collects all RSSI readings during the 3-second window
    val rssiMap = mutableMapOf<String, MutableList<Int>>()
    val metaMap = mutableMapOf<String, Pair<Int, Int>>()

    val settings = ScanSettings.Builder()
        .setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY)
        .setReportDelay(0)   // deliver every packet immediately, no batching
        .build()

    val callback = object : ScanCallback() {
        override fun onScanResult(callbackType: Int, result: ScanResult) {
            Log.d(TAG, "Device: ${result.device.address} RSSI: ${result.rssi}")

            val beacon = parseIBeacon(result.scanRecord) ?: return

            Log.d(TAG, "iBeacon — UUID: ${beacon.uuid} major: ${beacon.major} minor: ${beacon.minor}")

            if (!beacon.uuid.equals(targetUuid, ignoreCase = true)) return

            // Filter by expected major if provided (matching iOS behaviour)
            if (expectedMajor != null && beacon.major.toString() != expectedMajor) return

            Log.d(TAG, "TARGET MATCHED — major=${beacon.major} minor=${beacon.minor} rssi=${result.rssi}")

            val key = "${beacon.major}-${beacon.minor}"
            rssiMap.getOrPut(key) { mutableListOf() }.add(result.rssi)
            metaMap[key] = Pair(beacon.major, beacon.minor)
        }

        override fun onScanFailed(errorCode: Int) {
            Log.e(TAG, "Scan failed — error code: $errorCode")
        }
    }

    // No filters — same as the working debugger app (null filter list)
    scanner.startScan(null, settings, callback)
    Log.d(TAG, "Scan started for UUID: $targetUuid")

    Handler(Looper.getMainLooper()).postDelayed({
        scanner.stopScan(callback)
        Log.d(TAG, "Scan stopped — ${rssiMap.size} matching beacon(s) found")

        val results = rssiMap.map { (key, rssiList) ->
            val (major, minor) = metaMap[key]!!
            BleBeaconResult(
                uuid    = targetUuid,
                major   = major,
                minor   = minor,
                avgRssi = rssiList.average()
            ).also {
                Log.d(TAG, "Result: major=$major minor=$minor avgRssi=${"%.1f".format(it.avgRssi)} dBm over ${rssiList.size} samples")
            }
        }
        onResult(results)
    }, 3000L)
}
