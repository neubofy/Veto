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
        <h1 style={{ fontSize: '3rem', fontWeight: '800', color: '#fff', marginBottom: '2rem', letterSpacing: '-1px' }}>Privacy Policy</h1>
        
        <h2 style={{ fontSize: '1.5rem', color: '#fff', marginTop: '2rem', marginBottom: '1rem' }}>1. Zero Analytics & Tracking</h2>
        <p>Veto is built on a foundation of absolute privacy. We do not use any analytics, trackers, or advertising SDKs. Your activity within the app and on the dashboard is your own.</p>

        <h2 style={{ fontSize: '1.5rem', color: '#fff', marginTop: '2rem', marginBottom: '1rem' }}>2. Data Collection & Encryption</h2>
        <p>We do not passively track your location or device data. Data such as GPS coordinates or device battery stats are only fetched securely upon your explicit request. All data transmitted between your device and the dashboard is 100% encrypted in transit. We cannot see, read, or access your device data.</p>

        <h2 style={{ fontSize: '1.5rem', color: '#fff', marginTop: '2rem', marginBottom: '1rem' }}>3. Device Administration</h2>
        <p>Veto requires Device Administrator privileges to execute core security features like Remote Lock and Remote Wipe. This data is entirely controlled by you.</p>

        <h2 style={{ fontSize: '1.5rem', color: '#fff', marginTop: '2rem', marginBottom: '1rem' }}>4. Data Storage & Deletion</h2>
        <p>Your authentication tokens are stored securely using Firebase Authentication. Command execution results and photos are stored securely in Vercel Blob with strict private access rules. You have the absolute right to delete all your data and your account instantly from the dashboard settings.</p>
        
        <div style={{ marginTop: '4rem', paddingTop: '2rem', borderTop: '1px solid rgba(255,255,255,0.1)' }}>
          <Link href="/" className="btn" style={{ padding: '0.8rem 2rem' }}>&larr; Back to Home</Link>
        </div>
      </section>
    </main>
  );
}
