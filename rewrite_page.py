import re

with open('website/app/page.tsx', 'r') as f:
    content = f.read()

# We want to extract the footer
footer_start = content.find('      {/* Footer */}')
if footer_start == -1:
    footer_start = content.find('<footer')

footer_content = content[footer_start:]

new_page = """'use client';
import { useState, useEffect } from 'react';
import Link from 'next/link';
import { auth } from '@/lib/firebaseClient';
import { onAuthStateChanged, signOut, User } from 'firebase/auth';
import { motion } from 'framer-motion';
import { Shield, Bluetooth, MapPin, BellRing, Lock, EyeOff, Smartphone, ServerOff, Sliders, HardDrive, Image as ImageIcon, Video, Mic } from 'lucide-react';

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
    <main style={{ minHeight: '100vh', display: 'flex', flexDirection: 'column', backgroundColor: 'var(--bg-color)', color: 'var(--text-primary)' }}>
      {/* Navigation Bar */}
      <nav style={{
        padding: '1rem 2rem',
        display: 'flex',
        justifyContent: 'space-between',
        alignItems: 'center',
        borderBottom: '1px solid var(--border-light, rgba(255,255,255,0.1))',
        background: 'var(--nav-bg)',
        backdropFilter: 'blur(12px)',
        position: 'sticky',
        top: 0,
        zIndex: 100,
        flexWrap: 'wrap',
        gap: '1rem'
      }}>
        <div style={{ display: 'flex', alignItems: 'center', gap: '0.75rem' }}>
          <div style={{ fontSize: '1.8rem', fontWeight: '900', background: 'linear-gradient(135deg, #2f81f7, #a482d8)', WebkitBackgroundClip: 'text', WebkitTextFillColor: 'transparent', letterSpacing: '-0.5px' }}>
            VETO
          </div>
          <span style={{ fontSize: '0.75rem', backgroundColor: 'rgba(47, 129, 247, 0.15)', color: '#2f81f7', border: '1px solid rgba(47, 129, 247, 0.4)', padding: '2px 8px', borderRadius: '12px', fontWeight: 'bold' }}>
            v2.0 Pro Security
          </span>
        </div>
        <div style={{ display: 'flex', gap: '1.5rem', alignItems: 'center', flexWrap: 'wrap' }}>
          <a href="#pr-management" className="nav-link" style={{ textDecoration: 'none', color: 'var(--text-secondary)' }}>Core Features</a>
          <a href="#unbypassable-security" className="nav-link" style={{ textDecoration: 'none', color: 'var(--text-secondary)' }}>Unbypassable Security</a>
          <a href="#privacy-first" className="nav-link" style={{ textDecoration: 'none', color: 'var(--text-secondary)' }}>Privacy First</a>
          <Link href="/privacy" className="nav-link" style={{ textDecoration: 'none', color: 'var(--text-secondary)' }}>Privacy Policy</Link>
          <Link href="/terms" className="nav-link" style={{ textDecoration: 'none', color: 'var(--text-secondary)' }}>Terms</Link>
          {loadingAuth ? (
            <div style={{ padding: '0.6rem 1.5rem', width: '80px' }}></div>
          ) : user ? (
            <>
              <Link href="/dashboard" className="btn btn-primary" style={{ padding: '0.5rem 1.2rem', borderRadius: '30px', backgroundColor: 'var(--primary-color)', color: '#fff', textDecoration: 'none', fontWeight: '600' }}>Dashboard</Link>
              <button onClick={handleLogout} className="btn btn-danger" style={{ padding: '0.5rem 1.2rem', borderRadius: '30px', backgroundColor: 'rgba(248,81,73,0.2)', color: '#f85149', border: '1px solid rgba(248,81,73,0.4)', cursor: 'pointer' }}>Logout</button>
            </>
          ) : (
            <Link href="/login" className="btn btn-primary" style={{ padding: '0.5rem 1.2rem', borderRadius: '30px', backgroundColor: 'var(--primary-color)', color: '#fff', textDecoration: 'none', fontWeight: '600' }}>Sign In</Link>
          )}
        </div>
      </nav>

      {/* Hero Section */}
      <motion.section
        initial={{ opacity: 0, y: 30 }}
        animate={{ opacity: 1, y: 0 }}
        transition={{ duration: 0.8, ease: "easeOut" }}
        style={{ flex: 1, display: 'flex', flexDirection: 'column', justifyContent: 'center', alignItems: 'center', padding: '6rem 1.5rem 4rem 1.5rem', textAlign: 'center', position: 'relative', overflow: 'hidden' }}
      >
        <div style={{ background: 'radial-gradient(circle, rgba(47, 129, 247, 0.2) 0%, rgba(13, 17, 23, 0) 70%)', width: '700px', height: '700px', position: 'absolute', top: '40%', left: '50%', transform: 'translate(-50%, -50%)', zIndex: 0, pointerEvents: 'none' }}></div>

        <div style={{ position: 'relative', zIndex: 1, maxWidth: '1000px' }}>
          <div style={{ display: 'inline-flex', alignItems: 'center', gap: '8px', padding: '6px 16px', borderRadius: '30px', backgroundColor: 'rgba(47, 129, 247, 0.1)', border: '1px solid var(--primary-color)', marginBottom: '1.5rem', fontSize: '0.9rem', color: '#58a6ff' }}>
            <Shield size={16} /> The Ultimate PR Management & Device Recovery Tool
          </div>

          <h1 style={{ fontSize: 'clamp(2.5rem, 6vw, 4.5rem)', fontWeight: '900', lineHeight: 1.1, marginBottom: '1.5rem', letterSpacing: '-1.5px' }}>
            Absolute Control.<br/>
            <span style={{ background: 'linear-gradient(90deg, #2f81f7, #a482d8)', WebkitBackgroundClip: 'text', WebkitTextFillColor: 'transparent' }}>Zero Compromise.</span>
          </h1>

          <p style={{ fontSize: 'clamp(1.1rem, 2.5vw, 1.35rem)', color: 'var(--text-secondary)', maxWidth: '800px', margin: '0 auto 3rem auto', lineHeight: 1.6 }}>
            Veto redefines Find My Device apps. Remotely toggle GPS, Bluetooth, and lock your phone with unbypassable security from Dashboard, SMS, WhatsApp, or Telegram. Total data privacy, offline control, and direct Google Drive storage—we just handle the links.
          </p>

          <div style={{ display: 'flex', gap: '1.25rem', flexWrap: 'wrap', justifyContent: 'center' }}>
            {loadingAuth ? (
              <div className="btn btn-primary" style={{ padding: '1rem 2.2rem', fontSize: '1.05rem', borderRadius: '40px', visibility: 'hidden' }}>Loading...</div>
            ) : user ? (
              <Link href="/dashboard" className="btn btn-primary" style={{ padding: '1rem 2.2rem', fontSize: '1.05rem', borderRadius: '40px', backgroundColor: 'var(--primary-color)', color: '#fff', textDecoration: 'none', fontWeight: '700', boxShadow: '0 8px 24px rgba(47, 129, 247, 0.4)' }}>
                Open Web Dashboard ➔
              </Link>
            ) : (
              <Link href="/login" className="btn btn-primary" style={{ padding: '1rem 2.2rem', fontSize: '1.05rem', borderRadius: '40px', backgroundColor: 'var(--primary-color)', color: '#fff', textDecoration: 'none', fontWeight: '700', boxShadow: '0 8px 24px rgba(47, 129, 247, 0.4)' }}>
                Launch Dashboard ➔
              </Link>
            )}
            <a href="https://github.com/neubofy/Veto/releases" target="_blank" rel="noreferrer" className="btn hover-lift" style={{ padding: '1rem 2.2rem', fontSize: '1.05rem', borderRadius: '40px', backgroundColor: 'var(--glass-bg)', color: 'var(--text-primary)', border: '1px solid var(--glass-border)', textDecoration: 'none', fontWeight: '600' }}>
              Download App
            </a>
          </div>
        </div>
      </motion.section>

      {/* PR Management & Core Features */}
      <motion.section
        id="pr-management"
        initial={{ opacity: 0, y: 30 }}
        whileInView={{ opacity: 1, y: 0 }}
        viewport={{ once: true, margin: "-100px" }}
        transition={{ duration: 0.8 }}
        style={{ padding: '4rem 1.5rem', backgroundColor: 'var(--bg-color)', borderTop: '1px solid var(--border-light)' }}
      >
        <div style={{ maxWidth: '1200px', margin: '0 auto' }}>
          <div style={{ textAlign: 'center', marginBottom: '3.5rem' }}>
            <h2 style={{ fontSize: 'clamp(2rem, 4vw, 3rem)', fontWeight: '800', marginBottom: '1rem' }}>Ultimate Remote Control</h2>
            <p style={{ color: 'var(--text-secondary)', fontSize: '1.1rem', maxWidth: '650px', margin: '0 auto' }}>Beyond standard FMD apps. Seamless control over your hardware without touching it.</p>
          </div>

          <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fit, minmax(320px, 1fr))', gap: '2rem' }}>
            <div className="glass-panel" style={{ borderRadius: '16px', padding: '2rem' }}>
              <div style={{ marginBottom: '1.25rem', color: 'var(--primary-color)' }}><Bluetooth size={40} /></div>
              <h3 style={{ fontSize: '1.35rem', fontWeight: '700', marginBottom: '0.75rem', color: 'var(--text-primary)' }}>Remote Toggles via Multi-Channel</h3>
              <p style={{ color: 'var(--text-secondary)', lineHeight: 1.6, fontSize: '0.95rem' }}>
                Turn on Bluetooth, toggle GPS remotely via your Web Dashboard, SMS, WhatsApp, or Telegram using a secret PIN. Control your hardware no matter what connection is available.
              </p>
            </div>

            <div className="glass-panel" style={{ borderRadius: '16px', padding: '2rem' }}>
              <div style={{ marginBottom: '1.25rem', color: 'var(--primary-color)' }}><MapPin size={40} /></div>
              <h3 style={{ fontSize: '1.35rem', fontWeight: '700', marginBottom: '0.75rem', color: 'var(--text-primary)' }}>Privacy-Focused Tracking</h3>
              <p style={{ color: 'var(--text-secondary)', lineHeight: 1.6, fontSize: '0.95rem' }}>
                We don't track your live location by default. Get an instant ping of your current location on demand. <strong>Coming soon:</strong> Auto-update location to dashboard every 15 minutes if the device moves beyond a 100-meter radius.
              </p>
            </div>

            <div className="glass-panel" style={{ borderRadius: '16px', padding: '2rem' }}>
              <div style={{ marginBottom: '1.25rem', color: 'var(--primary-color)' }}><Sliders size={40} /></div>
              <h3 style={{ fontSize: '1.35rem', fontWeight: '700', marginBottom: '0.75rem', color: 'var(--text-primary)' }}>Granular Granular Control</h3>
              <p style={{ color: 'var(--text-secondary)', lineHeight: 1.6, fontSize: '0.95rem' }}>
                You are in the driver's seat. Disable any specific command or restrict any transport method directly from the app. High security meets personalized access control.
              </p>
            </div>
          </div>
        </div>
      </motion.section>

      {/* Unbypassable Security */}
      <motion.section
        id="unbypassable-security"
        initial={{ opacity: 0, y: 30 }}
        whileInView={{ opacity: 1, y: 0 }}
        viewport={{ once: true, margin: "-100px" }}
        transition={{ duration: 0.8 }}
        style={{ padding: '5rem 1.5rem', backgroundColor: 'var(--bg-color)', borderTop: '1px solid var(--border-light)' }}
      >
        <div style={{ maxWidth: '1100px', margin: '0 auto' }}>
          <div style={{ textAlign: 'center', marginBottom: '3.5rem' }}>
            <div style={{ color: '#f85149', fontWeight: 'bold', fontSize: '0.9rem', letterSpacing: '0.1em', textTransform: 'uppercase', marginBottom: '0.5rem' }}>Intruder Deterrence</div>
            <h2 style={{ fontSize: 'clamp(2rem, 4vw, 2.75rem)', fontWeight: '800', marginBottom: '1rem' }}>Unbypassable Lock & Ring</h2>
            <p style={{ color: 'var(--text-secondary)', fontSize: '1.05rem', maxWidth: '650px', margin: '0 auto' }}>When a device is stolen, compromised, or missing, Veto takes complete lockdown measures.</p>
          </div>

          <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fit, minmax(280px, 1fr))', gap: '1.5rem' }}>
            <div className="glass-panel" style={{ border: '1px solid var(--primary-color)', borderRadius: '12px', padding: '1.75rem' }}>
              <div style={{ marginBottom: '1rem', color: '#f85149' }}><BellRing size={32} /></div>
              <h3 style={{ fontSize: '1.2rem', fontWeight: '700', marginBottom: '0.5rem', color: '#f85149' }}>Invincible Ring Mode</h3>
              <p style={{ color: 'var(--text-secondary)', fontSize: '0.9rem', lineHeight: 1.5 }}>
                When triggered, the volume is maximized and cannot be turned down or forcefully stopped. Continuous vibration is paired with smart speaker health care—taking auto-rests and resuming until unlocked. Select custom siren sounds to convey messages to surroundings.
              </p>
            </div>

            <div className="glass-panel" style={{ border: '1px solid var(--primary-color)', borderRadius: '12px', padding: '1.75rem' }}>
              <div style={{ marginBottom: '1rem', color: '#58a6ff' }}><Lock size={32} /></div>
              <h3 style={{ fontSize: '1.2rem', fontWeight: '700', marginBottom: '0.5rem', color: '#58a6ff' }}>Auto-Trigger Lock</h3>
              <p style={{ color: 'var(--text-secondary)', fontSize: '0.9rem', lineHeight: 1.5 }}>
                Triggering Ring Mode automatically executes the lock command. Veto puts up a dominant overlay proving the device is locked, bypassing standard lock screens and deterring thieves instantly.
              </p>
            </div>

            <div className="glass-panel" style={{ border: '1px solid var(--primary-color)', borderRadius: '12px', padding: '1.75rem' }}>
              <div style={{ marginBottom: '1rem', color: '#a482d8' }}><EyeOff size={32} /></div>
              <h3 style={{ fontSize: '1.2rem', fontWeight: '700', marginBottom: '0.5rem', color: '#a482d8' }}>Biometric Blackout & Messaging</h3>
              <p style={{ color: 'var(--text-secondary)', fontSize: '0.9rem', lineHeight: 1.5 }}>
                Once locked, biometric unlock is strictly disabled. You can render a custom full-screen owner contact overlay message to instruct anyone who finds the device on how to return it.
              </p>
            </div>
          </div>
        </div>
      </motion.section>

      {/* Privacy First */}
      <motion.section
        id="privacy-first"
        initial={{ opacity: 0, y: 30 }}
        whileInView={{ opacity: 1, y: 0 }}
        viewport={{ once: true, margin: "-100px" }}
        transition={{ duration: 0.8 }}
        style={{ padding: '5rem 1.5rem', backgroundColor: 'var(--bg-color)', borderTop: '1px solid var(--border-light)' }}
      >
        <div style={{ maxWidth: '900px', margin: '0 auto' }}>
          <div style={{ textAlign: 'center', marginBottom: '3.5rem' }}>
            <div style={{ color: '#a482d8', fontWeight: 'bold', fontSize: '0.9rem', letterSpacing: '0.1em', textTransform: 'uppercase', marginBottom: '0.5rem' }}>Zero Trust Privacy</div>
            <h2 style={{ fontSize: 'clamp(2rem, 4vw, 2.75rem)', fontWeight: '800', marginBottom: '1rem' }}>End-to-End Encryption & Cloudless Control</h2>
            <p style={{ color: 'var(--text-secondary)', fontSize: '1.05rem', lineHeight: 1.6 }}>We don't want your data. Period.</p>
          </div>

          <div style={{ display: 'flex', flexDirection: 'column', gap: '1.5rem' }}>
            <div className="glass-panel" style={{ borderRadius: '14px', padding: '2rem', display: 'flex', gap: '1.5rem', alignItems: 'flex-start' }}>
              <div style={{ color: 'var(--primary-color)' }}><ServerOff size={32} /></div>
              <div>
                <h3 style={{ fontSize: '1.25rem', fontWeight: '700', color: 'var(--text-primary)', marginBottom: '0.75rem' }}>Fully Functional Offline Mode</h3>
                <p style={{ color: 'var(--text-secondary)', lineHeight: 1.6, fontSize: '0.95rem' }}>
                  Use Veto without ever signing into a cloud sync service. Trigger commands directly through SMS text or messaging app auto-replies. Absolute remote control with zero data touching the internet.
                </p>
              </div>
            </div>

            <div className="glass-panel" style={{ borderRadius: '14px', padding: '2rem', display: 'flex', gap: '1.5rem', alignItems: 'flex-start' }}>
              <div style={{ color: 'var(--primary-color)' }}><Lock size={32} /></div>
              <div>
                <h3 style={{ fontSize: '1.25rem', fontWeight: '700', color: 'var(--text-primary)', marginBottom: '0.75rem' }}>Firestore End-to-End Encryption</h3>
                <p style={{ color: 'var(--text-secondary)', lineHeight: 1.6, fontSize: '0.95rem' }}>
                  Every piece of data that touches our server and Firestore is End-to-End Encrypted. Only your device and your dashboard hold the keys, making server-side data completely unreadable by us or anyone else.
                </p>
              </div>
            </div>

            <div className="glass-panel" style={{ borderRadius: '14px', padding: '2rem', display: 'flex', gap: '1.5rem', alignItems: 'flex-start' }}>
              <div style={{ color: 'var(--primary-color)' }}><HardDrive size={32} /></div>
              <div>
                <h3 style={{ fontSize: '1.25rem', fontWeight: '700', color: 'var(--text-primary)', marginBottom: '0.75rem' }}>Personal Drive Storage</h3>
                <p style={{ color: 'var(--text-secondary)', lineHeight: 1.6, fontSize: '0.95rem' }}>
                  When you execute <code style={{color: '#58a6ff'}}>veto camera</code>, <code style={{color: '#58a6ff'}}>veto audio</code>, or <code style={{color: '#58a6ff'}}>veto video</code>, the files are uploaded directly to <strong>your personal Google Drive</strong>. Veto app just processes a restricted access link that requires your login to view. No photos or media are stored on our servers.
                </p>
              </div>
            </div>
          </div>
        </div>
      </motion.section>
"""

with open('website/app/page.tsx', 'w') as f:
    f.write(new_page)
    f.write(footer_content)
