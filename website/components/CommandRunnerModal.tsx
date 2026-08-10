'use client';

import React, { useState } from 'react';

interface CommandRunnerModalProps {
  commandName: string;
  onClose: () => void;
  onConfirm: (fullCommandString: string) => void;
}

export default function CommandRunnerModal({ commandName, onClose, onConfirm }: CommandRunnerModalProps) {
  const [ringSecs, setRingSecs] = useState('120');
  const [audioSecs, setAudioSecs] = useState('60');
  const [lockMsg, setLockMsg] = useState('Device Locked by Owner');
  const [cameraType, setCameraType] = useState('front');
  const [withFlash, setWithFlash] = useState(false);

  const [ringerMode, setRingerMode] = useState('normal');

  const handleExecute = () => {
    let cmdString = commandName;

    switch (commandName) {
      case 'ring':
        cmdString = `ring ${ringSecs || '30'}`;
        break;
      case 'ringermode':
        cmdString = `ringermode ${ringerMode}`;
        break;
      case 'audio':
        cmdString = `audio ${audioSecs || '30'}`;
        break;
      case 'lock':
        cmdString = lockMsg.trim() ? `lock ${lockMsg.trim()}` : 'lock';
        break;
      case 'photo':
        cmdString = `photo ${cameraType}${withFlash ? ' flash' : ''}`;
        break;
      case 'video':
        cmdString = `video ${cameraType}${withFlash ? ' flash' : ''}`;
        break;

    }

    onConfirm(cmdString);
    onClose();
  };

  return (
    <div style={{
      position: 'fixed', top: 0, left: 0, right: 0, bottom: 0,
      backgroundColor: 'rgba(0,0,0,0.75)', zIndex: 1100,
      display: 'flex', justifyContent: 'center', alignItems: 'center', padding: '1rem'
    }} onClick={onClose}>
      <div className="glass-panel" style={{
        width: '100%', maxWidth: '480px', padding: '1.75rem',
        position: 'relative', border: '1px solid var(--glass-border)',
        backgroundColor: '#161b22'
      }} onClick={e => e.stopPropagation()}>
        <button onClick={onClose} style={{
          position: 'absolute', top: '1rem', right: '1rem', background: 'none', border: 'none',
          color: 'var(--text-secondary)', fontSize: '1.5rem', cursor: 'pointer'
        }}>×</button>

        <h3 style={{ fontSize: '1.25rem', fontWeight: 'bold', marginBottom: '0.5rem', textTransform: 'capitalize' }}>
          Configure {commandName} Command
        </h3>
        <p style={{ color: 'var(--text-secondary)', fontSize: '0.85rem', marginBottom: '1.25rem' }}>
          Set custom parameters for this execution
        </p>

        {commandName === 'ring' && (
          <div style={{ marginBottom: '1.25rem' }}>
            <label style={{ display: 'block', fontSize: '0.85rem', marginBottom: '0.5rem', color: 'var(--text-secondary)' }}>
              Ring Duration (5s to 7200s / 2 hours):
            </label>
            <input
              type="number"
              min="5"
              max="7200"
              value={ringSecs}
              onChange={e => setRingSecs(e.target.value)}
              style={{ width: '100%', padding: '10px', borderRadius: '6px', border: '1px solid var(--glass-border)', backgroundColor: '#0d1117', color: '#fff' }}
            />
            <div style={{ display: 'flex', gap: '6px', marginTop: '8px' }}>
              {['30', '180', '600', '1800', '3600', '7200'].map(sec => (
                <button key={sec} type="button" onClick={() => setRingSecs(sec)} style={{ padding: '4px 8px', fontSize: '0.75rem', borderRadius: '4px', border: '1px solid var(--glass-border)', background: 'var(--border-light)', cursor: 'pointer' }}>
                  {parseInt(sec) >= 3600 ? `${parseInt(sec)/3600}h` : parseInt(sec) >= 60 ? `${parseInt(sec)/60}m` : `${sec}s`}
                </button>
              ))}
            </div>
          </div>
        )}

        {commandName === 'ringermode' && (
          <div style={{ marginBottom: '1.25rem' }}>
            <label style={{ display: 'block', fontSize: '0.85rem', marginBottom: '0.5rem', color: 'var(--text-secondary)' }}>
              Select Ringer Sound Mode:
            </label>
            <div style={{ display: 'flex', gap: '8px' }}>
              <button type="button" onClick={() => setRingerMode('normal')} className={`btn ${ringerMode === 'normal' ? 'btn-primary' : ''}`} style={{ flex: 1, padding: '10px 8px', fontSize: '0.85rem' }}>🔔 Normal</button>
              <button type="button" onClick={() => setRingerMode('vibrate')} className={`btn ${ringerMode === 'vibrate' ? 'btn-primary' : ''}`} style={{ flex: 1, padding: '10px 8px', fontSize: '0.85rem' }}>📳 Vibrate</button>
              <button type="button" onClick={() => setRingerMode('silent')} className={`btn ${ringerMode === 'silent' ? 'btn-primary' : ''}`} style={{ flex: 1, padding: '10px 8px', fontSize: '0.85rem' }}>🔕 Silent</button>
            </div>
          </div>
        )}

        {commandName === 'audio' && (
          <div style={{ marginBottom: '1.25rem' }}>
            <label style={{ display: 'block', fontSize: '0.85rem', marginBottom: '0.5rem', color: 'var(--text-secondary)' }}>
              Recording Duration (seconds):
            </label>
            <input
              type="number"
              min="5"
              max="300"
              value={audioSecs}
              onChange={e => setAudioSecs(e.target.value)}
              style={{ width: '100%', padding: '10px', borderRadius: '6px', border: '1px solid var(--glass-border)', backgroundColor: '#0d1117', color: '#fff' }}
            />
            <div style={{ display: 'flex', gap: '6px', marginTop: '8px' }}>
              {['15', '30', '60', '120', '300'].map(sec => (
                <button key={sec} type="button" onClick={() => setAudioSecs(sec)} style={{ padding: '4px 8px', fontSize: '0.75rem', borderRadius: '4px', border: '1px solid var(--glass-border)', background: 'var(--border-light)', cursor: 'pointer' }}>
                  {sec}s
                </button>
              ))}
            </div>
          </div>
        )}

        {commandName === 'lock' && (
          <div style={{ marginBottom: '1.25rem' }}>
            <label style={{ display: 'block', fontSize: '0.85rem', marginBottom: '0.5rem', color: 'var(--text-secondary)' }}>
              Lock Screen Display Message:
            </label>
            <input
              type="text"
              value={lockMsg}
              onChange={e => setLockMsg(e.target.value)}
              placeholder="e.g. Phone lost! Call 555-0192"
              style={{ width: '100%', padding: '10px', borderRadius: '6px', border: '1px solid var(--glass-border)', backgroundColor: '#0d1117', color: '#fff' }}
            />
          </div>
        )}

        {(commandName === 'photo' || commandName === 'video') && (
          <div style={{ marginBottom: '1.25rem' }}>
            <label style={{ display: 'block', fontSize: '0.85rem', marginBottom: '0.5rem', color: 'var(--text-secondary)' }}>
              Camera Facing:
            </label>
            <div style={{ display: 'flex', gap: '10px', marginBottom: '10px' }}>
              <button type="button" onClick={() => setCameraType('front')} className={`btn ${cameraType === 'front' ? 'btn-primary' : ''}`} style={{ flex: 1 }}>Front</button>
              <button type="button" onClick={() => setCameraType('back')} className={`btn ${cameraType === 'back' ? 'btn-primary' : ''}`} style={{ flex: 1 }}>Back</button>
            </div>
            <label style={{ display: 'flex', alignItems: 'center', gap: '8px', cursor: 'pointer', fontSize: '0.9rem', color: '#f0f6fc', marginTop: '10px' }}>
              <input type="checkbox" checked={withFlash} onChange={e => setWithFlash(e.target.checked)} />
              Enable Flashlight / Torch ⚡
            </label>
          </div>
        )}


        <div style={{ display: 'flex', gap: '10px', justifyContent: 'flex-end', marginTop: '1.5rem' }}>
          <button onClick={onClose} className="btn" style={{ padding: '8px 16px' }}>Cancel</button>
          <button onClick={handleExecute} className="btn btn-primary" style={{ padding: '8px 16px', backgroundColor: '#238636' }}>
            Execute Command
          </button>
        </div>
      </div>
    </div>
  );
}
