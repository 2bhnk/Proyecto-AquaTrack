package com.example.aquatrackx.domain.model

/**
 * Modelos de dominio compartidos por toda la aplicación (data / domain / ui).
 * Mantenerlos en un paquete neutral evita acoplar la lógica a la interfaz o a la persistencia.
 */

/** Roles de usuario del caso (pregunta 13): un rol principal por usuario (pregunta 15). */
enum class Rol(val titulo: String) {
    OPERARIO("Operario"),
    SUPERVISOR("Supervisor"),
    JEFATURA("Jefatura")
}

/** Estados del cloro libre según rangos del MVP (pregunta 1). */
enum class EstadoCloro(val titulo: String) {
    NORMAL("Normal"),
    ADVERTENCIA("Advertencia"),
    CRITICO("Crítico")
}

/** Tipo de punto de medición (pregunta 7). */
enum class TipoPunto(val titulo: String) {
    TANQUE("Tanque"),
    LINEA("Línea de distribución"),
    BEDEDERO("Bebedero")
}

/**
 * Tipo de desviación detectada (pregunta 5): un valor bajo y uno alto generan
 * alertas distintas para poder diferenciar la acción correctiva.
 */
enum class TipoDesviacion(val titulo: String) {
    INSUFICIENTE("Cloro insuficiente"),
    EXCESIVO("Cloro excesivo")
}

/** Estado de ciclo de vida de una alerta (preguntas 21-22). */
enum class EstadoAlerta(val titulo: String) {
    ABIERTA("Abierta"),
    CERRADA("Cerrada")
}

/** Estado de una acción correctiva (pregunta 23). */
enum class EstadoAccion(val titulo: String) {
    PENDIENTE("Pendiente"),
    COMPLETADA("Completada"),
    VERIFICADA("Verificada")
}

/** Origen de una medición: API simulada o ingreso manual (pregunta 11). */
enum class OrigenMedicion(val titulo: String) {
    API("API simulada"),
    MANUAL("Manual")
}

/** Períodos de historial soportados (pregunta 27). */
enum class PeriodoHistorial(val titulo: String, val horas: Long) {
    H24("Últimas 24 h", 24),
    D7("Últimos 7 días", 24 * 7),
    D30("Últimos 30 días", 24 * 30)
}
