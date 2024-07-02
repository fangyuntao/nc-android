/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2019 Chris Narkiewicz <hello@ezaquarii.com>
 * SPDX-License-Identifier: AGPL-3.0-or-later OR GPL-2.0-only
 */
package com.nextcloud.client.onboarding

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.res.Resources
import com.nextcloud.client.account.CurrentAccountProvider
import com.nextcloud.client.preferences.AppPreferences
import com.owncloud.android.BuildConfig
import com.owncloud.android.R
import com.owncloud.android.authentication.AuthenticatorActivity
import com.owncloud.android.features.FeatureItem
import com.owncloud.android.ui.activity.PassCodeActivity

internal class OnboardingServiceImpl constructor(
    private val resources: Resources,
    private val preferences: AppPreferences,
    private val accountProvider: CurrentAccountProvider
) : OnboardingService {

    private companion object {
        const val ITEM_VERSION_CODE = 99999999
    }

    private val notSeenYet: Boolean
        get() {
            return BuildConfig.VERSION_CODE >= ITEM_VERSION_CODE && preferences.lastSeenVersionCode < ITEM_VERSION_CODE
        }

    override val whatsNew: Array<FeatureItem>
        get() = if (!isFirstRun && notSeenYet) {
            emptyArray()
        } else {
            emptyArray()
        }

    override val isFirstRun: Boolean
        get() {
            return accountProvider.currentAccount == null
        }

    override fun shouldShowWhatsNew(callingContext: Context): Boolean {
        return callingContext !is PassCodeActivity && whatsNew.isNotEmpty()
    }

    override fun launchActivityIfNeeded(activity: Activity) {
        if (!resources.getBoolean(R.bool.show_whats_new) || activity is WhatsNewActivity) {
            return
        }

        if (shouldShowWhatsNew(activity)) {
            activity.startActivity(Intent(activity, WhatsNewActivity::class.java))
        }
    }

    override fun launchFirstRunIfNeeded(activity: Activity): Boolean {
        val isProviderOrOwnInstallationVisible = resources.getBoolean(R.bool.show_provider_or_own_installation)
        val canLaunch = isProviderOrOwnInstallationVisible && isFirstRun && activity is AuthenticatorActivity
        if (canLaunch) {
            val intent = Intent(activity, FirstRunActivity::class.java)
            activity.startActivityForResult(intent, AuthenticatorActivity.REQUEST_CODE_FIRST_RUN)
        }
        return canLaunch
    }
}
