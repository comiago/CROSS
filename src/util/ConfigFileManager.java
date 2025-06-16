package util;

import java.io.FileInputStream;
import java.io.IOException;
import java.lang.reflect.Field;
import java.util.Properties;

public class ConfigFileManager {

    /**
     * Carica un file di configurazione e mappa le proprietà
     * nei campi della classe passata come parametro.
     *
     * @param configFilePath percorso del file di configurazione
     * @param configClass classe della configurazione
     * @param <T> tipo della configurazione
     * @return istanza popolata della configurazione
     * @throws IOException in caso di errore di I/O
     * @throws ReflectiveOperationException in caso di problemi riflessione
     */
    public static <T> T loadConfig(String configFilePath, Class<T> configClass)
            throws IOException, ReflectiveOperationException {

        Properties properties = new Properties();
        try (FileInputStream fis = new FileInputStream(configFilePath)) {
            properties.load(fis);
        }

        T configObject = configClass.getDeclaredConstructor().newInstance();

        for (Field field : configClass.getDeclaredFields()) {
            String propertyValue = properties.getProperty(field.getName());
            if (propertyValue != null) {
                field.setAccessible(true);
                Class<?> type = field.getType();

                if (type == String.class) {
                    field.set(configObject, propertyValue);
                } else if (type == int.class || type == Integer.class) {
                    field.set(configObject, Integer.parseInt(propertyValue));
                } else if (type == boolean.class || type == Boolean.class) {
                    field.set(configObject, Boolean.parseBoolean(propertyValue));
                } else if (type == long.class || type == Long.class) {
                    field.set(configObject, Long.parseLong(propertyValue));
                } else if (type == double.class || type == Double.class) {
                    field.set(configObject, Double.parseDouble(propertyValue));
                } else {
                    // Se vuoi supportare altri tipi, aggiungi qui
                    throw new IllegalArgumentException("Tipo non supportato: " + type);
                }
            }
        }
        return configObject;
    }
}
