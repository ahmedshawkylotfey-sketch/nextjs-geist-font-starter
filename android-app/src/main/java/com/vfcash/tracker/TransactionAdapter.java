package com.vfcash.tracker;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

import java.text.SimpleDateFormat;
import java.util.List;
import java.util.Locale;

public class TransactionAdapter extends BaseAdapter {
    
    private Context context;
    private List<Transaction> transactions;
    private LayoutInflater inflater;
    private SimpleDateFormat dateFormat;

    public TransactionAdapter(Context context, List<Transaction> transactions) {
        this.context = context;
        this.transactions = transactions;
        this.inflater = LayoutInflater.from(context);
        this.dateFormat = new SimpleDateFormat("MMM dd, yyyy HH:mm", Locale.getDefault());
    }

    @Override
    public int getCount() {
        return transactions.size();
    }

    @Override
    public Object getItem(int position) {
        return transactions.get(position);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        ViewHolder holder;
        
        if (convertView == null) {
            convertView = inflater.inflate(R.layout.item_transaction, parent, false);
            holder = new ViewHolder();
            holder.typeIcon = convertView.findViewById(R.id.type_icon);
            holder.amountText = convertView.findViewById(R.id.amount_text);
            holder.phoneText = convertView.findViewById(R.id.phone_text);
            holder.dateText = convertView.findViewById(R.id.date_text);
            holder.balanceText = convertView.findViewById(R.id.balance_text);
            holder.detailsText = convertView.findViewById(R.id.details_text);
            convertView.setTag(holder);
        } else {
            holder = (ViewHolder) convertView.getTag();
        }
        
        Transaction transaction = transactions.get(position);
        
        // Set transaction type icon (using text symbols)
        if ("transfer".equals(transaction.getType())) {
            holder.typeIcon.setText("↗");
            holder.typeIcon.setTextColor(context.getResources().getColor(android.R.color.holo_red_dark));
            holder.amountText.setTextColor(context.getResources().getColor(android.R.color.holo_red_dark));
        } else {
            holder.typeIcon.setText("↙");
            holder.typeIcon.setTextColor(context.getResources().getColor(android.R.color.holo_green_dark));
            holder.amountText.setTextColor(context.getResources().getColor(android.R.color.holo_green_dark));
        }
        
        // Set amount
        holder.amountText.setText(String.format("EGP %.2f", transaction.getAmount()));
        
        // Set phone number
        String phoneDisplay = transaction.getPhoneNumber();
        if (phoneDisplay != null && phoneDisplay.length() > 7) {
            // Format phone number for better readability
            phoneDisplay = phoneDisplay.substring(0, 3) + " " + 
                          phoneDisplay.substring(3, 6) + " " + 
                          phoneDisplay.substring(6);
        }
        holder.phoneText.setText(phoneDisplay);
        
        // Set date
        holder.dateText.setText(dateFormat.format(transaction.getDate()));
        
        // Set balance
        holder.balanceText.setText(String.format("Balance: EGP %.2f", transaction.getBalanceAfter()));
        
        // Set additional details
        StringBuilder details = new StringBuilder();
        
        if ("transfer".equals(transaction.getType())) {
            if (transaction.getServiceFees() > 0) {
                details.append(String.format("Fees: EGP %.2f", transaction.getServiceFees()));
            }
        } else {
            if (transaction.getSenderName() != null && !transaction.getSenderName().isEmpty()) {
                details.append("From: ").append(transaction.getSenderName());
            }
            if (transaction.getTransactionNumber() != null && !transaction.getTransactionNumber().isEmpty()) {
                if (details.length() > 0) details.append(" • ");
                details.append("Ref: ").append(transaction.getTransactionNumber());
            }
        }
        
        if (details.length() > 0) {
            holder.detailsText.setText(details.toString());
            holder.detailsText.setVisibility(View.VISIBLE);
        } else {
            holder.detailsText.setVisibility(View.GONE);
        }
        
        return convertView;
    }
    
    public void updateTransactions(List<Transaction> newTransactions) {
        this.transactions = newTransactions;
        notifyDataSetChanged();
    }
    
    static class ViewHolder {
        TextView typeIcon;
        TextView amountText;
        TextView phoneText;
        TextView dateText;
        TextView balanceText;
        TextView detailsText;
    }
}
