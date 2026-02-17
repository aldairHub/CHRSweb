package org.uteq.backend.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.Cipher;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.util.Base64;

/**
 * Servicio para cifrar/descifrar contraseñas de BD usando AES-256
 */
@Service
public class AesCipherService {

    // Algoritmo AES con CBC y padding PKCS5
    private static final String ALGORITHM  = "AES/CBC/PKCS5Padding";
    private static final String KEY_ALG    = "AES";

    // IV fijo (16 bytes) - puedes cambiarlo pero debe ser consistente
    private static final byte[] IV = {
            0x00, 0x01, 0x02, 0x03, 0x04, 0x05, 0x06, 0x07,
            0x08, 0x09, 0x0A, 0x0B, 0x0C, 0x0D, 0x0E, 0x0F
    };

    private final SecretKeySpec secretKey;

    public AesCipherService(@Value("${app.security.aes-secret}") String secret) {
        // Asegurar que la clave tenga exactamente 32 bytes (AES-256)
        byte[] keyBytes = secret.getBytes();
        byte[] key = new byte[32];
        System.arraycopy(keyBytes, 0, key, 0, Math.min(keyBytes.length, 32));
        this.secretKey = new SecretKeySpec(key, KEY_ALG);
    }

    /**
     * Cifra una contraseña en texto plano
     * @param textPlano Contraseña real del usuario BD
     * @return Contraseña cifrada en Base64 para guardar en tabla usuarios
     */
    public String cifrar(String textPlano) {
        try {
            Cipher cipher = Cipher.getInstance(ALGORITHM);
            cipher.init(Cipher.ENCRYPT_MODE, secretKey, new IvParameterSpec(IV));
            byte[] encrypted = cipher.doFinal(textPlano.getBytes());
            return Base64.getEncoder().encodeToString(encrypted);
        } catch (Exception e) {
            throw new RuntimeException("Error al cifrar contraseña BD: " + e.getMessage(), e);
        }
    }

    /**
     * Descifra una contraseña cifrada
     * @param textoCifrado Contraseña cifrada guardada en tabla usuarios
     * @return Contraseña real en texto plano para hacer switch de conexión
     */
    public String descifrar(String textoCifrado) {
        try {
            Cipher cipher = Cipher.getInstance(ALGORITHM);
            cipher.init(Cipher.DECRYPT_MODE, secretKey, new IvParameterSpec(IV));
            byte[] decrypted = cipher.doFinal(Base64.getDecoder().decode(textoCifrado));
            return new String(decrypted);
        } catch (Exception e) {
            throw new RuntimeException("Error al descifrar contraseña BD: " + e.getMessage(), e);
        }
    }
}