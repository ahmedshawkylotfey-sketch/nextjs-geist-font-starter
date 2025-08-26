import { NextRequest, NextResponse } from 'next/server';

interface Transaction {
  id: string;
  type: 'transfer' | 'received';
  amount: number;
  phoneNumber: string;
  date: string;
  balanceBefore: number;
  balanceAfter: number;
  senderName?: string;
  transactionNumber?: string;
  serviceFees?: number;
}

// In-memory storage for transactions (in production, use a database)
let transactions: Transaction[] = [];

export async function GET() {
  try {
    return NextResponse.json({
      success: true,
      transactions: transactions,
      count: transactions.length
    });
  } catch (error) {
    console.error('Error fetching transactions:', error);
    return NextResponse.json(
      { success: false, error: 'Failed to fetch transactions' },
      { status: 500 }
    );
  }
}

export async function POST(request: NextRequest) {
  try {
    const body = await request.json();
    
    // Validate the transaction data
    if (!body || typeof body !== 'object') {
      return NextResponse.json(
        { success: false, error: 'Invalid request body' },
        { status: 400 }
      );
    }

    // Check if it's a single transaction or bulk upload
    const isArray = Array.isArray(body);
    const transactionsToAdd = isArray ? body : [body];

    // Validate each transaction
    for (const transaction of transactionsToAdd) {
      const validation = validateTransaction(transaction);
      if (!validation.valid) {
        return NextResponse.json(
          { success: false, error: validation.error },
          { status: 400 }
        );
      }
    }

    // Add transactions to storage
    for (const transaction of transactionsToAdd) {
      // Check if transaction already exists (prevent duplicates)
      const existingIndex = transactions.findIndex(t => t.id === transaction.id);
      
      if (existingIndex >= 0) {
        // Update existing transaction
        transactions[existingIndex] = transaction;
        console.log(`Updated existing transaction: ${transaction.id}`);
      } else {
        // Add new transaction
        transactions.unshift(transaction); // Add to beginning for newest first
        console.log(`Added new transaction: ${transaction.id}`);
      }
    }

    // Keep only the latest 1000 transactions to prevent memory issues
    if (transactions.length > 1000) {
      transactions = transactions.slice(0, 1000);
    }

    console.log(`Total transactions stored: ${transactions.length}`);

    return NextResponse.json({
      success: true,
      message: isArray ? 
        `Successfully processed ${transactionsToAdd.length} transactions` : 
        'Transaction received successfully',
      transactionCount: transactions.length
    });

  } catch (error) {
    console.error('Error processing transaction:', error);
    return NextResponse.json(
      { success: false, error: 'Failed to process transaction' },
      { status: 500 }
    );
  }
}

export async function DELETE() {
  try {
    transactions = [];
    console.log('All transactions cleared');
    
    return NextResponse.json({
      success: true,
      message: 'All transactions cleared'
    });
  } catch (error) {
    console.error('Error clearing transactions:', error);
    return NextResponse.json(
      { success: false, error: 'Failed to clear transactions' },
      { status: 500 }
    );
  }
}

function validateTransaction(transaction: any): { valid: boolean; error?: string } {
  // Check required fields
  if (!transaction.id || typeof transaction.id !== 'string') {
    return { valid: false, error: 'Transaction ID is required and must be a string' };
  }

  if (!transaction.type || !['transfer', 'received'].includes(transaction.type)) {
    return { valid: false, error: 'Transaction type must be either "transfer" or "received"' };
  }

  if (typeof transaction.amount !== 'number' || transaction.amount <= 0) {
    return { valid: false, error: 'Amount must be a positive number' };
  }

  if (!transaction.phoneNumber || typeof transaction.phoneNumber !== 'string') {
    return { valid: false, error: 'Phone number is required and must be a string' };
  }

  if (!transaction.date) {
    return { valid: false, error: 'Date is required' };
  }

  // Validate date format
  const date = new Date(transaction.date);
  if (isNaN(date.getTime())) {
    return { valid: false, error: 'Invalid date format' };
  }

  if (typeof transaction.balanceBefore !== 'number' || transaction.balanceBefore < 0) {
    return { valid: false, error: 'Balance before must be a non-negative number' };
  }

  if (typeof transaction.balanceAfter !== 'number' || transaction.balanceAfter < 0) {
    return { valid: false, error: 'Balance after must be a non-negative number' };
  }

  // Validate phone number format (Egyptian mobile numbers)
  const phoneRegex = /^01[0-9]{9}$/;
  if (!phoneRegex.test(transaction.phoneNumber)) {
    console.warn(`Phone number format warning: ${transaction.phoneNumber}`);
    // Don't fail validation, just warn
  }

  return { valid: true };
}
