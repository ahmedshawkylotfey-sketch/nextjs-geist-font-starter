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

// Import the same storage from the main transactions route
// In a real application, this would be a shared database
let transactions: Transaction[] = [];

export async function POST(request: NextRequest) {
  try {
    const body = await request.json();
    
    // Validate that body is an array
    if (!Array.isArray(body)) {
      return NextResponse.json(
        { success: false, error: 'Bulk upload requires an array of transactions' },
        { status: 400 }
      );
    }

    if (body.length === 0) {
      return NextResponse.json(
        { success: false, error: 'No transactions provided' },
        { status: 400 }
      );
    }

    // Validate each transaction
    const validationResults = body.map((transaction, index) => ({
      index,
      transaction,
      validation: validateTransaction(transaction)
    }));

    // Check for validation errors
    const errors = validationResults.filter(result => !result.validation.valid);
    if (errors.length > 0) {
      return NextResponse.json(
        { 
          success: false, 
          error: 'Validation failed',
          details: errors.map(error => ({
            index: error.index,
            error: error.validation.error
          }))
        },
        { status: 400 }
      );
    }

    // Process all valid transactions
    let addedCount = 0;
    let updatedCount = 0;

    for (const transaction of body) {
      // Check if transaction already exists (prevent duplicates)
      const existingIndex = transactions.findIndex(t => t.id === transaction.id);
      
      if (existingIndex >= 0) {
        // Update existing transaction
        transactions[existingIndex] = transaction;
        updatedCount++;
        console.log(`Updated existing transaction: ${transaction.id}`);
      } else {
        // Add new transaction
        transactions.unshift(transaction); // Add to beginning for newest first
        addedCount++;
        console.log(`Added new transaction: ${transaction.id}`);
      }
    }

    // Keep only the latest 1000 transactions to prevent memory issues
    if (transactions.length > 1000) {
      transactions = transactions.slice(0, 1000);
    }

    console.log(`Bulk upload completed: ${addedCount} added, ${updatedCount} updated. Total: ${transactions.length}`);

    return NextResponse.json({
      success: true,
      message: `Bulk upload completed successfully`,
      summary: {
        totalProcessed: body.length,
        added: addedCount,
        updated: updatedCount,
        totalTransactions: transactions.length
      }
    });

  } catch (error) {
    console.error('Error processing bulk transactions:', error);
    return NextResponse.json(
      { success: false, error: 'Failed to process bulk transactions' },
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

  return { valid: true };
}
