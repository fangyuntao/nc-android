/*
 * Nextcloud - Android Client
 *
 * SPDX-FileCopyrightText: 2020 Chris Narkiewicz <hello@ezaquarii.com>
 * SPDX-FileCopyrightText: 2019 Tobias Kaminsky
 * SPDX-FileCopyrightText: 2019 Nextcloud GmbH
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */
package com.nextcloud.client.device

import android.content.Context
import android.os.PowerManager
import com.nextcloud.client.preferences.AppPreferences
import dagger.Module
import dagger.Provides

@Module
class DeviceModule {

    @Provides
    fun powerManagementService(context: Context, preferences: AppPreferences): PowerManagementService {
        val platformPowerManager = context.getSystemService(Context.POWER_SERVICE) as PowerManager
        return PowerManagementServiceImpl(
            context = context,
            platformPowerManager = platformPowerManager,
            deviceInfo = DeviceInfo(),
            preferences = preferences
        )
    }
}
