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

  const renderResult = () => {
    const res = history.find(h => h.command === command || h.command.startsWith(command));
    if (!res) return <div style={{ marginTop: '0.75rem', fontSize: '0.85rem', color: 'var(--text-secondary)' }}>No data yet</div>;
    return (
      <button
        type="button"
        onClick={() => onSelectOutput(res.command)}
        className="btn"
        style={{ marginTop: '0.75rem', width: '100%', fontSize: '0.85rem', backgroundColor: 'var(--border-light)' }}
      >
        View Output ({new Date(res.timestamp).toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' })})
      </button>
    );
  };

  return (
    <div className="glass-panel" style={{ padding: '1.25rem', display: 'flex', flexDirection: 'column', justifyContent: 'space-between' }}>
      <div>
        <div style={{ fontSize: '1.8rem', marginBottom: '0.75rem' }}>{icon}</div>
        <h3 style={{ fontSize: '1.1rem', marginBottom: '0.5rem' }}>{title}</h3>
        {description && <p style={{ color: 'var(--text-secondary)', fontSize: '0.85rem', marginBottom: '0.75rem' }}>{description}</p>}
      </div>

      <div>
        {buttons && buttons.length > 0 ? (
          <div style={{ display: 'flex', gap: '8px', flexWrap: 'wrap' }}>
            {buttons.map((b, idx) => (
              <button
                key={idx}
                disabled={isPending}
                onClick={() => onSendCommand(b.cmd)}
                className={`btn ${b.primary ? 'btn-primary' : b.danger ? 'btn-danger' : ''}`}
                style={{ flex: 1, padding: '8px 10px', fontSize: '0.85rem', ...getBtnStyle(b.cmd) }}
              >
                {b.label}
              </button>
            ))}
          </div>
        ) : (
          <div style={{ display: 'flex', gap: '8px' }}>
            <button
              disabled={isPending}
              onClick={() => onSendCommand(command)}
              className="btn btn-primary"
              style={{ flex: 1, ...getBtnStyle(command) }}
            >
              {activeCmd === command ? 'Sending...' : 'Run Command'}
            </button>
            {onOpenRunnerModal && (
              <button
                type="button"
                onClick={() => onOpenRunnerModal(command)}
                className="btn"
                title="Configure parameters"
                style={{ padding: '8px 12px' }}
              >
                ⚙️
              </button>
            )}
          </div>
        )}
        {renderResult()}
      </div>
    </div>
  );
}
