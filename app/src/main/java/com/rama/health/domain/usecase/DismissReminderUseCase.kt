package com.rama.health.domain.usecase

import javax.inject.Inject

class DismissReminderUseCase @Inject constructor() {
    suspend operator fun invoke(notificationId: Int) {
        // No-op: notification dismissal is handled by the system or data layer.
    }
}
