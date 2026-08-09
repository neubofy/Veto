'use client';

import { useState, useEffect } from 'react';
import { signInWithPopup, onAuthStateChanged } from 'firebase/auth';
import { auth, googleProvider } from '@/lib/firebaseClient';
import { useRouter } from 'next/navigation';

export default function LoginPage() {
  const [error, setError] = useState<string | null>(null);
  const router = useRouter();

  useEffect(() => {
    if (!auth) return;
    const unsubscribe = onAuthStateChanged(auth, (user) => {
      if (user) {
        router.push('/dashboard');
      }
    });
    return () => unsubscribe();
  }, [router]);

  const handleGoogleSignIn = async () => {
    try {
      const result = await signInWithPopup(auth, googleProvider);
      console.log('Logged in as:', result.user.email);
      router.push('/dashboard');
    } catch (err: any) {
      console.error(err);
      setError(err.message);
    }
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
        maxWidth: '400px',
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

        {!auth ? (
          <div style={{
            padding: '16px',
            backgroundColor: 'rgba(218, 54, 51, 0.1)',
            border: '1px solid #da3633',
            borderRadius: '8px',
            color: '#ff7b72',
            textAlign: 'left',
            fontSize: '0.9rem',
            lineHeight: 1.5
          }}>
            <strong>⚠️ Firebase Not Configured</strong>
            <p style={{ marginTop: '8px', marginBottom: 0 }}>
              You need to create a <code>.env.local</code> file in the <code>website/</code> directory and add your Firebase credentials before you can log in.
            </p>
          </div>
        ) : (
          <button 
            onClick={handleGoogleSignIn}
            className="btn" 
            style={{ width: '100%', padding: '14px', background: '#fff', color: '#000', fontWeight: 'bold' }}
          >
            <img src="https://www.gstatic.com/firebasejs/ui/2.0.0/images/auth/google.svg" alt="Google" style={{ width: '20px', height: '20px', marginRight: '10px', verticalAlign: 'middle' }} />
            Sign in with Google
          </button>
        )}
      </div>
    </main>
  );
}

