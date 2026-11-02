
package com.example.plantillaweb.util;

import org.mindrot.jbcrypt.BCrypt;

public class Password {

    private Password() {
    }

    public static String encriptar(String clave) {
        return BCrypt.hashpw(clave, BCrypt.gensalt());
    }

    public static boolean verificar(String clave, String hash) {
        return BCrypt.checkpw(clave, hash);
    }
}
