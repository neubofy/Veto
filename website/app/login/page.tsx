'use client';

import { useState, useEffect } from 'react';
import { signInWithPopup, onAuthStateChanged, GoogleAuthProvider } from 'firebase/auth';
import { auth, googleProvider } from '@/lib/firebaseClient';
import { useRouter } from 'next/navigation';
import { accountManager, StoredAccount } from '@/lib/accountManager';
import { motion } from 'framer-motion';
import { LogIn, Trash2, ChevronRight } from 'lucide-react';

export default function LoginPage() {
  const [error, setError] = useState<string | null>(null);
  const [accounts, setAccounts] = useState<StoredAccount[]>([]);
  const router = useRouter();

  useEffect(() => {
    setAccounts(accountManager.getStoredAccounts());
    
    const unsubscribe = onAuthStateChanged(auth, (user) => {
      if (user) {
        router.push('/dashboard');
      }
    });
    return () => unsubscribe();
  }, [router]);

  const handleGoogleSignIn = async () => {
    try {
      const provider = new GoogleAuthProvider();
      provider.setCustomParameters({ prompt: 'select_account' });
      const result = await signInWithPopup(auth, provider);
      
      accountManager.addAccount({
        uid: result.user.uid,
        email: result.user.email || '',
        displayName: result.user.displayName || 'Unknown User',
        photoURL: result.user.photoURL || '',
      });
      
      router.push('/dashboard');
    } catch (err: any) {
      console.error(err);
      setError(err.message);
    }
  };

  const handleAccountClick = async (account: StoredAccount) => {
    try {
      const provider = new GoogleAuthProvider();
      provider.setCustomParameters({ prompt: 'select_account', login_hint: account.email });
      await signInWithPopup(auth, provider);
      router.push('/dashboard');
    } catch (err: any) {
      console.error(err);
      setError(err.message);
    }
  };

  const handleRemoveAccount = (e: React.MouseEvent, uid: string) => {
    e.stopPropagation();
    accountManager.removeAccount(uid);
    setAccounts(accountManager.getStoredAccounts());
  };

  return (
    <main style={{ 
      display: 'flex', 
      alignItems: 'center', 
      justifyContent: 'center', 
      minHeight: '100vh',
      width: '100%',
      padding: '1rem'
    }}>
      <div className="glass-panel" style={{ 
        padding: '2.5rem', 
        width: '100%', 
        maxWidth: '440px',
        textAlign: 'center',
        borderRadius: '24px'
      }}>
        <div style={{ fontSize: '3rem', marginBottom: '1rem' }}>🛡️</div>
        <h1 style={{ fontSize: '2rem', fontWeight: '700', marginBottom: '0.5rem' }}>Veto</h1>
        <p style={{ color: 'var(--text-secondary)', marginBottom: '2rem' }}>
          Secure Remote Access Dashboard
        </p>
        
        {error && (
          <div style={{ 
            padding: '12px', 
            backgroundColor: 'rgba(248, 81, 73, 0.1)', 
            border: '1px solid rgba(248, 81, 73, 0.3)',
            borderRadius: '8px',
            color: 'var(--danger-color)',
            marginBottom: '1.5rem',
            fontSize: '0.9rem'
          }}>
            {error}
          </div>
        )}

        {accounts.length > 0 && (
          <div style={{ marginBottom: '1.5rem', textAlign: 'left' }}>
            <h3 style={{ fontSize: '0.875rem', fontWeight: 500, color: '#9ca3af', marginBottom: '0.5rem' }}>Saved Accounts</h3>
            <div style={{ display: 'flex', flexDirection: 'column', gap: '12px' }}>
              {accounts.map(account => (
                <motion.div
                  key={account.uid}
                  whileHover={{ scale: 1.02, backgroundColor: 'rgba(31, 41, 55, 1)', borderColor: 'rgba(55, 65, 81, 1)' }}
                  whileTap={{ scale: 0.98 }}
                  onClick={() => handleAccountClick(account)}
                  style={{
                    position: 'relative',
                    display: 'flex',
                    cursor: 'pointer',
                    alignItems: 'center',
                    justifyContent: 'space-between',
                    borderRadius: '12px',
                    border: '1px solid rgba(31, 41, 55, 1)',
                    backgroundColor: 'rgba(17, 24, 39, 0.5)',
                    padding: '12px',
                    transition: 'all 0.2s ease'
                  }}
                >
                  <div style={{ display: 'flex', alignItems: 'center', gap: '12px' }}>
                    <img
                      src={account.photoURL || `https://ui-avatars.com/api/?name=${encodeURIComponent(account.displayName)}`}
                      alt=""
                      style={{ height: '40px', width: '40px', borderRadius: '50%', objectFit: 'cover', border: '2px solid rgba(31, 41, 55, 1)' }}
                    />
                    <div>
                      <div style={{ fontSize: '0.875rem', fontWeight: 500, color: '#fff' }}>{account.displayName}</div>
                      <div style={{ fontSize: '0.75rem', color: '#9ca3af' }}>{account.email}</div>
                    </div>
                  </div>
                  
                  <div style={{ display: 'flex', alignItems: 'center', gap: '8px' }}>
                    <button
                      onClick={(e) => handleRemoveAccount(e, account.uid)}
                      style={{
                        padding: '6px',
                        color: '#6b7280',
                        background: 'transparent',
                        border: 'none',
                        cursor: 'pointer',
                        borderRadius: '8px',
                        transition: 'all 0.2s ease',
                      }}
                      onMouseEnter={(e) => { e.currentTarget.style.color = '#f87171'; e.currentTarget.style.backgroundColor = 'rgba(55, 65, 81, 1)'; }}
                      onMouseLeave={(e) => { e.currentTarget.style.color = '#6b7280'; e.currentTarget.style.backgroundColor = 'transparent'; }}
                      title="Remove account"
                    >
                      <Trash2 style={{ height: '16px', width: '16px' }} />
                    </button>
                    <ChevronRight style={{ height: '20px', width: '20px', color: '#4b5563' }} />
                  </div>
                </motion.div>
              ))}
            </div>
          </div>
        )}

        <div style={{ position: 'relative', display: 'flex', alignItems: 'center', padding: '16px 0' }}>
          <div style={{ flexGrow: 1, borderTop: '1px solid rgba(31, 41, 55, 1)' }}></div>
          <span style={{ flexShrink: 0, padding: '0 16px', fontSize: '0.75rem', fontWeight: 500, color: '#6b7280' }}>
            {accounts.length > 0 ? 'Or use another account' : 'Sign in to continue'}
          </span>
          <div style={{ flexGrow: 1, borderTop: '1px solid rgba(31, 41, 55, 1)' }}></div>
        </div>

        <button 
          onClick={handleGoogleSignIn}
          className="btn hover-lift"
          style={{ 
            display: 'flex', 
            width: '100%', 
            alignItems: 'center', 
            justifyContent: 'center', 
            gap: '12px', 
            borderRadius: '12px', 
            backgroundColor: '#ffffff', 
            padding: '12px 16px', 
            fontSize: '0.875rem', 
            fontWeight: 700, 
            color: '#000000',
            border: 'none',
            marginTop: '8px'
          }}
        >
          <img src="https://www.gstatic.com/firebasejs/ui/2.0.0/images/auth/google.svg" alt="Google" style={{ height: '20px', width: '20px' }} />
          {accounts.length > 0 ? 'Add New Account' : 'Sign in with Google'}
        </button>
      </div>
    </main>
  );
}
