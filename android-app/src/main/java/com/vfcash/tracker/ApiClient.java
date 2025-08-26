package com.vfcash.tracker;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.util.Log;

import com.google.gson.Gson;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.List;

public class ApiClient {
    private static final String TAG = "ApiClient";
    private static final String PREFS_NAME = "api_settings";
    private static final String KEY_BASE_URL = "base_url";
    private static final String DEFAULT_BASE_URL = "http://192.168.1.100:3000"; // Change to your server IP
    
    private static ApiClient instance;
    private SharedPreferences prefs;
    private Gson gson;
    private String baseUrl;

    private ApiClient(Context context) {
        prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        gson = new Gson();
        baseUrl = prefs.getString(KEY_BASE_URL, DEFAULT_BASE_URL);
    }

    public static synchronized ApiClient getInstance(Context context) {
        if (instance == null) {
            instance = new ApiClient(context.getApplicationContext());
        }
        return instance;
    }

    public void setBaseUrl(String url) {
        this.baseUrl = url;
        prefs.edit().putString(KEY_BASE_URL, url).apply();
        Log.d(TAG, "Base URL set to: " + url);
    }

    public String getBaseUrl() {
        return baseUrl;
    }

    // Sync single transaction
    public void syncTransaction(Transaction transaction) {
        new SyncTransactionTask().execute(transaction);
    }

    // Sync all transactions
    public void syncAllTransactions(Context context) {
        TransactionManager transactionManager = TransactionManager.getInstance(context);
        List<Transaction> transactions = transactionManager.getAllTransactions();
        new SyncAllTransactionsTask().execute(transactions.toArray(new Transaction[0]));
    }

    // Sync limits
    public void syncLimits(Context context) {
        LimitsManager limitsManager = LimitsManager.getInstance(context);
        LimitsData limitsData = new LimitsData(
            limitsManager.getDailyTransferLimit(),
            limitsManager.getMonthlyTransferLimit(),
            limitsManager.getDailyReceiveLimit(),
            limitsManager.getMonthlyReceiveLimit()
        );
        new SyncLimitsTask().execute(limitsData);
    }

    // AsyncTask for syncing single transaction
    private class SyncTransactionTask extends AsyncTask<Transaction, Void, Boolean> {
        @Override
        protected Boolean doInBackground(Transaction... transactions) {
            if (transactions.length == 0) return false;
            
            try {
                Transaction transaction = transactions[0];
                String jsonData = gson.toJson(transaction);
                
                URL url = new URL(baseUrl + "/api/transactions");
                HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                
                connection.setRequestMethod("POST");
                connection.setRequestProperty("Content-Type", "application/json");
                connection.setDoOutput(true);
                
                // Send data
                try (OutputStream os = connection.getOutputStream()) {
                    byte[] input = jsonData.getBytes(StandardCharsets.UTF_8);
                    os.write(input, 0, input.length);
                }
                
                int responseCode = connection.getResponseCode();
                Log.d(TAG, "Sync response code: " + responseCode);
                
                if (responseCode == HttpURLConnection.HTTP_OK) {
                    // Read response
                    try (BufferedReader br = new BufferedReader(
                            new InputStreamReader(connection.getInputStream(), StandardCharsets.UTF_8))) {
                        StringBuilder response = new StringBuilder();
                        String responseLine;
                        while ((responseLine = br.readLine()) != null) {
                            response.append(responseLine.trim());
                        }
                        Log.d(TAG, "Sync response: " + response.toString());
                    }
                    return true;
                } else {
                    Log.e(TAG, "Sync failed with response code: " + responseCode);
                    return false;
                }
                
            } catch (IOException e) {
                Log.e(TAG, "Error syncing transaction: " + e.getMessage(), e);
                return false;
            }
        }

        @Override
        protected void onPostExecute(Boolean success) {
            if (success) {
                Log.d(TAG, "Transaction synced successfully");
            } else {
                Log.e(TAG, "Failed to sync transaction");
            }
        }
    }

