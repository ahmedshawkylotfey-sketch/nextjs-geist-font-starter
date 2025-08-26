package com.vfcash.tracker;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

public class TransactionManager {
    private static final String TAG = "TransactionManager";
    private static final String PREFS_NAME = "vfcash_transactions";
    private static final String KEY_TRANSACTIONS = "transactions";
    
    private static TransactionManager instance;
    private SharedPreferences prefs;
    private Gson gson;
    private List<Transaction> transactions;

    private TransactionManager(Context context) {
        prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        gson = new Gson();
        loadTransactions();
    }

    public static synchronized TransactionManager getInstance(Context context) {
        if (instance == null) {
            instance = new TransactionManager(context.getApplicationContext());
        }
        return instance;
    }

    private void loadTransactions() {
        String json = prefs.getString(KEY_TRANSACTIONS, "[]");
        Type listType = new TypeToken<List<Transaction>>(){}.getType();
        transactions = gson.fromJson(json, listType);
        
        if (transactions == null) {
            transactions = new ArrayList<>();
        }
        
        Log.d(TAG, "Loaded " + transactions.size() + " transactions");
    }

    private void saveTransactions() {
        String json = gson.toJson(transactions);
        prefs.edit().putString(KEY_TRANSACTIONS, json).apply();
        Log.d(TAG, "Saved " + transactions.size() + " transactions");
    }

    public void addTransaction(Transaction transaction) {
        if (transaction != null) {
            transactions.add(0, transaction); // Add to beginning for newest first
            saveTransactions();
            Log.d(TAG, "Added new transaction: " + transaction.toString());
        }
    }

    public List<Transaction> getAllTransactions() {
        return new ArrayList<>(transactions);
    }

    public List<Transaction> getTransactionsForDate(Date date) {
        List<Transaction> dayTransactions = new ArrayList<>();
        Calendar targetCal = Calendar.getInstance();
        targetCal.setTime(date);
        
        Calendar transCal = Calendar.getInstance();
        
        for (Transaction transaction : transactions) {
            transCal.setTime(transaction.getDate());
            
            if (targetCal.get(Calendar.YEAR) == transCal.get(Calendar.YEAR) &&
                targetCal.get(Calendar.DAY_OF_YEAR) == transCal.get(Calendar.DAY_OF_YEAR)) {
                dayTransactions.add(transaction);
            }
        }
        
        return dayTransactions;
    }

    public List<Transaction> getTransactionsForMonth(int year, int month) {
        List<Transaction> monthTransactions = new ArrayList<>();
        Calendar transCal = Calendar.getInstance();
        
        for (Transaction transaction : transactions) {
            transCal.setTime(transaction.getDate());
            
            if (transCal.get(Calendar.YEAR) == year && 
                transCal.get(Calendar.MONTH) == month) {
                monthTransactions.add(transaction);
            }
        }
        
        return monthTransactions;
    }

    public double getTotalTransferredToday() {
        return getTotalTransferredForDate(new Date());
    }

    public double getTotalTransferredForDate(Date date) {
        List<Transaction> dayTransactions = getTransactionsForDate(date);
        double total = 0;
        
        for (Transaction transaction : dayTransactions) {
            if ("transfer".equals(transaction.getType())) {
                total += transaction.getAmount();
            }
        }
        
        return total;
    }

    public double getTotalTransferredThisMonth() {
        Calendar cal = Calendar.getInstance();
        return getTotalTransferredForMonth(cal.get(Calendar.YEAR), cal.get(Calendar.MONTH));
    }

    public double getTotalTransferredForMonth(int year, int month) {
        List<Transaction> monthTransactions = getTransactionsForMonth(year, month);
        double total = 0;
        
        for (Transaction transaction : monthTransactions) {
            if ("transfer".equals(transaction.getType())) {
                total += transaction.getAmount();
            }
        }
        
        return total;
    }

    public double getTotalReceivedToday() {
        return getTotalReceivedForDate(new Date());
    }

    public double getTotalReceivedForDate(Date date) {
        List<Transaction> dayTransactions = getTransactionsForDate(date);
        double total = 0;
        
        for (Transaction transaction : dayTransactions) {
            if ("received".equals(transaction.getType())) {
                total += transaction.getAmount();
            }
        }
        
        return total;
    }

    public double getTotalReceivedThisMonth() {
        Calendar cal = Calendar.getInstance();
        return getTotalReceivedForMonth(cal.get(Calendar.YEAR), cal.get(Calendar.MONTH));
    }

    public double getTotalReceivedForMonth(int year, int month) {
        List<Transaction> monthTransactions = getTransactionsForMonth(year, month);
        double total = 0;
        
        for (Transaction transaction : monthTransactions) {
            if ("received".equals(transaction.getType())) {
                total += transaction.getAmount();
            }
        }
        
        return total;
    }

    public void clearAllTransactions() {
        transactions.clear();
        saveTransactions();
        Log.d(TAG, "Cleared all transactions");
    }

    public int getTransactionCount() {
        return transactions.size();
    }

    public Transaction getLatestTransaction() {
        if (!transactions.isEmpty()) {
            return transactions.get(0);
        }
        return null;
    }
}
