'use client';

import { useEffect, useState } from 'react';
import { useRouter } from 'next/navigation';
import { auth, db } from '@/lib/firebaseClient';
import { onAuthStateChanged, signOut, User } from 'firebase/auth';
import { doc, collection, onSnapshot, query, orderBy, limit } from 'firebase/firestore';

type FeedbackType = 'info' | 'success' | 'error';
interface Feedback { type: FeedbackType; text: string; }

export default function Home() {
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

  // Auto-resolve pending state when a new result arrives for the active command
  useEffect(() => {
    if (activeCmd && isCommandPending) {
       const baseCmd = activeCmd.split(' ')[0];
       const latestResult = history.find(h => h.command === baseCmd || h.command.startsWith(baseCmd));
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

    const unsubscribeAuth = onAuthStateChanged(auth, (currentUser) => {
      if (currentUser) {
        setUser(currentUser);
        
        unsubUser = onSnapshot(doc(db, 'users', currentUser.uid), (docSnap) => {
          if (docSnap.exists()) {
            setDeviceLinked(!!docSnap.data().fcmToken);
          }
        });

        const historyQuery = query(collection(db, 'users', currentUser.uid, 'command_history'), orderBy('timestamp', 'desc'), limit(50));
        unsubHistory = onSnapshot(historyQuery, (snapshot) => {
          const newHistory: any[] = [];
          snapshot.forEach(d => { newHistory.push({ id: d.id, ...d.data() }); });
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
    
    let finalCommand = command;
    if (command.startsWith('delete ')) {
      const password = command.slice(7).trim();
      finalCommand = `delete ${password}`;
    }
    
    setActiveCmd(command.startsWith('delete') ? 'delete' : command);
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
        body: JSON.stringify({ command: finalCommand })
      });
      const data = await res.json();
      if (!res.ok) throw new Error(data.error);
      
      setFeedback({ type: 'success', text: 'Command sent! Waiting for device response...' });
      
      setTimeout(() => {
        setIsCommandPending((prev) => {
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
    if (!confirm(`Are you sure you want to delete ${all ? 'ALL telemetry and photos' : `the ${commandName} data`}? This cannot be undone.`)) return;

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
    const confirmText = prompt('Type "DELETE" to permanently delete your account and all data. This cannot be undone.');
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

  const getBtnStyle = (cmd: string) => {
    if (activeCmd === cmd) {
      return feedback?.type === 'error' ? { animation: 'errorGlow 2s infinite' } : { animation: 'pulseGlow 2s infinite' };
    }
    return {};
  };

  const renderTelemetryContent = (rawPayload: any) => {
    if (!rawPayload) return null;

    // Unwrap double-wrapped payloads
    let payload = rawPayload;
    if (typeof payload === 'object' && payload.type === 'text' && typeof payload.content === 'string') {
      try {
        const parsed = JSON.parse(payload.content);
        payload = parsed;
      } catch {
        payload = payload.content;
      }
    }

    // Structured JSON Payload
    if (typeof payload === 'object') {
      if (payload.type === 'location') {
        const { lat, lon, provider, accuracy, battery, batteryLevel, speed, altitude } = payload;
        const googleEmbedUrl = `https://maps.google.com/maps?q=${lat},${lon}&t=&z=15&ie=UTF8&iwloc=&output=embed`;
        const battText = battery || (batteryLevel !== undefined ? `${batteryLevel}%` : null);
        
        return (
          <div style={{ display: 'flex', flexDirection: 'column', gap: '1rem', width: '100%' }}>
            <div style={{ width: '100%', height: '300px', borderRadius: '12px', overflow: 'hidden', border: '1px solid var(--glass-border)', boxShadow: '0 4px 20px rgba(0,0,0,0.2)' }}>
              <iframe width="100%" height="100%" frameBorder="0" scrolling="no" src={googleEmbedUrl} style={{ border: 'none' }}></iframe>
            </div>
            <div style={{ 
              fontSize: '0.85rem', 
              backgroundColor: 'var(--border-light)',
              padding: '0.85rem', 
              borderRadius: '8px',
              display: 'grid', 
              gridTemplateColumns: 'repeat(auto-fit, minmax(130px, 1fr))', 
              gap: '8px',
              width: '100%'
            }}>
              <div style={{ wordBreak: 'break-all' }}><strong>Latitude:</strong> {lat}</div>
              <div style={{ wordBreak: 'break-all' }}><strong>Longitude:</strong> {lon}</div>
              <div><strong>Provider:</strong> {provider || 'GPS'}</div>
              {accuracy && <div><strong>Accuracy:</strong> {accuracy}</div>}
              {battText && <div><strong>Battery:</strong> {battText} 🔋</div>}
              {speed && <div><strong>Speed:</strong> {speed}</div>}
              {altitude && <div><strong>Altitude:</strong> {altitude}</div>}
            </div>
            <a href={`https://maps.google.com/?q=${lat},${lon}`} target="_blank" rel="noreferrer" className="btn btn-primary" style={{ textAlign: 'center', textDecoration: 'none', display: 'block', padding: '0.75rem', width: '100%' }}>
              Open in Google Maps ↗
            </a>
          </div>
        );
      }

      if (payload.type === 'media' || payload.url) {
        const url = payload.url || payload.content;
        return (
          <div style={{ 
            display: 'flex', flexDirection: 'column', alignItems: 'center', justifyContent: 'center', 
            padding: '2rem 1rem', textAlign: 'center', backgroundColor: 'rgba(15, 157, 88, 0.1)', 
            borderRadius: '12px', border: '1px solid rgba(15, 157, 88, 0.4)', width: '100%' 
          }}>
            <div style={{ fontSize: '2.5rem', marginBottom: '0.75rem' }}>📁</div>
            <h3 style={{ fontSize: '1.1rem', marginBottom: '1rem', color: '#0f9d58' }}>Media Saved to Google Drive</h3>
            <a href={url} target="_blank" rel="noreferrer" className="btn btn-primary" style={{ display: 'flex', alignItems: 'center', justifyContent: 'center', gap: '8px', width: '100%', maxWidth: '280px', margin: '0 auto', textDecoration: 'none', backgroundColor: '#0f9d58', color: '#fff', border: 'none' }}>
              <span>↗️</span> Open in Google Drive
            </a>
          </div>
       );
      }

      if (payload.type === 'stats') {
        const { device, os, battery, sim, ips, wifi } = payload;
        const items = [
          { icon: '📱', label: 'Device', value: device },
          { icon: '💻', label: 'OS', value: os },
          { icon: '🔋', label: 'Battery', value: battery ? `${battery}%` : 'N/A' },
          { icon: '📶', label: 'SIM', value: sim },
          { icon: '🌐', label: 'IPs', value: ips },
          { icon: '📡', label: 'WiFi', value: wifi },
        ].filter(item => item.value);

        return (
          <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fit, minmax(200px, 1fr))', gap: '0.75rem', width: '100%' }}>
            {items.map((item, idx) => (
              <div key={idx} style={{ backgroundColor: 'var(--border-light)', padding: '0.85rem', borderRadius: '8px', border: '1px solid var(--glass-border)' }}>
                <div style={{ fontSize: '0.75rem', color: 'var(--text-secondary)', textTransform: 'uppercase', marginBottom: '0.25rem' }}>
                  {item.icon} {item.label}
                </div>
                <div style={{ fontSize: '0.95rem', fontWeight: '500', wordBreak: 'break-word' }}>{item.value}</div>
              </div>
            ))}
          </div>
        );
      }

      // generic object
      return <pre style={{ whiteSpace: 'pre-wrap', lineHeight: '1.5', overflowX: 'auto', maxWidth: '100%' }}>{JSON.stringify(payload, null, 2)}</pre>;
    }

    // Fallback for text string format
    const text = payload;
    let lat: number | null = null;
    let lon: number | null = null;

    const mapsMatch = text.match(/https?:\/\/[^\s]*[\?&](?:q|ll|query)=(-?\d+(?:\.\d+)?),(-?\d+(?:\.\d+)?)/i);
    if (mapsMatch) {
      lat = parseFloat(mapsMatch[1]);
      lon = parseFloat(mapsMatch[2]);
    }

    if (!lat || !lon) {
      const latMatch = text.match(/Lat(?:itude)?:\s*(-?\d+(?:\.\d+)?)/i);
      const lonMatch = text.match(/Lon(?:gitude)?:\s*(-?\d+(?:\.\d+)?)/i);
      if (latMatch && lonMatch) {
        lat = parseFloat(latMatch[1]);
        lon = parseFloat(lonMatch[2]);
      }
    }

    if (!lat || !lon) {
      const pairMatch = text.match(/(-?\d{1,2}(?:\.\d+)?)\s*,\s*(-?\d{1,3}(?:\.\d+)?)/);
      if (pairMatch) {
        lat = parseFloat(pairMatch[1]);
        lon = parseFloat(pairMatch[2]);
      }
    }
    
    if (lat && lon) {
      const googleEmbedUrl = `https://maps.google.com/maps?q=${lat},${lon}&t=&z=15&ie=UTF8&iwloc=&output=embed`;
      
      return (
        <div style={{ display: 'flex', flexDirection: 'column', gap: '1rem', width: '100%' }}>
          <div style={{ width: '100%', height: '300px', borderRadius: '12px', overflow: 'hidden', border: '1px solid var(--glass-border)', boxShadow: '0 4px 20px rgba(0,0,0,0.2)' }}>
            <iframe width="100%" height="100%" frameBorder="0" scrolling="no" src={googleEmbedUrl} style={{ border: 'none' }}></iframe>
          </div>
          <div style={{ 
            fontSize: '0.85rem', 
            backgroundColor: 'var(--border-light)',
            padding: '0.85rem', 
            borderRadius: '8px',
            fontFamily: 'monospace',
            whiteSpace: 'pre-wrap',
            wordBreak: 'break-word',
            width: '100%'
          }}>
            {text}
          </div>
          <a href={`https://maps.google.com/?q=${lat},${lon}`} target="_blank" rel="noreferrer" className="btn btn-primary" style={{ textAlign: 'center', textDecoration: 'none', display: 'block', padding: '0.75rem', width: '100%' }}>
            Open in Google Maps ↗
          </a>
        </div>
      );
    }
    
    // Key-Value pairs
    if (text.includes(':')) {
      const lines = text.split('\n').filter((line: string) => line.trim().length > 0);
      const kvLines = lines.filter((line: string) => line.includes(':'));
      if (kvLines.length > 1) {
        return (
          <div className="responsive-grid" style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fit, minmax(180px, 1fr))', gap: '0.75rem', width: '100%' }}>
            {lines.map((line: string, i: number) => {
              const parts = line.split(':');
              if (parts.length < 2) return <div key={i} style={{ gridColumn: '1 / -1' }}>{line}</div>;
              const key = parts[0];
              const val = parts.slice(1).join(':').trim();
              
              const handleCopy = () => {
                navigator.clipboard.writeText(val);
                alert(`Copied ${key.trim()} to clipboard!`);
              };

              const isDriveLink = val.includes('drive.google.com');
              return (
                <div key={i} style={{ 
                  backgroundColor: isDriveLink ? 'rgba(15, 157, 88, 0.1)' : 'var(--border-light)',
                  padding: '0.85rem', 
                  borderRadius: '8px',
                  border: isDriveLink ? '1px solid rgba(15, 157, 88, 0.4)' : '1px solid var(--glass-border)',
                  display: 'flex',
                  flexDirection: 'column',
                  justifyContent: 'space-between',
                  overflow: 'hidden',
                  width: '100%'
                }}>
                  <div>
                    <div style={{ 
                      display: 'flex', 
                      justifyContent: 'space-between', 
                      alignItems: 'center',
                      marginBottom: '0.25rem'
                    }}>
                      <div style={{ fontSize: '0.75rem', color: isDriveLink ? '#0f9d58' : 'var(--text-secondary)', textTransform: 'uppercase', fontWeight: isDriveLink ? 'bold' : 'normal' }}>
                        {key.trim()}
                      </div>
                      <button 
                        onClick={handleCopy}
                        title="Copy"
                        style={{ background: 'none', border: 'none', color: 'var(--text-secondary)', cursor: 'pointer', fontSize: '1rem', padding: '0 4px' }}
                      >
                        📋
                      </button>
                    </div>
                    <div style={{ 
                      fontSize: '0.9rem', 
                      fontWeight: '500', 
                      color: 'var(--text-primary)',
                      wordBreak: 'break-word',
                      whiteSpace: 'pre-wrap'
                    }}>
                      {isDriveLink ? (
                        <a href={val} target="_blank" rel="noreferrer" className="btn btn-primary" style={{ display: 'flex', alignItems: 'center', justifyContent: 'center', gap: '6px', width: '100%', textDecoration: 'none', backgroundColor: '#0f9d58', color: '#fff', border: 'none', padding: '6px 12px', fontSize: '0.85rem' }}>
                          <span>📁</span> Google Drive ↗
                        </a>
                      ) : val.startsWith('http') ? (
                        <a href={val} target="_blank" rel="noreferrer" style={{color: '#58a6ff', textDecoration: 'underline'}}>{val}</a>
                      ) : val}
                    </div>
                  </div>
                </div>
              );
            })}
          </div>
        );
      }
    }

    const driveLinkMatch = text.match(/(https?:\/\/drive\.google\.com[^\s]+)/);
    if (driveLinkMatch) {
       const url = driveLinkMatch[1];
       return (
          <div style={{ 
            display: 'flex', flexDirection: 'column', alignItems: 'center', justifyContent: 'center', 
            padding: '2rem 1rem', textAlign: 'center', backgroundColor: 'rgba(15, 157, 88, 0.1)', 
            borderRadius: '12px', border: '1px solid rgba(15, 157, 88, 0.4)', width: '100%' 
          }}>
            <div style={{ fontSize: '2.5rem', marginBottom: '0.75rem' }}>📁</div>
            <h3 style={{ fontSize: '1.1rem', marginBottom: '1rem', color: '#0f9d58' }}>Media Saved to Google Drive</h3>
            <a href={url} target="_blank" rel="noreferrer" className="btn btn-primary" style={{ display: 'flex', alignItems: 'center', justifyContent: 'center', gap: '8px', width: '100%', maxWidth: '280px', margin: '0 auto', textDecoration: 'none', backgroundColor: '#0f9d58', color: '#fff', border: 'none' }}>
              <span>↗️</span> Open in Google Drive
            </a>
          </div>
       );
    }

    const isError = text.toLowerCase().includes('failed');
    if (isError) {
      return (
          <div style={{ 
            display: 'flex', flexDirection: 'column', alignItems: 'center', justifyContent: 'center', 
            padding: '2rem 1rem', textAlign: 'center', backgroundColor: 'rgba(248, 81, 73, 0.1)', 
            borderRadius: '12px', border: '1px solid rgba(248, 81, 73, 0.4)', width: '100%' 
          }}>
            <div style={{ fontSize: '2.5rem', marginBottom: '0.5rem' }}>❌</div>
            <h3 style={{ fontSize: '1.1rem', marginBottom: '0.5rem', color: 'var(--danger-color)' }}>Task Failed</h3>
            <p style={{ color: 'var(--text-secondary)', fontSize: '0.9rem', wordBreak: 'break-word' }}>{text}</p>
          </div>
      );
    }

    return <div style={{ whiteSpace: 'pre-wrap', lineHeight: '1.5', wordBreak: 'break-word', width: '100%' }}>{text}</div>;
  };

  const renderResult = (baseCmd: string) => {
    const res = history.find(h => h.command === baseCmd || h.command.startsWith(baseCmd));
    if (!res) return <div style={{ marginTop: '0.75rem', fontSize: '0.85rem', color: 'var(--text-secondary)' }}>No data yet</div>;
    return (
      <button onClick={() => setSelectedOutput(res.command)} className="btn" style={{ marginTop: '0.75rem', width: '100%', fontSize: '0.85rem', backgroundColor: 'var(--border-light)' }}>
        View Output ({new Date(res.timestamp).toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' })})
      </button>
    );
  };

  if (loading) return (
    <main style={{ padding: '1.5rem 1rem', maxWidth: '1200px', margin: '0 auto', width: '100%' }}>
      <header style={{ marginBottom: '2rem', display: 'flex', flexWrap: 'wrap', gap: '1rem', justifyContent: 'space-between', alignItems: 'center' }}>
        <div>
          <div className="skeleton" style={{ width: '220px', height: '36px', marginBottom: '0.5rem' }}></div>
          <div className="skeleton" style={{ width: '160px', height: '20px' }}></div>
        </div>
        <div className="skeleton" style={{ width: '180px', height: '50px', borderRadius: '12px' }}></div>
      </header>
      <div className="skeleton" style={{ width: '180px', height: '24px', marginBottom: '1rem' }}></div>
      <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fit, minmax(220px, 1fr))', gap: '1rem', marginBottom: '2rem' }}>
        <div className="glass-panel skeleton" style={{ height: '180px' }}></div>
        <div className="glass-panel skeleton" style={{ height: '180px' }}></div>
      </div>
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
      
      <header style={{ marginBottom: '2rem', display: 'flex', flexWrap: 'wrap', gap: '1rem', justifyContent: 'space-between', alignItems: 'center' }}>
        <div>
          <h1 style={{ fontSize: '2.2rem', fontWeight: '700', marginBottom: '0.25rem', letterSpacing: '-0.02em' }}>Veto Dashboard</h1>
          <p style={{ color: 'var(--text-secondary)', fontSize: '1rem' }}>Device control & telemetry</p>
        </div>
        <div style={{ display: 'flex', alignItems: 'center', gap: '12px', flexWrap: 'wrap' }}>
          <div className="glass-panel" style={{ padding: '8px 14px', display: 'flex', flexDirection: 'column', gap: '2px', borderRadius: '12px' }}>
            <div style={{ display: 'flex', alignItems: 'center', gap: '8px' }}>
              <span style={{ fontSize: '0.85rem', fontWeight: '600', color: 'var(--text-primary)', wordBreak: 'break-all' }}>
                {user.email}
              </span>
            </div>
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
        <div className="glass-panel" style={{ padding: '1.25rem' }}>
          <div style={{ fontSize: '1.8rem', marginBottom: '0.75rem' }}>📍</div>
          <h3 style={{ fontSize: '1.1rem', marginBottom: '0.5rem' }}>Locate Device</h3>
          <button disabled={activeCmd === 'locate'} onClick={() => sendCommand('locate')} className="btn btn-primary" style={{ width: '100%', ...getBtnStyle('locate') }}>
            {activeCmd === 'locate' ? 'Locating...' : 'Locate'}
          </button>
          {renderResult('locate')}
        </div>

        <div className="glass-panel" style={{ padding: '1.25rem' }}>
          <div style={{ fontSize: '1.8rem', marginBottom: '0.75rem' }}>🔊</div>
          <h3 style={{ fontSize: '1.1rem', marginBottom: '0.5rem' }}>Ring Alarm</h3>
          <button disabled={activeCmd === 'ring'} onClick={() => sendCommand('ring')} className="btn" style={{ width: '100%', ...getBtnStyle('ring') }}>
            {activeCmd === 'ring' ? 'Sending...' : 'Trigger Siren'}
          </button>
          {renderResult('ring')}
        </div>

        <div className="glass-panel" style={{ padding: '1.25rem' }}>
          <div style={{ fontSize: '1.8rem', marginBottom: '0.75rem' }}>🔒</div>
          <h3 style={{ fontSize: '1.1rem', marginBottom: '0.5rem' }}>Lock Device</h3>
          <button disabled={activeCmd === 'lock'} onClick={() => sendCommand('lock')} className="btn" style={{ width: '100%', ...getBtnStyle('lock') }}>
            {activeCmd === 'lock' ? 'Locking...' : 'Lock Screen'}
          </button>
          {renderResult('lock')}
        </div>
        
        <div className="glass-panel" style={{ padding: '1.25rem' }}>
          <div style={{ fontSize: '1.8rem', marginBottom: '0.75rem' }}>📊</div>
          <h3 style={{ fontSize: '1.1rem', marginBottom: '0.5rem' }}>Device Stats</h3>
          <button disabled={activeCmd === 'stats'} onClick={() => sendCommand('stats')} className="btn" style={{ width: '100%', ...getBtnStyle('stats') }}>
            {activeCmd === 'stats' ? 'Fetching...' : 'Get Stats'}
          </button>
          {renderResult('stats')}
        </div>
      </div>

      {/* Device Toggles */}
      <h2 style={{ fontSize: '1.3rem', marginBottom: '1rem' }}>Device Toggles</h2>
      <div className="responsive-grid" style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fit, minmax(220px, 1fr))', gap: '1rem', marginBottom: '2rem' }}>
        
        <div className="glass-panel" style={{ padding: '1.25rem' }}>
          <div style={{ fontSize: '1.8rem', marginBottom: '0.75rem' }}>🔦</div>
          <h3 style={{ fontSize: '1.1rem', marginBottom: '0.5rem' }}>Flashlight</h3>
          <div style={{ display: 'flex', gap: '8px', marginBottom: '0.75rem' }}>
            <button disabled={activeCmd === 'flash on'} onClick={() => sendCommand('flash on')} className="btn btn-primary" style={{ flex: 1, padding: '8px 10px', fontSize: '0.85rem', ...getBtnStyle('flash on') }}>On</button>
            <button disabled={activeCmd === 'flash off'} onClick={() => sendCommand('flash off')} className="btn" style={{ flex: 1, padding: '8px 10px', fontSize: '0.85rem', ...getBtnStyle('flash off') }}>Off</button>
          </div>
          {renderResult('flash')}
        </div>

        <div className="glass-panel" style={{ padding: '1.25rem' }}>
          <div style={{ fontSize: '1.8rem', marginBottom: '0.75rem' }}>🔵</div>
          <h3 style={{ fontSize: '1.1rem', marginBottom: '0.5rem' }}>Bluetooth</h3>
          <div style={{ display: 'flex', gap: '8px', marginBottom: '0.75rem' }}>
            <button disabled={activeCmd === 'bluetooth on'} onClick={() => sendCommand('bluetooth on')} className="btn btn-primary" style={{ flex: 1, padding: '8px 10px', fontSize: '0.85rem', ...getBtnStyle('bluetooth on') }}>On</button>
            <button disabled={activeCmd === 'bluetooth off'} onClick={() => sendCommand('bluetooth off')} className="btn" style={{ flex: 1, padding: '8px 10px', fontSize: '0.85rem', ...getBtnStyle('bluetooth off') }}>Off</button>
          </div>
          {renderResult('bluetooth')}
        </div>

        <div className="glass-panel" style={{ padding: '1.25rem' }}>
          <div style={{ fontSize: '1.8rem', marginBottom: '0.75rem' }}>🛰️</div>
          <h3 style={{ fontSize: '1.1rem', marginBottom: '0.5rem' }}>GPS Toggle</h3>
          <div style={{ display: 'flex', gap: '8px', marginBottom: '0.75rem' }}>
            <button disabled={activeCmd === 'gps on'} onClick={() => sendCommand('gps on')} className="btn btn-primary" style={{ flex: 1, padding: '8px 10px', fontSize: '0.85rem', ...getBtnStyle('gps on') }}>On</button>
            <button disabled={activeCmd === 'gps off'} onClick={() => sendCommand('gps off')} className="btn" style={{ flex: 1, padding: '8px 10px', fontSize: '0.85rem', ...getBtnStyle('gps off') }}>Off</button>
          </div>
          {renderResult('gps')}
        </div>

        <div className="glass-panel" style={{ padding: '1.25rem' }}>
          <div style={{ fontSize: '1.8rem', marginBottom: '0.75rem' }}>🌙</div>
          <h3 style={{ fontSize: '1.1rem', marginBottom: '0.5rem' }}>Do Not Disturb</h3>
          <div style={{ display: 'flex', gap: '8px', marginBottom: '0.75rem' }}>
            <button disabled={activeCmd === 'nodisturb on'} onClick={() => sendCommand('nodisturb on')} className="btn btn-primary" style={{ flex: 1, padding: '8px 10px', fontSize: '0.85rem', ...getBtnStyle('nodisturb on') }}>On</button>
            <button disabled={activeCmd === 'nodisturb off'} onClick={() => sendCommand('nodisturb off')} className="btn" style={{ flex: 1, padding: '8px 10px', fontSize: '0.85rem', ...getBtnStyle('nodisturb off') }}>Off</button>
          </div>
          {renderResult('nodisturb')}
        </div>
      </div>

      {/* Surveillance & Security */}
      <h2 style={{ fontSize: '1.3rem', marginBottom: '1rem' }}>Surveillance & Security</h2>
      <div className="responsive-grid" style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fit, minmax(220px, 1fr))', gap: '1rem', marginBottom: '2rem' }}>
        <div className="glass-panel" style={{ padding: '1.25rem' }}>
          <div style={{ fontSize: '1.8rem', marginBottom: '0.75rem' }}>📷</div>
          <h3 style={{ fontSize: '1.1rem', marginBottom: '0.5rem' }}>Take Photo</h3>
          <div style={{ display: 'flex', gap: '8px', marginBottom: '0.75rem' }}>
            <button disabled={activeCmd === 'photo front'} onClick={() => sendCommand('photo front')} className="btn btn-primary" style={{ flex: 1, padding: '8px 10px', fontSize: '0.85rem', ...getBtnStyle('photo front') }}>Front</button>
            <button disabled={activeCmd === 'photo back'} onClick={() => sendCommand('photo back')} className="btn" style={{ flex: 1, padding: '8px 10px', fontSize: '0.85rem', ...getBtnStyle('photo back') }}>Back</button>
          </div>
          {renderResult('photo')}
        </div>

        <div className="glass-panel" style={{ padding: '1.25rem' }}>
          <div style={{ fontSize: '1.8rem', marginBottom: '0.75rem' }}>🎙️</div>
          <h3 style={{ fontSize: '1.1rem', marginBottom: '0.5rem' }}>Record Audio</h3>
          <button disabled={activeCmd === 'audio'} onClick={() => sendCommand('audio')} className="btn" style={{ width: '100%', ...getBtnStyle('audio') }}>
            {activeCmd === 'audio' ? 'Recording...' : 'Record 30s Audio'}
          </button>
          {renderResult('audio')}
        </div>

        <div className="glass-panel" style={{ padding: '1.25rem' }}>
          <div style={{ fontSize: '1.8rem', marginBottom: '0.75rem' }}>🎥</div>
          <h3 style={{ fontSize: '1.1rem', marginBottom: '0.5rem' }}>Record Video</h3>
          <div style={{ display: 'flex', gap: '8px', marginBottom: '0.75rem' }}>
            <button disabled={activeCmd === 'video front'} onClick={() => sendCommand('video front')} className="btn btn-primary" style={{ flex: 1, padding: '8px 10px', fontSize: '0.85rem', ...getBtnStyle('video front') }}>Front</button>
            <button disabled={activeCmd === 'video back'} onClick={() => sendCommand('video back')} className="btn" style={{ flex: 1, padding: '8px 10px', fontSize: '0.85rem', ...getBtnStyle('video back') }}>Back</button>
          </div>
          {renderResult('video')}
        </div>
        
        <div className="glass-panel" style={{ padding: '1.25rem' }}>
          <div style={{ fontSize: '1.8rem', marginBottom: '0.75rem' }}>🚨</div>
          <h3 style={{ fontSize: '1.1rem', marginBottom: '0.5rem' }}>Theft Mode</h3>
          <button disabled={activeCmd === 'theft'} onClick={() => sendCommand('theft')} className="btn" style={{ width: '100%', borderColor: '#eba336', color: '#eba336', ...getBtnStyle('theft') }}>
            {activeCmd === 'theft' ? 'Activating...' : 'Activate Theft Mode'}
          </button>
          {renderResult('theft')}
        </div>
      </div>
      
      {/* Danger Zone */}
      <h2 style={{ fontSize: '1.3rem', marginBottom: '1rem', color: 'var(--danger-color)' }}>Danger Zone</h2>
      <div className="responsive-grid" style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fit, minmax(220px, 1fr))', gap: '1rem', marginBottom: '2rem' }}>
        <div className="glass-panel" style={{ padding: '1.25rem', border: '1px solid rgba(248, 81, 73, 0.3)' }}>
          <div style={{ fontSize: '1.8rem', marginBottom: '0.75rem' }}>⚠️</div>
          <h3 style={{ fontSize: '1.1rem', marginBottom: '0.5rem', color: 'var(--danger-color)' }}>Factory Reset</h3>
          <p style={{ color: 'var(--text-secondary)', fontSize: '0.85rem', marginBottom: '1rem' }}>Permanently wipe all data from your device.</p>
          <button disabled={activeCmd === 'delete'} onClick={() => {
            const password = window.prompt('WARNING: This will PERMANENTLY WIPE all data from your phone!\n\nTo proceed, please enter your Veto app password:');
            if (password) {
              sendCommand(`delete ${password}`);
            } else if (password !== null) {
              alert('Wipe cancelled. Password cannot be empty.');
            }
          }} className="btn btn-danger" style={{ width: '100%', ...getBtnStyle('delete') }}>
            {activeCmd === 'delete' ? 'Wiping...' : 'Wipe Device'}
          </button>
        </div>

        <div className="glass-panel" style={{ padding: '1.25rem', border: '1px solid rgba(248, 81, 73, 0.3)' }}>
          <div style={{ fontSize: '1.8rem', marginBottom: '0.75rem' }}>🧹</div>
          <h3 style={{ fontSize: '1.1rem', marginBottom: '0.5rem', color: 'var(--danger-color)' }}>Delete Cloud Data</h3>
          <p style={{ color: 'var(--text-secondary)', fontSize: '0.85rem', marginBottom: '1rem' }}>Erase all stored telemetry and history from database.</p>
          <button onClick={() => deleteData(undefined, true)} className="btn btn-danger" style={{ width: '100%' }}>
            Delete All Data
          </button>
        </div>

        <div className="glass-panel" style={{ padding: '1.25rem', border: '1px solid rgba(248, 81, 73, 0.3)' }}>
          <div style={{ fontSize: '1.8rem', marginBottom: '0.75rem' }}>☠️</div>
          <h3 style={{ fontSize: '1.1rem', marginBottom: '0.5rem', color: 'var(--danger-color)' }}>Delete Account</h3>
          <p style={{ color: 'var(--text-secondary)', fontSize: '0.85rem', marginBottom: '1rem' }}>Permanently delete your account and all data.</p>
          <button onClick={() => deleteAccount()} className="btn btn-danger" style={{ width: '100%' }}>
            Delete Account
          </button>
        </div>
      </div>

      {/* Reusable Output Modal */}
      {selectedOutput && (() => {
        const matchingEntries = history.filter(h => h.command === selectedOutput || h.command.startsWith(selectedOutput));
        if (matchingEntries.length === 0) return null;
        const res = matchingEntries[0];

        return (
          <div style={{
            position: 'fixed', top: 0, left: 0, right: 0, bottom: 0,
            backgroundColor: 'var(--overlay-bg)', zIndex: 1000,
            display: 'flex', justifyContent: 'center', alignItems: 'center', padding: '0.75rem'
          }} onClick={() => setSelectedOutput(null)}>
            <div className="glass-panel" style={{
              width: '100%', maxWidth: '750px', maxHeight: '92vh', overflowY: 'auto',
              padding: '1.5rem 1.25rem', position: 'relative', border: '1px solid var(--glass-border)'
            }} onClick={e => e.stopPropagation()}>
              <button onClick={() => setSelectedOutput(null)} style={{
                position: 'absolute', top: '1rem', right: '1rem', background: 'none', border: 'none',
                color: 'var(--text-primary)', fontSize: '1.5rem', cursor: 'pointer', lineHeight: '1'
              }}>×</button>
              
              <h2 style={{ fontSize: '1.3rem', marginBottom: '0.5rem', textTransform: 'capitalize', display: 'flex', flexWrap: 'wrap', gap: '10px', alignItems: 'center', justifyContent: 'space-between' }}>
                <span>{selectedOutput} Result</span>
                <button onClick={() => deleteData(selectedOutput)} className="btn btn-danger" style={{ padding: '4px 10px', fontSize: '0.75rem', marginRight: '2rem' }}>
                  Delete Entry
                </button>
              </h2>
              <div style={{ color: 'var(--text-secondary)', marginBottom: '1rem', fontSize: '0.85rem' }}>
                Received: {new Date(res.timestamp).toLocaleString()}
              </div>
              
              <div style={{ 
                backgroundColor: 'var(--code-bg)', padding: '1.25rem 1rem', borderRadius: '12px',
                border: '1px solid var(--border-light)', color: 'var(--text-primary)', width: '100%', overflowX: 'hidden'
              }}>
                {renderTelemetryContent(res.payload)}
              </div>
            </div>
          </div>
        );
      })()}
    </main>
  );
}
