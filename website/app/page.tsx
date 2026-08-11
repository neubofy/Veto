'use client';
import { useState, useEffect } from 'react';
import Link from 'next/link';
import { auth } from '@/lib/firebaseClient';
import { onAuthStateChanged, signOut, User } from 'firebase/auth';
import { motion } from 'framer-motion';
import { ShieldCheck, Crosshair, Lock, BellRing, Smartphone, MapPin, Search, Server, HardDrive, Share2, Shield, Settings } from 'lucide-react';


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
          <img src="/icon.png" width="32" height="32" alt="Veto Logo" style={{ borderRadius: '8px' }} />
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
            Veto provides a completely free, highly reliable toolkit to manage and secure your Android device. Designed to work <i>with</i> Google&apos;s Find My Device, it fills in the missing gaps without the battery drain—remaining entirely dormant until a command is received. With multi-transport fallbacks like offline SMS and mesh networks, you stay connected and protected even without internet. Made simple for the average user, yet endlessly capable for power users.
          </p>

          <p style={{ fontSize: '0.95rem', color: 'var(--text-secondary)', maxWidth: '750px', margin: '0 auto 3rem auto', lineHeight: 1.5, opacity: 0.8, backgroundColor: 'rgba(255, 255, 255, 0.05)', padding: '1rem', borderRadius: '8px' }}>
            <strong>Note on Device Compatibility & Security:</strong> This app is designed for modern Android devices. Newer devices natively protect the quick settings panel and power-off menu on the lock screen by requiring the device to be unlocked. Veto leverages this by not using a separate custom password to prove ownership; instead, it relies on your device&apos;s existing lock screen security. <strong>Make sure you have a strong, secure PIN or password set on your device.</strong>
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
            <a href="/api/latest-apk" target="_blank" rel="noreferrer" className="btn" style={{ padding: '1rem 2.2rem', fontSize: '1.05rem', borderRadius: '40px', backgroundColor: 'var(--glass-bg)', color: 'var(--text-primary)', border: '1px solid var(--glass-border)', textDecoration: 'none', fontWeight: '600' }}>
              Download Android App <span style={{display: "block", fontSize: "0.8rem", opacity: 0.7, fontWeight: "normal"}}>Smartly chooses latest APK</span>
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
              <div style={{ marginBottom: '1.25rem', color: '#58a6ff' }}><ShieldCheck size={48} /></div>
              <h3 style={{ fontSize: '1.35rem', fontWeight: '700', marginBottom: '0.75rem', color: 'var(--text-primary)' }}>Total Control & Sovereignty</h3>
              <p style={{ color: 'var(--text-secondary)', lineHeight: 1.6, fontSize: '0.95rem' }}>
                You have full control to disable any command or transport method from your app. Use the app without cloud sync via SMS and messaging apps. Media files are stored securely in your Google Drive. Veto handles restricted access links that require your account login to view, ensuring total privacy.
              </p>
            </div>

            <div className="glass-panel" style={{ borderRadius: '16px', padding: '2rem' }}>
              <div style={{ marginBottom: '1.25rem', color: '#58a6ff' }}><MapPin size={48} /></div>
              <h3 style={{ fontSize: '1.35rem', fontWeight: '700', marginBottom: '0.75rem', color: 'var(--text-primary)' }}>Advanced Location Tracking</h3>
              <p style={{ color: 'var(--text-secondary)', lineHeight: 1.6, fontSize: '0.95rem' }}>
                Fetch your current location anytime. Upcoming Feature: If your device moves beyond a 100-meter radius, its location will automatically update on the dashboard within 15 minutes.
              </p>
            </div>

            <div className="glass-panel" style={{ borderRadius: '16px', padding: '2rem' }}>
              <div style={{ marginBottom: '1.25rem', color: '#58a6ff' }}><Lock size={48} /></div>
              <h3 style={{ fontSize: '1.35rem', fontWeight: '700', marginBottom: '0.75rem', color: 'var(--text-primary)' }}>Advanced Theft Mode</h3>
              <p style={{ color: 'var(--text-secondary)', lineHeight: 1.6, fontSize: '0.95rem' }}>
                Uniquely designed for maximum deterrence. The `theft` command instantly bypasses Do Not Disturb to blare a max-volume siren, activates a mesh-network Radar UI to intimidate the thief, toggles the flashlight, and begins silently capturing background photos and audio to secure your data.
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
                Lost your device? This strictly fetches high-accuracy GPS coordinates and generates a quick Google Maps link. <strong>It forces the GPS on even if disabled!</strong><br/><br/>Usage: <code style={{ color: 'var(--text-primary)' }}>veto locate</code><br/>Requires: <code style={{ color: 'var(--text-primary)' }}>LocationPermission</code>
              </p>
            </div>

            <div className="glass-panel" style={{ borderRadius: '12px', padding: '1.5rem' }}>
              <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '0.5rem' }}>
                <span style={{ fontSize: '1.1rem', fontWeight: 'bold', color: '#eba336' }}>🚨 veto theft</span>
                <span style={{ fontSize: '0.75rem', backgroundColor: 'rgba(235, 163, 54, 0.15)', color: '#eba336', padding: '2px 8px', borderRadius: '4px' }}>Macro</span>
              </div>
              <p style={{ color: 'var(--text-secondary)', fontSize: '0.88rem', lineHeight: 1.5, margin: 0 }}>
                The ultimate panic button. Enables Bluetooth (enhancing FMD network visibility), fetches GPS, turns off DND, loops a deafening alarm, and locks the screen, all at once. Pass &quot;end&quot; to cancel.<br/><br/>Usage: <code style={{ color: 'var(--text-primary)' }}>veto theft [end]</code><br/>Requires: <code style={{ color: 'var(--text-primary)' }}>LocationPermission</code>
              </p>
            </div>

            <div className="glass-panel" style={{ borderRadius: '12px', padding: '1.5rem' }}>
              <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '0.5rem' }}>
                <span style={{ fontSize: '1.1rem', fontWeight: 'bold', color: '#58a6ff' }}>🔊 veto ring</span>
                <span style={{ fontSize: '0.75rem', backgroundColor: 'rgba(47, 129, 247, 0.15)', color: '#58a6ff', padding: '2px 8px', borderRadius: '4px' }}>Alarm</span>
              </div>
              <p style={{ color: 'var(--text-secondary)', fontSize: '0.88rem', lineHeight: 1.5, margin: 0 }}>
                Dropped it under the couch? Force your device to ring at maximum volume, totally bypassing Do Not Disturb and silent mode. You can specify duration in seconds (max 30 minutes).<br/><br/>Usage: <code style={{ color: 'var(--text-primary)' }}>veto ring [seconds]</code><br/>Requires: <code style={{ color: 'var(--text-primary)' }}>DoNotDisturbAccessPermission</code>
              </p>
            </div>

            <div className="glass-panel" style={{ borderRadius: '12px', padding: '1.5rem' }}>
              <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '0.5rem' }}>
                <span style={{ fontSize: '1.1rem', fontWeight: 'bold', color: '#58a6ff' }}>🔒 veto lock</span>
                <span style={{ fontSize: '0.75rem', backgroundColor: 'rgba(47, 129, 247, 0.15)', color: '#58a6ff', padding: '2px 8px', borderRadius: '4px' }}>Security</span>
              </div>
              <p style={{ color: 'var(--text-secondary)', fontSize: '0.88rem', lineHeight: 1.5, margin: 0 }}>
                Instantly secure your device. It completely disables biometric unlocks, requiring a hard password to get back in. You can also display an optional message on the lock screen.<br/><br/>Usage: <code style={{ color: 'var(--text-primary)' }}>veto lock [msg]</code><br/>Requires: <code style={{ color: 'var(--text-primary)' }}>DeviceAdminPermission</code>
              </p>
            </div>

            <div className="glass-panel" style={{ borderRadius: '12px', padding: '1.5rem' }}>
              <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '0.5rem' }}>
                <span style={{ fontSize: '1.1rem', fontWeight: 'bold', color: '#58a6ff' }}>📸 veto photo</span>
                <span style={{ fontSize: '0.75rem', backgroundColor: 'rgba(47, 129, 247, 0.15)', color: '#58a6ff', padding: '2px 8px', borderRadius: '4px' }}>Media</span>
              </div>
              <p style={{ color: 'var(--text-secondary)', fontSize: '0.88rem', lineHeight: 1.5, margin: 0 }}>
                Secretly snaps a photo from the front or rear camera, right over the lock screen, and beams it straight to your linked Google Drive. Add &quot;flash&quot; if it&apos;s dark.<br/><br/>Usage: <code style={{ color: 'var(--text-primary)' }}>veto photo [front | back] [flash]</code><br/>Requires: <code style={{ color: 'var(--text-primary)' }}>CameraPermission</code>
              </p>
            </div>

            <div className="glass-panel" style={{ borderRadius: '12px', padding: '1.5rem' }}>
              <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '0.5rem' }}>
                <span style={{ fontSize: '1.1rem', fontWeight: 'bold', color: '#58a6ff' }}>🎙️ veto audio</span>
                <span style={{ fontSize: '0.75rem', backgroundColor: 'rgba(47, 129, 247, 0.15)', color: '#58a6ff', padding: '2px 8px', borderRadius: '4px' }}>Media</span>
              </div>
              <p style={{ color: 'var(--text-secondary)', fontSize: '0.88rem', lineHeight: 1.5, margin: 0 }}>
                Want to know what&apos;s going on around your lost phone? Quietly records high-quality AAC audio for the specified duration and uploads it to Google Drive.<br/><br/>Usage: <code style={{ color: 'var(--text-primary)' }}>veto audio [duration_secs]</code><br/>Requires: <code style={{ color: 'var(--text-primary)' }}>RecordAudioPermission</code>
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
                Similar to photo, but captures a quick video clip from either camera in the background. It&apos;s backed up to Google Drive automatically.<br/><br/>Usage: <code style={{ color: 'var(--text-primary)' }}>veto video [front | back] [flash]</code><br/>Requires: <code style={{ color: 'var(--text-primary)' }}>CameraPermission</code>, <code style={{ color: 'var(--text-primary)' }}>RecordAudioPermission</code>
              </p>
            </div>

            <div className="glass-panel" style={{ borderRadius: '12px', padding: '1.5rem' }}>
              <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '0.5rem' }}>
                <span style={{ fontSize: '1.1rem', fontWeight: 'bold', color: '#58a6ff' }}>💡 veto flash</span>
                <span style={{ fontSize: '0.75rem', backgroundColor: 'rgba(47, 129, 247, 0.15)', color: '#58a6ff', padding: '2px 8px', borderRadius: '4px' }}>Hardware</span>
              </div>
              <p style={{ color: 'var(--text-secondary)', fontSize: '0.88rem', lineHeight: 1.5, margin: 0 }}>
                Need a visual beacon? Toggles your device&apos;s flashlight on or off so you can spot it in the dark.<br/><br/>Usage: <code style={{ color: 'var(--text-primary)' }}>veto flash [on | off]</code><br/>Requires: <code style={{ color: 'var(--text-primary)' }}>None</code>
              </p>
            </div>

            <div className="glass-panel" style={{ borderRadius: '12px', padding: '1.5rem' }}>
              <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '0.5rem' }}>
                <span style={{ fontSize: '1.1rem', fontWeight: 'bold', color: '#58a6ff' }}>📊 veto stats</span>
                <span style={{ fontSize: '0.75rem', backgroundColor: 'rgba(47, 129, 247, 0.15)', color: '#58a6ff', padding: '2px 8px', borderRadius: '4px' }}>Telemetry</span>
              </div>
              <p style={{ color: 'var(--text-secondary)', fontSize: '0.88rem', lineHeight: 1.5, margin: 0 }}>
                Get the full scoop. Pulls vital details like battery percentage, SIM carrier info, IP addresses, and surrounding Wi-Fi networks in a flash.<br/><br/>Usage: <code style={{ color: 'var(--text-primary)' }}>veto stats</code><br/>Requires: <code style={{ color: 'var(--text-primary)' }}>LocationPermission</code>
              </p>
            </div>

            <div className="glass-panel" style={{ borderRadius: '12px', padding: '1.5rem' }}>
              <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '0.5rem' }}>
                <span style={{ fontSize: '1.1rem', fontWeight: 'bold', color: '#58a6ff' }}>📡 veto bluetooth</span>
                <span style={{ fontSize: '0.75rem', backgroundColor: 'rgba(47, 129, 247, 0.15)', color: '#58a6ff', padding: '2px 8px', borderRadius: '4px' }}>Hardware</span>
              </div>
              <p style={{ color: 'var(--text-secondary)', fontSize: '0.88rem', lineHeight: 1.5, margin: 0 }}>
                Remotely toggle Bluetooth connectivity. Perfect for getting your device onto Google&apos;s Find My Device network if it&apos;s disconnected.<br/><br/>Usage: <code style={{ color: 'var(--text-primary)' }}>veto bluetooth [on | off]</code><br/>Requires: <code style={{ color: 'var(--text-primary)' }}>BluetoothConnectPermission</code>
              </p>
            </div>

            <div className="glass-panel" style={{ borderRadius: '12px', padding: '1.5rem' }}>
              <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '0.5rem' }}>
                <span style={{ fontSize: '1.1rem', fontWeight: 'bold', color: '#58a6ff' }}>🔇 veto nodisturb</span>
                <span style={{ fontSize: '0.75rem', backgroundColor: 'rgba(47, 129, 247, 0.15)', color: '#58a6ff', padding: '2px 8px', borderRadius: '4px' }}>System</span>
              </div>
              <p style={{ color: 'var(--text-secondary)', fontSize: '0.88rem', lineHeight: 1.5, margin: 0 }}>
                Flip Do Not Disturb mode on or off. Useful if you need your phone to start accepting calls again.<br/><br/>Usage: <code style={{ color: 'var(--text-primary)' }}>veto nodisturb [on | off]</code><br/>Requires: <code style={{ color: 'var(--text-primary)' }}>DoNotDisturbAccessPermission</code>
              </p>
            </div>

            <div className="glass-panel" style={{ borderRadius: '12px', padding: '1.5rem' }}>
              <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '0.5rem' }}>
                <span style={{ fontSize: '1.1rem', fontWeight: 'bold', color: '#58a6ff' }}>📳 veto ringermode</span>
                <span style={{ fontSize: '0.75rem', backgroundColor: 'rgba(47, 129, 247, 0.15)', color: '#58a6ff', padding: '2px 8px', borderRadius: '4px' }}>System</span>
              </div>
              <p style={{ color: 'var(--text-secondary)', fontSize: '0.88rem', lineHeight: 1.5, margin: 0 }}>
                Switch between normal, vibrate, and silent modes. Note: Android bundles silent mode with Do Not Disturb.<br/><br/>Usage: <code style={{ color: 'var(--text-primary)' }}>veto ringermode [normal | vibrate | silent]</code><br/>Requires: <code style={{ color: 'var(--text-primary)' }}>DoNotDisturbAccessPermission</code>
              </p>
            </div>

            <div className="glass-panel" style={{ borderRadius: '12px', padding: '1.5rem' }}>
              <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '0.5rem' }}>
                <span style={{ fontSize: '1.1rem', fontWeight: 'bold', color: '#58a6ff' }}>🛰️ veto gps</span>
                <span style={{ fontSize: '0.75rem', backgroundColor: 'rgba(47, 129, 247, 0.15)', color: '#58a6ff', padding: '2px 8px', borderRadius: '4px' }}>Hardware</span>
              </div>
              <p style={{ color: 'var(--text-secondary)', fontSize: '0.88rem', lineHeight: 1.5, margin: 0 }}>
                Forcefully flip Location Services (GPS) on or off using deep Android secure settings.<br/><br/>Usage: <code style={{ color: 'var(--text-primary)' }}>veto gps [on | off]</code><br/>Requires: <code style={{ color: 'var(--text-primary)' }}>WriteSecureSettingsPermission</code>
              </p>
            </div>

            <div className="glass-panel" style={{ borderRadius: '12px', padding: '1.5rem' }}>
              <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '0.5rem' }}>
                <span style={{ fontSize: '1.1rem', fontWeight: 'bold', color: '#58a6ff' }}>❓ veto help</span>
                <span style={{ fontSize: '0.75rem', backgroundColor: 'rgba(47, 129, 247, 0.15)', color: '#58a6ff', padding: '2px 8px', borderRadius: '4px' }}>Utility</span>
              </div>
              <p style={{ color: 'var(--text-secondary)', fontSize: '0.88rem', lineHeight: 1.5, margin: 0 }}>
                Need a quick refresher? Prints out the list of commands available to your device.<br/><br/>Usage: <code style={{ color: 'var(--text-primary)' }}>veto help</code><br/>Requires: <code style={{ color: 'var(--text-primary)' }}>None</code>
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

            <div id="faq-fmd" className="glass-panel" style={{ borderRadius: '14px', padding: '2rem' }}>
              <h3 style={{ fontSize: '1.25rem', fontWeight: '700', color: 'var(--text-primary)', marginBottom: '0.75rem' }}>Q: Why isn&apos;t this just an alternative to Google Find My Device?</h3>
              <p style={{ color: 'var(--text-secondary)', lineHeight: 1.6, fontSize: '0.95rem' }}>
                A: Veto isn&apos;t here to replace Google Find My Device; it&apos;s designed to complement it. For example, our `theft` command intentionally turns on Bluetooth to make your device highly visible to the billions of devices on the FMD network, even if it&apos;s completely offline. We fill in the gaps with hardcore features like siren alarms, biometric lockouts, and offline SMS control.
              </p>
            </div>

            <div id="faq-nosim" className="glass-panel" style={{ borderRadius: '14px', padding: '2rem' }}>
              <h3 style={{ fontSize: '1.25rem', fontWeight: '700', color: 'var(--text-primary)', marginBottom: '0.75rem' }}>Q: What if my phone doesn&apos;t have a SIM card?</h3>
              <p style={{ color: 'var(--text-secondary)', lineHeight: 1.6, fontSize: '0.95rem' }}>
                A: No problem! While SMS is a great fallback, Veto fully supports commanding your device through our Web Dashboard or via Notification Auto-Replies (like WhatsApp or Telegram) over standard Wi-Fi.
              </p>
            </div>

            <div id="faq-tracking" className="glass-panel" style={{ borderRadius: '14px', padding: '2rem' }}>
              <h3 style={{ fontSize: '1.25rem', fontWeight: '700', color: 'var(--text-primary)', marginBottom: '0.75rem' }}>Q: Can I track my phone&apos;s location constantly?</h3>
              <p style={{ color: 'var(--text-secondary)', lineHeight: 1.6, fontSize: '0.95rem' }}>
                A: No. Veto uses a completely dormant architecture to save your battery. It only wakes up when you explicitly send a command (like `locate`). We do not continuously poll your location in the background.
              </p>
            </div>

            <div id="faq-sms" className="glass-panel" style={{ borderRadius: '14px', padding: '2rem' }}>
              <h3 style={{ fontSize: '1.25rem', fontWeight: '700', color: 'var(--text-primary)', marginBottom: '0.75rem' }}>Q: Why didn&apos;t my SMS command work?</h3>
              <p style={{ color: 'var(--text-secondary)', lineHeight: 1.6, fontSize: '0.95rem' }}>
                A: If you are sending an SMS from a number that is not on your <strong>Allowlist</strong>, you must include your PIN in the command: <code>veto &lt;PIN&gt; locate</code>. Sending a correct PIN will temporarily whitelist that number for future commands. Also, verify that Veto has SMS permissions enabled.
              </p>
            </div>

            <div id="faq-notification" className="glass-panel" style={{ borderRadius: '14px', padding: '2rem' }}>
              <h3 style={{ fontSize: '1.25rem', fontWeight: '700', color: 'var(--text-primary)', marginBottom: '0.75rem' }}>Q: Why is Notification Reply not working?</h3>
              <p style={{ color: 'var(--text-secondary)', lineHeight: 1.6, fontSize: '0.95rem' }}>
                A: First, ensure you have granted Veto <strong>Notification Access</strong> in your device settings. Second, you must select the specific messaging app (e.g., WhatsApp, Telegram) within Veto&apos;s settings. Lastly, ensure you are using the PIN format (<code>veto &lt;PIN&gt; command</code>) if required by your settings.
              </p>
            </div>

            <div id="faq-background" className="glass-panel" style={{ borderRadius: '14px', padding: '2rem' }}>
              <h3 style={{ fontSize: '1.25rem', fontWeight: '700', color: 'var(--text-primary)', marginBottom: '0.75rem' }}>Q: Why are my commands being ignored when the app is in the background?</h3>
              <p style={{ color: 'var(--text-secondary)', lineHeight: 1.6, fontSize: '0.95rem' }}>
                A: Modern Android versions aggressively kill background tasks. You must <strong>disable battery optimizations</strong> for Veto in your system settings to ensure it can always receive and process your commands.
              </p>
            </div>


            <div id="faq-write-secure-settings" className="glass-panel" style={{ borderRadius: '14px', padding: '2rem' }}>
              <h3 style={{ fontSize: '1.25rem', fontWeight: '700', color: 'var(--text-primary)', marginBottom: '0.75rem' }}>Q: How do I grant Write Secure Settings permission?</h3>
              <p style={{ color: 'var(--text-secondary)', lineHeight: 1.6, fontSize: '0.95rem' }}>
                A: The <code>WriteSecureSettingsPermission</code> is required for powerful hardware toggles like GPS and Bluetooth. Because Android restricts this for normal apps, you must grant it manually using a computer via ADB (Android Debug Bridge) with the command: <br/><code style={{ display: 'block', marginTop: '8px', padding: '8px', backgroundColor: 'rgba(0,0,0,0.2)', borderRadius: '4px' }}>adb shell pm grant com.neubofy.veto android.permission.WRITE_SECURE_SETTINGS</code>
              </p>
            </div>

            <div id="faq-media" className="glass-panel" style={{ borderRadius: '14px', padding: '2rem' }}>
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
            <h2 style={{ fontSize: 'clamp(2rem, 4vw, 2.75rem)', fontWeight: '800', marginBottom: '1rem' }}>Zero-Trust Privacy</h2>
            <p style={{ color: 'var(--text-secondary)', fontSize: '1.05rem', lineHeight: 1.6 }}>How Veto guarantees data privacy.</p>
          </div>

          <div style={{ display: 'flex', flexDirection: 'column', gap: '1.5rem' }}>
            
            <div className="glass-panel" style={{ borderRadius: '14px', padding: '2rem' }}>
              <h3 style={{ fontSize: '1.25rem', fontWeight: '700', color: 'var(--text-primary)', marginBottom: '0.75rem' }}>🔑 Argon2id Password &amp; PIN Hashing</h3>
              <p style={{ color: 'var(--text-secondary)', lineHeight: 1.6, fontSize: '0.95rem' }}>
                Veto never stores plaintext PINs or passwords. It uses <strong>Argon2id</strong> (iterations = 1, memory = 128MB, parallelism = 4) with context separation to hash security PINs and prevent GPU dictionary attacks.
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
          <p style={{ marginTop: '0.5rem', opacity: 0.6, fontSize: '0.8rem' }}>Open source software under GPLv3 maintained by Neubofy</p>
        </div>
      </footer>

      <script
        type="application/ld+json"
        dangerouslySetInnerHTML={{
          __html: JSON.stringify({
            "@context": "https://schema.org",
            "@type": "FAQPage",
            "mainEntity": [
              {
                "@type": "Question",
                "name": "How do I grant Write Secure Settings permission?",
                "acceptedAnswer": {
                  "@type": "Answer",
                  "text": "The WriteSecureSettingsPermission is required for powerful hardware toggles like GPS and Bluetooth. Because Android restricts this for normal apps, you must grant it manually using a computer via ADB with the command: adb shell pm grant com.neubofy.veto android.permission.WRITE_SECURE_SETTINGS"
                }
              },
              {
                "@type": "Question",
                "name": "Why didn't my SMS command work?",
                "acceptedAnswer": {
                  "@type": "Answer",
                  "text": "If you are sending an SMS from a number that is not on your Allowlist, you must include your PIN in the command: veto <PIN> locate. Sending a correct PIN will temporarily whitelist that number for future commands. Also, verify that Veto has SMS permissions enabled."
                }
              },
              {
                "@type": "Question",
                "name": "Why is Notification Reply not working?",
                "acceptedAnswer": {
                  "@type": "Answer",
                  "text": "Ensure you have granted Veto Notification Access in your device settings. You must select the specific messaging app within Veto's settings. Also ensure you are using the PIN format if required."
                }
              }
            ]
          })
        }}
      />
    </main>
  );
}
