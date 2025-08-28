package util;

import com.google.gson.*;
import com.google.gson.reflect.TypeToken;
import model.*;

import java.io.*;
import java.lang.reflect.Modifier;
import java.lang.reflect.Type;
import java.util.*;

public class OrderStorage {
    private static final String EXECUTED_ORDERS_FILE = "src/server/executedOrders.json";
    private static final String PENDING_LIMIT_ORDERS_FILE = "src/server/pendingLimitOrders.json";
    private static final String PENDING_STOP_ORDERS_FILE = "src/server/pendingStopOrders.json";

    private static final Gson gson = new GsonBuilder()
            .setPrettyPrinting()
            .excludeFieldsWithModifiers(Modifier.TRANSIENT, Modifier.STATIC)
            .create();

    // =========================
    // Executed Orders (CLOSED)
    // =========================

    public static synchronized void appendToExecutedOrders(ExecutedTrade trade) {
        List<ExecutedTrade> trades = loadExecutedTrades();
        trades.add(trade);
        saveExecutedTrades(trades);
    }

    public static List<ExecutedTrade> loadExecutedTrades() {
        try (Reader reader = new FileReader(EXECUTED_ORDERS_FILE)) {
            Type listType = new TypeToken<List<ExecutedTrade>>() {}.getType();
            List<ExecutedTrade> trades = gson.fromJson(reader, listType);
            return trades != null ? trades : new ArrayList<>();
        } catch (IOException e) {
            return new ArrayList<>();
        }
    }


    private static void saveExecutedTrades(List<ExecutedTrade> trades) {
        try (Writer writer = new FileWriter(EXECUTED_ORDERS_FILE)) {
            gson.toJson(trades, writer);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // =========================
    // Pending Limit Orders (OPEN)
    // =========================

    public static synchronized void savePendingLimitOrders(OrderBook book) {
        Map<String, List<LimitOrder>> pending = new HashMap<>();
        pending.put("bids", flatten(book.getLimitBids()));
        pending.put("asks", flatten(book.getLimitAsks()));

        try (Writer writer = new FileWriter(PENDING_LIMIT_ORDERS_FILE)) {
            gson.toJson(pending, writer);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static Map<String, List<LimitOrder>> loadPendingLimitOrders() {
        try (Reader reader = new FileReader(PENDING_LIMIT_ORDERS_FILE)) {
            Type mapType = new TypeToken<Map<String, List<LimitOrder>>>() {}.getType();
            return gson.fromJson(reader, mapType);
        } catch (IOException e) {
            return new HashMap<>();
        }
    }

    // =========================
    // Pending Stop Orders (OPEN)
    // =========================

    public static synchronized void savePendingStopOrders(OrderBook book) {
        Map<String, List<StopOrder>> pending = new HashMap<>();
        pending.put("bids", flatten(book.getStopBids()));
        pending.put("asks", flatten(book.getStopAsks()));

        try (Writer writer = new FileWriter(PENDING_STOP_ORDERS_FILE)) {
            gson.toJson(pending, writer);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static Map<String, List<StopOrder>> loadPendingStopOrders() {
        try (Reader reader = new FileReader(PENDING_STOP_ORDERS_FILE)) {
            Type mapType = new TypeToken<Map<String, List<StopOrder>>>() {}.getType();
            return gson.fromJson(reader, mapType);
        } catch (IOException e) {
            return new HashMap<>();
        }
    }

    // =========================
    // Utility: flatten map
    // =========================

    private static <T> List<T> flatten(Map<Integer, List<T>> map) {
        List<T> result = new ArrayList<>();
        for (List<T> orders : map.values()) {
            result.addAll(orders);
        }
        return result;
    }
}
