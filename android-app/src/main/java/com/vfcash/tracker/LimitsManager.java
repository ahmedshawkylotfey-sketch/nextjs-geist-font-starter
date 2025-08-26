package com.vfcash.tracker;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import java.util.Calendar;
import java.util.Date;

public class LimitsManager {
    private static final String TAG = "LimitsManager";
    private static final String PREFS_NAME = "vfcash_limits";
    
    // Limit keys
    private static final String KEY_DAILY_TRANSFER_LIMIT = "daily_transfer_limit";
    private static final String KEY_MONTHLY_TRANSFER_LIMIT = "monthly_transfer_limit";
    private static final String KEY_DAILY_RECEIVE_LIMIT = "daily_receive_limit";
    private static final String KEY_MONTHLY_RECEIVE_LIMIT = "monthly_receive_limit";
    
    // Usage tracking keys
    private static final String KEY_LAST_RESET_DATE = "last_reset_date";
    private static final String KEY_LAST_RESET_MONTH = "last_reset_month";
    
    // Default limits (can be customized by user)
    private static final double DEFAULT_DAILY_TRANSFER_LIMIT = 5000.0;
    private static final double DEFAULT_MONTHLY_TRANSFER_LIMIT = 50000.0;
    private static final double DEFAULT_DAILY_RECEIVE_LIMIT = 10000.0;
    private static final double DEFAULT_MONTHLY_RECEIVE_LIMIT = 100000.0;
    
    private static LimitsManager instance;
    private SharedPreferences prefs;
    private TransactionManager transactionManager;

    private LimitsManager(Context context) {
        prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        transactionManager = TransactionManager.getInstance(context);
        initializeDefaultLimits();
    }

    public static synchronized LimitsManager getInstance(Context context) {
        if (instance == null) {
            instance = new LimitsManager(context.getApplicationContext());
        }
        return instance;
    }

    private void initializeDefaultLimits() {
        if (!prefs.contains(KEY_DAILY_TRANSFER_LIMIT)) {
            setDailyTransferLimit(DEFAULT_DAILY_TRANSFER_LIMIT);
        }
        if (!prefs.contains(KEY_MONTHLY_TRANSFER_LIMIT)) {
            setMonthlyTransferLimit(DEFAULT_MONTHLY_TRANSFER_LIMIT);
        }
        if (!prefs.contains(KEY_DAILY_RECEIVE_LIMIT)) {
            setDailyReceiveLimit(DEFAULT_DAILY_RECEIVE_LIMIT);
        }
        if (!prefs.contains(KEY_MONTHLY_RECEIVE_LIMIT)) {
            setMonthlyReceiveLimit(DEFAULT_MONTHLY_RECEIVE_LIMIT);
        }
    }

    // Getters for limits
    public double getDailyTransferLimit() {
        return prefs.getFloat(KEY_DAILY_TRANSFER_LIMIT, (float) DEFAULT_DAILY_TRANSFER_LIMIT);
    }

    public double getMonthlyTransferLimit() {
        return prefs.getFloat(KEY_MONTHLY_TRANSFER_LIMIT, (float) DEFAULT_MONTHLY_TRANSFER_LIMIT);
    }

    public double getDailyReceiveLimit() {
        return prefs.getFloat(KEY_DAILY_RECEIVE_LIMIT, (float) DEFAULT_DAILY_RECEIVE_LIMIT);
    }

    public double getMonthlyReceiveLimit() {
        return prefs.getFloat(KEY_MONTHLY_RECEIVE_LIMIT, (float) DEFAULT_MONTHLY_RECEIVE_LIMIT);
    }

    // Setters for limits
    public void setDailyTransferLimit(double limit) {
        prefs.edit().putFloat(KEY_DAILY_TRANSFER_LIMIT, (float) limit).apply();
        Log.d(TAG, "Daily transfer limit set to: " + limit);
    }

    public void setMonthlyTransferLimit(double limit) {
        prefs.edit().putFloat(KEY_MONTHLY_TRANSFER_LIMIT, (float) limit).apply();
        Log.d(TAG, "Monthly transfer limit set to: " + limit);
    }

    public void setDailyReceiveLimit(double limit) {
        prefs.edit().putFloat(KEY_DAILY_RECEIVE_LIMIT, (float) limit).apply();
        Log.d(TAG, "Daily receive limit set to: " + limit);
    }

    public void setMonthlyReceiveLimit(double limit) {
        prefs.edit().putFloat(KEY_MONTHLY_RECEIVE_LIMIT, (float) limit).apply();
        Log.d(TAG, "Monthly receive limit set to: " + limit);
    }

