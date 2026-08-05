'use client';

import { useEffect, useState } from 'react';
import { useRouter } from 'next/navigation';
import Link from 'next/link';
import { auth, db } from '@/lib/firebaseClient';
import { onAuthStateChanged, signOut, User } from 'firebase/auth';
import { doc, collection, onSnapshot, query, orderBy, limit } from 'firebase/firestore';

import CommandCard from '@/components/CommandCard';
import CommandRunnerModal from '@/components/CommandRunnerModal';
import TelemetryModal from '@/components/TelemetryModal';
import DangerZone from '@/components/DangerZone';

type FeedbackType = 'info' | 'success' | 'error';
interface Feedback { type: FeedbackType; text: string; }

export default function DashboardPage() {
  const router = useRouter();
  const [user, setUser] = useState<User | null>(null);
  const [loading, setLoading] = useState(true);
  const [feedback, setFeedback] = useState<Feedback | null>(null);
  const [activeCmd, setActiveCmd] = useState<string | null>(null);
  const [isCommandPending, setIsCommandPending] = useState<boolean>(false);
  const [deviceLinked, setDeviceLinked] = useState<boolean>(false);

  const [history, setHistory] = useState<any[]>([]);
  const [commandStartTime, setCommandStartTime] = useState<number>(0);
  const [selectedOutput, setSelectedOutput] = useState<string | null>(null);
  const [modalCmd, setModalCmd] = useState<string | null>(null);

  // Auto-resolve pending state when a new result arrives
  useEffect(() => {
    if (activeCmd && isCommandPending) {
      const baseCmd = activeCmd.split(' ')[0];
      const latestResult = history.find((h: any) => h.command === baseCmd || h.command.startsWith(baseCmd));
      if (latestResult && new Date(latestResult.timestamp).getTime() > commandStartTime) {
        setIsCommandPending(false);
        setActiveCmd(null);
        setFeedback({ type: 'success', text: 'Device response received!' });
        setTimeout(() => setFeedback(null), 5000);
      }
    }
  }, [history, activeCmd, isCommandPending, commandStartTime]);

  // Real-time Firebase listeners
  useEffect(() => {
    let unsubUser = () => {};
    let unsubHistory = () => {};

    const unsubscribeAuth = onAuthStateChanged(auth, (currentUser: User | null) => {
      if (currentUser) {
        setUser(currentUser);

        unsubUser = onSnapshot(doc(db, 'users', currentUser.uid), (docSnap: any) => {
          if (docSnap.exists()) {
            setDeviceLinked(!!docSnap.data().fcmToken);
          }
        });

        const historyQuery = query(collection(db, 'users', currentUser.uid, 'command_history'), orderBy('timestamp', 'desc'), limit(50));
        unsubHistory = onSnapshot(historyQuery, (snapshot: any) => {
          const newHistory: any[] = [];
          snapshot.forEach((d: any) => { newHistory.push({ id: d.id, ...d.data() }); });
          setHistory(newHistory);
        });

      } else {
        setUser(null);
        unsubUser();
        unsubHistory();
        setHistory([]);
        router.push('/login');
      }
      setLoading(false);
    });

    return () => {
      unsubscribeAuth();
      unsubUser();
      unsubHistory();
    };
  }, [router]);

  const handleLogout = async () => {
    setHistory([]);
    await signOut(auth);
    router.push('/login');
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

  const deleteData = async (commandName?: string, all?: boolean) => {
    if (!user || (!commandName && !all)) return;
    if (!confirm(`Are you sure you want to delete ${all ? 'ALL history & telemetry' : `the ${commandName} data`}? This cannot be undone.`)) return;

    setFeedback({ type: 'info', text: 'Deleting data...' });
    try {
      const token = await user.getIdToken();
      const res = await fetch('/api/data/delete', {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json',
          'Authorization': `Bearer ${token}`
        },
        body: JSON.stringify({ token, commandName, all })
      });
      const data = await res.json();
      if (!res.ok) throw new Error(data.error);

      setFeedback({ type: 'success', text: data.message });
      setTimeout(() => setFeedback(null), 3000);

      if (all) {
        setHistory([]);
      } else if (commandName) {
        setSelectedOutput(null);
      }
    } catch (error: any) {
      setFeedback({ type: 'error', text: `Delete failed: ${error.message}` });
      setTimeout(() => setFeedback(null), 5000);
    }
  };

  const deleteAccount = async () => {
    if (!user) return;
    const confirmText = prompt('Type "DELETE" to permanently delete your account and all data.');
    if (confirmText !== 'DELETE') {
      alert('Account deletion cancelled.');
      return;
    }

    setFeedback({ type: 'info', text: 'Deleting account...' });
    try {
      const token = await user.getIdToken();
      const res = await fetch('/api/user/delete', {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json',
          'Authorization': `Bearer ${token}`
        },
        body: JSON.stringify({ token })
      });
      const data = await res.json();
      if (!res.ok) throw new Error(data.error);

      setHistory([]);
      await signOut(auth);
      router.push('/login');
    } catch (error: any) {
      setFeedback({ type: 'error', text: `Account deletion failed: ${error.message}` });
      setTimeout(() => setFeedback(null), 5000);
    }
  };

  if (loading) return (
    <main style={{ padding: '1.5rem 1rem', maxWidth: '1200px', margin: '0 auto', width: '100%' }}>
      <div className="skeleton" style={{ width: '220px', height: '36px', marginBottom: '1rem' }}></div>
      <div className="glass-panel skeleton" style={{ height: '200px' }}></div>
    </main>
  );

  if (!user) return null;

  return (
    <main style={{ padding: '1.5rem 1rem', maxWidth: '1200px', margin: '0 auto', width: '100%' }}>
      {feedback && (
        <div className={`notification-banner notification-${feedback.type}`}>
          {feedback.text}
        </div>
      )}

      {/* Header */}
      <header style={{ marginBottom: '2rem', display: 'flex', flexWrap: 'wrap', gap: '1rem', justifyContent: 'space-between', alignItems: 'center' }}>
        <div>
          <h1 style={{ fontSize: '2.2rem', fontWeight: '700', marginBottom: '0.25rem', letterSpacing: '-0.02em' }}>Veto Dashboard</h1>
          <p style={{ color: 'var(--text-secondary)', fontSize: '1rem' }}>Device control & telemetry</p>
        </div>

        <div style={{ display: 'flex', alignItems: 'center', gap: '12px', flexWrap: 'wrap' }}>
          <Link
            href="/dashboard/console"
            className="btn btn-primary"
            style={{ padding: '10px 16px', display: 'flex', alignItems: 'center', gap: '8px', textDecoration: 'none', backgroundColor: '#238636', border: 'none' }}
          >
            <span>💻</span> Terminal Console ↗
          </Link>

          <div className="glass-panel" style={{ padding: '8px 14px', display: 'flex', flexDirection: 'column', gap: '2px', borderRadius: '12px' }}>
            <span style={{ fontSize: '0.85rem', fontWeight: '600', color: 'var(--text-primary)', wordBreak: 'break-all' }}>
              {user.email}
            </span>
            <div style={{ display: 'flex', alignItems: 'center', gap: '6px', marginTop: '2px' }}>
              <div style={{
                width: '8px', height: '8px', borderRadius: '50%',
                backgroundColor: deviceLinked ? '#2ea043' : '#f85149',
                boxShadow: deviceLinked ? '0 0 8px #2ea043' : '0 0 8px #f85149'
              }}></div>
              <span style={{ fontSize: '0.75rem', color: 'var(--text-secondary)' }}>
                {deviceLinked ? 'App Connected' : 'App Not Connected'}
              </span>
            </div>
          </div>
          <button onClick={handleLogout} className="btn btn-danger" style={{ padding: '8px 14px', fontSize: '0.85rem' }}>Logout</button>
        </div>
      </header>

      {/* Core Commands */}
      <h2 style={{ fontSize: '1.3rem', marginBottom: '1rem' }}>Core Commands</h2>
      <div className="responsive-grid" style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fit, minmax(220px, 1fr))', gap: '1rem', marginBottom: '2rem' }}>
        <CommandCard
          icon="📍" title="Locate Device" command="locate"
          onSendCommand={sendCommand} isPending={isCommandPending}
          activeCmd={activeCmd} history={history} onSelectOutput={setSelectedOutput}
        />
        <CommandCard
          icon="🔊" title="Ring Alarm" command="ring"
          onOpenRunnerModal={setModalCmd}
          onSendCommand={sendCommand} isPending={isCommandPending}
          activeCmd={activeCmd} history={history} onSelectOutput={setSelectedOutput}
        />
        <CommandCard
          icon="🔒" title="Lock Device" command="lock"
          onOpenRunnerModal={setModalCmd}
          onSendCommand={sendCommand} isPending={isCommandPending}
          activeCmd={activeCmd} history={history} onSelectOutput={setSelectedOutput}
        />
        <CommandCard
          icon="📊" title="Device Stats" command="stats"
          onSendCommand={sendCommand} isPending={isCommandPending}
          activeCmd={activeCmd} history={history} onSelectOutput={setSelectedOutput}
        />
      </div>

      {/* Device Toggles */}
      <h2 style={{ fontSize: '1.3rem', marginBottom: '1rem' }}>Device Toggles</h2>
      <div className="responsive-grid" style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fit, minmax(220px, 1fr))', gap: '1rem', marginBottom: '2rem' }}>
        <CommandCard
          icon="🔔" title="Ringer Mode" command="ringermode"
          onOpenRunnerModal={setModalCmd}
          buttons={[
            { label: 'Normal', cmd: 'ringermode normal', primary: true },
            { label: 'Vibrate', cmd: 'ringermode vibrate' },
            { label: 'Silent', cmd: 'ringermode silent' }
          ]}
          onSendCommand={sendCommand} isPending={isCommandPending}
          activeCmd={activeCmd} history={history} onSelectOutput={setSelectedOutput}
        />
        <CommandCard
          icon="🔦" title="Flashlight" command="flash"
          buttons={[
            { label: 'On', cmd: 'flash on', primary: true },
            { label: 'Off', cmd: 'flash off' }
          ]}
          onSendCommand={sendCommand} isPending={isCommandPending}
          activeCmd={activeCmd} history={history} onSelectOutput={setSelectedOutput}
        />
        <CommandCard
          icon="🔵" title="Bluetooth" command="bluetooth"
          buttons={[
            { label: 'On', cmd: 'bluetooth on', primary: true },
            { label: 'Off', cmd: 'bluetooth off' }
          ]}
          onSendCommand={sendCommand} isPending={isCommandPending}
          activeCmd={activeCmd} history={history} onSelectOutput={setSelectedOutput}
        />
        <CommandCard
          icon="🛰️" title="GPS Toggle" command="gps"
          buttons={[
            { label: 'On', cmd: 'gps on', primary: true },
            { label: 'Off', cmd: 'gps off' }
          ]}
          onSendCommand={sendCommand} isPending={isCommandPending}
          activeCmd={activeCmd} history={history} onSelectOutput={setSelectedOutput}
        />
        <CommandCard
          icon="🌙" title="Do Not Disturb" command="nodisturb"
          buttons={[
            { label: 'On', cmd: 'nodisturb on', primary: true },
            { label: 'Off', cmd: 'nodisturb off' }
          ]}
          onSendCommand={sendCommand} isPending={isCommandPending}
          activeCmd={activeCmd} history={history} onSelectOutput={setSelectedOutput}
        />
      </div>

      {/* Surveillance & Security */}
      <h2 style={{ fontSize: '1.3rem', marginBottom: '1rem' }}>Surveillance & Security</h2>
      <div className="responsive-grid" style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fit, minmax(220px, 1fr))', gap: '1rem', marginBottom: '2rem' }}>
        <CommandCard
          icon="📷" title="Take Photo" command="photo"
          onOpenRunnerModal={setModalCmd}
          buttons={[
            { label: 'Front', cmd: 'photo front', primary: true },
            { label: 'Back', cmd: 'photo back' }
          ]}
          onSendCommand={sendCommand} isPending={isCommandPending}
          activeCmd={activeCmd} history={history} onSelectOutput={setSelectedOutput}
        />
        <CommandCard
          icon="🎙️" title="Record Audio" command="audio"
          onOpenRunnerModal={setModalCmd}
          onSendCommand={sendCommand} isPending={isCommandPending}
          activeCmd={activeCmd} history={history} onSelectOutput={setSelectedOutput}
        />
        <CommandCard
          icon="🎥" title="Record Video" command="video"
          onOpenRunnerModal={setModalCmd}
          buttons={[
            { label: 'Front', cmd: 'video front', primary: true },
            { label: 'Back', cmd: 'video back' }
          ]}
          onSendCommand={sendCommand} isPending={isCommandPending}
          activeCmd={activeCmd} history={history} onSelectOutput={setSelectedOutput}
        />
        <CommandCard
          icon="🚨" title="Theft Mode" command="theft"
          description="GPS + Locate + Ring Siren + Lock + Disable DND"
          onSendCommand={sendCommand} isPending={isCommandPending}
          activeCmd={activeCmd} history={history} onSelectOutput={setSelectedOutput}
        />
      </div>

      {/* Danger Zone */}
      <DangerZone
        onOpenRunnerModal={setModalCmd}
        onDeleteData={deleteData}
        onDeleteAccount={deleteAccount}
        isPending={isCommandPending}
      />

      {/* Config Pop-up Modal */}
      {modalCmd && (
        <CommandRunnerModal
          commandName={modalCmd}
          onClose={() => setModalCmd(null)}
          onConfirm={sendCommand}
        />
      )}

      {/* Telemetry Output Modal */}
      {selectedOutput && (
        <TelemetryModal
          selectedOutput={selectedOutput}
          history={history}
          onClose={() => setSelectedOutput(null)}
          onDeleteData={deleteData}
        />
      )}
    </main>
  );
}
