package util;

public class Regex {
    public static boolean isNumber(String input) {
        if (input == null || input.trim().isEmpty()) {
            return false;
        }

        // Regex per numeri interi e decimali (positivi e negativi)
        return input.trim().matches("-?\\d+(\\.\\d+)?");
    }
}
