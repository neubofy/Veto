package com.neubofy.veto.permissions

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import androidx.core.content.ContextCompat

class RecordAudioPermission : Permission() {
    override val identifier = "RECORD_AUDIO"
    override val displayName = "Microphone"
    override val rationale = "Veto needs microphone access for the audio command."

    override fun isGranted(context: Context): Boolean {
        return ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED
    }

    override fun getIntents(context: Context) = emptyList<android.content.Intent>()
    override fun getManifestPermissions() = listOf(Manifest.permission.RECORD_AUDIO)
}
