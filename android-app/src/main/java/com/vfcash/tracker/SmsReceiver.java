package com.vfcash.tracker;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.telephony.SmsMessage;
import android.util.Log;
import android.widget.Toast;

import java.text.ParseException;

public class SmsReceiver extends BroadcastReceiver {
    
    private static final String TAG = "SmsReceiver";
    private static final String SMS_RECEIVED = "android.provider.Telephony.SMS_RECEIVED";

    @Override
    public void onReceive(Context context, Intent intent) {
        if (SMS_RECEIVED.equals(intent.getAction())) {
            Bundle bundle = intent.getExtras();
            
            if (bundle != null) {
                try {
                    Object[] pdus = (Object[]) bundle.get("pdus");
                    String format = bundle.getString("format");
                    
                    if (pdus != null) {
                        for (Object pdu : pdus) {
                            SmsMessage smsMessage;
                            
                            // Handle different Android versions
                            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
                                smsMessage = SmsMessage.createFromPdu((byte[]) pdu, format);
                            } else {
                                smsMessage = SmsMessage.createFromPdu((byte[]) pdu);
                            }
                            
                            if (smsMessage != null) {
                                String messageBody = smsMessage.getMessageBody();
                                String sender = smsMessage.getOriginatingAddress();
                                
                                Log.d(TAG, "SMS received from: " + sender);
                                Log.d(TAG, "SMS body: " + messageBody);
                                
                                // Process VF-Cash messages
                                processVfCashMessage(context, messageBody, sender);
                            }
                        }
                    }
                } catch (Exception e) {
                    Log.e(TAG, "Error processing SMS: " + e.getMessage(), e);
                }
            }
        }
    }

    private void processVfCashMessage(Context context, String messageBody, String sender) {
        try {
            // Parse the SMS message
            Transaction transaction = SmsParser.parseSmsMessage(messageBody);
            
            if (transaction != null) {
                Log.d(TAG, "VF-Cash transaction parsed: " + transaction.toString());
                
                // Save transaction to local storage
                TransactionManager.getInstance(context).addTransaction(transaction);
                
                // Update daily/monthly limits
                LimitsManager.getInstance(context).updateLimitsAfterTransaction(transaction);
                
                // Show notification to user
                showTransactionNotification(context, transaction);
                
                // Optionally sync to web dashboard immediately
                syncToWebDashboard(context, transaction);
                
            }
        } catch (ParseException e) {
            Log.d(TAG, "SMS not a VF-Cash message or parsing failed: " + e.getMessage());
            // This is normal - not all SMS are VF-Cash messages
        } catch (Exception e) {
            Log.e(TAG, "Error processing VF-Cash message: " + e.getMessage(), e);
            Toast.makeText(context, "Error processing VF-Cash transaction", Toast.LENGTH_SHORT).show();
        }
    }

    private void showTransactionNotification(Context context, Transaction transaction) {
        String message;
        if ("transfer".equals(transaction.getType())) {
            message = String.format("Transfer: EGP %.2f to %s. Balance: EGP %.2f", 
                    transaction.getAmount(), 
                    transaction.getPhoneNumber(), 
                    transaction.getBalanceAfter());
        } else {
            message = String.format("Received: EGP %.2f from %s. Balance: EGP %.2f", 
                    transaction.getAmount(), 
                    transaction.getPhoneNumber(), 
                    transaction.getBalanceAfter());
        }
        
        Toast.makeText(context, message, Toast.LENGTH_LONG).show();
    }

    private void syncToWebDashboard(Context context, Transaction transaction) {
        // This will be handled by the ApiClient in a background thread
        try {
            ApiClient.getInstance(context).syncTransaction(transaction);
        } catch (Exception e) {
            Log.e(TAG, "Failed to sync transaction to web dashboard: " + e.getMessage(), e);
        }
    }
}
