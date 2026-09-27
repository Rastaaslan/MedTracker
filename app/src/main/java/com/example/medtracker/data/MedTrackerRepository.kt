package com.example.medtracker.data

import com.example.medtracker.domain.*
import kotlinx.coroutines.flow.*
import java.time.*

class MedTrackerRepository(private val dao: MedTrackerDao) {
    val treatments = dao.observeTreatments().map { rows -> rows.map { it.domain() } }
    val intakes = dao.observeIntakes().map { rows -> rows.map { it.domain() } }

    suspend fun create(name:String, notes:String, dose:String, start:LocalDate, end:LocalDate?, mode:ScheduleMode, interval:Long?, rules:List<Pair<String,LocalTime?>>):Long {
        require(name.isNotBlank() && dose.isNotBlank() && rules.isNotEmpty())
        require(mode != ScheduleMode.INTERVAL_FROM_LAST || (interval ?: 0) > 0)
        val now=System.currentTimeMillis()
        val medicationId=dao.insertMedication(MedicationEntity(name=name.trim(),notes=notes.trim(),createdAt=now,updatedAt=now))
        val treatmentId=dao.insertTreatment(TreatmentEntity(medicationId=medicationId,doseText=dose.trim(),startDate=start.toString(),endDate=end?.toString(),scheduleMode=mode.name,minIntervalMinutes=interval,active=true,createdAt=now,updatedAt=now))
        dao.insertRules(rules.mapIndexed { i,r -> ScheduleRuleEntity(treatmentId=treatmentId,label=r.first.trim(),timeOfDay=r.second?.toString(),sortOrder=i,enabled=true) })
        return treatmentId
    }
    suspend fun markTaken(treatmentId:Long,key:String?,at:Instant=Instant.now())=dao.insertIntake(IntakeEventEntity(treatmentId=treatmentId,scheduledDoseKey=key,takenAt=at.toEpochMilli(),createdAt=System.currentTimeMillis(),updatedAt=System.currentTimeMillis()))
    suspend fun update(item:TreatmentWithRules,name:String,notes:String,dose:String,start:LocalDate,end:LocalDate?,mode:ScheduleMode,interval:Long?,rules:List<Pair<String,LocalTime?>>){
        val now=System.currentTimeMillis();require(name.isNotBlank()&&dose.isNotBlank()&&rules.isNotEmpty())
        dao.updateMedication(MedicationEntity(item.medication.id,name.trim(),notes.trim(),item.medication.createdAt.toEpochMilli(),now))
        dao.updateTreatment(TreatmentEntity(item.treatment.id,item.medication.id,dose.trim(),start.toString(),end?.toString(),mode.name,interval,item.treatment.active,item.treatment.createdAt.toEpochMilli(),now))
        dao.deleteRules(item.treatment.id);dao.insertRules(rules.mapIndexed{i,r->ScheduleRuleEntity(treatmentId=item.treatment.id,label=r.first,timeOfDay=r.second?.toString(),sortOrder=i,enabled=true)})
    }
    suspend fun correct(event:IntakeEvent,at:Instant)=dao.updateIntake(IntakeEventEntity(event.id,event.treatmentId,event.scheduledDoseKey,at.toEpochMilli(),event.createdAt.toEpochMilli(),System.currentTimeMillis()))
    suspend fun cancel(id:Long)=dao.deleteIntake(id)
    suspend fun archive(id:Long)=dao.archive(id,System.currentTimeMillis())
    suspend fun latest(id:Long)=dao.latestIntake(id)?.domain()
}

private fun TreatmentAggregate.domain()=TreatmentWithRules(
    Medication(medication.id,medication.name,medication.notes,Instant.ofEpochMilli(medication.createdAt),Instant.ofEpochMilli(medication.updatedAt)),
    Treatment(treatment.id,treatment.medicationId,treatment.doseText,LocalDate.parse(treatment.startDate),treatment.endDate?.let(LocalDate::parse),ScheduleMode.valueOf(treatment.scheduleMode),treatment.minIntervalMinutes,treatment.active,Instant.ofEpochMilli(treatment.createdAt),Instant.ofEpochMilli(treatment.updatedAt)),
    rules.map { ScheduleRule(it.id,it.treatmentId,it.label,it.timeOfDay?.let(LocalTime::parse),it.sortOrder,it.enabled) })
private fun IntakeEventEntity.domain()=IntakeEvent(id,treatmentId,scheduledDoseKey,Instant.ofEpochMilli(takenAt),Instant.ofEpochMilli(createdAt),Instant.ofEpochMilli(updatedAt))
