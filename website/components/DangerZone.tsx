'use client';

import React from 'react';

interface DangerZoneProps {
  onOpenRunnerModal: (cmdName: string) => void;
  onDeleteData: (commandName?: string, all?: boolean) => void;
  onDeleteAccount: () => void;
  isPending: boolean;
}

export default function DangerZone({ onOpenRunnerModal, onDeleteData, onDeleteAccount, isPending }: DangerZoneProps) {
  return (
    <>
      <h2 style={{ fontSize: '1.3rem', marginBottom: '1rem', color: 'var(--danger-color)' }}>Danger Zone</h2>
      <div className="responsive-grid" style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fit, minmax(220px, 1fr))', gap: '1rem', marginBottom: '2rem' }}>
        <div className="glass-panel" style={{ padding: '1.25rem', border: '1px solid rgba(248, 81, 73, 0.3)' }}>
          <div style={{ fontSize: '1.8rem', marginBottom: '0.75rem' }}>🧹</div>
          <h3 style={{ fontSize: '1.1rem', marginBottom: '0.5rem', color: 'var(--danger-color)' }}>Delete Cloud Data</h3>
          <p style={{ color: 'var(--text-secondary)', fontSize: '0.85rem', marginBottom: '1rem' }}>Erase all stored telemetry and command history from database.</p>
          <button onClick={() => onDeleteData(undefined, true)} className="btn btn-danger" style={{ width: '100%' }}>
            Delete All Data
          </button>
        </div>

        <div className="glass-panel" style={{ padding: '1.25rem', border: '1px solid rgba(248, 81, 73, 0.3)' }}>
          <div style={{ fontSize: '1.8rem', marginBottom: '0.75rem' }}>☠️</div>
          <h3 style={{ fontSize: '1.1rem', marginBottom: '0.5rem', color: 'var(--danger-color)' }}>Delete Account</h3>
          <p style={{ color: 'var(--text-secondary)', fontSize: '0.85rem', marginBottom: '1rem' }}>Permanently delete your account and all associated data.</p>
          <button onClick={onDeleteAccount} className="btn btn-danger" style={{ width: '100%' }}>
            Delete Account
          </button>
        </div>
      </div>
    </>
  );
}
