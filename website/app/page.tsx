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
          <a href="#features" className="nav-link" style={{ textDecoration: 'none', color: 'var(--text-secondary)' }}>Features</a>
          <a href="#transports" className="nav-link" style={{ textDecoration: 'none', color: 'var(--text-secondary)' }}>Transports</a>
          <a href="#security" className="nav-link" style={{ textDecoration: 'none', color: 'var(--text-secondary)' }}>Security &amp; Privacy</a>
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
        style={{ flex: 1, display: 'flex', flexDirection: 'column', justifyContent: 'center', alignItems: 'center', padding: '5rem 1.5rem 4rem 1.5rem', textAlign: 'center', position: 'relative', overflow: 'hidden' }}
      >
        <div style={{ background: 'radial-gradient(circle, rgba(47, 129, 247, 0.2) 0%, rgba(13, 17, 23, 0) 70%)', width: '700px', height: '700px', position: 'absolute', top: '40%', left: '50%', transform: 'translate(-50%, -50%)', zIndex: 0, pointerEvents: 'none' }}></div>
        

        <div style={{ position: 'relative', zIndex: 1, maxWidth: '900px' }}>
          <div style={{ display: 'inline-flex', alignItems: 'center', gap: '8px', padding: '6px 16px', borderRadius: '30px', backgroundColor: 'rgba(47, 129, 247, 0.1)', border: '1px solid var(--primary-color)', marginBottom: '1.5rem', fontSize: '0.9rem', color: '#58a6ff' }}>
            <span>🛡️</span> Highly Reliable Remote Device Management
          </div>

          <h1 style={{ fontSize: 'clamp(2.5rem, 6vw, 4.5rem)', fontWeight: '900', lineHeight: 1.1, marginBottom: '1.5rem', letterSpacing: '-1.5px' }}>
            User-Friendly Control.<br/>
            <span style={{ background: 'linear-gradient(90deg, #2f81f7, #a482d8)', WebkitBackgroundClip: 'text', WebkitTextFillColor: 'transparent' }}>Zero Compromise on Security.</span>
          </h1>
          
          <p style={{ fontSize: 'clamp(1.1rem, 2.5vw, 1.35rem)', color: 'var(--text-secondary)', maxWidth: '750px', margin: '0 auto 3rem auto', lineHeight: 1.6 }}>
            Veto provides a reliable toolkit to manage, track, and secure your Android device. Introduce new users to our app with next-gen PR management. It uses advanced multi-transport fallbacks, including offline SMS and Web Dashboard access, ensuring you stay connected and protected.
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
            <a href="https://github.com/neubofy/Veto/releases" target="_blank" rel="noreferrer" className="btn" style={{ padding: '1rem 2.2rem', fontSize: '1.05rem', borderRadius: '40px', backgroundColor: 'var(--glass-bg)', color: 'var(--text-primary)', border: '1px solid var(--glass-border)', textDecoration: 'none', fontWeight: '600' }}>
              Download Android App
            </a>
          </div>
        </div>
      </motion.section>

      {/* Key Architectural Highlights */}
      <motion.section
        id="features"
        initial={{ opacity: 0, y: 30 }}
        whileInView={{ opacity: 1, y: 0 }}
        viewport={{ once: true, margin: "-100px" }}
        transition={{ duration: 0.8 }}
        style={{ padding: '4rem 1.5rem', backgroundColor: 'var(--bg-color)', borderTop: '1px solid var(--border-light)', borderBottom: '1px solid var(--border-light)' }}
      >
        <div style={{ maxWidth: '1200px', margin: '0 auto' }}>
          <div style={{ textAlign: 'center', marginBottom: '3.5rem' }}>
            <h2 style={{ fontSize: 'clamp(2rem, 4vw, 3rem)', fontWeight: '800', marginBottom: '1rem' }}>Architected for Maximum Resilience</h2>
            <p style={{ color: 'var(--text-secondary)', fontSize: '1.1rem', maxWidth: '650px', margin: '0 auto' }}>Built with zero-trust cryptography and multi-transport redundancy so you never lose control of your device.</p>
          </div>

                    <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fit, minmax(320px, 1fr))', gap: '2rem' }}>
            
            <div className="glass-panel" style={{ borderRadius: '16px', padding: '2rem' }}>
              <div style={{ fontSize: '2.5rem', marginBottom: '1.25rem' }}>🛡️</div>
              <h3 style={{ fontSize: '1.35rem', fontWeight: '700', marginBottom: '0.75rem', color: 'var(--text-primary)' }}>Total Control & Sovereignty</h3>
              <p style={{ color: 'var(--text-secondary)', lineHeight: 1.6, fontSize: '0.95rem' }}>
                You have full control to disable any command or transport method from your app. Veto offers end-to-end encryption for server and Firestore data. Use the app without cloud sync via SMS and messaging apps. Media files are stored in your Google Drive with restricted access links that do not require login.
              </p>
            </div>

            <div className="glass-panel" style={{ borderRadius: '16px', padding: '2rem' }}>
              <div style={{ fontSize: '2.5rem', marginBottom: '1.25rem' }}>📍</div>
              <h3 style={{ fontSize: '1.35rem', fontWeight: '700', marginBottom: '0.75rem', color: 'var(--text-primary)' }}>Advanced Location Tracking</h3>
              <p style={{ color: 'var(--text-secondary)', lineHeight: 1.6, fontSize: '0.95rem' }}>
                Fetch your current location anytime. Upcoming Feature: If your device moves beyond a 100-meter radius, its location will automatically update on the dashboard within 15 minutes.
              </p>
            </div>

            <div className="glass-panel" style={{ borderRadius: '16px', padding: '2rem' }}>
              <div style={{ fontSize: '2.5rem', marginBottom: '1.25rem' }}>🔒</div>
              <h3 style={{ fontSize: '1.35rem', fontWeight: '700', marginBottom: '0.75rem', color: 'var(--text-primary)' }}>Unbypassable Security</h3>
              <p style={{ color: 'var(--text-secondary)', lineHeight: 1.6, fontSize: '0.95rem' }}>
                Remote locking completely disables biometrics. The Ring mode automatically triggers the lock command, playing a continuous, unbypassable custom siren with device vibration. It includes smart speaker health management, automatically resting and resuming until unlocked.
              </p>
            </div>

          </div>
        </div>
      </motion.section>

      {/* Multi-Transport Deep Dive */}
      <motion.section
        id="transports"
        initial={{ opacity: 0, y: 30 }}
        whileInView={{ opacity: 1, y: 0 }}
        viewport={{ once: true, margin: "-100px" }}
        transition={{ duration: 0.8 }}
        style={{ padding: '5rem 1.5rem', backgroundColor: 'var(--bg-color)' }}
      >
        <div style={{ maxWidth: '1100px', margin: '0 auto' }}>
          <div style={{ textAlign: 'center', marginBottom: '3.5rem' }}>
            <div style={{ color: '#58a6ff', fontWeight: 'bold', fontSize: '0.9rem', letterSpacing: '0.1em', textTransform: 'uppercase', marginBottom: '0.5rem' }}>Redundant Control Pipeline</div>
            <h2 style={{ fontSize: 'clamp(2rem, 4vw, 2.75rem)', fontWeight: '800', marginBottom: '1rem' }}>4 Independent Communication Channels</h2>
            <p style={{ color: 'var(--text-secondary)', fontSize: '1.05rem', maxWidth: '650px', margin: '0 auto' }}>Whether your phone has a high-speed 5G connection or no cellular data at all, Veto remains accessible.</p>
          </div>

                    <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fit, minmax(250px, 1fr))', gap: '1.5rem' }}>
            
            <div className="glass-panel" style={{ border: '1px solid var(--primary-color)', borderRadius: '12px', padding: '1.75rem' }}>
              <div style={{ fontSize: '2rem', marginBottom: '1rem' }}>🌐</div>
              <h3 style={{ fontSize: '1.2rem', fontWeight: '700', marginBottom: '0.5rem', color: '#58a6ff' }}>1. Web Dashboard</h3>
              <p style={{ color: 'var(--text-secondary)', fontSize: '0.9rem', lineHeight: 1.5 }}>
                Real-time command dispatch using Firebase FCM push messaging. Features interactive Google Maps location tracking, live telemetry, and media gallery.
              </p>
            </div>

            <div className="glass-panel" style={{ border: '1px solid var(--primary-color)', borderRadius: '12px', padding: '1.75rem' }}>
              <div style={{ fontSize: '2rem', marginBottom: '1rem' }}>💬</div>
              <h3 style={{ fontSize: '1.2rem', fontWeight: '700', marginBottom: '0.5rem', color: '#58a6ff' }}>2. Offline SMS Control</h3>
              <p style={{ color: 'var(--text-secondary)', fontSize: '0.9rem', lineHeight: 1.5 }}>
                Execute commands remotely without an internet connection, including toggling Bluetooth and GPS. If calling from an untrusted number, use your PIN: <code style={{ color: '#58a6ff' }}>VETO &lt;PIN&gt; LOCATE</code>. This temporarily whitelists you. Allowed contacts can simply use: <code style={{ color: '#58a6ff' }}>VETO LOCATE</code>.
              </p>
            </div>

            <div className="glass-panel" style={{ border: '1px solid var(--primary-color)', borderRadius: '12px', padding: '1.75rem' }}>
              <div style={{ fontSize: '2rem', marginBottom: '1rem' }}>🔔</div>
              <h3 style={{ fontSize: '1.2rem', fontWeight: '700', marginBottom: '0.5rem', color: '#58a6ff' }}>3. Notification Interception</h3>
              <p style={{ color: 'var(--text-secondary)', fontSize: '0.9rem', lineHeight: 1.5 }}>
                Intercepts auto-reply notification intents from messaging apps like WhatsApp and Telegram. Similarly to SMS, use <code style={{ color: '#58a6ff' }}>VETO &lt;PIN&gt; COMMAND</code> to securely authenticate via messaging platforms if your number isn&apos;t whitelisted.
              </p>
            </div>

            <div className="glass-panel" style={{ border: '1px solid var(--primary-color)', borderRadius: '12px', padding: '1.75rem' }}>
              <div style={{ fontSize: '2rem', marginBottom: '1rem' }}>📱</div>
              <h3 style={{ fontSize: '1.2rem', fontWeight: '700', marginBottom: '0.5rem', color: '#58a6ff' }}>4. In-App Test Sandbox</h3>
              <p style={{ color: 'var(--text-secondary)', fontSize: '0.9rem', lineHeight: 1.5 }}>
                Integrated test environment directly inside the Android application interface to preview command behavior and verify system permissions.
              </p>
            </div>

          </div>
        </div>
      </motion.section>

      {/* Complete Command Manual Matrix */}
      <motion.section
        initial={{ opacity: 0, y: 30 }}
        whileInView={{ opacity: 1, y: 0 }}
        viewport={{ once: true, margin: "-100px" }}
        transition={{ duration: 0.8 }}
        style={{ padding: '5rem 1.5rem', backgroundColor: 'var(--bg-color)', borderTop: '1px solid var(--border-light)' }}
      >
        <div style={{ maxWidth: '1100px', margin: '0 auto' }}>
          <div style={{ textAlign: 'center', marginBottom: '3rem' }}>
            <h2 style={{ fontSize: 'clamp(2rem, 4vw, 2.75rem)', fontWeight: '800', marginBottom: '1rem' }}>Complete Remote Command Manual</h2>
            <p style={{ color: 'var(--text-secondary)', fontSize: '1.05rem', maxWidth: '650px', margin: '0 auto' }}>Full matrix of commands supported by Veto via Web Dashboard, SMS, or Notification Auto-Reply.</p>
          </div>


          <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fit, minmax(300px, 1fr))', gap: '1.25rem' }}>
            
            <div className="glass-panel" style={{ borderRadius: '12px', padding: '1.5rem' }}>
              <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '0.5rem' }}>
                <span style={{ fontSize: '1.1rem', fontWeight: 'bold', color: '#58a6ff' }}>📍 veto locate</span>
                <span style={{ fontSize: '0.75rem', backgroundColor: 'rgba(47, 129, 247, 0.15)', color: '#58a6ff', padding: '2px 8px', borderRadius: '4px' }}>Location</span>
              </div>
              <p style={{ color: 'var(--text-secondary)', fontSize: '0.88rem', lineHeight: 1.5, margin: 0 }}>
                Powers on location hardware automatically and fetches accurate GPS/Network/Cell coordinates with Google Maps links.<br/><br/>Usage: <code style={{ color: 'var(--text-primary)' }}>veto locate</code><br/>Requires: <code style={{ color: 'var(--text-primary)' }}>LocationPermission</code>
              </p>
            </div>

            <div className="glass-panel" style={{ borderRadius: '12px', padding: '1.5rem' }}>
              <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '0.5rem' }}>
                <span style={{ fontSize: '1.1rem', fontWeight: 'bold', color: '#eba336' }}>🚨 veto theft</span>
                <span style={{ fontSize: '0.75rem', backgroundColor: 'rgba(235, 163, 54, 0.15)', color: '#eba336', padding: '2px 8px', borderRadius: '4px' }}>Macro</span>
              </div>
              <p style={{ color: 'var(--text-secondary)', fontSize: '0.88rem', lineHeight: 1.5, margin: 0 }}>
                Master security macro. Activates Theft Mode, fetches GPS coordinates, enables Bluetooth, turns off DND, and blares a siren alarm.<br/><br/>Usage: <code style={{ color: 'var(--text-primary)' }}>veto theft</code><br/>Requires: <code style={{ color: 'var(--text-primary)' }}>LocationPermission</code>
              </p>
            </div>

            <div className="glass-panel" style={{ borderRadius: '12px', padding: '1.5rem' }}>
              <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '0.5rem' }}>
                <span style={{ fontSize: '1.1rem', fontWeight: 'bold', color: '#58a6ff' }}>🔊 veto ring</span>
                <span style={{ fontSize: '0.75rem', backgroundColor: 'rgba(47, 129, 247, 0.15)', color: '#58a6ff', padding: '2px 8px', borderRadius: '4px' }}>Alarm</span>
              </div>
              <p style={{ color: 'var(--text-secondary)', fontSize: '0.88rem', lineHeight: 1.5, margin: 0 }}>
                Bypasses DND &amp; silent mode, maxes out alarm stream volume, wakes the screen, and loops a high-decibel alert.<br/><br/>Usage: <code style={{ color: 'var(--text-primary)' }}>veto ring [long]</code><br/>Requires: <code style={{ color: 'var(--text-primary)' }}>DoNotDisturbAccessPermission</code>
              </p>
            </div>

            <div className="glass-panel" style={{ borderRadius: '12px', padding: '1.5rem' }}>
              <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '0.5rem' }}>
                <span style={{ fontSize: '1.1rem', fontWeight: 'bold', color: '#58a6ff' }}>🔒 veto lock</span>
                <span style={{ fontSize: '0.75rem', backgroundColor: 'rgba(47, 129, 247, 0.15)', color: '#58a6ff', padding: '2px 8px', borderRadius: '4px' }}>Security</span>
              </div>
              <p style={{ color: 'var(--text-secondary)', fontSize: '0.88rem', lineHeight: 1.5, margin: 0 }}>
                Instantly locks screen using Device Admin API and optionally renders full-screen owner contact overlay message.<br/><br/>Usage: <code style={{ color: 'var(--text-primary)' }}>veto lock [msg]</code><br/>Requires: <code style={{ color: 'var(--text-primary)' }}>DeviceAdminPermission</code>
              </p>
            </div>

            <div className="glass-panel" style={{ borderRadius: '12px', padding: '1.5rem' }}>
              <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '0.5rem' }}>
                <span style={{ fontSize: '1.1rem', fontWeight: 'bold', color: '#58a6ff' }}>📸 veto photo</span>
                <span style={{ fontSize: '0.75rem', backgroundColor: 'rgba(47, 129, 247, 0.15)', color: '#58a6ff', padding: '2px 8px', borderRadius: '4px' }}>Media</span>
              </div>
              <p style={{ color: 'var(--text-secondary)', fontSize: '0.88rem', lineHeight: 1.5, margin: 0 }}>
                Silently captures photo using front or rear camera over lock screens and backs up file to Google Drive.<br/><br/>Usage: <code style={{ color: 'var(--text-primary)' }}>veto photo [front | back] [flash]</code><br/>Requires: <code style={{ color: 'var(--text-primary)' }}>CameraPermission</code>
              </p>
            </div>

            <div className="glass-panel" style={{ borderRadius: '12px', padding: '1.5rem' }}>
              <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '0.5rem' }}>
                <span style={{ fontSize: '1.1rem', fontWeight: 'bold', color: '#58a6ff' }}>🎙️ veto audio</span>
                <span style={{ fontSize: '0.75rem', backgroundColor: 'rgba(47, 129, 247, 0.15)', color: '#58a6ff', padding: '2px 8px', borderRadius: '4px' }}>Media</span>
              </div>
              <p style={{ color: 'var(--text-secondary)', fontSize: '0.88rem', lineHeight: 1.5, margin: 0 }}>
                Records high-quality AAC ambient audio from microphone in background and uploads to Google Drive.<br/><br/>Usage: <code style={{ color: 'var(--text-primary)' }}>veto audio</code><br/>Requires: <code style={{ color: 'var(--text-primary)' }}>RecordAudioPermission</code>
              </p>
            </div>

            <div className="glass-panel" style={{ borderRadius: '12px', padding: '1.5rem' }}>
              <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '0.5rem' }}>
                <div style={{ display: 'flex', alignItems: 'center', gap: '6px' }}>
                  <span style={{ fontSize: '1.1rem', fontWeight: 'bold', color: '#58a6ff' }}>🎥 veto video</span>
                </div>
                <span style={{ fontSize: '0.75rem', backgroundColor: 'rgba(47, 129, 247, 0.15)', color: '#58a6ff', padding: '2px 8px', borderRadius: '4px' }}>Media</span>
              </div>
              <p style={{ color: 'var(--text-secondary)', fontSize: '0.88rem', lineHeight: 1.5, margin: 0 }}>
                Records background video from front or back camera and backs up to Google Drive.<br/><br/>Usage: <code style={{ color: 'var(--text-primary)' }}>veto video [front | back]</code><br/>Requires: <code style={{ color: 'var(--text-primary)' }}>CameraPermission</code>, <code style={{ color: 'var(--text-primary)' }}>RecordAudioPermission</code>
              </p>
            </div>

            <div className="glass-panel" style={{ borderRadius: '12px', padding: '1.5rem' }}>
              <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '0.5rem' }}>
                <span style={{ fontSize: '1.1rem', fontWeight: 'bold', color: '#58a6ff' }}>💡 veto flash</span>
                <span style={{ fontSize: '0.75rem', backgroundColor: 'rgba(47, 129, 247, 0.15)', color: '#58a6ff', padding: '2px 8px', borderRadius: '4px' }}>Hardware</span>
              </div>
              <p style={{ color: 'var(--text-secondary)', fontSize: '0.88rem', lineHeight: 1.5, margin: 0 }}>
                Toggles camera flashlight hardware ON/OFF or blinks torch for visual location signaling.<br/><br/>Usage: <code style={{ color: 'var(--text-primary)' }}>veto flash [on | off]</code><br/>Requires: <code style={{ color: 'var(--text-primary)' }}>None</code>
              </p>
            </div>

            <div className="glass-panel" style={{ borderRadius: '12px', padding: '1.5rem' }}>
              <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '0.5rem' }}>
                <span style={{ fontSize: '1.1rem', fontWeight: 'bold', color: '#58a6ff' }}>📊 veto stats</span>
                <span style={{ fontSize: '0.75rem', backgroundColor: 'rgba(47, 129, 247, 0.15)', color: '#58a6ff', padding: '2px 8px', borderRadius: '4px' }}>Telemetry</span>
              </div>
              <p style={{ color: 'var(--text-secondary)', fontSize: '0.88rem', lineHeight: 1.5, margin: 0 }}>
                Returns device model, Android release, battery %, SIM carrier, SIM phone number, IP addresses, and Wi-Fi SSID scan results.<br/><br/>Usage: <code style={{ color: 'var(--text-primary)' }}>veto stats</code><br/>Requires: <code style={{ color: 'var(--text-primary)' }}>LocationPermission</code>
              </p>
            </div>

            <div className="glass-panel" style={{ borderRadius: '12px', padding: '1.5rem' }}>
              <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '0.5rem' }}>
                <span style={{ fontSize: '1.1rem', fontWeight: 'bold', color: '#f85149' }}>💥 veto delete</span>
                <span style={{ fontSize: '0.75rem', backgroundColor: 'rgba(248, 81, 73, 0.15)', color: '#f85149', padding: '2px 8px', borderRadius: '4px' }}>Danger</span>
              </div>
              <p style={{ color: 'var(--text-secondary)', fontSize: '0.88rem', lineHeight: 1.5, margin: 0 }}>
                Constant-time password-verified emergency factory reset with a transmission buffer to deliver confirmation.<br/><br/>Usage: <code style={{ color: 'var(--text-primary)' }}>veto delete &lt;password&gt; [dryrun]</code><br/>Requires: <code style={{ color: 'var(--text-primary)' }}>DeviceAdminPermission</code>
              </p>
            </div>

            <div className="glass-panel" style={{ borderRadius: '12px', padding: '1.5rem' }}>
              <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '0.5rem' }}>
                <span style={{ fontSize: '1.1rem', fontWeight: 'bold', color: '#58a6ff' }}>📡 veto bluetooth</span>
                <span style={{ fontSize: '0.75rem', backgroundColor: 'rgba(47, 129, 247, 0.15)', color: '#58a6ff', padding: '2px 8px', borderRadius: '4px' }}>Hardware</span>
              </div>
              <p style={{ color: 'var(--text-secondary)', fontSize: '0.88rem', lineHeight: 1.5, margin: 0 }}>
                Toggles bluetooth hardware ON/OFF.<br/><br/>Usage: <code style={{ color: 'var(--text-primary)' }}>veto bluetooth [on | off]</code><br/>Requires: <code style={{ color: 'var(--text-primary)' }}>BluetoothConnectPermission</code>
              </p>
            </div>

            <div className="glass-panel" style={{ borderRadius: '12px', padding: '1.5rem' }}>
              <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '0.5rem' }}>
                <span style={{ fontSize: '1.1rem', fontWeight: 'bold', color: '#58a6ff' }}>🔇 veto nodisturb</span>
                <span style={{ fontSize: '0.75rem', backgroundColor: 'rgba(47, 129, 247, 0.15)', color: '#58a6ff', padding: '2px 8px', borderRadius: '4px' }}>System</span>
              </div>
              <p style={{ color: 'var(--text-secondary)', fontSize: '0.88rem', lineHeight: 1.5, margin: 0 }}>
                Toggles Do Not Disturb mode ON/OFF.<br/><br/>Usage: <code style={{ color: 'var(--text-primary)' }}>veto nodisturb [on | off]</code><br/>Requires: <code style={{ color: 'var(--text-primary)' }}>DoNotDisturbAccessPermission</code>
              </p>
            </div>

            <div className="glass-panel" style={{ borderRadius: '12px', padding: '1.5rem' }}>
              <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '0.5rem' }}>
                <span style={{ fontSize: '1.1rem', fontWeight: 'bold', color: '#58a6ff' }}>📳 veto ringermode</span>
                <span style={{ fontSize: '0.75rem', backgroundColor: 'rgba(47, 129, 247, 0.15)', color: '#58a6ff', padding: '2px 8px', borderRadius: '4px' }}>System</span>
              </div>
              <p style={{ color: 'var(--text-secondary)', fontSize: '0.88rem', lineHeight: 1.5, margin: 0 }}>
                Changes the device ringer profile.<br/><br/>Usage: <code style={{ color: 'var(--text-primary)' }}>veto ringermode [normal | vibrate | silent]</code><br/>Requires: <code style={{ color: 'var(--text-primary)' }}>DoNotDisturbAccessPermission</code>
              </p>
            </div>

            <div className="glass-panel" style={{ borderRadius: '12px', padding: '1.5rem' }}>
              <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '0.5rem' }}>
                <span style={{ fontSize: '1.1rem', fontWeight: 'bold', color: '#58a6ff' }}>🛰️ veto gps</span>
                <span style={{ fontSize: '0.75rem', backgroundColor: 'rgba(47, 129, 247, 0.15)', color: '#58a6ff', padding: '2px 8px', borderRadius: '4px' }}>Hardware</span>
              </div>
              <p style={{ color: 'var(--text-secondary)', fontSize: '0.88rem', lineHeight: 1.5, margin: 0 }}>
                Toggles Location / GPS ON/OFF.<br/><br/>Usage: <code style={{ color: 'var(--text-primary)' }}>veto gps [on | off]</code><br/>Requires: <code style={{ color: 'var(--text-primary)' }}>WriteSecureSettingsPermission</code>
              </p>
            </div>

            <div className="glass-panel" style={{ borderRadius: '12px', padding: '1.5rem' }}>
              <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '0.5rem' }}>
                <span style={{ fontSize: '1.1rem', fontWeight: 'bold', color: '#58a6ff' }}>❓ veto help</span>
                <span style={{ fontSize: '0.75rem', backgroundColor: 'rgba(47, 129, 247, 0.15)', color: '#58a6ff', padding: '2px 8px', borderRadius: '4px' }}>Utility</span>
              </div>
              <p style={{ color: 'var(--text-secondary)', fontSize: '0.88rem', lineHeight: 1.5, margin: 0 }}>
                Displays list of available commands.<br/><br/>Usage: <code style={{ color: 'var(--text-primary)' }}>veto help</code><br/>Requires: <code style={{ color: 'var(--text-primary)' }}>None</code>
              </p>
            </div>

          </div>
        </div>
      </motion.section>


      {/* Help & FAQ Section */}
      <motion.section
        id="faq"
        initial={{ opacity: 0, y: 30 }}
        whileInView={{ opacity: 1, y: 0 }}
        viewport={{ once: true, margin: "-100px" }}
        transition={{ duration: 0.8 }}
        style={{ padding: '5rem 1.5rem', backgroundColor: 'var(--bg-color)', borderTop: '1px solid var(--border-light)' }}
      >
        <div style={{ maxWidth: '900px', margin: '0 auto' }}>
          <div style={{ textAlign: 'center', marginBottom: '3.5rem' }}>
            <h2 style={{ fontSize: 'clamp(2rem, 4vw, 2.75rem)', fontWeight: '800', marginBottom: '1rem' }}>Help &amp; FAQ</h2>
            <p style={{ color: 'var(--text-secondary)', fontSize: '1.05rem', lineHeight: 1.6 }}>Common questions and troubleshooting for Veto commands and transports.</p>
          </div>

          <div style={{ display: 'flex', flexDirection: 'column', gap: '1.5rem' }}>

            <div className="glass-panel" style={{ borderRadius: '14px', padding: '2rem' }}>
              <h3 style={{ fontSize: '1.25rem', fontWeight: '700', color: 'var(--text-primary)', marginBottom: '0.75rem' }}>Q: Why didn&apos;t my SMS command work?</h3>
              <p style={{ color: 'var(--text-secondary)', lineHeight: 1.6, fontSize: '0.95rem' }}>
                A: If you are sending an SMS from a number that is not on your <strong>Allowlist</strong>, you must include your PIN in the command: <code>veto &lt;PIN&gt; locate</code>. Sending a correct PIN will temporarily whitelist that number for future commands. Also, verify that Veto has SMS permissions enabled.
              </p>
            </div>

            <div className="glass-panel" style={{ borderRadius: '14px', padding: '2rem' }}>
              <h3 style={{ fontSize: '1.25rem', fontWeight: '700', color: 'var(--text-primary)', marginBottom: '0.75rem' }}>Q: Why is Notification Reply not working?</h3>
              <p style={{ color: 'var(--text-secondary)', lineHeight: 1.6, fontSize: '0.95rem' }}>
                A: First, ensure you have granted Veto <strong>Notification Access</strong> in your device settings. Second, you must select the specific messaging app (e.g., WhatsApp, Telegram) within Veto&apos;s settings. Lastly, ensure you are using the PIN format (<code>veto &lt;PIN&gt; command</code>) if required by your settings.
              </p>
            </div>

            <div className="glass-panel" style={{ borderRadius: '14px', padding: '2rem' }}>
              <h3 style={{ fontSize: '1.25rem', fontWeight: '700', color: 'var(--text-primary)', marginBottom: '0.75rem' }}>Q: Why are my commands being ignored when the app is in the background?</h3>
              <p style={{ color: 'var(--text-secondary)', lineHeight: 1.6, fontSize: '0.95rem' }}>
                A: Modern Android versions aggressively kill background tasks. You must <strong>disable battery optimizations</strong> for Veto in your system settings to ensure it can always receive and process your commands.
              </p>
            </div>

            <div className="glass-panel" style={{ borderRadius: '14px', padding: '2rem' }}>
              <h3 style={{ fontSize: '1.25rem', fontWeight: '700', color: 'var(--text-primary)', marginBottom: '0.75rem' }}>Q: Why did media upload (photo/audio) fail?</h3>
              <p style={{ color: 'var(--text-secondary)', lineHeight: 1.6, fontSize: '0.95rem' }}>
                A: Verify that you have linked your <strong>Google Drive</strong> account within the Veto app settings. The device also needs an active internet connection to upload the files.
              </p>
            </div>

          </div>
        </div>
      </motion.section>

      {/* Privacy & Cryptography Deep Dive */}
      <motion.section
        id="security"
        initial={{ opacity: 0, y: 30 }}
        whileInView={{ opacity: 1, y: 0 }}
        viewport={{ once: true, margin: "-100px" }}
        transition={{ duration: 0.8 }}
        style={{ padding: '5rem 1.5rem', backgroundColor: 'var(--bg-color)' }}
      >
        <div style={{ maxWidth: '900px', margin: '0 auto' }}>
          <div style={{ textAlign: 'center', marginBottom: '3.5rem' }}>
            <div style={{ color: '#a482d8', fontWeight: 'bold', fontSize: '0.9rem', letterSpacing: '0.1em', textTransform: 'uppercase', marginBottom: '0.5rem' }}>Security Specifications</div>
            <h2 style={{ fontSize: 'clamp(2rem, 4vw, 2.75rem)', fontWeight: '800', marginBottom: '1rem' }}>Zero-Trust Privacy &amp; Cryptography</h2>
            <p style={{ color: 'var(--text-secondary)', fontSize: '1.05rem', lineHeight: 1.6 }}>How Veto protects your encryption keys and guarantees data privacy.</p>
          </div>

          <div style={{ display: 'flex', flexDirection: 'column', gap: '1.5rem' }}>
            
            <div className="glass-panel" style={{ borderRadius: '14px', padding: '2rem' }}>
              <h3 style={{ fontSize: '1.25rem', fontWeight: '700', color: 'var(--text-primary)', marginBottom: '0.75rem' }}>🔑 Argon2id Password &amp; PIN Hashing</h3>
              <p style={{ color: 'var(--text-secondary)', lineHeight: 1.6, fontSize: '0.95rem' }}>
                Veto never stores plaintext PINs or passwords. It uses <strong>Argon2id</strong> (iterations = 1, memory = 128MB, parallelism = 4) with context separation to hash security PINs and prevent GPU dictionary attacks.
              </p>
            </div>

            <div className="glass-panel" style={{ borderRadius: '14px', padding: '2rem' }}>
              <h3 style={{ fontSize: '1.25rem', fontWeight: '700', color: 'var(--text-primary)', marginBottom: '0.75rem' }}>🔒 AES-256-GCM &amp; RSA-3072 Encryption</h3>
              <p style={{ color: 'var(--text-secondary)', lineHeight: 1.6, fontSize: '0.95rem' }}>
                All media payloads and telemetry data are symmetrically encrypted using <strong>AES-256-GCM</strong> (96-bit random IV, 128-bit auth tag) and asymmetric key wrapping with <strong>RSA-3072 OAEP</strong> (SHA-256 MGF1).
              </p>
            </div>

            <div className="glass-panel" style={{ borderRadius: '14px', padding: '2rem' }}>
              <h3 style={{ fontSize: '1.25rem', fontWeight: '700', color: 'var(--text-primary)', marginBottom: '0.75rem' }}>🚫 Zero Tracking &amp; Zero Ads</h3>
              <p style={{ color: 'var(--text-secondary)', lineHeight: 1.6, fontSize: '0.95rem' }}>
                Veto contains zero proprietary analytics SDKs, zero crash-reporting telemetry services, and zero advertisement networks. All log entries remain stored strictly on your local device storage.
              </p>
            </div>

          </div>
        </div>
      </motion.section>

      {/* Footer */}
      <footer style={{ padding: '4rem 2rem', borderTop: '1px solid rgba(255,255,255,0.1)', color: 'var(--text-secondary)', fontSize: '0.9rem', backgroundColor: 'var(--bg-color)' }}>
        <div style={{ maxWidth: '1200px', margin: '0 auto', display: 'flex', flexWrap: 'wrap', gap: '3rem', justifyContent: 'space-between' }}>

          <div style={{ flex: '1 1 300px' }}>
            <h4 style={{ color: 'var(--text-primary)', fontSize: '1.2rem', marginBottom: '1rem', fontWeight: '600' }}>Veto Security</h4>
            <p style={{ marginBottom: '1rem', lineHeight: 1.6 }}>Absolute control. Zero compromise. The ultimate Android security and remote management platform.</p>
            <div style={{ display: 'flex', flexDirection: 'column', gap: '0.5rem' }}>
              <a href="https://github.com/neubofy/Veto" target="_blank" rel="noreferrer" className="nav-link" style={{ color: '#58a6ff', textDecoration: 'none' }}>GitHub Repository</a>
              <a href="mailto:support@neubofy.in" className="nav-link" style={{ color: '#58a6ff', textDecoration: 'none' }}>Support: support@neubofy.in</a>
              <a href="https://github.com/pawanwashudev-official" target="_blank" rel="noreferrer" className="nav-link" style={{ color: '#58a6ff', textDecoration: 'none' }}>Developer Profile</a>
            </div>
          </div>

          <div style={{ flex: '1 1 300px' }}>
            <h4 style={{ color: 'var(--text-primary)', fontSize: '1.2rem', marginBottom: '1rem', fontWeight: '600' }}>Social &amp; Contact</h4>
            <div style={{ display: 'flex', flexDirection: 'column', gap: '0.5rem' }}>
              <a href="https://instagram.com/pawanwashudev" target="_blank" rel="noreferrer" className="nav-link" style={{ color: '#58a6ff', textDecoration: 'none' }}>Instagram @pawanwashudev</a>
              <a href="https://t.me/pawanwashudev" target="_blank" rel="noreferrer" className="nav-link" style={{ color: '#58a6ff', textDecoration: 'none' }}>Telegram @pawanwashudev</a>
              <a href="https://x.com/pawanwashudev" target="_blank" rel="noreferrer" className="nav-link" style={{ color: '#58a6ff', textDecoration: 'none' }}>X @pawanwashudev</a>
              <a href="https://wa.me/pawanwashudev" target="_blank" rel="noreferrer" className="nav-link" style={{ color: '#58a6ff', textDecoration: 'none' }}>WhatsApp @pawanwashudev</a>
            </div>
          </div>

          <div style={{ flex: '1 1 320px', display: 'flex', justifyContent: 'center' }}>
            <div className="glass-panel" style={{ width: '320px', borderRadius: '14px', padding: '16px' }}>
              {/* eslint-disable-next-line @next/next/no-img-element */}
              <img
                src="https://0.gravatar.com/avatar/f1b5ad3b6a5c5ff2617ea5477983f25a265036eee4f453f3c4806fb78c894494?s=256&d=initials"
                width="64"
                height="64"
                alt="Pawan Washudev"
                style={{ marginBottom: '8px', borderRadius: '50%' }}
              />
              <div style={{ color: 'var(--text-primary)', fontSize: '18px', fontWeight: '700' }}>
                Pawan Washudev
              </div>
              <div style={{ color: 'var(--text-secondary)', fontSize: '14px' }}>
                Founder, Neubofy
              </div>
              <div style={{ color: 'var(--text-secondary)', fontSize: '14px' }}>
                Patna, Bihar, India
              </div>
              <a
                href="https://gravatar.com/pawanwashudevofficial?utm_source=email_signature"
                target="_blank"
                rel="noreferrer"
                style={{ display: 'block', color: '#58a6ff', marginTop: '8px', fontSize: '14px', textDecoration: 'none' }}
              >
                gravatar.com/pawanwashudevofficial
              </a>
            </div>
          </div>

        </div>
        <div style={{ textAlign: 'center', marginTop: '4rem', paddingTop: '2rem', borderTop: '1px solid var(--border-light)' }}>
          <p>&copy; {new Date().getFullYear()} Veto Security. All rights reserved.</p>
          <p style={{ marginTop: '0.5rem', opacity: 0.6, fontSize: '0.8rem' }}>Proprietary software maintained by Neubofy</p>
        </div>
      </footer>
    </main>
  );
}
