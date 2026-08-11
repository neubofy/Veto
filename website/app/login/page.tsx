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
      width: '100%'
    }}>
      <div className="glass-panel" style={{ 
        padding: '3rem', 
        width: '100%', 
        maxWidth: '440px',
        textAlign: 'center'
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
          <div className="mb-6 space-y-3 text-left">
            <h3 className="text-sm font-medium text-gray-400 mb-2">Saved Accounts</h3>
            {accounts.map(account => (
              <motion.div
                key={account.uid}
                whileHover={{ scale: 1.02 }}
                whileTap={{ scale: 0.98 }}
                onClick={() => handleAccountClick(account)}
                className="group relative flex cursor-pointer items-center justify-between rounded-xl border border-gray-800 bg-gray-900/50 p-3 hover:border-gray-700 hover:bg-gray-800 transition-all"
              >
                <div className="flex items-center gap-3">
                  <img
                    src={account.photoURL || `https://ui-avatars.com/api/?name=${encodeURIComponent(account.displayName)}`}
                    alt=""
                    className="h-10 w-10 rounded-full object-cover ring-2 ring-gray-800"
                  />
                  <div>
                    <div className="text-sm font-medium text-white">{account.displayName}</div>
                    <div className="text-xs text-gray-400">{account.email}</div>
                  </div>
                </div>
                
                <div className="flex items-center gap-2">
                  <button
                    onClick={(e) => handleRemoveAccount(e, account.uid)}
                    className="p-1.5 text-gray-500 hover:text-red-400 opacity-0 group-hover:opacity-100 transition-opacity rounded-lg hover:bg-gray-700"
                  >
                    <Trash2 className="h-4 w-4" />
                  </button>
                  <ChevronRight className="h-5 w-5 text-gray-600 group-hover:text-gray-400" />
                </div>
              </motion.div>
            ))}
          </div>
        )}

        <div className="relative flex items-center py-4">
          <div className="flex-grow border-t border-gray-800"></div>
          <span className="flex-shrink-0 px-4 text-xs font-medium text-gray-500">
            {accounts.length > 0 ? 'Or use another account' : 'Sign in to continue'}
          </span>
          <div className="flex-grow border-t border-gray-800"></div>
        </div>

        <button 
          onClick={handleGoogleSignIn}
          className="btn flex w-full items-center justify-center gap-3 rounded-xl bg-white px-4 py-3 text-sm font-bold text-black transition-transform hover:scale-[1.02] active:scale-[0.98]" 
        >
          <img src="https://www.gstatic.com/firebasejs/ui/2.0.0/images/auth/google.svg" alt="Google" className="h-5 w-5" />
          {accounts.length > 0 ? 'Add New Account' : 'Sign in with Google'}
        </button>
      </div>
    </main>
  );
}
