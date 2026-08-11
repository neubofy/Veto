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
        const firebaseClient = await import('@/lib/firebaseClient');
        const uid = firebaseClient.auth.currentUser?.uid;
        
        if (!uid) {
          throw new Error('Not logged in');
        }

        const { decryptClient } = await import('@/lib/clientCrypto');
        await decryptClient(testPayload, pin, uid);
        
        onUnlock(pin);
      } catch (err: any) {
        console.error(err);
        setError('Incorrect PIN. Please try again.');
        setPin('');
      } finally {
        setLoading(false);
      }
    } else {
      // If no test payload, just accept it
      onUnlock(pin);
      setLoading(false);
    }
  };

  return (
    <AnimatePresence>
      <div className="modal-overlay">
        <motion.div
          initial={{ opacity: 0 }}
          animate={{ opacity: 1 }}
          exit={{ opacity: 0 }}
          style={{ position: 'absolute', inset: 0 }}
        />
        
        <motion.div
          initial={{ opacity: 0, scale: 0.95, y: 20 }}
          animate={{ opacity: 1, scale: 1, y: 0 }}
          exit={{ opacity: 0, scale: 0.95, y: -20 }}
          transition={{ type: "spring", duration: 0.5, bounce: 0.3 }}
          className="modal-content-glass"
          style={{ padding: '40px', textAlign: 'center' }}
        >
          <div style={{ position: 'relative' }}>
            {/* Glowing Shield Icon */}
            <div className="pingate-shield-container">
              <div className="pingate-shield-glow"></div>
              <div className="pingate-shield-inner">
                <Shield style={{ width: '40px', height: '40px', color: '#60a5fa' }} />
              </div>
            </div>
            
            <h2 style={{ fontSize: '1.875rem', fontWeight: 800, marginBottom: '12px', background: 'linear-gradient(to right, #fff, #9ca3af)', WebkitBackgroundClip: 'text', WebkitTextFillColor: 'transparent' }}>
              Unlock Dashboard
            </h2>
            <p style={{ fontSize: '0.875rem', fontWeight: 500, color: '#9ca3af', marginBottom: '40px', lineHeight: 1.6 }}>
              Your data is end-to-end encrypted. Enter your Veto PIN to decrypt telemetry.
            </p>

            <form onSubmit={handleSubmit}>
              <div className="pingate-input-container">
                <div className="pingate-input-icon">
                  <KeyRound style={{ width: '20px', height: '20px' }} />
                </div>
                <input
                  type="password"
                  value={pin}
                  onChange={(e) => setPin(e.target.value)}
                  placeholder="Enter your PIN"
                  className="pingate-input"
                  autoFocus
                  disabled={loading}
                />
              </div>

              {error && (
                <motion.div
                  initial={{ opacity: 0, y: -10, height: 0 }}
                  animate={{ opacity: 1, y: 0, height: 'auto' }}
                  style={{ display: 'flex', alignItems: 'center', gap: '12px', background: 'rgba(239, 68, 68, 0.1)', border: '1px solid rgba(239, 68, 68, 0.2)', borderRadius: '16px', padding: '16px', fontSize: '0.875rem', fontWeight: 500, color: '#f87171', marginBottom: '24px' }}
                >
                  <ShieldAlert style={{ width: '20px', height: '20px', flexShrink: 0 }} />
                  <p style={{ textAlign: 'left', margin: 0 }}>{error}</p>
                </motion.div>
              )}

              <button
                type="submit"
                disabled={!pin || loading}
                className="pingate-btn"
                style={{ marginTop: error ? '0' : '24px' }}
              >
                {loading ? (
                  <>
                    <Loader2 style={{ width: '24px', height: '24px', animation: 'spin 1s linear infinite' }} />
                    <span>Decrypting...</span>
                  </>
                ) : (
                  <>
                    <span>Decrypt</span>
                    <svg style={{ width: '20px', height: '20px' }} fill="none" viewBox="0 0 24 24" stroke="currentColor">
                      <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M14 5l7 7m0 0l-7 7m7-7H3" />
                    </svg>
                  </>
                )}
              </button>
            </form>
          </div>
        </motion.div>
      </div>
    </AnimatePresence>
  );
}
