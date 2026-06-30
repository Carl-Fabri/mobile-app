package com.example.utpstudywork.core

import com.example.utpstudywork.domain.model.SessionType

data class ClassificationResult(
    val suggested: SessionType,
    val confidence: Float,   // 0.0–1.0 (0 = sin palabras reconocidas)
    val source: ClassificationSource
)

enum class ClassificationSource { KEYWORDS, LEARNED, COMBINED, NONE }

/**
 * Clasificador de texto en dos capas:
 *   1. Keywords estáticos con pesos (base de conocimiento)
 *   2. Frecuencias aprendidas del usuario (Naive Bayes simplificado)
 *
 * learnedFreq: Map<"WORK"|"STUDY", Map<palabra, conteo>>
 */
class NoteClassifier(
    private val learnedFreq: Map<String, Map<String, Int>> = emptyMap()
) {

    // ── Keywords estáticos ────────────────────────────────────────────────────
    // Peso 3 = señal fuerte, 2 = moderada, 1 = débil

    private val workKeywords: Map<String, Int> = mapOf(
        // Reuniones / gestión
        "reunión" to 3, "reuniones" to 3, "meeting" to 3, "junta" to 3,
        "cliente" to 3, "clientes" to 3, "proveedor" to 3,
        "deadline" to 3, "entrega" to 3, "vencimiento" to 3,
        // Documentos
        "informe" to 2, "reporte" to 2, "propuesta" to 2, "contrato" to 3,
        "presupuesto" to 3, "factura" to 3, "cotización" to 3,
        // Empresa
        "empresa" to 2, "negocio" to 2, "oficina" to 2, "office" to 2,
        "jefe" to 2, "gerente" to 2, "director" to 2, "manager" to 2,
        // Proyectos / dev
        "proyecto" to 2, "sprint" to 3, "deploy" to 2, "bug" to 2,
        "feature" to 2, "ticket" to 2, "pull" to 2, "release" to 2,
        // Ventas / marketing
        "ventas" to 3, "marketing" to 2, "campaña" to 2, "revenue" to 3,
        // Comunicación
        "correo" to 1, "email" to 1, "llamada" to 2, "call" to 2,
        "presentación" to 2, "trabajo" to 1, "laboral" to 2
    )

    private val studyKeywords: Map<String, Int> = mapOf(
        // Evaluaciones
        "examen" to 3, "exámenes" to 3, "exam" to 3, "parcial" to 3,
        "quiz" to 3, "test" to 2, "midterm" to 3, "evaluación" to 2,
        "calificación" to 3, "nota" to 1, "grado" to 1,
        // Material
        "apuntes" to 3, "resumen" to 2, "capítulo" to 2, "chapter" to 2,
        "lectura" to 2, "libro" to 2, "bibliografía" to 3, "paper" to 2,
        "artículo" to 2, "ensayo" to 2, "tesis" to 3,
        // Institución
        "universidad" to 3, "facultad" to 3, "semestre" to 3, "carrera" to 2,
        "clase" to 2, "profesor" to 2, "cátedra" to 3, "materia" to 2,
        "asignatura" to 3, "ciclo" to 2, "campus" to 2,
        // Actividades
        "estudio" to 2, "estudiar" to 2, "práctica" to 2, "laboratorio" to 2,
        "investigación" to 2, "homework" to 3, "tarea" to 1,
        "assignment" to 3, "lecture" to 2, "library" to 2, "biblioteca" to 2
    )

    // ── Clasificación ─────────────────────────────────────────────────────────

    fun classify(title: String, description: String): ClassificationResult {
        if (title.isBlank() && description.isBlank()) {
            return ClassificationResult(SessionType.WORK, 0f, ClassificationSource.NONE)
        }

        val tokens = tokenize("$title $description")

        val keywordScores = keywordScore(tokens)
        val learnedScores = learnedScore(tokens)

        // Combinar: keywords base + aprendizaje ponderado
        val workTotal = keywordScores.first + learnedScores.first * 2f
        val studyTotal = keywordScores.second + learnedScores.second * 2f
        val total = workTotal + studyTotal

        val source = when {
            total == 0f -> ClassificationSource.NONE
            learnedScores.first == 0f && learnedScores.second == 0f -> ClassificationSource.KEYWORDS
            keywordScores.first == 0f && keywordScores.second == 0f -> ClassificationSource.LEARNED
            else -> ClassificationSource.COMBINED
        }

        if (total == 0f) {
            return ClassificationResult(SessionType.WORK, 0f, ClassificationSource.NONE)
        }

        return if (workTotal >= studyTotal) {
            ClassificationResult(SessionType.WORK, workTotal / total, source)
        } else {
            ClassificationResult(SessionType.STUDY, studyTotal / total, source)
        }
    }

    // ── Internos ──────────────────────────────────────────────────────────────

    private fun keywordScore(tokens: List<String>): Pair<Float, Float> {
        var work = 0f
        var study = 0f
        for (token in tokens) {
            work += workKeywords.getOrDefault(token, 0)
            study += studyKeywords.getOrDefault(token, 0)
        }
        return work to study
    }

    private fun learnedScore(tokens: List<String>): Pair<Float, Float> {
        // Manejar que solo exista una categoría entrenada — no cancelar con early return
        val workFreq  = learnedFreq["WORK"]  ?: emptyMap()
        val studyFreq = learnedFreq["STUDY"] ?: emptyMap()
        if (workFreq.isEmpty() && studyFreq.isEmpty()) return 0f to 0f

        val totalWork  = workFreq.values.sum().toFloat().coerceAtLeast(1f)
        val totalStudy = studyFreq.values.sum().toFloat().coerceAtLeast(1f)

        var work = 0f
        var study = 0f
        for (token in tokens) {
            work  += (workFreq.getOrDefault(token, 0).toFloat()  / totalWork)
            study += (studyFreq.getOrDefault(token, 0).toFloat() / totalStudy)
        }
        return work to study
    }

    companion object {
        fun tokenize(text: String): List<String> =
            text.lowercase()
                .split(Regex("[^a-záéíóúüñ]+"))
                .filter { it.length > 3 }
    }
}
