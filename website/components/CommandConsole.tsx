'use client';

import React, { useState } from 'react';

interface CommandConsoleProps {
  onSendCommand: (command: string) => void;
  isPending: boolean;
}

const PRESETS = [
  { label: '📍 Locate Device', cmd: 'locate' },
  { label: '🔊 Ring 2 min', cmd: 'ring 120' },
  { label: '🔊 Ring 1 hour', cmd: 'ring 3600' },
  { label: '🎙️ Record 45s Audio', cmd: 'audio 45' },
  { label: '📷 Front Flash Photo', cmd: 'photo front flash' },
  { label: '🎥 Back Video', cmd: 'video back' },
  { label: '🔒 Lock with Custom Msg', cmd: 'lock Emergency Contact: 911' },
  { label: '📊 Device Stats', cmd: 'stats' },
  { label: '🚨 Theft Mode', cmd: 'theft' },
  { label: '🔔 Ringer Normal', cmd: 'ringermode normal' },
  { label: '📳 Ringer Vibrate', cmd: 'ringermode vibrate' },
  { label: '🔕 Ringer Silent', cmd: 'ringermode silent' },
  { label: '🔦 Flashlight On', cmd: 'flash on' },
  { label: '🛰️ GPS On', cmd: 'gps on' },
  { label: '🔵 Bluetooth On', cmd: 'bluetooth on' },
  { label: '🌙 DND Off', cmd: 'nodisturb off' },
];

export default function CommandConsole({ onSendCommand, isPending }: CommandConsoleProps) {
  const [inputVal, setInputVal] = useState('');

  const handleSubmit = (e: React.FormEvent) => {
    e.preventDefault();
    if (!inputVal.trim() || isPending) return;
    onSendCommand(inputVal.trim());
  };

  return (
    <div className="glass-panel" style={{ padding: '1.5rem', marginBottom: '2rem', border: '1px solid rgba(88, 166, 255, 0.3)', backgroundColor: 'rgba(13, 17, 23, 0.85)' }}>
      <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '1rem', flexWrap: 'wrap', gap: '8px' }}>
        <div style={{ display: 'flex', alignItems: 'center', gap: '10px' }}>
          <span style={{ fontSize: '1.5rem' }}>💻</span>
          <div>
            <h2 style={{ fontSize: '1.2rem', fontWeight: '700', margin: 0 }}>Command Console</h2>
            <p style={{ color: 'var(--text-secondary)', fontSize: '0.85rem', margin: 0 }}>Execute any command or custom argument string directly on target device</p>
          </div>
        </div>
        <span style={{ fontSize: '0.75rem', padding: '4px 10px', borderRadius: '12px', backgroundColor: 'rgba(88, 166, 255, 0.15)', color: '#58a6ff', border: '1px solid rgba(88, 166, 255, 0.3)', fontFamily: 'monospace' }}>
          Terminal Mode
        </span>
      </div>

      <form onSubmit={handleSubmit} style={{ display: 'flex', gap: '10px', width: '100%', flexWrap: 'wrap', marginBottom: '1rem' }}>
        <div style={{ position: 'relative', flex: 1, minWidth: '240px' }}>
          <span style={{ position: 'absolute', left: '12px', top: '50%', transform: 'translateY(-50%)', color: '#58a6ff', fontFamily: 'monospace', fontWeight: 'bold' }}>
            $
          </span>
          <input
            type="text"
            value={inputVal}
            onChange={(e) => setInputVal(e.target.value)}
            placeholder="Type any command: audio 60, ring 1800, lock SOS Message, stats..."
            style={{
              width: '100%',
              padding: '12px 14px 12px 30px',
              backgroundColor: 'rgba(0, 0, 0, 0.4)',
              border: '1px solid var(--glass-border)',
              borderRadius: '8px',
              color: '#fff',
              fontFamily: 'monospace',
              fontSize: '0.95rem',
              outline: 'none'
            }}
          />
        </div>
        <button
          type="submit"
          disabled={isPending || !inputVal.trim()}
          className="btn btn-primary"
          style={{ padding: '12px 20px', display: 'flex', alignItems: 'center', gap: '8px', fontSize: '0.9rem', backgroundColor: '#238636', border: 'none' }}
        >
          <span>▶</span> Run Command
        </button>
      </form>

      {/* Quick Suggestion Badges */}
      <div style={{ display: 'flex', flexWrap: 'wrap', gap: '6px', alignItems: 'center' }}>
        <span style={{ fontSize: '0.75rem', color: 'var(--text-secondary)', marginRight: '4px' }}>Quick Presets:</span>
        {PRESETS.map((p) => (
          <button
            key={p.cmd}
            type="button"
            onClick={() => setInputVal(p.cmd)}
            style={{
              background: 'rgba(255, 255, 255, 0.05)',
              border: '1px solid var(--glass-border)',
              borderRadius: '6px',
              color: 'var(--text-primary)',
              padding: '4px 10px',
              fontSize: '0.75rem',
              fontFamily: 'monospace',
              cursor: 'pointer',
              transition: 'all 0.15s ease'
            }}
          >
            {p.label}
          </button>
        ))}
      </div>
    </div>
  );
}
