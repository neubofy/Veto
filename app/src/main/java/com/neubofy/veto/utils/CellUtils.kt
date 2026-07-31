package com.neubofy.veto.utils

import android.Manifest
import android.content.Context
import android.os.Build
import android.os.SystemClock
import android.telephony.CellIdentityNr
import android.telephony.CellInfo
import android.telephony.CellInfoGsm
import android.telephony.CellInfoLte
import android.telephony.CellInfoNr
import android.telephony.CellInfoWcdma
import android.telephony.TelephonyManager
import androidx.annotation.RequiresApi
import androidx.annotation.RequiresPermission

// Inspired by https://github.com/mjaakko/NeoStumbler/blob/8f289eedd4f8df3c0f1715a146a0813f407ab443/app/src/main/java/xyz/malkki/neostumbler/domain/CellTower.kt
// (under MIT license)

enum class RadioType {
    GSM,
    WCDMA,
    LTE,
    NR
}

data class CellParameters(
    val mobileCountryCode: String?,
    val mobileNetworkCode: String?,
    val locationAreaCode: Int?,
    val cellId: Long?,
    val radio: RadioType,
    val timeMillis: Long,
) {
    fun prettyPrint(): String {
        return """
            CellParameters:
            mcc: $mobileCountryCode
            mnc: $mobileNetworkCode
            lac: $locationAreaCode
            cid: $cellId
            radio: $radio
        """.trimIndent()
    }

    /**
     * Checks if the cell info has enough useful data. Used for filtering neighbouring cells which
     * don't specify their codes.
     */
    fun hasEnoughData(): Boolean {
        if (mobileCountryCode == null || mobileNetworkCode == null) {
            return false
        }

        return when (radio) {
            RadioType.GSM -> cellId != null || locationAreaCode != null
            RadioType.WCDMA,
            RadioType.LTE,
            RadioType.NR ->
                cellId != null || locationAreaCode != null //|| primaryScramblingCode != null
        }
    }

    companion object {
        private val TAG = CellParameters::class.simpleName

        fun fromCellInfo(cellInfo: CellInfo): CellParameters? {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q && cellInfo is CellInfoNr) {
                return cellInfo.toCellParameters()
            }
            return when (cellInfo) {
                is CellInfoLte -> cellInfo.toCellParameters()
                is CellInfoWcdma -> cellInfo.toCellParameters()
                is CellInfoGsm -> cellInfo.toCellParameters()
                else -> null
            }
        }
    }
}

fun List<CellParameters>.prettyPrint(): String {
    return this.joinToString("\n\n") { it.prettyPrint() }
}

// Helper function because CellInfo (unhelpfully) returns a timestamp that is relative to the last boot.
fun CellInfo.timestampUnixMillis(): Long {
    val nowUnixMillis = System.currentTimeMillis()
    val millisSinceBoot = SystemClock.elapsedRealtime()
    val cellInfoAgeMillis = millisSinceBoot - (this.timeStamp / 1_000_000)
    val cellInfoUnixMillis = nowUnixMillis - cellInfoAgeMillis
    return cellInfoUnixMillis
}

// 5G
@RequiresApi(Build.VERSION_CODES.Q)
fun CellInfoNr.toCellParameters(): CellParameters {
    val id = this.cellIdentity as CellIdentityNr
    return CellParameters(
        id.mccString,
        id.mncString,
        id.tac.takeIf { it != CellInfo.UNAVAILABLE && it != 0 },
        id.nci.takeIf { it != CellInfo.UNAVAILABLE_LONG && it != 0L },
        RadioType.NR,
        this.timestampUnixMillis(),
    )
}

// 4G
@Suppress("Deprecation")
fun CellInfoLte.toCellParameters(): CellParameters {
    val id = this.cellIdentity
    val mccString: String?
    val mncString: String?

    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
        mccString = id.mccString
        mncString = id.mncString
    } else {
        mccString = id.mcc.toString()
        mncString = id.mnc.toString()
    }

    return CellParameters(
        mccString,
        mncString,
        id.tac.takeIf { it != UNAVAILABLE && it != 0 },
        id.ci.takeIf { it != UNAVAILABLE && it != 0 }?.toLong(),
        RadioType.LTE,
        this.timestampUnixMillis(),
    )
}

// 3G
@Suppress("Deprecation")
fun CellInfoWcdma.toCellParameters(): CellParameters {
    val id = this.cellIdentity
    val mccString: String?
    val mncString: String?

    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
        mccString = id.mccString
        mncString = id.mncString
    } else {
        mccString = id.mcc.toString()
        mncString = id.mnc.toString()
    }

    return CellParameters(
        mccString,
        mncString,
        id.lac.takeIf { it != UNAVAILABLE && it != 0 },
        id.cid.takeIf { it != UNAVAILABLE && it != 0 }?.toLong(),
        RadioType.WCDMA,
        this.timestampUnixMillis(),
    )
}

// 2G
@Suppress("Deprecation")
fun CellInfoGsm.toCellParameters(): CellParameters {
    val id = this.cellIdentity
    val mccString: String?
    val mncString: String?

    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
        mccString = id.mccString
        mncString = id.mncString
    } else {
        mccString = id.mcc.toString()
        mncString = id.mnc.toString()
    }

    return CellParameters(
        mccString,
        mncString,
        id.lac.takeIf { it != UNAVAILABLE && it != 0 },
        id.cid.takeIf { it != UNAVAILABLE && it != 0 }?.toLong(),
        RadioType.GSM,
        this.timestampUnixMillis(),
    )
}

private val UNAVAILABLE = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
    CellInfo.UNAVAILABLE
} else {
    Integer.MAX_VALUE
}

@RequiresPermission(Manifest.permission.ACCESS_FINE_LOCATION)
fun requestCellInfo(context: Context, onCellParas: (List<CellParameters>) -> Unit) {
    val telephonyManager = context.getSystemService(TelephonyManager::class.java)

    val onCellInfoUpdate = { cellInfos: List<CellInfo> ->
        val paras = cellInfos
            .mapNotNull { CellParameters.fromCellInfo(it) }
            .filter { it.hasEnoughData() }
            .toList()
        onCellParas(paras)
    }

    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
        val cellInfos = telephonyManager.allCellInfo?.toList() ?: emptyList()
        onCellInfoUpdate(cellInfos)
        return
    }

    telephonyManager.requestCellInfoUpdate(
        context.mainExecutor,
        object : TelephonyManager.CellInfoCallback() {
            override fun onCellInfo(cellInfo: List<CellInfo>) {
                onCellInfoUpdate(cellInfo)
            }

            @RequiresPermission(Manifest.permission.ACCESS_FINE_LOCATION)
            override fun onError(errorCode: Int, detail: Throwable?) {
                // super.onError(errorCode, detail)
                context.log().w(
                    "CellUtils",
                    "CellInfo update failed with errorCode=$errorCode message='${detail?.message}'"
                            + " Falling back to cached CellInfo."
                )
                // Fall back to cached CellInfo
                val cellInfos = telephonyManager.allCellInfo?.toList() ?: emptyList()
                onCellInfoUpdate(cellInfos)
            }
        })
}
