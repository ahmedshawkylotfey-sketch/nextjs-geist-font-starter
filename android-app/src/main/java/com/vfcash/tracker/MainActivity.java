package com.vfcash.tracker;

import android.Manifest;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import java.util.List;

public class MainActivity extends AppCompatActivity {
    
    private static final int SMS_PERMISSION_REQUEST_CODE = 100;
    
    private ListView transactionsList;
    private TextView limitsStatus;
    private Button syncButton;
    private Button editLimitsButton;
    private Button settingsButton;
    private Button testConnectionButton;
    
    private TransactionManager transactionManager;
    private LimitsManager limitsManager;
    private ApiClient apiClient;
    private TransactionAdapter transactionAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        
        // Initialize managers
        transactionManager = TransactionManager.getInstance(this);
        limitsManager = LimitsManager.getInstance(this);
        apiClient = ApiClient.getInstance(this);
        
        // Initialize UI components
        initializeViews();
        
        // Check and request SMS permissions
        checkSmsPermissions();
        
        // Load data
        loadTransactions();
        updateLimitsDisplay();
    }

    private void initializeViews() {
        transactionsList = findViewById(R.id.transactions_list);
        limitsStatus = findViewById(R.id.limits_status);
        syncButton = findViewById(R.id.sync_button);
        editLimitsButton = findViewById(R.id.edit_limits_button);
        settingsButton = findViewById(R.id.settings_button);
        testConnectionButton = findViewById(R.id.test_connection_button);
        
        // Set click listeners
        syncButton.setOnClickListener(v -> syncAllData());
        editLimitsButton.setOnClickListener(v -> showEditLimitsDialog());
        settingsButton.setOnClickListener(v -> showSettingsDialog());
        testConnectionButton.setOnClickListener(v -> testConnection());
    }

    private void checkSmsPermissions() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECEIVE_SMS) 
                != PackageManager.PERMISSION_GRANTED ||
            ContextCompat.checkSelfPermission(this, Manifest.permission.READ_SMS) 
                != PackageManager.PERMISSION_GRANTED) {
            
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.RECEIVE_SMS, Manifest.permission.READ_SMS},
                    SMS_PERMISSION_REQUEST_CODE);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        
        if (requestCode == SMS_PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(this, "SMS permissions granted. App will now monitor VF-Cash messages.", 
                        Toast.LENGTH_LONG).show();
            } else {
                Toast.makeText(this, "SMS permissions are required for the app to work properly.", 
                        Toast.LENGTH_LONG).show();
            }
        }
    }

    private void loadTransactions() {
        List<Transaction> transactions = transactionManager.getAllTransactions();
        transactionAdapter = new TransactionAdapter(this, transactions);
        transactionsList.setAdapter(transactionAdapter);
    }

    private void updateLimitsDisplay() {
        String limitsText = String.format(
            "Daily Transfer: %.0f/%.0f EGP (%.1f%%)\n" +
            "Monthly Transfer: %.0f/%.0f EGP (%.1f%%)\n" +
            "Daily Receive: %.0f/%.0f EGP (%.1f%%)\n" +
            "Monthly Receive: %.0f/%.0f EGP (%.1f%%)",
            
            transactionManager.getTotalTransferredToday(),
            limitsManager.getDailyTransferLimit(),
            limitsManager.getDailyTransferUsagePercentage(),
            
            transactionManager.getTotalTransferredThisMonth(),
            limitsManager.getMonthlyTransferLimit(),
            limitsManager.getMonthlyTransferUsagePercentage(),
            
            transactionManager.getTotalReceivedToday(),
            limitsManager.getDailyReceiveLimit(),
            limitsManager.getDailyReceiveUsagePercentage(),
            
            transactionManager.getTotalReceivedThisMonth(),
            limitsManager.getMonthlyReceiveLimit(),
            limitsManager.getMonthlyReceiveUsagePercentage()
        );
        
        limitsStatus.setText(limitsText);
    }

    private void syncAllData() {
        syncButton.setEnabled(false);
        syncButton.setText("Syncing...");
        
        Toast.makeText(this, "Syncing data to web dashboard...", Toast.LENGTH_SHORT).show();
        
        // Sync transactions
        apiClient.syncAllTransactions(this);
        
        // Sync limits
        apiClient.syncLimits(this);
        
        // Re-enable button after a delay
        syncButton.postDelayed(() -> {
            syncButton.setEnabled(true);
            syncButton.setText("Sync to Dashboard");
            Toast.makeText(MainActivity.this, "Sync completed", Toast.LENGTH_SHORT).show();
        }, 2000);
    }

    private void showEditLimitsDialog() {
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_edit_limits, null);
        
        EditText dailyTransferEdit = dialogView.findViewById(R.id.daily_transfer_limit);
        EditText monthlyTransferEdit = dialogView.findViewById(R.id.monthly_transfer_limit);
        EditText dailyReceiveEdit = dialogView.findViewById(R.id.daily_receive_limit);
        EditText monthlyReceiveEdit = dialogView.findViewById(R.id.monthly_receive_limit);
        
        // Set current values
        dailyTransferEdit.setText(String.valueOf((int) limitsManager.getDailyTransferLimit()));
        monthlyTransferEdit.setText(String.valueOf((int) limitsManager.getMonthlyTransferLimit()));
        dailyReceiveEdit.setText(String.valueOf((int) limitsManager.getDailyReceiveLimit()));
        monthlyReceiveEdit.setText(String.valueOf((int) limitsManager.getMonthlyReceiveLimit()));
        
        new AlertDialog.Builder(this)
                .setTitle("Edit Limits")
                .setView(dialogView)
                .setPositiveButton("Save", (dialog, which) -> {
                    try {
                        double dailyTransfer = Double.parseDouble(dailyTransferEdit.getText().toString());
                        double monthlyTransfer = Double.parseDouble(monthlyTransferEdit.getText().toString());
                        double dailyReceive = Double.parseDouble(dailyReceiveEdit.getText().toString());
                        double monthlyReceive = Double.parseDouble(monthlyReceiveEdit.getText().toString());
                        
                        limitsManager.setDailyTransferLimit(dailyTransfer);
                        limitsManager.setMonthlyTransferLimit(monthlyTransfer);
                        limitsManager.setDailyReceiveLimit(dailyReceive);
                        limitsManager.setMonthlyReceiveLimit(monthlyReceive);
                        
                        updateLimitsDisplay();
                        Toast.makeText(this, "Limits updated successfully", Toast.LENGTH_SHORT).show();
                        
                    } catch (NumberFormatException e) {
                        Toast.makeText(this, "Please enter valid numbers", Toast.LENGTH_SHORT).show();
                    }
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void showSettingsDialog() {
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_settings, null);
        
        EditText serverUrlEdit = dialogView.findViewById(R.id.server_url);
        serverUrlEdit.setText(apiClient.getBaseUrl());
        
        new AlertDialog.Builder(this)
                .setTitle("Settings")
                .setView(dialogView)
                .setPositiveButton("Save", (dialog, which) -> {
                    String newUrl = serverUrlEdit.getText().toString().trim();
                    if (!newUrl.isEmpty()) {
                        apiClient.setBaseUrl(newUrl);
                        Toast.makeText(this, "Server URL updated", Toast.LENGTH_SHORT).show();
                    }
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void testConnection() {
        testConnectionButton.setEnabled(false);
        testConnectionButton.setText("Testing...");
        
        apiClient.testConnection((success, message) -> {
            runOnUiThread(() -> {
                testConnectionButton.setEnabled(true);
                testConnectionButton.setText("Test Connection");
                
                String resultMessage = success ? "✓ " + message : "✗ " + message;
                Toast.makeText(MainActivity.this, resultMessage, Toast.LENGTH_LONG).show();
            });
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Refresh data when returning to the activity
        loadTransactions();
        updateLimitsDisplay();
    }

    public void refreshData() {
        runOnUiThread(() -> {
            loadTransactions();
            updateLimitsDisplay();
        });
    }
}
