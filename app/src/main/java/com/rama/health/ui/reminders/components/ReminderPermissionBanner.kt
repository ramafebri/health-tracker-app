package com.rama.health.ui.reminders.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.rama.health.R
import com.rama.health.ui.theme.HealthTrackerAppTheme

enum class ReminderPermissionBannerType {
    NOTIFICATIONS,
    EXACT_ALARM,
}

@Composable
fun ReminderPermissionBanner(
    type: ReminderPermissionBannerType,
    onActionClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val titleRes = when (type) {
        ReminderPermissionBannerType.NOTIFICATIONS -> R.string.reminder_notification_permission_title
        ReminderPermissionBannerType.EXACT_ALARM -> R.string.reminder_exact_alarm_permission_title
    }
    val messageRes = when (type) {
        ReminderPermissionBannerType.NOTIFICATIONS -> R.string.reminder_notification_permission_message
        ReminderPermissionBannerType.EXACT_ALARM -> R.string.reminder_exact_alarm_permission_message
    }
    val actionRes = when (type) {
        ReminderPermissionBannerType.NOTIFICATIONS -> R.string.reminder_notification_permission_action
        ReminderPermissionBannerType.EXACT_ALARM -> R.string.reminder_exact_alarm_permission_action
    }

    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.secondaryContainer,
        ),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(
                text = stringResource(titleRes),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSecondaryContainer,
            )
            Text(
                text = stringResource(messageRes),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSecondaryContainer,
            )
            Button(onClick = onActionClick) {
                Text(stringResource(actionRes))
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun ReminderPermissionBannerPreview() {
    HealthTrackerAppTheme {
        ReminderPermissionBanner(
            type = ReminderPermissionBannerType.NOTIFICATIONS,
            onActionClick = {},
            modifier = Modifier.padding(16.dp),
        )
    }
}
