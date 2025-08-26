import { NextRequest, NextResponse } from 'next/server';

interface Limits {
  dailyTransferLimit: number;
  monthlyTransferLimit: number;
  dailyReceiveLimit: number;
  monthlyReceiveLimit: number;
}

// In-memory storage for limits (in production, use a database)
let limits: Limits = {
  dailyTransferLimit: 5000,
  monthlyTransferLimit: 50000,
  dailyReceiveLimit: 10000,
  monthlyReceiveLimit: 100000
};

export async function GET() {
  try {
    return NextResponse.json({
      success: true,
      limits: limits
    });
  } catch (error) {
    console.error('Error fetching limits:', error);
    return NextResponse.json(
      { success: false, error: 'Failed to fetch limits' },
      { status: 500 }
    );
  }
}

export async function POST(request: NextRequest) {
  try {
    const body = await request.json();
    
    // Validate the limits data
    const validation = validateLimits(body);
    if (!validation.valid) {
      return NextResponse.json(
        { success: false, error: validation.error },
        { status: 400 }
      );
    }

    // Update limits
    limits = {
      dailyTransferLimit: body.dailyTransferLimit,
      monthlyTransferLimit: body.monthlyTransferLimit,
      dailyReceiveLimit: body.dailyReceiveLimit,
      monthlyReceiveLimit: body.monthlyReceiveLimit
    };

    console.log('Limits updated:', limits);

    return NextResponse.json({
      success: true,
      message: 'Limits updated successfully',
      limits: limits
    });

  } catch (error) {
    console.error('Error updating limits:', error);
    return NextResponse.json(
      { success: false, error: 'Failed to update limits' },
      { status: 500 }
    );
  }
}

export async function PUT(request: NextRequest) {
  // PUT method for updating limits (same as POST)
  return POST(request);
}

function validateLimits(limitsData: any): { valid: boolean; error?: string } {
  if (!limitsData || typeof limitsData !== 'object') {
    return { valid: false, error: 'Invalid limits data' };
  }

  // Check required fields and validate they are positive numbers
  const requiredFields = [
    'dailyTransferLimit',
    'monthlyTransferLimit', 
    'dailyReceiveLimit',
    'monthlyReceiveLimit'
  ];

  for (const field of requiredFields) {
    if (!(field in limitsData)) {
      return { valid: false, error: `Missing required field: ${field}` };
    }

    const value = limitsData[field];
    if (typeof value !== 'number' || value < 0) {
      return { valid: false, error: `${field} must be a non-negative number` };
    }

    // Check for reasonable limits (not too high)
    if (value > 10000000) { // 10 million EGP
      return { valid: false, error: `${field} seems unreasonably high` };
    }
  }

  // Validate logical relationships
  if (limitsData.dailyTransferLimit > limitsData.monthlyTransferLimit) {
    return { valid: false, error: 'Daily transfer limit cannot exceed monthly transfer limit' };
  }

  if (limitsData.dailyReceiveLimit > limitsData.monthlyReceiveLimit) {
    return { valid: false, error: 'Daily receive limit cannot exceed monthly receive limit' };
  }

  return { valid: true };
}
