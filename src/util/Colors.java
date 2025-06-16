package util;

/**
 * Codici colore ANSI per stampa colorata in console.
 * Classe non istanziabile.
 */
public final class Colors {
    public static final String RESET = "\033[0m";   // Reset colore
    public static final String CYAN = "\033[36m";   // Testo ciano
    public static final String GREEN = "\033[32m";  // Testo verde
    public static final String YELLOW = "\033[33m"; // Testo giallo
    public static final String RED = "\033[31m";    // Testo rosso
    public static final String BLUE = "\033[34m";   // Testo blu

    // Costruttore privato per impedire istanziazione
    private Colors() {
        throw new AssertionError("Classe non istanziabile");
    }
}
