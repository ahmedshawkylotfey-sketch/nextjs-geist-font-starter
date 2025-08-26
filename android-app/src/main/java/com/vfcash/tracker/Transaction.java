package com.vfcash.tracker;

import java.util.Date;

public class Transaction {
    private String id;
    private String type; // "transfer" or "received"
    private double amount;
    private String phoneNumber;
    private Date date;
    private double balanceBefore;
    private double balanceAfter;
    private String senderName; // For received transactions
    private String transactionNumber;
    private double serviceFees; // For transfer transactions

    // Constructor
    public Transaction() {
        this.id = String.valueOf(System.currentTimeMillis());
        this.date = new Date();
    }

    public Transaction(String type, double amount, String phoneNumber, 
                      double balanceBefore, double balanceAfter) {
        this();
        this.type = type;
        this.amount = amount;
        this.phoneNumber = phoneNumber;
        this.balanceBefore = balanceBefore;
        this.balanceAfter = balanceAfter;
    }

    // Getters and Setters
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getType() { return type; }
    public void setType(String type) { this.type = type; }

    public double getAmount() { return amount; }
    public void setAmount(double amount) { this.amount = amount; }

    public String getPhoneNumber() { return phoneNumber; }
    public void setPhoneNumber(String phoneNumber) { this.phoneNumber = phoneNumber; }

    public Date getDate() { return date; }
    public void setDate(Date date) { this.date = date; }

    public double getBalanceBefore() { return balanceBefore; }
    public void setBalanceBefore(double balanceBefore) { this.balanceBefore = balanceBefore; }

    public double getBalanceAfter() { return balanceAfter; }
    public void setBalanceAfter(double balanceAfter) { this.balanceAfter = balanceAfter; }

    public String getSenderName() { return senderName; }
    public void setSenderName(String senderName) { this.senderName = senderName; }

    public String getTransactionNumber() { return transactionNumber; }
    public void setTransactionNumber(String transactionNumber) { this.transactionNumber = transactionNumber; }

    public double getServiceFees() { return serviceFees; }
    public void setServiceFees(double serviceFees) { this.serviceFees = serviceFees; }

    @Override
    public String toString() {
        return "Transaction{" +
                "type='" + type + '\'' +
                ", amount=" + amount +
                ", phoneNumber='" + phoneNumber + '\'' +
                ", date=" + date +
                ", balanceAfter=" + balanceAfter +
                '}';
    }
}
