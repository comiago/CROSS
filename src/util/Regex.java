package util;

/**
 * Utility per validazioni con espressioni regolari.
 */
public final class Regex {

    // Costruttore privato per evitare istanziazione
    private Regex() { }

    /**
     * Verifica se la stringa rappresenta un numero valido.
     * Supporta interi e decimali, sia positivi che negativi.
     *
     * @param input la stringa da verificare
     * @return true se è un numero valido, false altrimenti
     */
    public static boolean isNumber(String input) {
        if (input == null || input.trim().isEmpty()) {
            return false;
        }

        // Regex:
        // -?      -> opzionale segno negativo
        // \d+     -> almeno una cifra
        // (\.\d+)? -> opzionale parte decimale con almeno una cifra
        return input.trim().matches("-?\\d+(\\.\\d+)?");
    }
}
