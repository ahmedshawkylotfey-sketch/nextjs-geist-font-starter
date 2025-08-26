package com.vfcash.tracker;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class SmsParser {
    
    // Patterns for different types of VF-Cash messages
    private static final Pattern TRANSFER_PATTERN = Pattern.compile(
        "EGP\\s+(\\d+(?:\\.\\d+)?)\\s+has been transferred to number\\s+(\\d+).*?" +
        "Service fees are\\s+(\\d+(?:\\.\\d+)?)\\s+EGP.*?" +
        "Your current Vodafone Cash account balance is\\s+(\\d+(?:\\.\\d+)?)",
        Pattern.CASE_INSENSITIVE | Pattern.DOTALL
    );
    
    private static final Pattern RECEIVED_PATTERN = Pattern.compile(
        "EGP\\s+(\\d+(?:\\.\\d+)?)\\s+has been received from number\\s+(\\d+)(?:;\\s*registered to\\s+([^.]+))?.*?" +
        "Your current balance is\\s+(\\d+(?:\\.\\d+)?)\\s+EGP.*?" +
        "Transaction date\\s+(\\d{2}/\\d{2}/\\d{2})\\s+(\\d{2}:\\d{2}).*?" +
        "Transaction number:\\s*(\\d+)",
        Pattern.CASE_INSENSITIVE | Pattern.DOTALL
    );

    public static Transaction parseTransferMessage(String smsText) throws ParseException {
        Matcher matcher = TRANSFER_PATTERN.matcher(smsText);
        
        if (!matcher.find()) {
            throw new ParseException("Transfer message format not recognized", 0);
        }

        try {
            Transaction transaction = new Transaction();
            transaction.setType("transfer");
            
            // Extract amount
            double amount = Double.parseDouble(matcher.group(1));
            transaction.setAmount(amount);
            
            // Extract phone number
            String phoneNumber = matcher.group(2);
            transaction.setPhoneNumber(phoneNumber);
            
            // Extract service fees
            double serviceFees = Double.parseDouble(matcher.group(3));
            transaction.setServiceFees(serviceFees);
            
            // Extract current balance (after transaction)
            double balanceAfter = Double.parseDouble(matcher.group(4));
            transaction.setBalanceAfter(balanceAfter);
            
            // Calculate balance before transaction
            double balanceBefore = balanceAfter + amount + serviceFees;
            transaction.setBalanceBefore(balanceBefore);
            
            return transaction;
            
        } catch (NumberFormatException e) {
            throw new ParseException("Error parsing numeric values from transfer message", 0);
        }
    }

    public static Transaction parseReceivedMessage(String smsText) throws ParseException {
        Matcher matcher = RECEIVED_PATTERN.matcher(smsText);
        
        if (!matcher.find()) {
            throw new ParseException("Received message format not recognized", 0);
        }

        try {
            Transaction transaction = new Transaction();
            transaction.setType("received");
            
            // Extract amount
            double amount = Double.parseDouble(matcher.group(1));
            transaction.setAmount(amount);
            
            // Extract phone number
            String phoneNumber = matcher.group(2);
            transaction.setPhoneNumber(phoneNumber);
            
            // Extract sender name (optional)
            String senderName = matcher.group(3);
            if (senderName != null) {
                transaction.setSenderName(senderName.trim());
            }
            
            // Extract current balance (after transaction)
            double balanceAfter = Double.parseDouble(matcher.group(4));
            transaction.setBalanceAfter(balanceAfter);
            
            // Calculate balance before transaction
            double balanceBefore = balanceAfter - amount;
            transaction.setBalanceBefore(balanceBefore);
            
            // Extract and parse date
            String dateStr = matcher.group(5); // MM/dd/yy format
            String timeStr = matcher.group(6); // HH:mm format
            
            SimpleDateFormat dateFormat = new SimpleDateFormat("MM/dd/yy HH:mm", Locale.getDefault());
            Date transactionDate = dateFormat.parse(dateStr + " " + timeStr);
            transaction.setDate(transactionDate);
            
            // Extract transaction number
            String transactionNumber = matcher.group(7);
            transaction.setTransactionNumber(transactionNumber);
            
            return transaction;
            
        } catch (NumberFormatException | java.text.ParseException e) {
            throw new ParseException("Error parsing values from received message: " + e.getMessage(), 0);
        }
    }

    public static Transaction parseSmsMessage(String smsText) throws ParseException {
        // Check if it's a VF-Cash message
        if (!isVfCashMessage(smsText)) {
            throw new ParseException("Not a VF-Cash message", 0);
        }
        
        // Try to parse as transfer message first
        try {
            return parseTransferMessage(smsText);
        } catch (ParseException e) {
            // If transfer parsing fails, try received message
            try {
                return parseReceivedMessage(smsText);
            } catch (ParseException e2) {
                throw new ParseException("Unable to parse VF-Cash message format", 0);
            }
        }
    }

    private static boolean isVfCashMessage(String smsText) {
        String lowerText = smsText.toLowerCase();
        return lowerText.contains("vodafone cash") || 
               lowerText.contains("vf-cash") ||
               (lowerText.contains("egp") && 
                (lowerText.contains("transferred") || lowerText.contains("received")));
    }

    // Utility method to validate phone number format
    public static boolean isValidPhoneNumber(String phoneNumber) {
        return phoneNumber != null && 
               phoneNumber.matches("^01[0-9]{9}$"); // Egyptian mobile number format
    }
}
