package com.example.medtracker

import com.example.medtracker.domain.*
import org.junit.Assert.*
import org.junit.Test
import java.time.*

class SchedulingEngineTest {
    private val zone=ZoneId.of("Europe/Paris")
    private val engine=SchedulingEngine()
    private fun item(mode:ScheduleMode=ScheduleMode.FIXED_TIMES,interval:Long?=null,end:LocalDate?=null,active:Boolean=true)=TreatmentWithRules(
        Medication(1,"Médicament Exemple"),Treatment(2,1,"1 unité",LocalDate.of(2026,3,1),end,mode,interval,active),
        listOf(ScheduleRule(3,2,"Matin",LocalTime.of(8,0)),ScheduleRule(4,2,"Soir",LocalTime.of(20,0))))
    @Test fun fixedTimesAreChronological(){val values=engine.dosesForDay(item(),LocalDate.of(2026,3,2),zone);assertEquals(listOf("Matin","Soir"),values.map{it.label})}
    @Test fun lateIntakeDoesNotMoveFixedTime(){val late=IntakeEvent(1,2,null,Instant.parse("2026-03-02T11:00:00Z"));val values=engine.dosesForDay(item(),LocalDate.of(2026,3,2),zone,late);assertEquals(LocalTime.of(20,0),values[1].scheduledAt.atZone(zone).toLocalTime())}
    @Test fun intervalUsesActualIntake(){val last=IntakeEvent(1,2,null,Instant.parse("2026-03-02T10:15:00Z"));val values=engine.dosesForDay(item(ScheduleMode.INTERVAL_FROM_LAST,480),LocalDate.of(2026,3,2),ZoneOffset.UTC,last);assertEquals(Instant.parse("2026-03-02T18:15:00Z"),values.single().scheduledAt)}
    @Test fun intervalCanCrossDay(){val last=IntakeEvent(1,2,null,Instant.parse("2026-03-02T22:00:00Z"));assertEquals(1,engine.dosesForDay(item(ScheduleMode.INTERVAL_FROM_LAST,180),LocalDate.of(2026,3,3),ZoneOffset.UTC,last).size)}
    @Test fun endedOrInactiveProducesNothing(){assertTrue(engine.dosesForDay(item(end=LocalDate.of(2026,3,1)),LocalDate.of(2026,3,2),zone).isEmpty());assertTrue(engine.dosesForDay(item(active=false),LocalDate.of(2026,3,2),zone).isEmpty())}
    @Test fun daylightSavingUsesLocalClock(){val dose=engine.dosesForDay(item(),LocalDate.of(2026,3,29),zone).first();assertEquals(LocalTime.of(8,0),dose.scheduledAt.atZone(zone).toLocalTime())}
    @Test fun warningIsFactual(){val treatment=item(interval=360).treatment;val prior=IntakeEvent(1,2,null,Instant.parse("2026-03-02T10:00:00Z"));assertEquals("Selon l’intervalle de 6 h que vous avez configuré, cette prise est espacée de 3 h 30.",engine.intervalWarning(treatment,prior,Instant.parse("2026-03-02T13:30:00Z")))}
}
