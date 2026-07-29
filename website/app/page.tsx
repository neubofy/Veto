'use client';
import { useState, useEffect } from 'react';
import Link from 'next/link';
import { auth } from '@/lib/firebaseClient';
import { onAuthStateChanged, signOut, User } from 'firebase/auth';
import { motion } from 'framer-motion';

export default function LandingPage() {
  const [user, setUser] = useState<User | null>(null);
  const [loadingAuth, setLoadingAuth] = useState(true);

  useEffect(() => {
    let unsubscribe: () => void = () => {};
    if (!auth) {
      setTimeout(() => setLoadingAuth(false), 0);
    } else {
      unsubscribe = onAuthStateChanged(auth, (currentUser) => {
        setUser(currentUser);
        setLoadingAuth(false);
      });
    }
    return () => unsubscribe();
  }, []);

  const handleLogout = async () => {
    await signOut(auth);
  };

  return (
    <main style={{ minHeight: '100vh', display: 'flex', flexDirection: 'column' }}>
      {/* Navbar */}
      <nav style={{ padding: '1rem 2rem', display: 'flex', justifyContent: 'space-between', alignItems: 'center', borderBottom: '1px solid var(--border-light)', background: 'var(--nav-bg)', backdropFilter: 'blur(10px)', position: 'sticky', top: 0, zIndex: 100, flexWrap: 'wrap', gap: '1rem' }}>
        <div style={{ fontSize: '1.5rem', fontWeight: '800', background: 'var(--hero-gradient)', WebkitBackgroundClip: 'text', WebkitTextFillColor: 'transparent', letterSpacing: '-0.5px' }}>
          VETO
        </div>
        <div style={{ display: 'flex', gap: '1.5rem', alignItems: 'center', flexWrap: 'wrap' }}>
          <Link href="/privacy" className="nav-link">Privacy</Link>
          <Link href="/terms" className="nav-link">Terms</Link>
          {loadingAuth ? (
            <div style={{ padding: '0.6rem 1.5rem', width: '80px' }}></div>
          ) : user ? (
            <>
              <Link href="/dashboard" className="btn btn-primary" style={{ padding: '0.5rem 1.2rem', borderRadius: '30px' }}>Dashboard</Link>
              <button onClick={handleLogout} className="btn btn-danger" style={{ padding: '0.5rem 1.2rem', borderRadius: '30px' }}>Logout</button>
            </>
          ) : (
            <Link href="/login" className="btn btn-primary" style={{ padding: '0.5rem 1.2rem', borderRadius: '30px' }}>Login</Link>
          )}
        </div>
      </nav>

      {/* Hero Section */}
      <motion.section
        initial={{ opacity: 0, y: 50 }}
        whileInView={{ opacity: 1, y: 0 }}
        viewport={{ once: true, margin: "-100px" }}
        transition={{ duration: 0.8, ease: "easeOut" }}
        style={{ flex: 1, display: 'flex', flexDirection: 'column', justifyContent: 'center', alignItems: 'center', padding: '4rem 1.5rem', textAlign: 'center', position: 'relative', overflow: 'hidden' }}
      >
        <div className="glow-blob" style={{ background: 'rgba(47, 129, 247, 0.15)', width: '600px', height: '600px', top: '50%', left: '50%', transform: 'translate(-50%, -50%)', zIndex: -1 }}></div>
        
        <div style={{ fontSize: 'clamp(5rem, 15vw, 12rem)', fontWeight: '900', lineHeight: 1, marginBottom: '1rem', letterSpacing: '-5px', background: 'var(--hero-gradient)', WebkitBackgroundClip: 'text', WebkitTextFillColor: 'transparent' }}>
          VETO
        </div>

        <h1 style={{ fontSize: 'clamp(2rem, 5vw, 4rem)', fontWeight: '800', lineHeight: 1.1, marginBottom: '1.5rem', letterSpacing: '-1.5px' }}>
          Absolute Control.<br/>
          <span style={{ background: 'linear-gradient(90deg, #2f81f7, #a482d8)', WebkitBackgroundClip: 'text', WebkitTextFillColor: 'transparent' }}>Zero Compromise.</span>
        </h1>
        
        <p style={{ fontSize: 'clamp(1rem, 3vw, 1.2rem)', color: 'var(--text-secondary)', maxWidth: '600px', marginBottom: '3rem', lineHeight: 1.6 }}>
          Veto is the ultimate Android security and remote management platform. 
          Track, lock, or wipe your device from anywhere in the world with military-grade precision. 
          No analytics. No tracking. No ads. 100% Encrypted.
        </p>

        <div style={{ display: 'flex', gap: '1rem', flexWrap: 'wrap', justifyContent: 'center' }}>
          {loadingAuth ? (
            <div className="btn btn-primary" style={{ padding: '1rem 2rem', fontSize: '1rem', borderRadius: '40px', visibility: 'hidden' }}>
              Loading...
            </div>
          ) : user ? (
            <Link href="/dashboard" className="btn btn-primary" style={{ padding: '1rem 2rem', fontSize: '1rem', borderRadius: '40px', boxShadow: '0 8px 24px rgba(47, 129, 247, 0.3)' }}>
              Go to Dashboard
            </Link>
          ) : (
            <Link href="/login" className="btn btn-primary" style={{ padding: '1rem 2rem', fontSize: '1rem', borderRadius: '40px', boxShadow: '0 8px 24px rgba(47, 129, 247, 0.3)' }}>
              Access Dashboard
            </Link>
          )}
          <a href="https://github.com/neubofy/Veto/releases" target="_blank" rel="noreferrer" className="btn" style={{ padding: '1rem 2rem', fontSize: '1rem', borderRadius: '40px', background: 'var(--border-light)', border: '1px solid var(--glass-border)' }}>
            Download App
          </a>
        </div>
      </motion.section>

      {/* Features Grid */}
      <motion.section
        initial={{ opacity: 0, y: 50 }}
        whileInView={{ opacity: 1, y: 0 }}
        viewport={{ once: true, margin: "-100px" }}
        transition={{ duration: 0.8, ease: "easeOut" }}
        style={{ padding: '4rem 1.5rem', background: 'var(--overlay-bg)', borderTop: '1px solid var(--border-light)' }}
      >
        <div style={{ maxWidth: '1200px', margin: '0 auto', display: 'grid', gridTemplateColumns: 'repeat(auto-fit, minmax(280px, 1fr))', gap: '2rem' }}>
          
          <div className="glass-panel hover-lift" style={{ padding: '2rem' }}>
            <div style={{ fontSize: '2.5rem', marginBottom: '1.5rem' }}>📍</div>
            <h3 style={{ fontSize: '1.3rem', marginBottom: '1rem' }}>Global Tracking</h3>
            <p style={{ color: 'var(--text-secondary)', lineHeight: 1.6 }}>Ping your device securely and instantly receive precise GPS coordinates displayed right on your dashboard.</p>
          </div>

          <div className="glass-panel hover-lift" style={{ padding: '2rem' }}>
            <div style={{ fontSize: '2.5rem', marginBottom: '1.5rem' }}>🔒</div>
            <h3 style={{ fontSize: '1.3rem', marginBottom: '1rem' }}>Maximum Security</h3>
            <p style={{ color: 'var(--text-secondary)', lineHeight: 1.6 }}>Your data is end-to-end encrypted. Even we can&apos;t see your data. No analytics, no tracking, no ads.</p>
          </div>

          <div className="glass-panel hover-lift" style={{ padding: '2rem' }}>
            <div style={{ fontSize: '2.5rem', marginBottom: '1.5rem' }}>🛡️</div>
            <h3 style={{ fontSize: '1.3rem', marginBottom: '1rem' }}>Source Available</h3>
            <p style={{ color: 'var(--text-secondary)', lineHeight: 1.6 }}>The complete app source is available for review, verification, and proof of security. Note: It is not open-source, and modifying it to create your own version is strictly prohibited.</p>
          </div>

        </div>
      </motion.section>

      {/* SMS Commands Guide */}
      <motion.section
        initial={{ opacity: 0, y: 50 }}
        whileInView={{ opacity: 1, y: 0 }}
        viewport={{ once: true, margin: "-100px" }}
        transition={{ duration: 0.8, ease: "easeOut" }}
        style={{ padding: '4rem 1.5rem', borderTop: '1px solid var(--border-light)' }}
      >
        <div style={{ maxWidth: '800px', margin: '0 auto' }}>
          <h2 style={{ fontSize: 'clamp(2rem, 4vw, 2.5rem)', fontWeight: '700', marginBottom: '1rem', textAlign: 'center' }}>SMS Commands Guide</h2>
          <p style={{ color: 'var(--text-secondary)', textAlign: 'center', marginBottom: '3rem', lineHeight: 1.6 }}>
            No internet? No problem. You can control your Veto app entirely via SMS. 
            Just text your phone from an allowed contact number using the commands below.
              (Assuming your trigger word is &quot;veto&quot;)
          </p>
          
          <div style={{ display: 'flex', flexDirection: 'column', gap: '1.5rem' }}>
            
            <div className="glass-panel" style={{ padding: '1.5rem', display: 'flex', flexDirection: 'column', gap: '0.5rem' }}>
              <h3 style={{ color: 'var(--text-primary)', fontSize: '1.1rem' }}>Find Location</h3>
              <code style={{ background: 'var(--code-bg)', padding: '0.5rem 1rem', borderRadius: '6px', color: 'var(--primary-color)', fontSize: '1.2rem', fontWeight: 'bold' }}>veto locate</code>
              <p style={{ color: 'var(--text-secondary)', fontSize: '0.9rem' }}>Forces the phone to acquire a GPS lock and replies back with a Google Maps link.</p>
            </div>
            
            <div className="glass-panel" style={{ padding: '1.5rem', display: 'flex', flexDirection: 'column', gap: '0.5rem' }}>
              <h3 style={{ color: 'var(--text-primary)', fontSize: '1.1rem' }}>Lock Screen</h3>
              <code style={{ background: 'var(--code-bg)', padding: '0.5rem 1rem', borderRadius: '6px', color: 'var(--primary-color)', fontSize: '1.2rem', fontWeight: 'bold' }}>veto lock</code>
              <p style={{ color: 'var(--text-secondary)', fontSize: '0.9rem' }}>Instantly locks the device screen using Device Administrator privileges.</p>
            </div>

            <div className="glass-panel" style={{ padding: '1.5rem', display: 'flex', flexDirection: 'column', gap: '0.5rem' }}>
              <h3 style={{ color: 'var(--text-primary)', fontSize: '1.1rem' }}>Ring Alarm</h3>
              <code style={{ background: 'var(--code-bg)', padding: '0.5rem 1rem', borderRadius: '6px', color: 'var(--primary-color)', fontSize: '1.2rem', fontWeight: 'bold' }}>veto ring</code>
              <p style={{ color: 'var(--text-secondary)', fontSize: '0.9rem' }}>Bypasses silent mode and blasts a loud siren to help you find your phone nearby.</p>
            </div>

            <div className="glass-panel" style={{ padding: '1.5rem', display: 'flex', flexDirection: 'column', gap: '0.5rem' }}>
              <h3 style={{ color: 'var(--text-primary)', fontSize: '1.1rem' }}>Take Photo</h3>
              <code style={{ background: 'var(--code-bg)', padding: '0.5rem 1rem', borderRadius: '6px', color: 'var(--primary-color)', fontSize: '1.2rem', fontWeight: 'bold' }}>veto camera</code>
              <p style={{ color: 'var(--text-secondary)', fontSize: '0.9rem' }}>Silently snaps a picture using the front or rear camera and uploads it to the dashboard.</p>
            </div>
            
            <div className="glass-panel" style={{ padding: '1.5rem', display: 'flex', flexDirection: 'column', gap: '0.5rem' }}>
              <h3 style={{ color: 'var(--text-primary)', fontSize: '1.1rem' }}>Flashlight</h3>
              <code style={{ background: 'var(--code-bg)', padding: '0.5rem 1rem', borderRadius: '6px', color: 'var(--primary-color)', fontSize: '1.2rem', fontWeight: 'bold' }}>veto flash</code>
              <p style={{ color: 'var(--text-secondary)', fontSize: '0.9rem' }}>Blinks the phone&apos;s flashlight rapidly so you can locate it in the dark.</p>
            </div>

            <div className="glass-panel" style={{ padding: '1.5rem', display: 'flex', flexDirection: 'column', gap: '0.5rem' }}>
              <h3 style={{ color: 'var(--text-primary)', fontSize: '1.1rem' }}>Factory Reset</h3>
              <code style={{ background: 'var(--code-bg)', padding: '0.5rem 1rem', borderRadius: '6px', color: 'var(--danger-color)', fontSize: '1.2rem', fontWeight: 'bold' }}>veto delete [password]</code>
              <p style={{ color: 'var(--text-secondary)', fontSize: '0.9rem' }}>Requires your factory reset password. Immediately wipes all data on the device permanently.</p>
            </div>

          </div>
        </div>
      </motion.section>

      {/* Footer */}
      <footer style={{ padding: '4rem 2rem', borderTop: '1px solid var(--border-light)', color: 'var(--text-secondary)', fontSize: '0.9rem' }}>
        <div style={{ maxWidth: '1200px', margin: '0 auto', display: 'flex', flexWrap: 'wrap', gap: '3rem', justifyContent: 'space-between' }}>

          <div style={{ flex: '1 1 300px' }}>
            <h4 style={{ color: 'var(--text-primary)', fontSize: '1.2rem', marginBottom: '1rem', fontWeight: '600' }}>Veto Security</h4>
            <p style={{ marginBottom: '1rem', lineHeight: 1.6 }}>Absolute control. Zero compromise. The ultimate Android security and remote management platform.</p>
            <div style={{ display: 'flex', flexDirection: 'column', gap: '0.5rem' }}>
              <a href="https://github.com/neubofy/Veto" target="_blank" rel="noreferrer" className="nav-link">GitHub Repository</a>
              <a href="mailto:support@neubofy.in" className="nav-link">Support: support@neubofy.in</a>
              <a href="https://github.com/pawanwashudev-official" target="_blank" rel="noreferrer" className="nav-link">Developer Profile</a>
            </div>
          </div>

          <div style={{ flex: '1 1 300px' }}>
            <h4 style={{ color: 'var(--text-primary)', fontSize: '1.2rem', marginBottom: '1rem', fontWeight: '600' }}>Social</h4>
            <div style={{ display: 'flex', flexDirection: 'column', gap: '0.5rem' }}>
              <a href="https://instagram.com/pawanwashudev" target="_blank" rel="noreferrer" className="nav-link">Instagram @pawanwashudev</a>
              <a href="https://t.me/pawanwashudev" target="_blank" rel="noreferrer" className="nav-link">Telegram @pawanwashudev</a>
              <a href="https://x.com/pawanwashudev" target="_blank" rel="noreferrer" className="nav-link">X @pawanwashudev</a>
              <a href="https://wa.me/pawanwashudev" target="_blank" rel="noreferrer" className="nav-link">WhatsApp @pawanwashudev</a>
            </div>
          </div>

          <div style={{ flex: '1 1 320px', display: 'flex', justifyContent: 'center' }}>
            <div className="gravatar-hovercard" style={{ width: '320px', minWidth: '320px', maxWidth: '320px', backgroundColor: 'var(--glass-bg)', border: '1px solid var(--border-light)', borderRadius: '12px', overflow: 'hidden', boxSizing: 'border-box', backdropFilter: 'blur(10px)' }}>
              <div style={{ padding: '16px' }}>
                {/* eslint-disable-next-line @next/next/no-img-element */}
                <img
                  src="https://0.gravatar.com/avatar/f1b5ad3b6a5c5ff2617ea5477983f25a265036eee4f453f3c4806fb78c894494?s=256&d=initials"
                  width="64"
                  height="64"
                  alt="Pawan Washudev"
                  style={{ marginBottom: '8px', borderRadius: '50%' }}
                />
                <div style={{ color: 'var(--text-primary)', fontSize: '20px', fontWeight: '700', lineHeight: '120%', margin: 0, fontFamily: 'SF Pro Text, -apple-system, BlinkMacSystemFont, Segoe UI, Roboto, Oxygen-Sans, Ubuntu, Cantarell, Helvetica Neue, sans-serif' }}>
                  Pawan Washudev
                </div>
                <div style={{ color: 'var(--text-secondary)', fontSize: '14px', fontFamily: 'SF Pro Text, -apple-system, BlinkMacSystemFont, Segoe UI, Roboto, Oxygen-Sans, Ubuntu, Cantarell, Helvetica Neue, sans-serif' }}>
                  Founder, Neubofy
                </div>
                <div style={{ color: 'var(--text-secondary)', fontSize: '14px', fontFamily: 'SF Pro Text, -apple-system, BlinkMacSystemFont, Segoe UI, Roboto, Oxygen-Sans, Ubuntu, Cantarell, Helvetica Neue, sans-serif' }}>
                  Patna, Bihar, India
                </div>
                <a
                  href="https://gravatar.com/pawanwashudevofficial?utm_source=email_signature"
                  target="_blank"
                  rel="noreferrer"
                  style={{ display: 'block', color: 'var(--primary-color)', marginTop: '8px', fontSize: '14px', fontFamily: 'SF Pro Text, -apple-system, BlinkMacSystemFont, Segoe UI, Roboto, Oxygen-Sans, Ubuntu, Cantarell, Helvetica Neue, sans-serif', textDecoration: 'none' }}
                >
                  gravatar.com/pawanwashudevofficial
                </a>
              </div>
            </div>
          </div>

        </div>
        <div style={{ textAlign: 'center', marginTop: '4rem', paddingTop: '2rem', borderTop: '1px solid var(--border-light)' }}>
          <p>&copy; {new Date().getFullYear()} Veto Security. All rights reserved.</p>
          <p style={{ marginTop: '0.5rem', opacity: 0.6, fontSize: '0.8rem' }}>Inspired by FMD</p>
        </div>
      </footer>
    </main>
  );
}
