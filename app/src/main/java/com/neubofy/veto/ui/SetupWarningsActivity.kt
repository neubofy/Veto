package com.neubofy.veto.ui

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.core.view.isVisible
import com.neubofy.veto.databinding.ActivitySetupWarningsBinding
import com.neubofy.veto.permissions.globalAppPermissions
import com.neubofy.veto.permissions.isMissingGlobalAppPermission
import com.neubofy.veto.ui.UiUtil.Companion.setupEdgeToEdgeAppBar
import com.neubofy.veto.ui.UiUtil.Companion.setupEdgeToEdgeScrollView


class SetupWarningsActivity : VetoActivity() {

    private lateinit var viewBinding: ActivitySetupWarningsBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        viewBinding = ActivitySetupWarningsBinding.inflate(layoutInflater)
        setContentView(viewBinding.root)

        setupEdgeToEdgeAppBar(viewBinding.appBar)
        setupEdgeToEdgeScrollView(viewBinding.scrollView)
    }

    override fun onResume() {
        super.onResume()

        // For simplicity, we always show all warnings/recommendations.
        // Easier for developers (no big if-else tree), and
        // more transparent for users (why did this suddenly disappear?).
        setupRecommConnectivity(this)
        setupPermissionsList(
            this,
            viewBinding.permissionsRequiredTitle,
            viewBinding.permissionsRequiredList,
            globalAppPermissions()
        )
    }

    private fun setupRecommConnectivity(context: Context) {
        val shouldNudge = false

        viewBinding.connectivity.icCheck.isVisible = !shouldNudge
        viewBinding.connectivity.recommendationConnCheckEnableButton.isVisible = shouldNudge
    }
}

fun shouldShowSetupWarnings(context: Context): Boolean {
    return isMissingGlobalAppPermission(context)
}
