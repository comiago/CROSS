package util;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public class Hash {

    /**
     * Calcola l'hash SHA-256 di una stringa di input e restituisce
     * il risultato come stringa esadecimale.
     *
     * @param input la stringa da hashare
     * @return hash SHA-256 in formato esadecimale
     * @throws RuntimeException se l'algoritmo SHA-256 non è disponibile
     */
    public static String sha256(String input) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hashBytes = digest.digest(input.getBytes());

            // Conversione dei byte in stringa esadecimale
            StringBuilder hexString = new StringBuilder();
            for (byte b : hashBytes) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) hexString.append('0'); // aggiunge zero se singolo carattere
                hexString.append(hex);
            }
            return hexString.toString();

        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("Algoritmo SHA-256 non disponibile", e);
        }
    }
}
