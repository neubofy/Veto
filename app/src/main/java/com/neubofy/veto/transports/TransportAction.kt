package com.neubofy.veto.transports

import androidx.annotation.StringRes
import androidx.appcompat.app.AppCompatActivity


data class TransportAction(
    @StringRes
    val titleResourceId: Int,

    val run: (activity: AppCompatActivity) -> Unit,
)