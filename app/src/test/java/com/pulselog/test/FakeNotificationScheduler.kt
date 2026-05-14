package com.pulselog.test

import com.pulselog.data.NotificationSettings
import com.pulselog.domain.NotificationScheduler

class FakeNotificationScheduler : NotificationScheduler {
    val appliedSettings = mutableListOf<NotificationSettings>()
    var cancelAllCallCount = 0

    override suspend fun apply(settings: NotificationSettings) {
        appliedSettings += settings
    }

    override suspend fun cancelAll() {
        cancelAllCallCount += 1
    }
}
