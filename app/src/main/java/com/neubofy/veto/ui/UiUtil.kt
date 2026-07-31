package com.neubofy.veto.ui

import android.view.View
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updatePadding
import com.neubofy.veto.permissions.Permission
import com.neubofy.veto.ui.home.PermissionView


class UiUtil {
    companion object {
        @JvmStatic
        @JvmOverloads
        fun setupEdgeToEdge(view: View, top: Boolean = true, bottom: Boolean = true) {
            ViewCompat.setOnApplyWindowInsetsListener(view) { v, insets ->
                val i = insets.getInsets(
                    WindowInsetsCompat.Type.systemBars() + WindowInsetsCompat.Type.displayCutout()
                )
                v.updatePadding(
                    left = i.left,
                    right = i.right,
                )
                if (top) {
                    v.updatePadding(top = i.top)
                }
                if (bottom) {
                    v.updatePadding(bottom = i.bottom)
                }
                WindowInsetsCompat.CONSUMED
            }
        }

        @JvmStatic
        fun setupEdgeToEdgeAppBar(view: View) {
            setupEdgeToEdge(view, bottom = false)
        }

        @JvmStatic
        fun setupEdgeToEdgeScrollView(view: View) {
            setupEdgeToEdge(view, top = false)
        }
    }
}

fun setupPermissionsList(
    activity: AppCompatActivity,
    title: TextView,
    list: LinearLayout,
    perms: List<Permission>,
    hideDescription: Boolean = false,
) {
    if (perms.isEmpty()) {
        title.visibility = View.GONE
        list.visibility = View.GONE
        return
    }
    title.visibility = View.VISIBLE
    list.visibility = View.VISIBLE

    list.removeAllViews()
    for (p in perms) {
        val pView = PermissionView(title.context)
        pView.setPermission(p, activity, hideDescription)
        list.addView(pView)
    }
}
