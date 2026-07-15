package com.rama.health.service.reminder

object ReminderAlarmRequestCodes {

    const val WATER_ENTITY_ID = "water"

    /** Upper bound when sweeping fixed water slots during [cancelWater]. */
    const val MAX_WATER_FIXED_SLOTS = 24

    /** Upper bound when sweeping medication time slots during [cancelMedication]. */
    const val MAX_MEDICATION_SLOTS = 24

    enum class Type {
        WATER,
        MEDICATION,
    }

    /**
     * Stable [PendingIntent] request code derived from reminder [type], [entityId], and [slotIndex].
     */
    fun requestCode(type: Type, entityId: String, slotIndex: Int): Int {
        var hash = type.ordinal
        hash = 31 * hash + entityId.hashCode()
        hash = 31 * hash + slotIndex
        return hash and 0x7FFFFFFF
    }
}
