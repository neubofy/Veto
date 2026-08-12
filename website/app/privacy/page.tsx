import Link from 'next/link';

export default function PrivacyPage() {
  return (
    <main style={{ minHeight: '100vh', display: 'flex', flexDirection: 'column' }}>
      <nav style={{ padding: '1rem 2rem', display: 'flex', justifyContent: 'space-between', alignItems: 'center', borderBottom: '1px solid rgba(255,255,255,0.05)', background: 'rgba(10,10,12,0.8)', backdropFilter: 'blur(10px)', position: 'sticky', top: 0, flexWrap: 'wrap', gap: '1rem' }}>
        <Link href="/" style={{ fontSize: '1.5rem', fontWeight: '800', background: 'linear-gradient(90deg, #fff, #888)', WebkitBackgroundClip: 'text', WebkitTextFillColor: 'transparent', letterSpacing: '-0.5px' }}>
          VETO
        </Link>
        <div style={{ display: 'flex', gap: '1.5rem', alignItems: 'center', flexWrap: 'wrap' }}>
          <Link href="/terms" className="nav-link">Terms</Link>
          <Link href="/login" className="btn btn-primary" style={{ padding: '0.5rem 1.2rem', borderRadius: '30px' }}>Login</Link>
        </div>
      </nav>

      <section style={{ maxWidth: '800px', margin: '4rem auto', padding: '2rem', lineHeight: 1.8, color: 'var(--text-secondary)' }}>
        <h1 style={{ fontSize: '3rem', fontWeight: '800', color: 'var(--text-primary)', marginBottom: '0.5rem', letterSpacing: '-1px' }}>Privacy Policy</h1>
        <p style={{ fontSize: '0.9rem', color: 'var(--text-muted)', marginBottom: '2rem' }}>Effective Date: May 19, 2030</p>
        
        <h2 style={{ fontSize: '1.5rem', color: 'var(--text-primary)', marginTop: '2rem', marginBottom: '1rem' }}>1. Zero Analytics & Tracking (Android App)</h2>
        <p>Veto is built on a foundation of absolute privacy. The Android app does not use any analytics, trackers, or advertising SDKs. Your activity within the app is entirely your own. Note: The web dashboard uses privacy-friendly Vercel Analytics for page views.</p>

        <h2 style={{ fontSize: '1.5rem', color: 'var(--text-primary)', marginTop: '2rem', marginBottom: '1rem' }}>2. Data Collection</h2>
        <p>We do not passively track your location or device data. Data such as GPS coordinates or device battery stats are only fetched securely upon your explicit request. All data transmitted between your device and the dashboard is securely encrypted.</p>

        <h2 style={{ fontSize: '1.5rem', color: 'var(--text-primary)', marginTop: '2rem', marginBottom: '1rem' }}>3. Device Administration</h2>
        <p>Veto requires Device Administrator privileges to execute core security features like Remote Lock. This data is entirely controlled by you.</p>

        <h2 style={{ fontSize: '1.5rem', color: 'var(--text-primary)', marginTop: '2rem', marginBottom: '1rem' }}>4. Data Storage & Deletion</h2>
        <p>Your authentication tokens are stored securely using Firebase Authentication. Command execution results are stored in Firebase Firestore, but they are end-to-end encrypted so even if our database gets hacked, you will not lose anything. Your photos and videos are stored directly in your Google Drive with restricted access, meaning only having the URL is not enough to view the content; it requires your Google account access. You have the absolute right to delete all your data and your account instantly from the dashboard settings.</p>
        
        <div style={{ marginTop: '4rem', paddingTop: '2rem', borderTop: '1px solid rgba(255,255,255,0.1)' }}>
          <Link href="/" className="btn" style={{ padding: '0.8rem 2rem' }}>&larr; Back to Home</Link>
        </div>
      </section>
    </main>
  );
}
