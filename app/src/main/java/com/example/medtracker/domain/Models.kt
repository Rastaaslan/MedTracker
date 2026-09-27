package com.example.medtracker.domain

import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime

enum class ScheduleMode { FIXED_TIMES, INTERVAL_FROM_LAST }

data class Medication(val id: Long = 0, val name: String, val optionalNotes: String = "", val createdAt: Instant = Instant.now(), val updatedAt: Instant = Instant.now())
data class Treatment(val id: Long = 0, val medicationId: Long, val doseText: String, val startDate: LocalDate, val endDate: LocalDate? = null, val scheduleMode: ScheduleMode, val minIntervalMinutes: Long? = null, val active: Boolean = true, val createdAt: Instant = Instant.now(), val updatedAt: Instant = Instant.now())
data class ScheduleRule(val id: Long = 0, val treatmentId: Long, val label: String, val timeOfDay: LocalTime? = null, val sortOrder: Int = 0, val enabled: Boolean = true)
data class ScheduledDose(val key: String, val treatmentId: Long, val scheduleRuleId: Long?, val scheduledAt: Instant, val label: String)
data class IntakeEvent(val id: Long = 0, val treatmentId: Long, val scheduledDoseKey: String?, val takenAt: Instant, val createdAt: Instant = Instant.now(), val updatedAt: Instant = Instant.now())

data class TreatmentWithRules(val medication: Medication, val treatment: Treatment, val rules: List<ScheduleRule>)
