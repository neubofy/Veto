'use client';

import React, { useEffect, useState } from 'react';
import { useRouter } from 'next/navigation';
import Link from 'next/link';
import { auth, db } from '@/lib/firebaseClient';
import { onAuthStateChanged, User } from 'firebase/auth';
import { collection, onSnapshot, query, orderBy, limit } from 'firebase/firestore';

export default function LocationHistoryPage() {
  const router = useRouter();
  const [user, setUser] = useState<User | null>(null);
  const [loading, setLoading] = useState(true);
  const [locations, setLocations] = useState<any[]>([]);

  useEffect(() => {
    let unsubLoc = () => {};

    const unsubscribeAuth = onAuthStateChanged(auth, (currentUser: User | null) => {
      if (currentUser) {
        setUser(currentUser);

        // Fetch last 5 location history entries
        const q = query(
          collection(db, 'users', currentUser.uid, 'location_history'),
          orderBy('timestamp', 'desc'),
          limit(5)
        );

        unsubLoc = onSnapshot(q, (snapshot: any) => {
          const locs: any[] = [];
          snapshot.forEach((docSnap: any) => {
            locs.push({ id: docSnap.id, ...docSnap.data() });
          });
          setLocations(locs);
        });

      } else {
        setUser(null);
        unsubLoc();
        setLocations([]);
        router.push('/login');
      }
      setLoading(false);
    });

    return () => {
      unsubscribeAuth();
      unsubLoc();
    };
  }, [router]);

  const handleCopy = (text: string, label: string) => {
    navigator.clipboard.writeText(text);
    alert(`Copied ${label} to clipboard!`);
  };

  const renderLocationCard = (loc: any, idx: number) => {
    let payload = loc.payload;
    if (typeof payload === 'string') {
      try { payload = JSON.parse(payload); } catch { payload = { content: payload }; }
    }

    let lat = payload?.lat;
    let lon = payload?.lon;

    if (!lat || !lon) {
      const text = payload?.content || loc.payload || '';
      const match = text.match(/Lat(?:itude)?:\s*(-?\d+(?:\.\d+)?).*?Lon(?:gitude)?:\s*(-?\d+(?:\.\d+)?)/i) ||
                    text.match(/q=(-?\d+(?:\.\d+)?),(-?\d+(?:\.\d+)?)/i);
      if (match) {
        lat = parseFloat(match[1]);
        lon = parseFloat(match[2]);
      }
    }

    const mapEmbedUrl = (lat && lon) ? `https://maps.google.com/maps?q=${lat},${lon}&t=&z=15&ie=UTF8&iwloc=&output=embed` : null;

    return (
      <div key={loc.id || idx} className="glass-panel" style={{
        padding: '1.25rem',
        marginBottom: '1.5rem',
        border: '1px solid #30363d',
        backgroundColor: '#161b22',
        borderRadius: '12px'
      }}>
        <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '1rem', flexWrap: 'wrap', gap: '8px' }}>
          <div>
            <span style={{ fontSize: '0.85rem', fontWeight: 'bold', color: '#58a6ff', backgroundColor: '#0d1117', padding: '4px 10px', borderRadius: '6px', border: '1px solid #30363d' }}>
              #{idx + 1} Fix
            </span>
            <span style={{ fontSize: '0.85rem', color: '#8b949e', marginLeft: '10px' }}>
              {new Date(loc.timestamp).toLocaleString()}
            </span>
          </div>
          {lat && lon && (
            <button type="button" onClick={() => handleCopy(`${lat}, ${lon}`, 'Coordinates')} className="btn" style={{ padding: '4px 10px', fontSize: '0.75rem' }}>
              📋 Copy Coordinates
            </button>
          )}
        </div>

        {mapEmbedUrl && (
          <div style={{ width: '100%', height: '260px', borderRadius: '8px', overflow: 'hidden', border: '1px solid #30363d', marginBottom: '1rem' }}>
            <iframe width="100%" height="100%" frameBorder="0" scrolling="no" src={mapEmbedUrl} style={{ border: 'none' }}></iframe>
          </div>
        )}

        <div style={{
          display: 'grid', gridTemplateColumns: 'repeat(auto-fit, minmax(140px, 1fr))', gap: '8px',
          backgroundColor: '#0d1117', padding: '0.85rem', borderRadius: '8px', border: '1px solid #30363d', fontSize: '0.85rem', color: '#f0f6fc'
        }}>
          <div><strong>Latitude:</strong> {lat || 'N/A'}</div>
          <div><strong>Longitude:</strong> {lon || 'N/A'}</div>
          <div><strong>Provider:</strong> {payload?.provider || 'GPS'}</div>
          {payload?.battery && <div><strong>Battery:</strong> {payload.battery} 🔋</div>}
          {payload?.accuracy && <div><strong>Accuracy:</strong> {payload.accuracy}</div>}
        </div>

        {lat && lon && (
          <a
            href={`https://maps.google.com/?q=${lat},${lon}`}
            target="_blank"
            rel="noreferrer"
            className="btn btn-primary"
            style={{ display: 'block', textAlign: 'center', textDecoration: 'none', padding: '0.75rem', marginTop: '1rem' }}
          >
            Open in Google Maps ↗
          </a>
        )}
      </div>
    );
  };

  if (loading) return (
    <main style={{ padding: '1.5rem 1rem', maxWidth: '900px', margin: '0 auto', width: '100%' }}>
      <div className="skeleton" style={{ width: '200px', height: '36px', marginBottom: '1rem' }}></div>
      <div className="glass-panel skeleton" style={{ height: '240px' }}></div>
    </main>
  );

  if (!user) return null;

  return (
    <main style={{ padding: '1.5rem 1rem', maxWidth: '900px', margin: '0 auto', width: '100%' }}>
      <header style={{ marginBottom: '2rem', display: 'flex', flexWrap: 'wrap', gap: '1rem', justifyContent: 'space-between', alignItems: 'center' }}>
        <div>
          <div style={{ display: 'flex', alignItems: 'center', gap: '12px', marginBottom: '0.5rem' }}>
            <Link href="/dashboard" className="btn" style={{ padding: '6px 12px', fontSize: '0.85rem', textDecoration: 'none' }}>
              ← Dashboard
            </Link>
          </div>
          <h1 style={{ fontSize: '2.2rem', fontWeight: '700', margin: 0, letterSpacing: '-0.02em' }}>Location History</h1>
          <p style={{ color: '#8b949e', fontSize: '0.95rem', marginTop: '4px' }}>
            Showing last 5 location fixes (Hard-capped for quota optimization)
          </p>
        </div>
      </header>

      {locations.length === 0 ? (
        <div className="glass-panel" style={{ padding: '2rem', textAlign: 'center', backgroundColor: '#161b22', border: '1px solid #30363d', borderRadius: '12px' }}>
          <p style={{ color: '#8b949e', fontSize: '1rem' }}>No location history recorded yet.</p>
        </div>
      ) : (
        locations.map((loc, idx) => renderLocationCard(loc, idx))
      )}
    </main>
  );
}
