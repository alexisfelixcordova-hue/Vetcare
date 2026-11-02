package com.example.plantillaweb.util;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PasswordTest {

    @Test
    void elHashNoEsIgualALaClaveOriginal() {
        String hash = Password.encriptar("admin123");
        assertNotEquals("admin123", hash);
    }

    @Test
    void verificaCorrectamenteLaClaveCorrecta() {
        String hash = Password.encriptar("admin123");
        assertTrue(Password.verificar("admin123", hash));
    }

    @Test
    void rechazaUnaClaveIncorrecta() {
        String hash = Password.encriptar("admin123");
        assertFalse(Password.verificar("otraClave", hash));
    }

    @Test
    void dosHashesDeLaMismaClaveSonDistintos() {
        String hash1 = Password.encriptar("admin123");
        String hash2 = Password.encriptar("admin123");
        assertNotEquals(hash1, hash2);
        assertTrue(Password.verificar("admin123", hash1));
        assertTrue(Password.verificar("admin123", hash2));
    }

    @Test
    void esSensibleAMayusculasYMinusculas() {
        String hash = Password.encriptar("admin123");
        assertFalse(Password.verificar("Admin123", hash));
    }

    @Test
    void rechazaClaveConEspaciosExtra() {
        String hash = Password.encriptar("admin123");
        assertFalse(Password.verificar(" admin123", hash));
        assertFalse(Password.verificar("admin123 ", hash));
    }

    @Test
    void funcionaConClaveVacia() {
        String hash = Password.encriptar("");
        assertTrue(Password.verificar("", hash));
        assertFalse(Password.verificar("algo", hash));
    }

    @Test
    void funcionaConCaracteresEspecialesYUnicode() {
        String clave = "Contraseña@2024!ñ#中文";
        String hash = Password.encriptar(clave);
        assertTrue(Password.verificar(clave, hash));
    }

    @Test
    void funcionaConClaveLarga() {
        String clave = "a".repeat(200);
        String hash = Password.encriptar(clave);
        assertTrue(Password.verificar(clave, hash));
        assertFalse(Password.verificar("b".repeat(200), hash));
    }

    @Test
    void elHashTieneElFormatoEsperadoDeBCrypt() {
        String hash = Password.encriptar("admin123");
        assertTrue(hash.matches("^\\$2[aby]\\$\\d{2}\\$.{53}$"),
                "El hash no tiene el formato BCrypt esperado: " + hash);
    }

    @Test
    void claveNulaAlEncriptarSeTrataComoLaPalabraNullEnVezDeFallar() {
        // Hallazgo de caja negra: BCrypt.hashpw concatena "password + ..." y la
        // concatenación de Java convierte null en el texto "null", por lo que
        // encriptar(null) no lanza excepción: genera el hash de la palabra "null".
        String hash = Password.encriptar(null);
        assertTrue(Password.verificar("null", hash));
    }

    @Test
    void claveNulaAlVerificarSeComparaComoLaPalabraNullEnVezDeFallar() {
        String hash = Password.encriptar("null");
        assertTrue(Password.verificar(null, hash));
        assertFalse(Password.verificar(null, Password.encriptar("admin123")));
    }

    @Test
    void lanzaExcepcionConHashNuloAlVerificar() {
        assertThrows(NullPointerException.class, () -> Password.verificar("admin123", null));
    }

    @Test
    void lanzaExcepcionConHashConFormatoInvalido() {
        assertThrows(IllegalArgumentException.class, () -> Password.verificar("admin123", "hash-no-valido"));
    }

    @Test
    void lanzaExcepcionConHashVacio() {
        assertThrows(StringIndexOutOfBoundsException.class, () -> Password.verificar("admin123", ""));
    }
}
