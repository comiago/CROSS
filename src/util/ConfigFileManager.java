package util;

import java.io.FileInputStream;
import java.io.IOException;
import java.lang.reflect.Field;
import java.util.Properties;


public class ConfigFileManager {
    public static <T> T loadConfig(String configFilePath, Class<T> configClass) throws IOException, IllegalAccessException, InstantiationException {
        // Carica il file di configurazione
        Properties properties = new Properties();
        try (FileInputStream fis = new FileInputStream(configFilePath)) {
            properties.load(fis);
        }

        // Crea una nuova istanza della classe di configurazione
        T configObject = configClass.newInstance();

        // Mappa le proprietà ai campi della classe di configurazione
        for (Field field : configClass.getDeclaredFields()) {
            String propertyValue = properties.getProperty(field.getName());
            if (propertyValue != null) {
                field.setAccessible(true); // Permette di accedere ai campi privati
                if (field.getType() == String.class) {
                    field.set(configObject, propertyValue);
                } else if (field.getType() == int.class) {
                    field.set(configObject, Integer.parseInt(propertyValue));
                }
                // Aggiungi altre conversioni se necessario (es. boolean, long, ecc.)
            }
        }

        return configObject;
    }
}
