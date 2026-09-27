package com.example.medtracker.domain

import java.time.*

class SchedulingEngine {
    fun dosesForDay(item: TreatmentWithRules, date: LocalDate, zone: ZoneId, lastIntake: IntakeEvent? = null): List<ScheduledDose> {
        val treatment = item.treatment
        if (!treatment.active || date < treatment.startDate || treatment.endDate?.let { date > it } == true) return emptyList()
        return when (treatment.scheduleMode) {
            ScheduleMode.FIXED_TIMES -> item.rules.filter { it.enabled && it.timeOfDay != null }.sortedWith(compareBy({ it.timeOfDay }, { it.sortOrder })).map { rule ->
                val at = date.atTime(rule.timeOfDay).atZone(zone).toInstant()
                ScheduledDose("${treatment.id}:${rule.id}:$date", treatment.id, rule.id, at, rule.label)
            }
            ScheduleMode.INTERVAL_FROM_LAST -> {
                val interval = treatment.minIntervalMinutes ?: return emptyList()
                val next = lastIntake?.takenAt?.plusSeconds(interval * 60) ?: return emptyList()
                if (next.atZone(zone).toLocalDate() == date) listOf(ScheduledDose("${treatment.id}:interval:${next.epochSecond}", treatment.id, null, next, item.rules.firstOrNull()?.label ?: "Prochaine prise")) else emptyList()
            }
        }
    }

    fun intervalWarning(treatment: Treatment, previous: IntakeEvent?, proposedAt: Instant): String? {
        val configured = treatment.minIntervalMinutes ?: return null
        val prior = previous ?: return null
        val actual = Duration.between(prior.takenAt, proposedAt).toMinutes()
        if (actual >= configured) return null
        return "Selon l’intervalle de ${format(configured)} que vous avez configuré, cette prise est espacée de ${format(actual.coerceAtLeast(0))}."
    }

    private fun format(minutes: Long): String = buildString {
        val h = minutes / 60; val m = minutes % 60
        if (h > 0) append("$h h")
        if (h > 0 && m > 0) append(" ")
        if (m > 0 || h == 0L) append("$m min")
    }
}
