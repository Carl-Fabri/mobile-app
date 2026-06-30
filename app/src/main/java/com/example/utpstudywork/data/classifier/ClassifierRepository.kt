package com.example.utpstudywork.data.classifier

import com.example.utpstudywork.core.ClassificationResult
import com.example.utpstudywork.core.ClassificationSource
import com.example.utpstudywork.core.NoteClassifier
import com.example.utpstudywork.data.local.dao.WordFrequencyDao
import com.example.utpstudywork.data.local.entity.WordFrequencyEntity
import com.example.utpstudywork.domain.model.SessionType

class ClassifierRepository(private val dao: WordFrequencyDao) {

    @Volatile
    private var classifier = NoteClassifier(emptyMap())

    @Volatile
    private var initialized = false

    // Carga las frecuencias aprendidas desde Room y reconstruye el clasificador
    suspend fun initialize() {
        val rows = dao.getAll()
        val map = rows
            .groupBy { it.category }
            .mapValues { (_, entries) -> entries.associate { it.word to it.count } }
        classifier = NoteClassifier(map)
        initialized = true
    }

    // Clasifica un texto dado título + descripción
    // Llama a initialize() la primera vez (lazy load)
    suspend fun classify(title: String, description: String): ClassificationResult {
        if (!initialized) initialize()
        return classifier.classify(title, description)
    }

    /**
     * El usuario guardó/corrigió la nota con la categoría [correct].
     * Actualizamos las frecuencias de las palabras del texto para esa categoría.
     * Esto entrena el Naive Bayes incremental.
     */
    suspend fun learn(title: String, description: String, correct: SessionType) {
        val words = NoteClassifier.tokenize("$title $description")
        if (words.isEmpty()) return

        words.forEach { word ->
            val existing = dao.get(word, correct.name)
            dao.upsert(
                existing?.copy(count = existing.count + 1)
                    ?: WordFrequencyEntity(word = word, category = correct.name, count = 1)
            )
        }

        // Invalidar caché y recargar
        initialized = false
        initialize()
    }

    // Estadísticas de aprendizaje para debug / UI informativa
    suspend fun stats(): ClassifierStats {
        if (!initialized) initialize()
        val rows = dao.getAll()
        val workWords = rows.filter { it.category == "WORK" }
        val studyWords = rows.filter { it.category == "STUDY" }
        return ClassifierStats(
            workTokens = workWords.size,
            studyTokens = studyWords.size,
            totalObservations = rows.sumOf { it.count }
        )
    }

    data class ClassifierStats(
        val workTokens: Int,
        val studyTokens: Int,
        val totalObservations: Int
    )
}
