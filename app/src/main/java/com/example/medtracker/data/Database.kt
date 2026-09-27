package com.example.medtracker.data

import android.content.Context
import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Entity data class MedicationEntity(@PrimaryKey(autoGenerate = true) val id: Long = 0, val name: String, val notes: String, val createdAt: Long, val updatedAt: Long)
@Entity(foreignKeys=[ForeignKey(entity=MedicationEntity::class,parentColumns=["id"],childColumns=["medicationId"],onDelete=ForeignKey.CASCADE)], indices=[Index("medicationId")])
data class TreatmentEntity(@PrimaryKey(autoGenerate=true) val id:Long=0,val medicationId:Long,val doseText:String,val startDate:String,val endDate:String?,val scheduleMode:String,val minIntervalMinutes:Long?,val active:Boolean,val createdAt:Long,val updatedAt:Long)
@Entity(foreignKeys=[ForeignKey(entity=TreatmentEntity::class,parentColumns=["id"],childColumns=["treatmentId"],onDelete=ForeignKey.CASCADE)], indices=[Index("treatmentId")])
data class ScheduleRuleEntity(@PrimaryKey(autoGenerate=true) val id:Long=0,val treatmentId:Long,val label:String,val timeOfDay:String?,val sortOrder:Int,val enabled:Boolean)
@Entity(foreignKeys=[ForeignKey(entity=TreatmentEntity::class,parentColumns=["id"],childColumns=["treatmentId"],onDelete=ForeignKey.CASCADE)], indices=[Index("treatmentId"),Index(value=["scheduledDoseKey"], unique=true)])
data class IntakeEventEntity(@PrimaryKey(autoGenerate=true) val id:Long=0,val treatmentId:Long,val scheduledDoseKey:String?,val takenAt:Long,val createdAt:Long,val updatedAt:Long)

data class TreatmentAggregate(@Embedded val treatment: TreatmentEntity, @Relation(parentColumn="medicationId",entityColumn="id") val medication:MedicationEntity, @Relation(parentColumn="id",entityColumn="treatmentId") val rules:List<ScheduleRuleEntity>)

@Dao interface MedTrackerDao {
    @Transaction @Query("SELECT * FROM TreatmentEntity ORDER BY active DESC, id DESC") fun observeTreatments():Flow<List<TreatmentAggregate>>
    @Query("SELECT * FROM IntakeEventEntity ORDER BY takenAt DESC") fun observeIntakes():Flow<List<IntakeEventEntity>>
    @Query("SELECT * FROM IntakeEventEntity WHERE treatmentId=:id ORDER BY takenAt DESC LIMIT 1") suspend fun latestIntake(id:Long):IntakeEventEntity?
    @Insert suspend fun insertMedication(value:MedicationEntity):Long
    @Insert suspend fun insertTreatment(value:TreatmentEntity):Long
    @Insert suspend fun insertRules(value:List<ScheduleRuleEntity>)
    @Update suspend fun updateMedication(value:MedicationEntity)
    @Update suspend fun updateTreatment(value:TreatmentEntity)
    @Query("DELETE FROM ScheduleRuleEntity WHERE treatmentId=:id") suspend fun deleteRules(id:Long)
    @Insert suspend fun insertIntake(value:IntakeEventEntity):Long
    @Update suspend fun updateIntake(value:IntakeEventEntity)
    @Query("DELETE FROM IntakeEventEntity WHERE id=:id") suspend fun deleteIntake(id:Long)
    @Query("UPDATE TreatmentEntity SET active=0, updatedAt=:now WHERE id=:id") suspend fun archive(id:Long,now:Long)
}

@Database(entities=[MedicationEntity::class,TreatmentEntity::class,ScheduleRuleEntity::class,IntakeEventEntity::class],version=1,exportSchema=false)
abstract class MedTrackerDatabase:RoomDatabase(){abstract fun dao():MedTrackerDao
    companion object { fun create(context:Context)=Room.databaseBuilder(context,MedTrackerDatabase::class.java,"medtracker.db").build() }
}
