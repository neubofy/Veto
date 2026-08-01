package com.neubofy.veto.commands

import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothManager
import android.content.Context
import android.os.Build
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import com.neubofy.veto.R
import com.neubofy.veto.permissions.BluetoothConnectPermission
import com.neubofy.veto.permissions.Permission
import com.neubofy.veto.transports.Transport
import com.neubofy.veto.utils.log


class BluetoothCommand(context: Context) : Command(context) {

    override val keyword = "bluetooth"
    override val usage = "bluetooth [on | off]"

    @get:DrawableRes
    override val icon = R.drawable.ic_bluetooth

    @get:StringRes
    override val shortDescription = R.string.cmd_bluetooth_description_short

    override val longDescription = null

    override val requiredPermissions = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        listOf(BluetoothConnectPermission())
        // TODO: device owner
    } else {
        emptyList<Permission>()
    }

    @SuppressLint("MissingPermission")
    override suspend fun <T> executeInternal(
        args: List<String>,
        transport: Transport<T>,
    ) {
        val bluetoothManager: BluetoothManager =
            context.getSystemService(BluetoothManager::class.java)
        val bluetoothAdapter: BluetoothAdapter? = bluetoothManager.adapter

        if (bluetoothAdapter == null) {
            // Device doesn't support Bluetooth
            transport.send(context, context.getString(R.string.cmd_bluetooth_response_no_bluetooth), keyword)
            return
        }

        if (args.isEmpty()) {
            val msg = if (bluetoothAdapter.isEnabled) {
                context.getString(R.string.cmd_bluetooth_response_is_on)
            } else {
                context.getString(R.string.cmd_bluetooth_response_is_off)
            }
            transport.send(context, msg, keyword)
        } else if (args.contains("on")) {
            if (bluetoothAdapter.isEnabled) {
                transport.send(context, context.getString(R.string.cmd_bluetooth_response_on), keyword)
                return
            }

            var enabled = false
            try {
                @Suppress("DEPRECATION")
                enabled = bluetoothAdapter.enable()
            } catch (e: Exception) {
                context.log().e("BluetoothCommand", "bluetoothAdapter.enable() failed: ${e.message}")
            }

            if (!enabled) {
                val secureSuccess = com.neubofy.veto.utils.SecureSettings.setBluetooth(context, true)
                context.log().d("BluetoothCommand", "SecureSettings.setBluetooth(on) result: $secureSuccess")
            }

            // Verify state
            kotlinx.coroutines.delay(500L)
            if (bluetoothAdapter.isEnabled) {
                transport.send(context, context.getString(R.string.cmd_bluetooth_response_on), keyword)
            } else {
                val errorMsg = "Failed to turn on Bluetooth. (Missing BLUETOOTH_CONNECT or WRITE_SECURE_SETTINGS permission)"
                context.log().e("BluetoothCommand", errorMsg)
                transport.send(context, errorMsg, keyword)
            }
        } else if (args.contains("off")) {
            if (!bluetoothAdapter.isEnabled) {
                transport.send(context, context.getString(R.string.cmd_bluetooth_response_off), keyword)
                return
            }

            var disabled = false
            try {
                @Suppress("DEPRECATION")
                disabled = bluetoothAdapter.disable()
            } catch (e: Exception) {
                context.log().e("BluetoothCommand", "bluetoothAdapter.disable() failed: ${e.message}")
            }

            if (!disabled) {
                val secureSuccess = com.neubofy.veto.utils.SecureSettings.setBluetooth(context, false)
                context.log().d("BluetoothCommand", "SecureSettings.setBluetooth(off) result: $secureSuccess")
            }

            // Verify state
            kotlinx.coroutines.delay(500L)
            if (!bluetoothAdapter.isEnabled) {
                transport.send(context, context.getString(R.string.cmd_bluetooth_response_off), keyword)
            } else {
                val errorMsg = "Failed to turn off Bluetooth. (Missing BLUETOOTH_CONNECT or WRITE_SECURE_SETTINGS permission)"
                context.log().e("BluetoothCommand", errorMsg)
                transport.send(context, errorMsg, keyword)
            }
        }
    }
}