    // Calculate remaining limits
    public double getRemainingDailyTransferLimit() {
        double limit = getDailyTransferLimit();
        double used = transactionManager.getTotalTransferredToday();
        return Math.max(0, limit - used);
    }

    public double getRemainingMonthlyTransferLimit() {
        double limit = getMonthlyTransferLimit();
        double used = transactionManager.getTotalTransferredThisMonth();
        return Math.max(0, limit - used);
    }

    public double getRemainingDailyReceiveLimit() {
        double limit = getDailyReceiveLimit();
        double used = transactionManager.getTotalReceivedToday();
        return Math.max(0, limit - used);
    }

    public double getRemainingMonthlyReceiveLimit() {
        double limit = getMonthlyReceiveLimit();
        double used = transactionManager.getTotalReceivedThisMonth();
        return Math.max(0, limit - used);
    }

    // Check if transaction exceeds limits
    public boolean isTransferWithinLimits(double amount) {
        double remainingDaily = getRemainingDailyTransferLimit();
        double remainingMonthly = getRemainingMonthlyTransferLimit();
        
        return amount <= remainingDaily && amount <= remainingMonthly;
    }

    public boolean isReceiveWithinLimits(double amount) {
        double remainingDaily = getRemainingDailyReceiveLimit();
        double remainingMonthly = getRemainingMonthlyReceiveLimit();
        
        return amount <= remainingDaily && amount <= remainingMonthly;
    }

    // Update limits after transaction (for tracking purposes)
    public void updateLimitsAfterTransaction(Transaction transaction) {
        if (transaction == null) return;
        
        String type = transaction.getType();
        double amount = transaction.getAmount();
        
        if ("transfer".equals(type)) {
            double remaining = getRemainingDailyTransferLimit();
            Log.d(TAG, String.format("Transfer of %.2f EGP. Daily remaining: %.2f EGP", 
                    amount, remaining - amount));
            
            if (!isTransferWithinLimits(amount)) {
                Log.w(TAG, "Transfer exceeds daily or monthly limits!");
            }
        } else if ("received".equals(type)) {
            double remaining = getRemainingDailyReceiveLimit();
            Log.d(TAG, String.format("Received %.2f EGP. Daily remaining: %.2f EGP", 
                    amount, remaining - amount));
            
            if (!isReceiveWithinLimits(amount)) {
                Log.w(TAG, "Received amount exceeds daily or monthly limits!");
            }
        }
    }

    // Get usage percentages
    public double getDailyTransferUsagePercentage() {
        double limit = getDailyTransferLimit();
        double used = transactionManager.getTotalTransferredToday();
        return limit > 0 ? (used / limit) * 100 : 0;
    }

    public double getMonthlyTransferUsagePercentage() {
        double limit = getMonthlyTransferLimit();
        double used = transactionManager.getTotalTransferredThisMonth();
        return limit > 0 ? (used / limit) * 100 : 0;
    }

    public double getDailyReceiveUsagePercentage() {
        double limit = getDailyReceiveLimit();
        double used = transactionManager.getTotalReceivedToday();
        return limit > 0 ? (used / limit) * 100 : 0;
    }

    public double getMonthlyReceiveUsagePercentage() {
        double limit = getMonthlyReceiveLimit();
        double used = transactionManager.getTotalReceivedThisMonth();
        return limit > 0 ? (used / limit) * 100 : 0;
    }

    // Reset limits (for testing or manual reset)
    public void resetDailyLimits() {
        // This is automatically handled by the transaction manager's date-based calculations
        Log.d(TAG, "Daily limits reset");
    }

    public void resetMonthlyLimits() {
        // This is automatically handled by the transaction manager's month-based calculations
        Log.d(TAG, "Monthly limits reset");
    }

    // Get limit status summary
    public String getLimitsSummary() {
        return String.format(
            "Daily Transfer: %.0f/%.0f EGP (%.1f%%)\n" +
            "Monthly Transfer: %.0f/%.0f EGP (%.1f%%)\n" +
            "Daily Receive: %.0f/%.0f EGP (%.1f%%)\n" +
            "Monthly Receive: %.0f/%.0f EGP (%.1f%%)",
            
            transactionManager.getTotalTransferredToday(),
            getDailyTransferLimit(),
            getDailyTransferUsagePercentage(),
            
            transactionManager.getTotalTransferredThisMonth(),
            getMonthlyTransferLimit(),
            getMonthlyTransferUsagePercentage(),
            
            transactionManager.getTotalReceivedToday(),
            getDailyReceiveLimit(),
            getDailyReceiveUsagePercentage(),
            
            transactionManager.getTotalReceivedThisMonth(),
            getMonthlyReceiveLimit(),
            getMonthlyReceiveUsagePercentage()
        );
    }
}
