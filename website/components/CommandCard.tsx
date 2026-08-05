'use client';

import React from 'react';

interface CommandCardProps {
  icon: string;
  title: string;
  command: string;
  description?: string;
  buttons?: Array<{
    label: string;
    cmd: string;
    primary?: boolean;
    danger?: boolean;
  }>;
  onOpenRunnerModal?: (cmdName: string) => void;
  onSendCommand: (commandString: string) => void;
  isPending: boolean;
  activeCmd: string | null;
  history: any[];
  onSelectOutput: (cmd: string) => void;
}

export default function CommandCard({
  icon,
  title,
  command,
  description,
  buttons,
  onOpenRunnerModal,
  onSendCommand,
  isPending,
  activeCmd,
  history,
  onSelectOutput
}: CommandCardProps) {
  const getBtnStyle = (cmd: string) => {
    if (activeCmd === cmd) {
      return { animation: 'pulseGlow 2s infinite' };
    }
    return {};
  };

  const res = history.find(h => h.command === command || h.command.startsWith(command) || h.id === command);

  const getMediaUrl = (payload: any) => {
    if (!payload) return null;
    if (typeof payload === 'object') {
      if (payload.type === 'media' && payload.url) return payload.url;
      if (payload.url) return payload.url;
    }
    if (typeof payload === 'string') {
      const match = payload.match(/https?:\/\/(?:drive|docs)\.google\.com\/[^\s]+/i);
      if (match) return match[0];
    }
    return null;
  };

  const mediaUrl = res ? getMediaUrl(res.payload) : null;

  return (
    <div className="glass-panel" style={{
      padding: '1.25rem',
      display: 'flex',
      flexDirection: 'column',
      justifyContent: 'space-between',
      border: '1px solid #30363d',
      backgroundColor: '#161b22',
      borderRadius: '12px',
      boxShadow: '0 4px 12px rgba(0,0,0,0.3)'
    }}>
      <div>
        <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '0.5rem' }}>
          <div style={{ fontSize: '1.8rem' }}>{icon}</div>
          {onOpenRunnerModal && (
            <button
              type="button"
              onClick={() => onOpenRunnerModal(command)}
              style={{
                background: '#21262d',
                border: '1px solid #30363d',
                borderRadius: '6px',
                padding: '4px 8px',
                fontSize: '0.85rem',
                color: '#8b949e',
                cursor: 'pointer'
              }}
              title="Configure parameters"
            >
              ⚙️ Config
            </button>
          )}
        </div>
        <h3 style={{ fontSize: '1.1rem', fontWeight: 'bold', marginBottom: '0.35rem', color: '#f0f6fc' }}>{title}</h3>
        {description && <p style={{ color: '#8b949e', fontSize: '0.85rem', marginBottom: '0.85rem' }}>{description}</p>}
      </div>

      <div>
        {buttons && buttons.length > 0 ? (
          <div style={{ display: 'flex', gap: '6px', flexWrap: 'wrap', marginBottom: '0.5rem' }}>
            {buttons.map((b, idx) => (
              <button
                key={idx}
                disabled={isPending}
                onClick={() => onSendCommand(b.cmd)}
                className={`btn ${b.primary ? 'btn-primary' : b.danger ? 'btn-danger' : ''}`}
                style={{ flex: 1, padding: '7px 8px', fontSize: '0.8rem', whiteSpace: 'nowrap', ...getBtnStyle(b.cmd) }}
              >
                {b.label}
              </button>
            ))}
          </div>
        ) : (
          <button
            disabled={isPending}
            onClick={() => onSendCommand(command)}
            className="btn btn-primary"
            style={{ width: '100%', padding: '8px 12px', fontSize: '0.85rem', marginBottom: '0.5rem', ...getBtnStyle(command) }}
          >
            {activeCmd === command ? 'Sending...' : `Run ${title}`}
          </button>
        )}

        {/* Media Button if Media Link is present */}
        {mediaUrl && (
          <a
            href={mediaUrl}
            target="_blank"
            rel="noreferrer"
            className="btn"
            style={{
              display: 'flex',
              alignItems: 'center',
              justifyContent: 'center',
              gap: '6px',
              width: '100%',
              padding: '6px 10px',
              fontSize: '0.8rem',
              backgroundColor: '#2ea043',
              color: '#fff',
              border: 'none',
              borderRadius: '6px',
              marginBottom: '0.5rem',
              textDecoration: 'none',
              fontWeight: '600'
            }}
          >
            📁 View Media in Google Drive ↗
          </a>
        )}

        {/* Output Result Button */}
        {res ? (
          <button
            type="button"
            onClick={() => onSelectOutput(res.command)}
            className="btn"
            style={{
              width: '100%',
              fontSize: '0.8rem',
              backgroundColor: '#21262d',
              color: '#58a6ff',
              border: '1px solid #30363d',
              padding: '6px 8px',
              borderRadius: '6px'
            }}
          >
            View Output ({new Date(res.timestamp).toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' })})
          </button>
        ) : (
          <div style={{ fontSize: '0.75rem', color: '#8b949e', textAlign: 'center', padding: '4px 0' }}>
            No output received yet
          </div>
        )}
      </div>
    </div>
  );
}
