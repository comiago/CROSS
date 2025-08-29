package util;

import com.google.gson.*;
import com.google.gson.reflect.TypeToken;
import model.*;

import java.io.*;
import java.lang.reflect.Modifier;
import java.lang.reflect.Type;
import java.util.*;

/**
 * Utility per la persistenza degli ordini e delle transazioni.
 * Gestisce:
 * - Ordini eseguiti (ExecutedTrade) → "storico"
 * - Ordini pendenti Limit e Stop → "open orders"
 */
public class OrderStorage {

    private static final String EXECUTED_ORDERS_FILE = "src/server/executedOrders.json";
    private static final String PENDING_LIMIT_ORDERS_FILE = "src/server/pendingLimitOrders.json";
    private static final String PENDING_STOP_ORDERS_FILE = "src/server/pendingStopOrders.json";

    private static final Gson gson = new GsonBuilder()
            .setPrettyPrinting()
            .excludeFieldsWithModifiers(Modifier.TRANSIENT, Modifier.STATIC)
            .create();

    // =========================
    // Executed Orders (storico)
    // =========================

    /** Aggiunge un trade eseguito alla lista persistente */
    public static synchronized void appendToExecutedOrders(ExecutedTrade trade) {
        List<ExecutedTrade> trades = loadExecutedTrades();
        trades.add(trade);
        saveExecutedTrades(trades);
    }

    /** Carica i trade eseguiti dal file, ritorna lista vuota se assente */
    public static List<ExecutedTrade> loadExecutedTrades() {
        try (Reader reader = new FileReader(EXECUTED_ORDERS_FILE)) {
            Type listType = new TypeToken<List<ExecutedTrade>>() {}.getType();
            List<ExecutedTrade> trades = gson.fromJson(reader, listType);
            return trades != null ? trades : new ArrayList<>();
        } catch (IOException e) {
            // Log di debug e ritorno lista vuota
            System.err.println("Errore caricamento executedOrders.json: " + e.getMessage());
            return new ArrayList<>();
        }
    }

    /** Salva tutti i trade eseguiti su file */
    private static void saveExecutedTrades(List<ExecutedTrade> trades) {
        try (Writer writer = new FileWriter(EXECUTED_ORDERS_FILE)) {
            gson.toJson(trades, writer);
        } catch (IOException e) {
            System.err.println("Errore salvataggio executedOrders.json: " + e.getMessage());
        }
    }

    // =========================
    // Pending Limit Orders
    // =========================

    public static synchronized void savePendingLimitOrders(OrderBook book) {
        Map<String, List<LimitOrder>> pending = new HashMap<>();
        pending.put("bids", flatten(book.getLimitBids()));
        pending.put("asks", flatten(book.getLimitAsks()));

        try (Writer writer = new FileWriter(PENDING_LIMIT_ORDERS_FILE)) {
            gson.toJson(pending, writer);
        } catch (IOException e) {
            System.err.println("Errore salvataggio pendingLimitOrders.json: " + e.getMessage());
        }
    }

    public static Map<String, List<LimitOrder>> loadPendingLimitOrders() {
        try (Reader reader = new FileReader(PENDING_LIMIT_ORDERS_FILE)) {
            Type mapType = new TypeToken<Map<String, List<LimitOrder>>>() {}.getType();
            Map<String, List<LimitOrder>> result = gson.fromJson(reader, mapType);
            return result != null ? result : new HashMap<>();
        } catch (IOException e) {
            System.err.println("Errore caricamento pendingLimitOrders.json: " + e.getMessage());
            return new HashMap<>();
        }
    }

    // =========================
    // Pending Stop Orders
    // =========================

    public static synchronized void savePendingStopOrders(OrderBook book) {
        Map<String, List<StopOrder>> pending = new HashMap<>();
        pending.put("bids", flatten(book.getStopBids()));
        pending.put("asks", flatten(book.getStopAsks()));

        try (Writer writer = new FileWriter(PENDING_STOP_ORDERS_FILE)) {
            gson.toJson(pending, writer);
        } catch (IOException e) {
            System.err.println("Errore salvataggio pendingStopOrders.json: " + e.getMessage());
        }
    }

    public static Map<String, List<StopOrder>> loadPendingStopOrders() {
        try (Reader reader = new FileReader(PENDING_STOP_ORDERS_FILE)) {
            Type mapType = new TypeToken<Map<String, List<StopOrder>>>() {}.getType();
            Map<String, List<StopOrder>> result = gson.fromJson(reader, mapType);
            return result != null ? result : new HashMap<>();
        } catch (IOException e) {
            System.err.println("Errore caricamento pendingStopOrders.json: " + e.getMessage());
            return new HashMap<>();
        }
    }

    // =========================
    // Utility
    // =========================

    /** Converte una mappa di liste (price -> ordini) in lista piatta */
    private static <T> List<T> flatten(Map<Integer, List<T>> map) {
        List<T> result = new ArrayList<>();
        for (List<T> orders : map.values()) {
            result.addAll(orders);
        }
        return result;
    }
}
