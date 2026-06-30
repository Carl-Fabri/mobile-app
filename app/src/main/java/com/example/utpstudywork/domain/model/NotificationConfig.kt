package com.example.utpstudywork.domain.model

data class NotificationConfig(
    val type: NotificationType,
    val customMessage: String = "",
    // null = se activa para cualquier tipo de sesión; WORK/STUDY = solo para esa sesión
    val filterCategory: SessionType? = null
)
