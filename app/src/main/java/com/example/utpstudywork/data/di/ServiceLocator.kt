package com.example.utpstudywork.data.di

import android.content.Context
import com.example.utpstudywork.core.PomodoroNotificationManager
import com.example.utpstudywork.core.TimerEngine
import com.example.utpstudywork.data.classifier.ClassifierRepository
import com.example.utpstudywork.data.local.AppDatabase
import com.example.utpstudywork.data.repository.NotesRepositoryRoom
import com.example.utpstudywork.data.repository.PlannerRepositoryRoom
import com.example.utpstudywork.data.repository.StatsRepositoryInMemory
import com.example.utpstudywork.domain.repository.NotesRepository
import com.example.utpstudywork.domain.repository.PlannerRepository
import com.example.utpstudywork.domain.repository.StatsRepository
import com.example.utpstudywork.domain.usecase.AddNoteUseCase
import com.example.utpstudywork.domain.usecase.GetUpcomingNotesUseCase
import com.example.utpstudywork.domain.usecase.PauseTimerUseCase
import com.example.utpstudywork.domain.usecase.StartTimerUseCase
import com.example.utpstudywork.domain.usecase.StopTimerUseCase

data class UseCases(
    val startTimer: StartTimerUseCase,
    val pauseTimer: PauseTimerUseCase,
    val stopTimer: StopTimerUseCase,
    val addNote: AddNoteUseCase,
    val getUpcomingNotes: GetUpcomingNotesUseCase,
    val statsRepo: StatsRepository,
    val notesRepo: NotesRepository,
    val notificationManager: PomodoroNotificationManager,
    val classifier: ClassifierRepository,
    val plannerRepo: PlannerRepository
)

object ServiceLocator {
    @Volatile private var instance: UseCases? = null

    fun provideUseCases(context: Context): UseCases =
        instance ?: synchronized(this) {
            instance ?: buildUseCases(context).also { instance = it }
        }

    private fun buildUseCases(context: Context): UseCases {
        val db = AppDatabase.getInstance(context)
        val notesRepo: NotesRepository = NotesRepositoryRoom(db.noteDao(), db.taskDao())
        val statsRepo: StatsRepository = StatsRepositoryInMemory()
        val timer = TimerEngine()

        return UseCases(
            startTimer = StartTimerUseCase(timer),
            pauseTimer = PauseTimerUseCase(timer),
            stopTimer = StopTimerUseCase(timer),
            addNote = AddNoteUseCase(notesRepo),
            getUpcomingNotes = GetUpcomingNotesUseCase(notesRepo),
            statsRepo = statsRepo,
            notesRepo = notesRepo,
            notificationManager = PomodoroNotificationManager(context),
            classifier = ClassifierRepository(db.wordFrequencyDao()),
            plannerRepo = PlannerRepositoryRoom(db.weekDao(), db.objectiveDao(), db.noteDao())
        )
    }
}
