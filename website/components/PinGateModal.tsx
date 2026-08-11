'use client';

import React, { useState } from 'react';
import { motion, AnimatePresence } from 'framer-motion';
import { Shield, ShieldAlert, KeyRound, Loader2 } from 'lucide-react';

interface PinGateModalProps {
  onUnlock: (pin: string) => void;
  testPayload?: string;
}

export default function PinGateModal({ onUnlock, testPayload }: PinGateModalProps) {
  const [pin, setPin] = useState('');
  const [error, setError] = useState<string | null>(null);
  const [loading, setLoading] = useState(false);

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!pin) return;

    setLoading(true);
    setError(null);

    if (testPayload) {
      try {
        const idToken = await (await import('@/lib/firebaseClient')).auth.currentUser?.getIdToken();
        const response = await fetch('/api/crypto/decrypt', {
          method: 'POST',
          headers: {
            'Content-Type': 'application/json',
            'Authorization': `Bearer ${idToken}`,
          },
          body: JSON.stringify({ data: testPayload, pin }),
        });

        if (!response.ok) {
          throw new Error('Incorrect PIN or decryption failed');
        }
        
        onUnlock(pin);
      } catch (err: any) {
        setError('Incorrect PIN. Please try again.');
        setPin('');
      } finally {
        setLoading(false);
      }
    } else {
      // If no test payload, just accept it (or we could wait until an actual decryption fails to prompt again)
      onUnlock(pin);
      setLoading(false);
    }
  };

  return (
    <AnimatePresence>
      <div className="fixed inset-0 z-[100] flex items-center justify-center bg-black/80 backdrop-blur-md">
        <motion.div
          initial={{ opacity: 0, scale: 0.95, y: 20 }}
          animate={{ opacity: 1, scale: 1, y: 0 }}
          exit={{ opacity: 0, scale: 0.95, y: -20 }}
          className="w-full max-w-md overflow-hidden rounded-2xl bg-gray-900 border border-gray-800 shadow-2xl shadow-blue-900/20"
        >
          <div className="p-8 text-center">
            <div className="mx-auto mb-6 flex h-20 w-20 items-center justify-center rounded-full bg-blue-500/10 text-blue-500 ring-1 ring-blue-500/20">
              <Shield className="h-10 w-10" />
            </div>
            
            <h2 className="mb-2 text-2xl font-bold text-white">Dashboard Locked</h2>
            <p className="mb-8 text-sm text-gray-400">
              Enter your Veto PIN to decrypt your end-to-end encrypted data.
            </p>

            <form onSubmit={handleSubmit} className="space-y-4">
              <div className="relative">
                <div className="pointer-events-none absolute inset-y-0 left-0 flex items-center pl-3">
                  <KeyRound className="h-5 w-5 text-gray-500" />
                </div>
                <input
                  type="password"
                  value={pin}
                  onChange={(e) => setPin(e.target.value)}
                  placeholder="Enter Veto PIN"
                  className="block w-full rounded-xl border-0 bg-gray-800 py-3.5 pl-10 pr-4 text-white ring-1 ring-inset ring-gray-700 placeholder:text-gray-500 focus:ring-2 focus:ring-inset focus:ring-blue-500 sm:text-sm sm:leading-6"
                  autoFocus
                  disabled={loading}
                />
              </div>

              {error && (
                <motion.div
                  initial={{ opacity: 0, y: -10 }}
                  animate={{ opacity: 1, y: 0 }}
                  className="flex items-center gap-2 rounded-lg bg-red-500/10 p-3 text-sm text-red-500"
                >
                  <ShieldAlert className="h-4 w-4 shrink-0" />
                  <p>{error}</p>
                </motion.div>
              )}

              <button
                type="submit"
                disabled={!pin || loading}
                className="flex w-full items-center justify-center gap-2 rounded-xl bg-blue-600 px-4 py-3.5 text-sm font-semibold text-white shadow-sm hover:bg-blue-500 focus-visible:outline focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-blue-600 disabled:opacity-50 transition-colors"
              >
                {loading ? (
                  <>
                    <Loader2 className="h-5 w-5 animate-spin" />
                    Decrypting...
                  </>
                ) : (
                  'Unlock Dashboard'
                )}
              </button>
            </form>
          </div>
        </motion.div>
      </div>
    </AnimatePresence>
  );
}
