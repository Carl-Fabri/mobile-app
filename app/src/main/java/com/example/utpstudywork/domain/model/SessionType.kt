package com.example.utpstudywork.domain.model

enum class SessionType(val defaultMinutes: Int) {
    WORK(25),
    STUDY(45),
    UNIDENTIFIED(25);   // categoría sin clasificar, usa tiempo de WORK por defecto

    val defaultSeconds: Int get() = defaultMinutes * 60
}
