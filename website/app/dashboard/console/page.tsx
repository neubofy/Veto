'use client';

import React, { useEffect, useState } from 'react';
import { useRouter } from 'next/navigation';
import Link from 'next/link';
import { auth, db } from '@/lib/firebaseClient';
import { onAuthStateChanged, signOut, User } from 'firebase/auth';
import { doc, collection, onSnapshot, query, orderBy, limit } from 'firebase/firestore';

import CommandConsole from '@/components/CommandConsole';
import TelemetryModal from '@/components/TelemetryModal';

type FeedbackType = 'info' | 'success' | 'error';
interface Feedback { type: FeedbackType; text: string; }

import PinGateModal from '@/components/PinGateModal';
import AccountSwitcher from '@/components/AccountSwitcher';
import { accountManager, StoredAccount } from '@/lib/accountManager';

export default function ConsolePage() {
  const router = useRouter();
  const [user, setUser] = useState<User | null>(null);
  const [loading, setLoading] = useState(true);
  const [feedback, setFeedback] = useState<Feedback | null>(null);
  const [activeCmd, setActiveCmd] = useState<string | null>(null);
  const [isCommandPending, setIsCommandPending] = useState<boolean>(false);

  const [history, setHistory] = useState<any[]>([]);
  const [commandStartTime, setCommandStartTime] = useState<number>(0);

  const [pin, setPin] = useState<string | null>(null);
  const [currentAccount, setCurrentAccount] = useState<StoredAccount | null>(null);
  const [needsPin, setNeedsPin] = useState<boolean>(false);

  // Check if we need a PIN
  useEffect(() => {
    if (pin) {
      setNeedsPin(false);
      return;
    }
    const hasEncryptedData = history.some(item => 
      item.payload?.type === 'encrypted' || 
      (item.history && item.history.some((h: any) => h.payload?.type === 'encrypted'))
    );
    if (hasEncryptedData) {
      setNeedsPin(true);
    }
  }, [history, pin]);

  useEffect(() => {
    // Next.js App Router sometimes fails to scroll to hash links on initial load of dynamic pages
    if (typeof window !== 'undefined' && window.location.hash) {
      const id = window.location.hash.substring(1); // remove the '#'
      setTimeout(() => {
        const el = document.getElementById(id);
        if (el) {
          el.scrollIntoView({ behavior: 'smooth' });
        }
      }, 500); // Wait for page to render
    }
  }, []);
  const [selectedOutput, setSelectedOutput] = useState<string | null>(null);

  // Auto-resolve pending state when a new result arrives
  useEffect(() => {
    if (activeCmd && isCommandPending) {
      const baseCmd = activeCmd.split(' ')[0];
      const latestResult = history.find((h: any) => h.command === baseCmd || h.command.startsWith(baseCmd));
      if (latestResult && new Date(latestResult.timestamp).getTime() > commandStartTime) {
        setIsCommandPending(false);
        setActiveCmd(null);
        setFeedback({ type: 'success', text: 'Device response received!' });
        
        // Show native browser notification
        if ('Notification' in window && Notification.permission === 'granted') {
          const notification = new Notification('Veto Alert', {
            body: `Response received for command: ${baseCmd}. Click here to see response.`,
            icon: '/favicon.ico'
          });
          notification.onclick = () => {
            window.focus();
            setSelectedOutput(baseCmd);
          };
        }
        
        setTimeout(() => setFeedback(null), 5000);
      }
    }
  }, [history, activeCmd, isCommandPending, commandStartTime]);

  // Real-time Firebase listeners
  useEffect(() => {
    // Request notification permissions
    if ('Notification' in window && Notification.permission === 'default') {
      Notification.requestPermission();
    }

    let unsubHistory = () => {};

    const unsubscribeAuth = onAuthStateChanged(auth, (currentUser: User | null) => {
      if (currentUser) {
        setUser(currentUser);

        // Ensure account is in manager
        const stored = accountManager.getStoredAccounts().find(a => a.uid === currentUser.uid);
        if (!stored) {
          accountManager.addAccount({
            uid: currentUser.uid,
            email: currentUser.email || '',
            displayName: currentUser.displayName || 'Unknown User',
            photoURL: currentUser.photoURL || '',
          });
        }
        setCurrentAccount(accountManager.getStoredAccounts().find(a => a.uid === currentUser.uid) || null);

        const historyRef = collection(db, 'users', currentUser.uid, 'command_history');
        unsubHistory = onSnapshot(historyRef, (snapshot: any) => {
          const newHistory: any[] = [];
          snapshot.forEach((d: any) => { newHistory.push({ id: d.id, ...d.data() }); });
          newHistory.sort((a, b) => {
            const timeA = new Date(a.timestamp).getTime();
            const timeB = new Date(b.timestamp).getTime();
            return timeB - timeA;
          });
          setHistory(newHistory);
        });

      } else {
        setUser(null);
        setCurrentAccount(null);
        setPin(null);
        unsubHistory();
        setHistory([]);
        router.push('/login');
      }
      setLoading(false);
    });

    return () => {
      unsubscribeAuth();
      unsubHistory();
    };
  }, [router]);

  const handleLogout = async () => {
    setHistory([]);
    setPin(null);
    await signOut(auth);
    router.push('/login');
  };

  const handleAccountSwitch = (account: StoredAccount) => {
    setPin(null);
    setHistory([]);
  };

  const sendCommand = async (command: string) => {
    if (!user) return;

    if (isCommandPending) {
      setFeedback({ type: 'error', text: 'Please wait! A previous command is still pending.' });
      setTimeout(() => setFeedback(null), 3000);
      return;
    }

    setActiveCmd(command);
    setIsCommandPending(true);
    setCommandStartTime(Date.now());
    setFeedback({ type: 'info', text: `Sending command: ${command}...` });

    try {
      const token = await user.getIdToken();
      const res = await fetch('/api/command', {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json',
          'Authorization': `Bearer ${token}`
        },
        body: JSON.stringify({ command })
      });
      const data = await res.json();
      if (!res.ok) throw new Error(data.error);

      setFeedback({ type: 'success', text: 'Command sent! Waiting for device response...' });

      setTimeout(() => {
        setIsCommandPending((prev: boolean) => {
          if (prev) {
            setFeedback({ type: 'info', text: 'Command execution in progress on device...' });
            setActiveCmd(null);
            setTimeout(() => setFeedback(null), 4000);
            return false;
          }
          return prev;
        });
      }, 30000);
    } catch (error: any) {
      setFeedback({ type: 'error', text: `Failed: ${error.message}` });
      setIsCommandPending(false);
      setActiveCmd(null);
      setTimeout(() => setFeedback(null), 5000);
    }
  };

  const deleteData = async (commandName?: string) => {
    if (!user || !commandName) return;
    if (!confirm(`Are you sure you want to delete ${commandName} entry?`)) return;

    try {
      const token = await user.getIdToken();
      const res = await fetch('/api/data/delete', {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json',
          'Authorization': `Bearer ${token}`
        },
        body: JSON.stringify({ token, commandName })
      });
      const data = await res.json();
      if (!res.ok) throw new Error(data.error);

      setSelectedOutput(null);
      setFeedback({ type: 'success', text: 'Entry deleted' });
      setTimeout(() => setFeedback(null), 3000);
    } catch (error: any) {
      setFeedback({ type: 'error', text: `Delete failed: ${error.message}` });
      setTimeout(() => setFeedback(null), 5000);
    }
  };

  if (loading) return (
    <main style={{ padding: '1.5rem 1rem', maxWidth: '1200px', margin: '0 auto', width: '100%' }}>
      <div className="skeleton" style={{ width: '220px', height: '36px', marginBottom: '1rem' }}></div>
      <div className="glass-panel skeleton" style={{ height: '300px' }}></div>
    </main>
  );

  if (!user) return null;

  let testEncryptedPayload: string | undefined;
  if (needsPin) {
    const encryptedItem = history.find(item => item.payload?.type === 'encrypted');
    if (encryptedItem) {
      testEncryptedPayload = encryptedItem.payload.content;
    } else {
      const parentWithEncrypted = history.find(item => item.history && item.history.some((h: any) => h.payload?.type === 'encrypted'));
      if (parentWithEncrypted) {
        testEncryptedPayload = parentWithEncrypted.history.find((h: any) => h.payload?.type === 'encrypted')?.payload.content;
      }
    }
  }

  return (
    <main style={{ padding: '1.5rem 1rem', maxWidth: '1200px', margin: '0 auto', width: '100%' }}>
      {needsPin && (
        <PinGateModal 
          onUnlock={(unlockedPin) => setPin(unlockedPin)} 
          testPayload={testEncryptedPayload}
        />
      )}

      {feedback && (
        <div className={`notification-banner notification-${feedback.type}`}>
          {feedback.text}
        </div>
      )}

      {/* Header */}
      <header style={{ marginBottom: '2rem', display: 'flex', flexWrap: 'wrap', gap: '1rem', justifyContent: 'space-between', alignItems: 'center' }}>
        <div>
          <div style={{ display: 'flex', alignItems: 'center', gap: '12px', marginBottom: '0.5rem' }}>
            <Link href="/dashboard" className="btn" style={{ padding: '6px 12px', fontSize: '0.85rem', textDecoration: 'none' }}>
              ← Dashboard
            </Link>
          </div>
          <h1 style={{ fontSize: '2.2rem', fontWeight: '700', margin: 0, letterSpacing: '-0.02em' }}>Terminal Console</h1>
          <p style={{ color: 'var(--text-secondary)', fontSize: '1rem', marginTop: '4px' }}>Raw CLI command execution & live output stream</p>
        </div>
        <div style={{ display: 'flex', alignItems: 'center', gap: '12px' }}>
          <AccountSwitcher 
            currentAccount={currentAccount}
            onAccountSwitch={handleAccountSwitch}
            onLogoutCurrent={handleLogout}
          />
        </div>
      </header>

      {/* Dedicated Command Console */}
      <CommandConsole onSendCommand={sendCommand} isPending={isCommandPending} />

      {/* Live Command Execution Logs */}
      <div id="logs" className="glass-panel" style={{ padding: '1.5rem', border: '1px solid #30363d', backgroundColor: '#161b22' }}>
        <h2 style={{ fontSize: '1.2rem', fontWeight: 'bold', marginBottom: '1rem', color: '#f0f6fc', display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
          <span>Recent Execution Logs</span>
        </h2>

        {history.length === 0 ? (
          <p style={{ color: '#8b949e', fontSize: '0.9rem' }}>No command logs yet.</p>
        ) : (
          <div style={{ display: 'flex', flexDirection: 'column', gap: '8px' }}>
            {history.map((item: any) => (
              <div
                key={item.id}
                onClick={() => setSelectedOutput(item.command)}
                style={{
                  padding: '12px 14px',
                  backgroundColor: '#0d1117',
                  border: '1px solid #30363d',
                  borderRadius: '8px',
                  display: 'flex',
                  justifyContent: 'space-between',
                  alignItems: 'center',
                  cursor: 'pointer',
                  transition: 'border-color 0.15s ease'
                }}
              >
                <div>
                  <div style={{ fontFamily: 'monospace', fontSize: '0.95rem', fontWeight: 'bold', color: '#58a6ff' }}>
                    $ {item.command}
                  </div>
                  <div style={{ fontSize: '0.75rem', color: '#8b949e', marginTop: '2px' }}>
                    {new Date(item.timestamp).toLocaleString()}
                  </div>
                </div>
                <button className="btn" style={{ fontSize: '0.75rem', padding: '4px 10px' }}>
                  View Output ↗
                </button>
              </div>
            ))}
          </div>
        )}
      </div>

      {/* Telemetry Output Modal */}
      {selectedOutput && (
        <TelemetryModal
          selectedOutput={selectedOutput}
          history={history}
          pin={pin}
          onClose={() => setSelectedOutput(null)}
          onDeleteData={deleteData}
        />
      )}
    </main>
  );
}
