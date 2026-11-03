package com.example.aquatrackx.core

import com.example.aquatrackx.core.validacion.AccionCorrectivaValidator
import com.example.aquatrackx.core.validacion.Campos
import com.example.aquatrackx.core.validacion.LoginValidator
import com.example.aquatrackx.core.validacion.MedicionValidator
import com.example.aquatrackx.domain.model.EstadoCloro
import com.example.aquatrackx.domain.model.TipoDesviacion
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Tests de la lógica de negocio centralizada y desacoplada de la interfaz (IE 2.2.1):
 * al no conocer Compose ni Android, estas funciones son directamente testeables con JUnit.
 */
class ChlorineRulesTest {

    // ---- Clasificación por rangos (pregunta 1) ----

    @Test
    fun `valores dentro de 3 y 5 ppm son normales`() {
        assertEquals(EstadoCloro.NORMAL, ChlorineRules.clasificar(3.0))
        assertEquals(EstadoCloro.NORMAL, ChlorineRules.clasificar(4.25))
        assertEquals(EstadoCloro.NORMAL, ChlorineRules.clasificar(5.0))
    }

    @Test
    fun `valores entre 2 y menos de 3 o sobre 5 hasta 6 son advertencia`() {
        assertEquals(EstadoCloro.ADVERTENCIA, ChlorineRules.clasificar(2.0))
        assertEquals(EstadoCloro.ADVERTENCIA, ChlorineRules.clasificar(2.99))
        assertEquals(EstadoCloro.ADVERTENCIA, ChlorineRules.clasificar(5.5))
        assertEquals(EstadoCloro.ADVERTENCIA, ChlorineRules.clasificar(6.0))
    }

    @Test
    fun `valores inferiores a 2 o superiores a 6 son criticos`() {
        assertEquals(EstadoCloro.CRITICO, ChlorineRules.clasificar(1.99))
        assertEquals(EstadoCloro.CRITICO, ChlorineRules.clasificar(0.0))
        assertEquals(EstadoCloro.CRITICO, ChlorineRules.clasificar(6.01))
        assertEquals(EstadoCloro.CRITICO, ChlorineRules.clasificar(9.0))
    }

    // ---- Formato con dos decimales (pregunta 3) ----

    @Test
    fun `el formato usa dos decimales con coma decimal`() {
        assertEquals("3,50", ChlorineRules.formatoPpm(3.5))
        assertEquals("4,00", ChlorineRules.formatoPpm(4.0))
        assertEquals("1,99", ChlorineRules.formatoPpm(1.99))
    }

    @Test
    fun `redondear conserva la precision del instrumento`() {
        assertEquals(3.5, ChlorineRules.redondear(3.504), 0.0001)
        assertEquals(3.51, ChlorineRules.redondear(3.511), 0.0001)
    }

    // ---- Desviación (pregunta 5) y condición de cierre (pregunta 22) ----

    @Test
    fun `tipoDesviacion diferencia insuficiente de excesivo`() {
        assertEquals(TipoDesviacion.INSUFICIENTE, ChlorineRules.tipoDesviacion(1.5))
        assertEquals(TipoDesviacion.EXCESIVO, ChlorineRules.tipoDesviacion(6.5))
        assertNull(ChlorineRules.tipoDesviacion(4.0))
    }

    @Test
    fun `dentroDeRango valida la medicion posterior de cierre`() {
        assertTrue(ChlorineRules.dentroDeRango(3.7))
        assertFalse(ChlorineRules.dentroDeRango(2.5))
        assertFalse(ChlorineRules.dentroDeRango(5.5))
    }

    // ---- Validadores de formularios (IE 2.2.1) ----

    @Test
    fun `loginValidator exige correo valido y password de al menos 6 caracteres`() {
        val vacio = LoginValidator.validar("", "")
        assertTrue(vacio.esValido.not())
        assertTrue(vacio.errores.containsKey(Campos.EMAIL))
        assertTrue(vacio.errores.containsKey(Campos.PASSWORD))

        val correoInvalido = LoginValidator.validar("no-es-correo", "123456")
        assertTrue(correoInvalido.errores.containsKey(Campos.EMAIL))

        val valido = LoginValidator.validar("operario@duoc.cl", "123456")
        assertTrue(valido.esValido)
    }

    @Test
    fun `medicionValidator valida punto, rango y maximo de decimales`() {
        assertTrue(
            MedicionValidator.validar(null, "3,50").errores.containsKey(Campos.PUNTO)
        )
        assertTrue(
            MedicionValidator.validar(1L, "").errores.containsKey(Campos.VALOR)
        )
        assertTrue(
            MedicionValidator.validar(1L, "abc").errores.containsKey(Campos.VALOR)
        )
        assertTrue(
            MedicionValidator.validar(1L, "25,00").errores.containsKey(Campos.VALOR)
        )
        assertTrue(
            MedicionValidator.validar(1L, "3,505").errores.containsKey(Campos.VALOR)
        )
        assertTrue(MedicionValidator.validar(1L, "3,50").esValido)
        assertEquals(3.5, MedicionValidator.aNumero("3,5"), 0.0001)
        assertEquals(3.5, MedicionValidator.aNumero("3.5"), 0.0001)
    }

    @Test
    fun `accionCorrectivaValidator exige descripcion y responsable`() {
        val corta = AccionCorrectivaValidator.validar("muy corta", "Ana")
        assertTrue(corta.errores.containsKey(Campos.DESCIPCION))

        val sinResponsable = AccionCorrectivaValidator.validar(
            "Se ajustó la dosificación de cloro y se verificó la lectura", ""
        )
        assertTrue(sinResponsable.errores.containsKey(Campos.RESPONSABLE))

        val valida = AccionCorrectivaValidator.validar(
            "Se ajustó la dosificación de cloro y se verificó la lectura",
            "Benito Supervisor"
        )
        assertTrue(valida.esValido)
    }
}
