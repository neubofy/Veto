'use client';

import { useEffect, useState } from 'react';
import { useRouter } from 'next/navigation';
import { auth, db } from '@/lib/firebaseClient';
import { onAuthStateChanged, signOut, User } from 'firebase/auth';
import { doc, collection, onSnapshot } from 'firebase/firestore';

type FeedbackType = 'info' | 'success' | 'error';
interface Feedback { type: FeedbackType; text: string; }

const AuthenticatedImage = ({ url, user }: { url: string, user: User }) => {
  const [src, setSrc] = useState<string>('');
  useEffect(() => {
    user.getIdToken().then(token => {
      setSrc(`/api/image?url=${encodeURIComponent(url)}&token=${token}`);
    });
  }, [url, user]);

  if (!src) return <div className="skeleton" style={{ width: '100%', height: '300px' }}></div>;
  return (
    <a href={src} target="_blank" rel="noreferrer" title="Click to open in new tab" style={{ display: 'block' }}>
      {/* eslint-disable-next-line @next/next/no-img-element */}
      <img src={src} alt="Captured" style={{ width: '100%', height: 'auto', objectFit: 'contain' }} />
    </a>
  );
};

export default function Home() {
  const router = useRouter();
  const [user, setUser] = useState<User | null>(null);
  const [loading, setLoading] = useState(true);
  const [feedback, setFeedback] = useState<Feedback | null>(null);
  const [activeCmd, setActiveCmd] = useState<string | null>(null);
  const [isCommandPending, setIsCommandPending] = useState<boolean>(false);
  const [deviceLinked, setDeviceLinked] = useState<boolean>(false);
  
  const [results, setResults] = useState<Record<string, any>>(() => {
    if (typeof window !== 'undefined') {
      const saved = localStorage.getItem('veto_results');
      if (saved) return JSON.parse(saved);
    }
    return {};
  });
  
  const [photos, setPhotos] = useState<Record<string, any>>(() => {
    if (typeof window !== 'undefined') {
      const saved = localStorage.getItem('veto_photos');
      if (saved) return JSON.parse(saved);
    }
    return {};
  });
  
  const [commandStartTime, setCommandStartTime] = useState<number>(0);
  const [selectedOutput, setSelectedOutput] = useState<string | null>(null);
  const [locations, setLocations] = useState<any[]>([]);
  const [selectedLocIndex, setSelectedLocIndex] = useState<number>(0);

  // Auto-resolve pending state when photo arrives
  useEffect(() => {
    if (activeCmd && isCommandPending) {
       const baseCmd = activeCmd.split(' ')[0];
       const phto = photos[baseCmd];
       if (phto && new Date(phto.timestamp).getTime() > commandStartTime) {
         setIsCommandPending(false);
         setActiveCmd(null);
         setFeedback({ type: 'success', text: 'Photo arrived!' });
         setTimeout(() => setFeedback(null), 5000);
       }
    }
  }, [photos, activeCmd, isCommandPending, commandStartTime]);

  // Auto-resolve pending state when result arrives
  useEffect(() => {
    if (activeCmd && isCommandPending) {
       const baseCmd = activeCmd.split(' ')[0];
       const result = results[baseCmd];
       if (result && new Date(result.timestamp).getTime() > commandStartTime) {
         setIsCommandPending(false);
         setActiveCmd(null);
         setFeedback({ type: 'success', text: 'Data arrived!' });
         setTimeout(() => setFeedback(null), 5000);
       }
    }
  }, [results, activeCmd, isCommandPending, commandStartTime]);

  // Real-time Firebase listeners
  useEffect(() => {
    let unsubUser = () => {};
    let unsubPhotos = () => {};
    let unsubResults = () => {};
    let unsubLocations = () => {};

    const unsubscribeAuth = onAuthStateChanged(auth, (currentUser) => {
      if (currentUser) {
        setUser(currentUser);
        
        unsubUser = onSnapshot(doc(db, 'users', currentUser.uid), (docSnap) => {
          if (docSnap.exists()) {
            setDeviceLinked(!!docSnap.data().fcmToken);
          }
        });

        unsubPhotos = onSnapshot(collection(db, 'users', currentUser.uid, 'photos'), (snapshot) => {
          const newPhotos: Record<string, any> = {};
          snapshot.forEach(d => { newPhotos[d.id] = d.data(); });
          setPhotos(newPhotos);
          localStorage.setItem('veto_photos', JSON.stringify(newPhotos));
        });

        unsubResults = onSnapshot(collection(db, 'users', currentUser.uid, 'results'), (snapshot) => {
          const newResults: Record<string, any> = {};
          snapshot.forEach(d => { newResults[d.id] = d.data(); });
          setResults(newResults);
          localStorage.setItem('veto_results', JSON.stringify(newResults));
        });

        unsubLocations = onSnapshot(collection(db, 'users', currentUser.uid, 'locations'), (snapshot) => {
          const newLocs: any[] = [];
          snapshot.forEach(d => { newLocs.push({ id: d.id, ...d.data() }); });
          newLocs.sort((a, b) => new Date(b.timestamp).getTime() - new Date(a.timestamp).getTime());
          setLocations(newLocs.slice(0, 5));
        });

      } else {
        setUser(null);
        unsubUser();
        unsubPhotos();
        unsubResults();
        unsubLocations();
        setPhotos({});
        setResults({});
        setLocations([]);
        localStorage.removeItem('veto_photos');
        localStorage.removeItem('veto_results');
        router.push('/login');
      }
      setLoading(false);
    });

    return () => {
      unsubscribeAuth();
      unsubUser();
      unsubPhotos();
      unsubResults();
      unsubLocations();
    };
  }, [router]);

  const handleLogout = async () => {
    localStorage.removeItem('veto_photos');
    localStorage.removeItem('veto_results');
    setPhotos({});
    setResults({});
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
      const msgUint8 = new TextEncoder().encode(password);
      const hashBuffer = await crypto.subtle.digest('SHA-256', msgUint8);
      const hashArray = Array.from(new Uint8Array(hashBuffer));
      const hashedPassword = hashArray.map(b => b.toString(16).padStart(2, '0')).join('');
      finalCommand = `delete ${hashedPassword}`;
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
        setResults({});
        setPhotos({});
        setLocations([]);
        localStorage.removeItem('veto_results');
        localStorage.removeItem('veto_photos');
      } else if (commandName) {
        setResults(prev => {
          const next = { ...prev };
          delete next[commandName];
          localStorage.setItem('veto_results', JSON.stringify(next));
          return next;
        });
        setPhotos(prev => {
          const next = { ...prev };
          delete next[commandName];
          localStorage.setItem('veto_photos', JSON.stringify(next));
          return next;
        });
        if (commandName === 'autoloc' || commandName === 'locate') {
          setLocations(prev => prev.filter(l => l.command !== commandName && !(commandName === 'autoloc' && l.sourceType?.includes('autoloc'))));
        }
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

      localStorage.removeItem('veto_results');
      localStorage.removeItem('veto_photos');
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

  const renderTelemetryContent = (text: string) => {
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
        <div style={{ display: 'flex', flexDirection: 'column', gap: '1rem' }}>
          <div style={{ width: '100%', height: '350px', borderRadius: '12px', overflow: 'hidden', border: '1px solid var(--glass-border)', boxShadow: '0 4px 20px rgba(0,0,0,0.2)' }}>
            <iframe width="100%" height="100%" frameBorder="0" scrolling="no" src={googleEmbedUrl} style={{ border: 'none' }}></iframe>
          </div>
          <div style={{ 
            fontSize: '0.9rem', 
            backgroundColor: 'var(--border-light)',
            padding: '1rem', 
            borderRadius: '8px',
            fontFamily: 'monospace',
            whiteSpace: 'pre-wrap'
          }}>
            {text}
          </div>
          <a href={`https://maps.google.com/?q=${lat},${lon}`} target="_blank" rel="noreferrer" className="btn btn-primary" style={{ textAlign: 'center', textDecoration: 'none', display: 'block', padding: '0.75rem' }}>
            Open in Google Maps
          </a>
        </div>
      );
    }
    
    // Key-Value pairs
    if (text.includes(':')) {
      const lines = text.split('\n').filter(line => line.trim().length > 0);
      const kvLines = lines.filter(line => line.includes(':'));
      if (kvLines.length > 1) {
        return (
          <div className="responsive-grid" style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fit, minmax(200px, 1fr))', gap: '1rem' }}>
            {lines.map((line, i) => {
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
                  padding: '1rem', 
                  borderRadius: '8px',
                  border: isDriveLink ? '1px solid rgba(15, 157, 88, 0.4)' : '1px solid var(--glass-border)',
                  display: 'flex',
                  flexDirection: 'column',
                  justifyContent: 'space-between',
                  overflow: 'hidden'
                }}>
                  <div>
                    <div style={{ 
                      display: 'flex', 
                      justifyContent: 'space-between', 
                      alignItems: 'center',
                      marginBottom: '0.5rem'
                    }}>
                      <div style={{ fontSize: '0.8rem', color: isDriveLink ? '#0f9d58' : 'var(--text-secondary)', textTransform: 'uppercase', letterSpacing: '0.05em', fontWeight: isDriveLink ? 'bold' : 'normal' }}>
                        {key.trim()}
                      </div>
                      <button 
                        onClick={handleCopy}
                        title="Copy"
                        style={{
                          background: 'none', border: 'none', color: 'var(--text-secondary)', cursor: 'pointer', fontSize: '1.2rem', padding: '0 4px'
                        }}
                      >
                        📋
                      </button>
                    </div>
                    <div style={{ 
                      fontSize: '1rem', 
                      fontWeight: '500', 
                      color: 'var(--text-primary)',
                      wordBreak: 'break-word',
                      whiteSpace: 'pre-wrap',
                      marginTop: isDriveLink ? '0.5rem' : '0'
                    }}>
                      {isDriveLink ? (
                        <a href={val} target="_blank" rel="noreferrer" className="btn btn-primary" style={{ display: 'flex', alignItems: 'center', justifyContent: 'center', gap: '8px', width: '100%', textDecoration: 'none', backgroundColor: '#0f9d58', color: '#fff', border: 'none' }}>
                          <span>📁</span> Open in Google Drive
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
    
    const isPendingMedia = text.toLowerCase().includes('recording') || text.toLowerCase().includes('uploading') || text.toLowerCase().includes('taking');
    if (isPendingMedia) {
      return (
        <div style={{ 
          display: 'flex', flexDirection: 'column', alignItems: 'center', justifyContent: 'center', 
          padding: '3rem 1rem', textAlign: 'center', backgroundColor: 'var(--border-light)', 
          borderRadius: '12px', border: '1px solid var(--glass-border)' 
        }}>
          <div style={{ fontSize: '3rem', marginBottom: '1rem', animation: 'pulseGlow 2s infinite' }}>☁️</div>
          <h3 style={{ fontSize: '1.2rem', marginBottom: '0.5rem' }}>Data is uploading to your Google Drive</h3>
          <p style={{ color: 'var(--text-secondary)' }}>A secure link will be available here shortly.</p>
        </div>
      );
    }

    const driveLinkMatch = text.match(/(https?:\/\/drive\.google\.com[^\s]+)/);
    if (driveLinkMatch) {
       const url = driveLinkMatch[1];
       return (
          <div style={{ 
            display: 'flex', flexDirection: 'column', alignItems: 'center', justifyContent: 'center', 
            padding: '3rem 1rem', textAlign: 'center', backgroundColor: 'rgba(15, 157, 88, 0.1)', 
            borderRadius: '12px', border: '1px solid rgba(15, 157, 88, 0.4)' 
          }}>
            <div style={{ fontSize: '3rem', marginBottom: '1rem' }}>📁</div>
            <h3 style={{ fontSize: '1.2rem', marginBottom: '1.5rem', color: '#0f9d58' }}>Media Uploaded Successfully</h3>
            <a href={url} target="_blank" rel="noreferrer" className="btn btn-primary" style={{ display: 'flex', alignItems: 'center', justifyContent: 'center', gap: '8px', width: '100%', maxWidth: '300px', margin: '0 auto', textDecoration: 'none', backgroundColor: '#0f9d58', color: '#fff', border: 'none' }}>
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
            padding: '3rem 1rem', textAlign: 'center', backgroundColor: 'rgba(248, 81, 73, 0.1)', 
            borderRadius: '12px', border: '1px solid rgba(248, 81, 73, 0.4)' 
          }}>
            <div style={{ fontSize: '3rem', marginBottom: '1rem' }}>❌</div>
            <h3 style={{ fontSize: '1.2rem', marginBottom: '0.5rem', color: 'var(--danger-color)' }}>Task Failed</h3>
            <p style={{ color: 'var(--text-secondary)' }}>{text}</p>
          </div>
      );
    }

    const urlRegex = /(https?:\/\/[^\s]+)/g;
    if (urlRegex.test(text)) {
      const parts = text.split(urlRegex);
      return (
          <div style={{ whiteSpace: 'pre-wrap', lineHeight: '1.5' }}>
              {parts.map((part, i) => urlRegex.test(part) ? <a key={i} href={part} target="_blank" rel="noreferrer" style={{color: '#58a6ff', textDecoration: 'underline'}}>{part}</a> : part)}
          </div>
      );
    }
    
    return <div style={{ whiteSpace: 'pre-wrap', lineHeight: '1.5' }}>{text}</div>;
  };

  const renderResult = (baseCmd: string) => {
    const res = results[baseCmd];
    const phto = photos[baseCmd];
    if (!res && !phto) return <div style={{ marginTop: '1rem', fontSize: '0.85rem', color: 'var(--text-secondary)' }}>No data yet</div>;
    const resTime = res?.timestamp ? new Date(res.timestamp).getTime() : 0;
    const phtoTime = phto?.timestamp ? new Date(phto.timestamp).getTime() : 0;
    const timestamp = Math.max(resTime, phtoTime);
    return (
      <button onClick={() => setSelectedOutput(baseCmd)} className="btn" style={{ marginTop: '1rem', width: '100%', fontSize: '0.9rem', backgroundColor: 'var(--border-light)' }}>
        View Output ({new Date(timestamp).toLocaleTimeString()})
      </button>
    );
  };

  if (loading) return (
    <main style={{ padding: '2rem', maxWidth: '1200px', margin: '0 auto', width: '100%' }}>
      <header style={{ marginBottom: '3rem', display: 'flex', flexWrap: 'wrap', gap: '1rem', justifyContent: 'space-between', alignItems: 'center' }}>
        <div>
          <div className="skeleton" style={{ width: '250px', height: '40px', marginBottom: '0.5rem' }}></div>
          <div className="skeleton" style={{ width: '200px', height: '24px' }}></div>
        </div>
        <div className="skeleton" style={{ width: '200px', height: '60px', borderRadius: '12px' }}></div>
      </header>

      <div className="skeleton" style={{ width: '200px', height: '28px', marginBottom: '1.5rem' }}></div>
      <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fit, minmax(250px, 1fr))', gap: '1.5rem', marginBottom: '3rem' }}>
        <div className="glass-panel skeleton" style={{ height: '200px' }}></div>
        <div className="glass-panel skeleton" style={{ height: '200px' }}></div>
        <div className="glass-panel skeleton" style={{ height: '200px' }}></div>
        <div className="glass-panel skeleton" style={{ gridColumn: '1 / -1', height: '150px' }}></div>
      </div>
    </main>
  );

  if (!user) return null;

  return (
    <main style={{ padding: '2rem', maxWidth: '1200px', margin: '0 auto', width: '100%' }}>
      {feedback && (
        <div className={`notification-banner notification-${feedback.type}`}>
          {feedback.text}
        </div>
      )}
      
      <header style={{ marginBottom: '3rem', display: 'flex', flexWrap: 'wrap', gap: '1rem', justifyContent: 'space-between', alignItems: 'center' }}>
        <div>
          <h1 style={{ fontSize: '2.5rem', fontWeight: '700', marginBottom: '0.5rem', letterSpacing: '-0.02em' }}>Veto Dashboard</h1>
          <p style={{ color: 'var(--text-secondary)', fontSize: '1.1rem' }}>Device control & telemetry</p>
        </div>
        <div style={{ display: 'flex', alignItems: 'center', gap: '16px', flexWrap: 'wrap' }}>
          <div className="glass-panel" style={{ padding: '8px 16px', display: 'flex', flexDirection: 'column', gap: '4px', borderRadius: '12px' }}>
            <div style={{ display: 'flex', alignItems: 'center', gap: '12px' }}>
              <span style={{ fontSize: '0.9rem', fontWeight: '600', color: 'var(--text-primary)' }}>
                {user.email}
              </span>
            </div>
            <div style={{ display: 'flex', alignItems: 'center', gap: '8px', marginTop: '4px' }}>
              <div style={{ 
                width: '8px', height: '8px', borderRadius: '50%', 
                backgroundColor: deviceLinked ? '#2ea043' : '#f85149', 
                boxShadow: deviceLinked ? '0 0 10px #2ea043' : '0 0 10px #f85149' 
              }}></div>
              <span style={{ fontSize: '0.8rem', color: 'var(--text-secondary)' }}>
                {deviceLinked ? 'App Connected' : 'App Not Connected'}
              </span>
            </div>
          </div>
          <button onClick={handleLogout} className="btn btn-danger" style={{ padding: '8px 16px', fontSize: '0.9rem' }}>Logout</button>
        </div>
      </header>

      {/* AutoLoc Location History Google Maps Card */}
      <section className="glass-panel" style={{ padding: '1.25rem', marginBottom: '3rem', borderRadius: '16px' }}>
        {(() => {
          const autoLocs = locations.filter(loc => loc.command === 'autoloc' || (loc.sourceType && loc.sourceType.includes('autoloc')) || loc.raw?.includes('AutoLoc'));

          return (
            <>
              <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', flexWrap: 'wrap', gap: '1rem', marginBottom: '1.25rem' }}>
                <div>
                  <h2 style={{ fontSize: '1.35rem', margin: 0 }}>🔄 AutoLoc Background History (Last 5)</h2>
                  <p style={{ fontSize: '0.85rem', color: 'var(--text-secondary)', marginTop: '4px' }}>
                    Automatic periodic background GPS tracking records sent by device.
                  </p>
                </div>
                {autoLocs.length > 0 && (
                  <div style={{ display: 'flex', gap: '6px', flexWrap: 'wrap' }}>
                    {autoLocs.map((loc, idx) => {
                      const isSelected = selectedLocIndex === idx;
                      return (
                        <button
                          key={loc.id || idx}
                          onClick={() => setSelectedLocIndex(idx)}
                          className="btn"
                          style={{
                            padding: '6px 10px',
                            fontSize: '0.8rem',
                            fontWeight: '600',
                            backgroundColor: isSelected ? '#238636' : 'var(--border-light)',
                            color: isSelected ? '#fff' : 'var(--text-primary)',
                            border: isSelected ? '1px solid #2ea043' : '1px solid var(--glass-border)',
                            borderRadius: '8px',
                            cursor: 'pointer'
                          }}
                        >
                          Loc #{idx + 1} {idx === 0 ? '(Latest)' : ''}
                        </button>
                      );
                    })}
                  </div>
                )}
              </div>

              {autoLocs.length === 0 ? (
                <div style={{ padding: '2rem 1rem', textAlign: 'center', color: 'var(--text-secondary)', backgroundColor: 'var(--border-light)', borderRadius: '12px', fontSize: '0.9rem' }}>
                  <div style={{ fontSize: '2rem', marginBottom: '0.5rem' }}>📍</div>
                  No background <strong>autoloc</strong> location tracks recorded yet.<br />
                  Enable <strong>AutoLoc Background Tracking</strong> in app settings or using the toggle below.
                </div>
              ) : (
                (() => {
                  const activeLoc = autoLocs[selectedLocIndex] || autoLocs[0];
                  let lat = typeof activeLoc.lat === 'number' && activeLoc.lat !== 0 ? activeLoc.lat : 0;
                  let lon = typeof activeLoc.lon === 'number' && activeLoc.lon !== 0 ? activeLoc.lon : 0;

                  if (!lat || !lon) {
                    const text = activeLoc.raw || activeLoc.mapsUrl || '';
                    const mapsMatch = text.match(/https?:\/\/[^\s]*[\?&](?:q|ll|query)=(-?\d+(?:\.\d+)?),(-?\d+(?:\.\d+)?)/i);
                    if (mapsMatch) {
                      lat = parseFloat(mapsMatch[1]);
                      lon = parseFloat(mapsMatch[2]);
                    }
                  }

                  if (!lat || !lon) {
                    const text = activeLoc.raw || '';
                    const latMatch = text.match(/Lat(?:itude)?:\s*(-?\d+(?:\.\d+)?)/i);
                    const lonMatch = text.match(/Lon(?:gitude)?:\s*(-?\d+(?:\.\d+)?)/i);
                    if (latMatch && lonMatch) {
                      lat = parseFloat(latMatch[1]);
                      lon = parseFloat(lonMatch[2]);
                    }
                  }

                  if (!lat || !lon) {
                    const text = activeLoc.raw || '';
                    const pairMatch = text.match(/(-?\d{1,2}(?:\.\d+)?)\s*,\s*(-?\d{1,3}(?:\.\d+)?)/);
                    if (pairMatch) {
                      lat = parseFloat(pairMatch[1]);
                      lon = parseFloat(pairMatch[2]);
                    }
                  }

                  const mapsUrl = activeLoc.mapsUrl || (lat && lon ? `https://maps.google.com/maps?q=${lat},${lon}` : '');
                  const embedUrl = lat && lon ? `https://maps.google.com/maps?q=${lat},${lon}&t=&z=15&ie=UTF8&iwloc=&output=embed` : '';

                  return (
                    <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fit, minmax(280px, 1fr))', gap: '1.25rem' }}>
                      <div style={{ width: '100%', height: '300px', borderRadius: '12px', overflow: 'hidden', border: '1px solid var(--glass-border)', boxShadow: '0 4px 16px rgba(0,0,0,0.15)' }}>
                        {lat && lon ? (
                          <iframe width="100%" height="100%" frameBorder="0" scrolling="no" src={embedUrl} style={{ border: 'none' }}></iframe>
                        ) : (
                          <div style={{ width: '100%', height: '100%', display: 'flex', alignItems: 'center', justifyContent: 'center', backgroundColor: 'var(--border-light)', fontSize: '0.9rem' }}>
                            Coordinates Unavailable
                          </div>
                        )}
                      </div>

                      <div style={{ display: 'flex', flexDirection: 'column', justifyContent: 'space-between', gap: '1rem' }}>
                        <div style={{ backgroundColor: 'var(--border-light)', padding: '1rem', borderRadius: '12px', border: '1px solid var(--glass-border)', display: 'flex', flexDirection: 'column', gap: '8px' }}>
                          <div style={{ fontSize: '0.8rem', fontWeight: 'bold', color: '#a5d6ff', paddingBottom: '4px', borderBottom: '1px solid var(--glass-border)' }}>
                            🔄 AutoLoc Background Track #{selectedLocIndex + 1}
                          </div>
                          <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: '8px', fontSize: '0.85rem' }}>
                            <div><strong>🕒 Timestamp:</strong><br />{activeLoc.timestamp ? new Date(activeLoc.timestamp).toLocaleString() : 'N/A'}</div>
                            <div><strong>📡 Source:</strong><br />{activeLoc.provider || 'GPS'}</div>
                            <div><strong>📍 Coordinates:</strong><br />{lat && lon ? `${lat}, ${lon}` : 'N/A'}</div>
                            {activeLoc.accuracy && activeLoc.accuracy !== 'N/A' && <div><strong>🎯 Accuracy:</strong><br />{activeLoc.accuracy}</div>}
                          </div>
                        </div>

                        <div style={{ display: 'flex', gap: '8px' }}>
                          <a href={mapsUrl} target="_blank" rel="noreferrer" className="btn btn-primary" style={{ flex: 1, textAlign: 'center', textDecoration: 'none', padding: '0.75rem', fontSize: '0.9rem', fontWeight: '600' }}>
                            🗺️ Open in Google Maps
                          </a>
                          <button onClick={() => deleteData('autoloc')} className="btn btn-danger" style={{ padding: '0.75rem 1rem', fontSize: '0.85rem' }}>
                            🗑️ Delete Location History
                          </button>
                        </div>
                      </div>
                    </div>
                  );
                })()
              )}
            </>
          );
        })()}
      </section>

      {/* Core Commands */}
      <h2 style={{ fontSize: '1.5rem', marginBottom: '1.5rem' }}>Core Commands</h2>
      <div className="responsive-grid" style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fit, minmax(250px, 1fr))', gap: '1.5rem', marginBottom: '3rem' }}>
        <div className="glass-panel" style={{ padding: '1.5rem' }}>
          <div style={{ fontSize: '2rem', marginBottom: '1rem' }}>📍</div>
          <h3 style={{ fontSize: '1.2rem', marginBottom: '0.5rem' }}>Locate Device</h3>
          <button disabled={activeCmd === 'locate'} onClick={() => sendCommand('locate')} className="btn btn-primary" style={{ width: '100%', marginBottom: '1rem', ...getBtnStyle('locate') }}>
            {activeCmd === 'locate' ? 'Locating...' : 'Locate'}
          </button>
          {renderResult('locate')}
        </div>

        <div className="glass-panel" style={{ padding: '1.5rem' }}>
          <div style={{ fontSize: '2rem', marginBottom: '1rem' }}>🔊</div>
          <h3 style={{ fontSize: '1.2rem', marginBottom: '0.5rem' }}>Ring Alarm</h3>
          <button disabled={activeCmd === 'ring'} onClick={() => sendCommand('ring')} className="btn" style={{ width: '100%', marginBottom: '1rem', ...getBtnStyle('ring') }}>
            {activeCmd === 'ring' ? 'Sending...' : 'Trigger Siren'}
          </button>
          {renderResult('ring')}
        </div>

        <div className="glass-panel" style={{ padding: '1.5rem' }}>
          <div style={{ fontSize: '2rem', marginBottom: '1rem' }}>🔒</div>
          <h3 style={{ fontSize: '1.2rem', marginBottom: '0.5rem' }}>Lock Device</h3>
          <button disabled={activeCmd === 'lock'} onClick={() => sendCommand('lock')} className="btn" style={{ width: '100%', marginBottom: '1rem', ...getBtnStyle('lock') }}>
            {activeCmd === 'lock' ? 'Locking...' : 'Lock Screen'}
          </button>
          {renderResult('lock')}
        </div>
        
        <div className="glass-panel" style={{ padding: '1.5rem' }}>
          <div style={{ fontSize: '2rem', marginBottom: '1rem' }}>📊</div>
          <h3 style={{ fontSize: '1.2rem', marginBottom: '0.5rem' }}>Device Stats</h3>
          <button disabled={activeCmd === 'stats'} onClick={() => sendCommand('stats')} className="btn" style={{ width: '100%', marginBottom: '1rem', ...getBtnStyle('stats') }}>
            {activeCmd === 'stats' ? 'Fetching...' : 'Get Stats'}
          </button>
          {renderResult('stats')}
        </div>
      </div>

      {/* Device Toggles */}
      <h2 style={{ fontSize: '1.5rem', marginBottom: '1.5rem' }}>Device Toggles</h2>
      <div className="responsive-grid" style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fit, minmax(250px, 1fr))', gap: '1.5rem', marginBottom: '3rem' }}>
        
        <div className="glass-panel" style={{ padding: '1.5rem' }}>
          <div style={{ fontSize: '2rem', marginBottom: '1rem' }}>🔦</div>
          <h3 style={{ fontSize: '1.2rem', marginBottom: '0.5rem' }}>Flashlight</h3>
          <div style={{ display: 'flex', gap: '10px', marginBottom: '1rem' }}>
            <button disabled={activeCmd === 'flash on'} onClick={() => sendCommand('flash on')} className="btn btn-primary" style={{ flex: 1, ...getBtnStyle('flash on') }}>On</button>
            <button disabled={activeCmd === 'flash off'} onClick={() => sendCommand('flash off')} className="btn" style={{ flex: 1, ...getBtnStyle('flash off') }}>Off</button>
          </div>
          {renderResult('flash')}
        </div>

        <div className="glass-panel" style={{ padding: '1.5rem' }}>
          <div style={{ fontSize: '2rem', marginBottom: '1rem' }}>🔵</div>
          <h3 style={{ fontSize: '1.2rem', marginBottom: '0.5rem' }}>Bluetooth</h3>
          <div style={{ display: 'flex', gap: '10px', marginBottom: '1rem' }}>
            <button disabled={activeCmd === 'bluetooth on'} onClick={() => sendCommand('bluetooth on')} className="btn btn-primary" style={{ flex: 1, ...getBtnStyle('bluetooth on') }}>On</button>
            <button disabled={activeCmd === 'bluetooth off'} onClick={() => sendCommand('bluetooth off')} className="btn" style={{ flex: 1, ...getBtnStyle('bluetooth off') }}>Off</button>
          </div>
          {renderResult('bluetooth')}
        </div>

        <div className="glass-panel" style={{ padding: '1.5rem' }}>
          <div style={{ fontSize: '2rem', marginBottom: '1rem' }}>🗺️</div>
          <h3 style={{ fontSize: '1.2rem', marginBottom: '0.5rem' }}>GPS Tracking</h3>
          <div style={{ display: 'flex', gap: '10px', marginBottom: '1rem' }}>
            <button disabled={activeCmd === 'gps on'} onClick={() => sendCommand('gps on')} className="btn btn-primary" style={{ flex: 1, ...getBtnStyle('gps on') }}>On</button>
            <button disabled={activeCmd === 'gps off'} onClick={() => sendCommand('gps off')} className="btn" style={{ flex: 1, ...getBtnStyle('gps off') }}>Off</button>
          </div>
          {renderResult('gps')}
        </div>

        <div className="glass-panel" style={{ padding: '1.5rem' }}>
          <div style={{ fontSize: '2rem', marginBottom: '1rem' }}>🌙</div>
          <h3 style={{ fontSize: '1.2rem', marginBottom: '0.5rem' }}>Do Not Disturb</h3>
          <div style={{ display: 'flex', gap: '10px', marginBottom: '1rem' }}>
            <button disabled={activeCmd === 'nodisturb on'} onClick={() => sendCommand('nodisturb on')} className="btn btn-primary" style={{ flex: 1, ...getBtnStyle('nodisturb on') }}>On</button>
            <button disabled={activeCmd === 'nodisturb off'} onClick={() => sendCommand('nodisturb off')} className="btn" style={{ flex: 1, ...getBtnStyle('nodisturb off') }}>Off</button>
          </div>
          {renderResult('nodisturb')}
        </div>

        <div className="glass-panel" style={{ padding: '1.5rem' }}>
          <div style={{ fontSize: '2rem', marginBottom: '1rem' }}>🔄</div>
          <h3 style={{ fontSize: '1.2rem', marginBottom: '0.5rem' }}>AutoLoc Tracking</h3>
          <p style={{ color: 'var(--text-secondary)', fontSize: '0.8rem', marginBottom: '1rem' }}>
            Periodic background GPS updates sent automatically.
          </p>
          <div style={{ display: 'flex', gap: '10px', marginBottom: '1rem' }}>
            <button disabled={activeCmd === 'autoloc on'} onClick={() => sendCommand('autoloc on')} className="btn btn-primary" style={{ flex: 1, ...getBtnStyle('autoloc on') }}>On</button>
            <button disabled={activeCmd === 'autoloc off'} onClick={() => sendCommand('autoloc off')} className="btn" style={{ flex: 1, ...getBtnStyle('autoloc off') }}>Off</button>
          </div>
          {renderResult('autoloc')}
        </div>
      </div>

      {/* Experimental Commands */}
      <h2 style={{ fontSize: '1.5rem', marginBottom: '1.5rem', color: 'var(--text-secondary)' }}>Experimental</h2>
      <div className="responsive-grid" style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fit, minmax(250px, 1fr))', gap: '1.5rem', marginBottom: '3rem' }}>
        <div className="glass-panel" style={{ padding: '1.5rem' }}>
          <div style={{ fontSize: '2rem', marginBottom: '1rem' }}>📸</div>
          <h3 style={{ fontSize: '1.2rem', marginBottom: '0.5rem' }}>Photo Capture</h3>
          <div style={{ display: 'flex', gap: '10px', marginBottom: '1rem' }}>
            <button disabled={activeCmd === 'photo front'} onClick={() => sendCommand('photo front')} className="btn btn-primary" style={{ flex: 1, ...getBtnStyle('photo front') }}>Front</button>
            <button disabled={activeCmd === 'photo back'} onClick={() => sendCommand('photo back')} className="btn" style={{ flex: 1, ...getBtnStyle('photo back') }}>Back</button>
          </div>
          {renderResult('photo')}
        </div>

        <div className="glass-panel" style={{ padding: '1.5rem' }}>
          <div style={{ fontSize: '2rem', marginBottom: '1rem' }}>🎤</div>
          <h3 style={{ fontSize: '1.2rem', marginBottom: '0.5rem' }}>Audio Recording</h3>
          <button disabled={activeCmd === 'audio'} onClick={() => sendCommand('audio')} className="btn" style={{ width: '100%', marginBottom: '1rem', ...getBtnStyle('audio') }}>
            {activeCmd === 'audio' ? 'Recording 30s...' : 'Record Audio'}
          </button>
          {renderResult('audio')}
        </div>
        
        <div className="glass-panel" style={{ padding: '1.5rem' }}>
          <div style={{ fontSize: '2rem', marginBottom: '1rem' }}>🎥</div>
          <h3 style={{ fontSize: '1.2rem', marginBottom: '0.5rem', display: 'flex', alignItems: 'center', gap: '6px' }}>
            <span>Video Capture</span>
            <span style={{ fontSize: '0.7rem', backgroundColor: 'rgba(235, 163, 54, 0.2)', color: '#eba336', border: '1px solid rgba(235, 163, 54, 0.4)', padding: '2px 6px', borderRadius: '4px', fontWeight: 'bold' }}>BETA</span>
          </h3>
          <div style={{ display: 'flex', gap: '10px', marginBottom: '1rem' }}>
            <button disabled={activeCmd === 'video front'} onClick={() => sendCommand('video front')} className="btn btn-primary" style={{ flex: 1, ...getBtnStyle('video front') }}>Front</button>
            <button disabled={activeCmd === 'video back'} onClick={() => sendCommand('video back')} className="btn" style={{ flex: 1, ...getBtnStyle('video back') }}>Back</button>
          </div>
          {renderResult('video')}
        </div>
        
        <div className="glass-panel" style={{ padding: '1.5rem' }}>
          <div style={{ fontSize: '2rem', marginBottom: '1rem' }}>🚨</div>
          <h3 style={{ fontSize: '1.2rem', marginBottom: '0.5rem' }}>Theft Mode</h3>
          <button disabled={activeCmd === 'theft'} onClick={() => sendCommand('theft')} className="btn" style={{ width: '100%', borderColor: '#eba336', color: '#eba336', marginBottom: '1rem', ...getBtnStyle('theft') }}>
            {activeCmd === 'theft' ? 'Activating...' : 'Activate Theft Mode'}
          </button>
          {renderResult('theft')}
        </div>
      </div>
      
      {/* Danger Zone */}
      <h2 style={{ fontSize: '1.5rem', marginBottom: '1.5rem', color: 'var(--danger-color)' }}>Danger Zone</h2>
      <div className="responsive-grid" style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fit, minmax(250px, 1fr))', gap: '1.5rem', marginBottom: '2rem' }}>
        <div className="glass-panel" style={{ padding: '1.5rem', border: '1px solid rgba(248, 81, 73, 0.3)' }}>
          <div style={{ fontSize: '2rem', marginBottom: '1rem' }}>⚠️</div>
          <h3 style={{ fontSize: '1.2rem', marginBottom: '0.5rem', color: 'var(--danger-color)' }}>Factory Reset</h3>
          <p style={{ color: 'var(--text-secondary)', fontSize: '0.9rem', marginBottom: '1.5rem' }}>Permanently wipe all data from your device.</p>
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

        <div className="glass-panel" style={{ padding: '1.5rem', border: '1px solid rgba(248, 81, 73, 0.3)' }}>
          <div style={{ fontSize: '2rem', marginBottom: '1rem' }}>🧹</div>
          <h3 style={{ fontSize: '1.2rem', marginBottom: '0.5rem', color: 'var(--danger-color)' }}>Delete Cloud Data</h3>
          <p style={{ color: 'var(--text-secondary)', fontSize: '0.9rem', marginBottom: '1.5rem' }}>Erase all stored telemetry and photos from the database.</p>
          <button onClick={() => deleteData(undefined, true)} className="btn btn-danger" style={{ width: '100%' }}>
            Delete All Data
          </button>
        </div>

        <div className="glass-panel" style={{ padding: '1.5rem', border: '1px solid rgba(248, 81, 73, 0.3)' }}>
          <div style={{ fontSize: '2rem', marginBottom: '1rem' }}>☠️</div>
          <h3 style={{ fontSize: '1.2rem', marginBottom: '0.5rem', color: 'var(--danger-color)' }}>Delete Account</h3>
          <p style={{ color: 'var(--text-secondary)', fontSize: '0.9rem', marginBottom: '1.5rem' }}>Permanently delete your account and all associated data.</p>
          <button onClick={() => deleteAccount()} className="btn btn-danger" style={{ width: '100%' }}>
            Delete Account
          </button>
        </div>
      </div>

      {/* Reusable Output Modal */}
      {selectedOutput && (results[selectedOutput] || photos[selectedOutput]) && (
        <div style={{
          position: 'fixed', top: 0, left: 0, right: 0, bottom: 0,
          backgroundColor: 'var(--overlay-bg)', zIndex: 1000,
          display: 'flex', justifyContent: 'center', alignItems: 'center', padding: '1rem'
        }} onClick={() => setSelectedOutput(null)}>
          <div className="glass-panel" style={{
            width: '100%', maxWidth: '800px', maxHeight: '90vh', overflowY: 'auto',
            padding: '2.5rem', position: 'relative', border: '1px solid var(--glass-border)'
          }} onClick={e => e.stopPropagation()}>
            <button onClick={() => setSelectedOutput(null)} style={{
              position: 'absolute', top: '1rem', right: '1rem', background: 'none', border: 'none',
              color: 'var(--text-primary)', fontSize: '1.5rem', cursor: 'pointer'
            }}>×</button>
            <h2 style={{ fontSize: '1.5rem', marginBottom: '1rem', textTransform: 'capitalize', display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
              <span>{selectedOutput} Output</span>
              <button onClick={() => deleteData(selectedOutput)} className="btn btn-danger" style={{ padding: '4px 12px', fontSize: '0.8rem', marginRight: '2rem' }}>
                Delete Data
              </button>
            </h2>
            <div style={{ color: 'var(--text-secondary)', marginBottom: '1.5rem', fontSize: '0.9rem' }}>
              Received: {new Date(Math.max(
                results[selectedOutput]?.timestamp ? new Date(results[selectedOutput].timestamp).getTime() : 0, 
                photos[selectedOutput]?.timestamp ? new Date(photos[selectedOutput].timestamp).getTime() : 0
              )).toLocaleString()}
            </div>
            
            {photos[selectedOutput] && (
              <div style={{ marginBottom: '1.5rem', borderRadius: '8px', overflow: 'hidden', border: '1px solid var(--glass-border)' }}>
                <AuthenticatedImage url={photos[selectedOutput].url} user={user} />
              </div>
            )}

            {results[selectedOutput] && (
              <div style={{ 
                backgroundColor: 'var(--code-bg)', padding: '1.5rem', borderRadius: '12px',
                border: '1px solid var(--border-light)', color: 'var(--text-primary)'
              }}>
                {renderTelemetryContent(results[selectedOutput].result)}
              </div>
            )}
          </div>
        </div>
      )}
    </main>
  );
}
