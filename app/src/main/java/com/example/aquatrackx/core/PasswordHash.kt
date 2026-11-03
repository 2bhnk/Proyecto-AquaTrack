package com.example.aquatrackx.core

import java.security.MessageDigest

/**
 * Hash simple SHA-256 con sal para las credenciales ficticias del MVP (pregunta 33:
 * autenticación con usuarios ficticios, sin información real de la empresa).
 * No sustituye a un backend real, pero evita guardar contraseñas en texto plano.
 */
object PasswordHash {

    private const val SAL = "AquaTrackX-DSY1105"

    fun hash(password: String): String {
        val digest = MessageDigest.getInstance("SHA-256")
        val bytes = digest.digest((SAL + password).toByteArray(Charsets.UTF_8))
        return bytes.joinToString("") { "%02x".format(it) }
    }

    fun verificar(password: String, hashAlmacenado: String): Boolean =
        hash(password) == hashAlmacenado
}
