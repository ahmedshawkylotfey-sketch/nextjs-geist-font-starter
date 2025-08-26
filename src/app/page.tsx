"use client";

import { useState, useEffect } from 'react';
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from '@/components/ui/card';
import { Button } from '@/components/ui/button';
import { Input } from '@/components/ui/input';
import { Label } from '@/components/ui/label';
import { Badge } from '@/components/ui/badge';
import { Separator } from '@/components/ui/separator';

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

interface Limits {
  dailyTransferLimit: number;
  monthlyTransferLimit: number;
  dailyReceiveLimit: number;
  monthlyReceiveLimit: number;
}

export default function Dashboard() {
  const [transactions, setTransactions] = useState<Transaction[]>([]);
  const [limits, setLimits] = useState<Limits>({
    dailyTransferLimit: 5000,
    monthlyTransferLimit: 50000,
    dailyReceiveLimit: 10000,
    monthlyReceiveLimit: 100000
  });
  const [editingLimits, setEditingLimits] = useState(false);
  const [tempLimits, setTempLimits] = useState<Limits>(limits);

  // Load data from localStorage on component mount
  useEffect(() => {
    const savedTransactions = localStorage.getItem('vfcash_transactions');
    const savedLimits = localStorage.getItem('vfcash_limits');
    
    if (savedTransactions) {
      try {
        setTransactions(JSON.parse(savedTransactions));
      } catch (e) {
        console.error('Error loading transactions:', e);
      }
    }
    
    if (savedLimits) {
      try {
        const parsedLimits = JSON.parse(savedLimits);
        setLimits(parsedLimits);
        setTempLimits(parsedLimits);
      } catch (e) {
        console.error('Error loading limits:', e);
      }
    }
  }, []);

  // Save data to localStorage whenever it changes
  useEffect(() => {
    localStorage.setItem('vfcash_transactions', JSON.stringify(transactions));
  }, [transactions]);

  useEffect(() => {
    localStorage.setItem('vfcash_limits', JSON.stringify(limits));
  }, [limits]);

  // Calculate usage statistics
  const calculateUsage = () => {
    const today = new Date();
    const currentMonth = today.getMonth();
    const currentYear = today.getFullYear();
    
    const todayTransactions = transactions.filter(t => {
      const transDate = new Date(t.date);
      return transDate.toDateString() === today.toDateString();
    });
    
    const monthTransactions = transactions.filter(t => {
      const transDate = new Date(t.date);
      return transDate.getMonth() === currentMonth && transDate.getFullYear() === currentYear;
    });
    
    const dailyTransferred = todayTransactions
      .filter(t => t.type === 'transfer')
      .reduce((sum, t) => sum + t.amount, 0);
    
    const monthlyTransferred = monthTransactions
      .filter(t => t.type === 'transfer')
      .reduce((sum, t) => sum + t.amount, 0);
    
    const dailyReceived = todayTransactions
      .filter(t => t.type === 'received')
      .reduce((sum, t) => sum + t.amount, 0);
    
    const monthlyReceived = monthTransactions
      .filter(t => t.type === 'received')
      .reduce((sum, t) => sum + t.amount, 0);
    
    return {
      dailyTransferred,
      monthlyTransferred,
      dailyReceived,
      monthlyReceived,
      dailyTransferPercentage: (dailyTransferred / limits.dailyTransferLimit) * 100,
      monthlyTransferPercentage: (monthlyTransferred / limits.monthlyTransferLimit) * 100,
      dailyReceivePercentage: (dailyReceived / limits.dailyReceiveLimit) * 100,
      monthlyReceivePercentage: (monthlyReceived / limits.monthlyReceiveLimit) * 100
    };
  };

  const usage = calculateUsage();

  const handleSaveLimits = () => {
    setLimits(tempLimits);
    setEditingLimits(false);
  };

  const handleCancelLimits = () => {
    setTempLimits(limits);
    setEditingLimits(false);
  };

  const formatCurrency = (amount: number) => {
    return `EGP ${amount.toLocaleString('en-US', { minimumFractionDigits: 2, maximumFractionDigits: 2 })}`;
  };

  const formatDate = (dateString: string) => {
    return new Date(dateString).toLocaleString('en-US', {
      year: 'numeric',
      month: 'short',
      day: 'numeric',
      hour: '2-digit',
      minute: '2-digit'
    });
  };

  const getUsageColor = (percentage: number) => {
    if (percentage >= 90) return 'text-red-600';
    if (percentage >= 70) return 'text-yellow-600';
    return 'text-green-600';
  };

  return (
    <div className="min-h-screen bg-gray-50 p-6">
      <div className="max-w-7xl mx-auto space-y-6">
        {/* Header */}
        <div className="text-center">
          <h1 className="text-4xl font-bold text-gray-900 mb-2">VF-Cash Transaction Dashboard</h1>
          <p className="text-gray-600">Monitor your Vodafone Cash transactions and limits</p>
        </div>

        {/* Limits Overview */}
        <Card>
          <CardHeader className="flex flex-row items-center justify-between">
            <div>
              <CardTitle>Transaction Limits</CardTitle>
              <CardDescription>Daily and monthly usage overview</CardDescription>
            </div>
            <Button 
              onClick={() => setEditingLimits(!editingLimits)}
              variant={editingLimits ? "outline" : "default"}
            >
              {editingLimits ? 'Cancel' : 'Edit Limits'}
            </Button>
          </CardHeader>
          <CardContent>
            {editingLimits ? (
              <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
                <div className="space-y-4">
                  <div>
                    <Label htmlFor="dailyTransfer">Daily Transfer Limit (EGP)</Label>
                    <Input
                      id="dailyTransfer"
                      type="number"
                      value={tempLimits.dailyTransferLimit}
                      onChange={(e) => setTempLimits({
                        ...tempLimits,
                        dailyTransferLimit: Number(e.target.value)
                      })}
                    />
                  </div>
                  <div>
                    <Label htmlFor="monthlyTransfer">Monthly Transfer Limit (EGP)</Label>
                    <Input
                      id="monthlyTransfer"
                      type="number"
                      value={tempLimits.monthlyTransferLimit}
                      onChange={(e) => setTempLimits({
                        ...tempLimits,
                        monthlyTransferLimit: Number(e.target.value)
                      })}
                    />
                  </div>
                </div>
                <div className="space-y-4">
                  <div>
                    <Label htmlFor="dailyReceive">Daily Receive Limit (EGP)</Label>
                    <Input
                      id="dailyReceive"
                      type="number"
                      value={tempLimits.dailyReceiveLimit}
                      onChange={(e) => setTempLimits({
                        ...tempLimits,
                        dailyReceiveLimit: Number(e.target.value)
                      })}
                    />
                  </div>
                  <div>
                    <Label htmlFor="monthlyReceive">Monthly Receive Limit (EGP)</Label>
                    <Input
                      id="monthlyReceive"
                      type="number"
                      value={tempLimits.monthlyReceiveLimit}
                      onChange={(e) => setTempLimits({
                        ...tempLimits,
                        monthlyReceiveLimit: Number(e.target.value)
                      })}
                    />
                  </div>
                </div>
                <div className="md:col-span-2 flex gap-2">
                  <Button onClick={handleSaveLimits}>Save Changes</Button>
                  <Button variant="outline" onClick={handleCancelLimits}>Cancel</Button>
                </div>
              </div>
            ) : (
              <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-4 gap-4">
                <div className="text-center p-4 bg-blue-50 rounded-lg">
                  <h3 className="font-semibold text-blue-900">Daily Transfer</h3>
                  <p className="text-2xl font-bold text-blue-700">
                    {formatCurrency(usage.dailyTransferred)}
                  </p>
                  <p className="text-sm text-blue-600">
                    of {formatCurrency(limits.dailyTransferLimit)}
                  </p>
                  <p className={`text-sm font-semibold ${getUsageColor(usage.dailyTransferPercentage)}`}>
                    {usage.dailyTransferPercentage.toFixed(1)}%
                  </p>
                </div>
                
                <div className="text-center p-4 bg-purple-50 rounded-lg">
                  <h3 className="font-semibold text-purple-900">Monthly Transfer</h3>
                  <p className="text-2xl font-bold text-purple-700">
                    {formatCurrency(usage.monthlyTransferred)}
                  </p>
                  <p className="text-sm text-purple-600">
                    of {formatCurrency(limits.monthlyTransferLimit)}
                  </p>
                  <p className={`text-sm font-semibold ${getUsageColor(usage.monthlyTransferPercentage)}`}>
                    {usage.monthlyTransferPercentage.toFixed(1)}%
                  </p>
                </div>
                
                <div className="text-center p-4 bg-green-50 rounded-lg">
                  <h3 className="font-semibold text-green-900">Daily Received</h3>
                  <p className="text-2xl font-bold text-green-700">
                    {formatCurrency(usage.dailyReceived)}
                  </p>
                  <p className="text-sm text-green-600">
                    of {formatCurrency(limits.dailyReceiveLimit)}
                  </p>
                  <p className={`text-sm font-semibold ${getUsageColor(usage.dailyReceivePercentage)}`}>
                    {usage.dailyReceivePercentage.toFixed(1)}%
                  </p>
                </div>
                
                <div className="text-center p-4 bg-orange-50 rounded-lg">
                  <h3 className="font-semibold text-orange-900">Monthly Received</h3>
                  <p className="text-2xl font-bold text-orange-700">
                    {formatCurrency(usage.monthlyReceived)}
                  </p>
                  <p className="text-sm text-orange-600">
                    of {formatCurrency(limits.monthlyReceiveLimit)}
                  </p>
                  <p className={`text-sm font-semibold ${getUsageColor(usage.monthlyReceivePercentage)}`}>
                    {usage.monthlyReceivePercentage.toFixed(1)}%
                  </p>
                </div>
              </div>
            )}
          </CardContent>
        </Card>

        {/* Transactions Table */}
        <Card>
          <CardHeader>
            <CardTitle>Recent Transactions</CardTitle>
            <CardDescription>
              {transactions.length} transactions found
            </CardDescription>
          </CardHeader>
          <CardContent>
            {transactions.length === 0 ? (
              <div className="text-center py-8 text-gray-500">
                <p className="text-lg mb-2">No transactions yet</p>
                <p className="text-sm">Transactions will appear here when your Android app syncs data</p>
              </div>
            ) : (
              <div className="overflow-x-auto">
                <table className="w-full border-collapse">
                  <thead>
                    <tr className="border-b">
                      <th className="text-left p-3 font-semibold">Type</th>
                      <th className="text-left p-3 font-semibold">Amount</th>
                      <th className="text-left p-3 font-semibold">Phone</th>
                      <th className="text-left p-3 font-semibold">Date</th>
                      <th className="text-left p-3 font-semibold">Balance</th>
                      <th className="text-left p-3 font-semibold">Details</th>
                    </tr>
                  </thead>
                  <tbody>
                    {transactions.map((transaction) => (
                      <tr key={transaction.id} className="border-b hover:bg-gray-50">
                        <td className="p-3">
                          <Badge 
                            variant={transaction.type === 'transfer' ? 'destructive' : 'default'}
                          >
                            {transaction.type === 'transfer' ? '↗ Transfer' : '↙ Received'}
                          </Badge>
                        </td>
                        <td className="p-3 font-semibold">
                          <span className={transaction.type === 'transfer' ? 'text-red-600' : 'text-green-600'}>
                            {formatCurrency(transaction.amount)}
                          </span>
                        </td>
                        <td className="p-3 font-mono text-sm">
                          {transaction.phoneNumber}
                        </td>
                        <td className="p-3 text-sm">
                          {formatDate(transaction.date)}
                        </td>
                        <td className="p-3 text-sm">
                          {formatCurrency(transaction.balanceAfter)}
                        </td>
                        <td className="p-3 text-sm text-gray-600">
                          {transaction.type === 'transfer' && transaction.serviceFees && (
                            <span>Fees: {formatCurrency(transaction.serviceFees)}</span>
                          )}
                          {transaction.type === 'received' && transaction.senderName && (
                            <span>From: {transaction.senderName}</span>
                          )}
                          {transaction.transactionNumber && (
                            <span className="block">Ref: {transaction.transactionNumber}</span>
                          )}
                        </td>
                      </tr>
                    ))}
                  </tbody>
                </table>
              </div>
            )}
          </CardContent>
        </Card>

        {/* Instructions */}
        <Card>
          <CardHeader>
            <CardTitle>Setup Instructions</CardTitle>
            <CardDescription>How to connect your Android app</CardDescription>
          </CardHeader>
          <CardContent className="space-y-4">
            <div className="grid grid-cols-1 md:grid-cols-2 gap-6">
              <div>
                <h3 className="font-semibold mb-2">Android App Setup:</h3>
                <ol className="list-decimal list-inside space-y-1 text-sm text-gray-600">
                  <li>Install the VF-Cash Tracker app on your Android device</li>
                  <li>Grant SMS permissions when prompted</li>
                  <li>Open the app and go to Settings</li>
                  <li>Enter this computer's IP address and port (usually :3000)</li>
                  <li>Test the connection</li>
                  <li>The app will automatically sync new VF-Cash SMS messages</li>
                </ol>
              </div>
              <div>
                <h3 className="font-semibold mb-2">Network Requirements:</h3>
                <ul className="list-disc list-inside space-y-1 text-sm text-gray-600">
                  <li>Both devices must be on the same WiFi network</li>
                  <li>This web dashboard should be running on port 3000</li>
                  <li>Your computer's firewall should allow incoming connections</li>
                  <li>Find your computer's IP address in network settings</li>
                </ul>
              </div>
            </div>
            <Separator />
            <div className="bg-blue-50 p-4 rounded-lg">
              <h4 className="font-semibold text-blue-900 mb-2">Current Server Status:</h4>
              <p className="text-sm text-blue-700">
                Dashboard is running and ready to receive data from your Android app.
                <br />
                API endpoint: <code className="bg-blue-100 px-1 rounded">/api/transactions</code>
              </p>
            </div>
          </CardContent>
        </Card>
      </div>
    </div>
  );
}
