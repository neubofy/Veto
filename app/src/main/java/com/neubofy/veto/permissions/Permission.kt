package com.neubofy.veto.permissions

import android.app.Activity
import android.content.Context
import androidx.annotation.StringRes


abstract class Permission {

    @get:StringRes
    abstract val name: Int

    @get:StringRes
    open val description: Int? = null

    abstract fun isGranted(context: Context): Boolean

    abstract fun request(activity: Activity)

    // This does NOT override toString() because it takes a Context!
    fun toString(context: Context): String {
        return context.getString(name)
    }
}