    // AsyncTask for syncing all transactions
    private class SyncAllTransactionsTask extends AsyncTask<Transaction, Void, Boolean> {
        @Override
        protected Boolean doInBackground(Transaction... transactions) {
            try {
                String jsonData = gson.toJson(transactions);
                
                URL url = new URL(baseUrl + "/api/transactions/bulk");
                HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                
                connection.setRequestMethod("POST");
                connection.setRequestProperty("Content-Type", "application/json");
                connection.setDoOutput(true);
                
                // Send data
                try (OutputStream os = connection.getOutputStream()) {
                    byte[] input = jsonData.getBytes(StandardCharsets.UTF_8);
                    os.write(input, 0, input.length);
                }
                
                int responseCode = connection.getResponseCode();
                Log.d(TAG, "Bulk sync response code: " + responseCode);
                
                return responseCode == HttpURLConnection.HTTP_OK;
                
            } catch (IOException e) {
                Log.e(TAG, "Error syncing all transactions: " + e.getMessage(), e);
                return false;
            }
        }

        @Override
        protected void onPostExecute(Boolean success) {
            if (success) {
                Log.d(TAG, "All transactions synced successfully");
            } else {
                Log.e(TAG, "Failed to sync all transactions");
            }
        }
    }

    // AsyncTask for syncing limits
    private class SyncLimitsTask extends AsyncTask<LimitsData, Void, Boolean> {
        @Override
        protected Boolean doInBackground(LimitsData... limitsData) {
            if (limitsData.length == 0) return false;
            
            try {
                String jsonData = gson.toJson(limitsData[0]);
                
                URL url = new URL(baseUrl + "/api/limits");
                HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                
                connection.setRequestMethod("POST");
                connection.setRequestProperty("Content-Type", "application/json");
                connection.setDoOutput(true);
                
                // Send data
                try (OutputStream os = connection.getOutputStream()) {
                    byte[] input = jsonData.getBytes(StandardCharsets.UTF_8);
                    os.write(input, 0, input.length);
                }
                
                int responseCode = connection.getResponseCode();
                Log.d(TAG, "Limits sync response code: " + responseCode);
                
                return responseCode == HttpURLConnection.HTTP_OK;
                
            } catch (IOException e) {
                Log.e(TAG, "Error syncing limits: " + e.getMessage(), e);
                return false;
            }
        }

        @Override
        protected void onPostExecute(Boolean success) {
            if (success) {
                Log.d(TAG, "Limits synced successfully");
            } else {
                Log.e(TAG, "Failed to sync limits");
            }
        }
    }

    // Data class for limits
    public static class LimitsData {
        private double dailyTransferLimit;
        private double monthlyTransferLimit;
        private double dailyReceiveLimit;
        private double monthlyReceiveLimit;

        public LimitsData(double dailyTransferLimit, double monthlyTransferLimit,
                         double dailyReceiveLimit, double monthlyReceiveLimit) {
            this.dailyTransferLimit = dailyTransferLimit;
            this.monthlyTransferLimit = monthlyTransferLimit;
            this.dailyReceiveLimit = dailyReceiveLimit;
            this.monthlyReceiveLimit = monthlyReceiveLimit;
        }

        // Getters
        public double getDailyTransferLimit() { return dailyTransferLimit; }
        public double getMonthlyTransferLimit() { return monthlyTransferLimit; }
        public double getDailyReceiveLimit() { return dailyReceiveLimit; }
        public double getMonthlyReceiveLimit() { return monthlyReceiveLimit; }
    }

    // Test connection to server
    public void testConnection(ConnectionTestCallback callback) {
        new TestConnectionTask(callback).execute();
    }

    public interface ConnectionTestCallback {
        void onResult(boolean success, String message);
    }

    private class TestConnectionTask extends AsyncTask<Void, Void, String> {
        private ConnectionTestCallback callback;
        private boolean success = false;

        public TestConnectionTask(ConnectionTestCallback callback) {
            this.callback = callback;
        }

        @Override
        protected String doInBackground(Void... voids) {
            try {
                URL url = new URL(baseUrl + "/api/health");
                HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                connection.setRequestMethod("GET");
                connection.setConnectTimeout(5000);
                connection.setReadTimeout(5000);
                
                int responseCode = connection.getResponseCode();
                if (responseCode == HttpURLConnection.HTTP_OK) {
                    success = true;
                    return "Connection successful";
                } else {
                    return "Server responded with code: " + responseCode;
                }
                
            } catch (IOException e) {
                return "Connection failed: " + e.getMessage();
            }
        }

        @Override
        protected void onPostExecute(String result) {
            if (callback != null) {
                callback.onResult(success, result);
            }
        }
    }
}
