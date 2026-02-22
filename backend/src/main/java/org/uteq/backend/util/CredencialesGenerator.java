package org.uteq.backend.util;

import java.security.SecureRandom;
import java.util.Random;

public class CredencialesGenerator {
    private static final String CARACTERES_USUARIO = "abcdefghijklmnopqrstuvwxyz0123456789";
    private static final String CARACTERES_CLAVE = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789@#$%&*";
    private static final Random RANDOM = new SecureRandom();

    /**
     * Genera un nombre de usuario basado en la cédula
     * Formato: usuario + últimos 6 dígitos de cédula
     * Ejemplo: usuario123456
     */
    public static String generarUsuario(String cedula) {
        if (cedula == null || cedula.length() < 6) {
            throw new IllegalArgumentException("La cédula debe tener al menos 6 caracteres");
        }
        String ultimosDigitos = cedula.substring(cedula.length() - 6);
        return "usuario" + ultimosDigitos;
    }

    /**
     * Genera una contraseña aleatoria segura
     * Formato: 8 caracteres alfanuméricos + símbolos
     */
    public static String generarClaveApp() {
        StringBuilder clave = new StringBuilder(12);

        // Al menos una mayúscula
        clave.append((char) ('A' + RANDOM.nextInt(26)));

        // Al menos una minúscula
        clave.append((char) ('a' + RANDOM.nextInt(26)));

        // Al menos un número
        clave.append((char) ('0' + RANDOM.nextInt(10)));

        // Al menos un símbolo
        clave.append("@#$%&*".charAt(RANDOM.nextInt(6)));

        // Resto de caracteres aleatorios
        for (int i = 0; i < 8; i++) {
            clave.append(CARACTERES_CLAVE.charAt(RANDOM.nextInt(CARACTERES_CLAVE.length())));
        }

        // Mezclar los caracteres
        return mezclarString(clave.toString());
    }

    /**
     * Genera una clave para la base de datos
     * Formato similar a la imagen: combinación de letras y números
     */
//    public static String generarClaveBd() {
//        StringBuilder clave = new StringBuilder(50);
//
//        for (int i = 0; i < 50; i++) {
//            if (RANDOM.nextBoolean()) {
//                // Letra mayúscula o minúscula
//                if (RANDOM.nextBoolean()) {
//                    clave.append((char) ('A' + RANDOM.nextInt(26)));
//                } else {
//                    clave.append((char) ('a' + RANDOM.nextInt(26)));
//                }
//            } else {
//                // Número
//                clave.append(RANDOM.nextInt(10));
//            }
//        }
//
//        return clave.toString();
//    }

    /**
     * Mezcla aleatoriamente los caracteres de un string
     */
    private static String mezclarString(String input) {
        char[] caracteres = input.toCharArray();
        for (int i = caracteres.length - 1; i > 0; i--) {
            int j = RANDOM.nextInt(i + 1);
            char temp = caracteres[i];
            caracteres[i] = caracteres[j];
            caracteres[j] = temp;
        }
        return new String(caracteres);
    }
}
