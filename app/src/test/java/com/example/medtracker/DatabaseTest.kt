package com.example.medtracker

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.medtracker.data.*
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.*
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.time.*

@RunWith(RobolectricTestRunner::class) class DatabaseTest {
    private lateinit var db:MedTrackerDatabase;private lateinit var repository:MedTrackerRepository
    @Before fun setup(){db=Room.inMemoryDatabaseBuilder(ApplicationProvider.getApplicationContext(),MedTrackerDatabase::class.java).allowMainThreadQueries().build();repository=MedTrackerRepository(db.dao())}
    @After fun close()=db.close()
    @Test fun treatmentAndIntakePersistAndCanBeCorrectedAndCancelled()= runTest {
        val id=repository.create("Médicament Exemple","","1 unité",LocalDate.now(),null,com.example.medtracker.domain.ScheduleMode.FIXED_TIMES,null,listOf("Matin" to LocalTime.of(8,0)))
        Assert.assertEquals("Médicament Exemple",repository.treatments.first().single().medication.name)
        repository.markTaken(id,"dose",Instant.parse("2026-01-01T08:00:00Z"));val event=repository.intakes.first().single();repository.correct(event,Instant.parse("2026-01-01T09:00:00Z"));Assert.assertEquals(9,repository.intakes.first().single().takenAt.atZone(ZoneOffset.UTC).hour)
        repository.cancel(event.id);Assert.assertTrue(repository.intakes.first().isEmpty())
    }
}
