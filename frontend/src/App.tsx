// Main React component for Kendrick Labernetes UI
// Handles quote submission, displays latest quote, node info, and error handling
import React, { useState, useEffect } from 'react';
import axios from 'axios';

// Import Inter font from Google Fonts
const interFontUrl = 'https://fonts.googleapis.com/css2?family=Inter:wght@400;700&display=swap';
const injectFont = () => {
  if (!document.getElementById('inter-font')) {
    const link = document.createElement('link');
    link.id = 'inter-font';
    link.rel = 'stylesheet';
    link.href = interFontUrl;
    document.head.appendChild(link);
  }
};

injectFont();


const App: React.FC = () => {
  const [quote, setQuote] = useState('');
  const [submittedQuote, setSubmittedQuote] = useState<any>(null);
  const [latestQuote, setLatestQuote] = useState<any>(null);
  const [nodeInfo, setNodeInfo] = useState<any>(null);
  const [dbStatus, setDbStatus] = useState<{connected: string, type: string, message: string} | null>(null);
  const [error, setError] = useState<string | null>(null);
  const [loading, setLoading] = useState(false);
  const [allQuotes, setAllQuotes] = useState<any[]>([]);

  const handleSubmit = async (e: React.FormEvent<HTMLFormElement>) => {
    e.preventDefault();
    setError(null);
    setLoading(true);
    try {
      const res = await axios.post('/api/quotes', { quote });
      if ((res.data as any).error) {
        setError((res.data as any).error);
        setSubmittedQuote(null);
      } else {
        setSubmittedQuote(res.data);
        setError(null);
        setAllQuotes(quotes => [res.data, ...quotes]);
      }
    } catch (err) {
      setError('Could not connect to backend or MongoDB.');
      setSubmittedQuote(null);
    }
    setLoading(false);
  };

  useEffect(() => {
    // Initial load for all data
    axios.get('/api/nodeinfo')
      .then((res: { data: any }) => setNodeInfo(res.data))
      .catch(() => setNodeInfo(null));
    axios.get('/api/quotes/latest')
      .then((res: { data: any }) => {
        if (res.data && !(res.data as any).error) {
          setLatestQuote(res.data);
        } else {
          setLatestQuote(null);
        }
      })
      .catch(() => setLatestQuote(null));
    axios.get('/api/dbstatus')
      .then((res: { data: any }) => setDbStatus(res.data))
      .catch(() => setDbStatus(null));
    axios.get('/api/quotes')
      .then((res: { data: any[] }) => setAllQuotes(res.data))
      .catch(() => setAllQuotes([]));

    // Poll nodeinfo and dbstatus every 5 seconds (no blink)
    const interval = setInterval(() => {
      axios.get('/api/nodeinfo')
        .then((res: { data: any }) => setNodeInfo(res.data))
        .catch(() => {});
      axios.get('/api/dbstatus')
        .then((res: { data: any }) => setDbStatus(res.data))
        .catch(() => {});
    }, 3000);
    return () => clearInterval(interval);
  }, []);

  const handleDeleteQuote = async (id: number) => {
    try {
      await axios.delete(`/api/quotes/${id}`);
      setAllQuotes(quotes => quotes.filter(q => q.id !== id));
      // If the deleted quote was the latest, clear latestQuote
      if (latestQuote && latestQuote.id === id) {
        setLatestQuote(null);
      }
      // If the deleted quote was the submittedQuote, clear submittedQuote
      if (submittedQuote && submittedQuote.id === id) {
        setSubmittedQuote(null);
      }
    } catch (err) {
      setError('Failed to delete quote.');
    }
  };

  return (
    <div
      style={{
        minHeight: '100vh',
        padding: 32,
        background: 'rgb(2, 84, 236)',
        fontFamily: 'Inter, sans-serif',
        color: '#fff',
        boxSizing: 'border-box',
      }}
    >
      <h1 style={{ fontWeight: 700, fontSize: 40, letterSpacing: 2, marginBottom: 16 }}>
        Kendrick Labernetes
      </h1>
      <form
        onSubmit={handleSubmit}
        style={{ display: 'flex', gap: 12, marginBottom: 24 }}
      >
        <input
          type="text"
          value={quote}
          onChange={(e: React.ChangeEvent<HTMLInputElement>) => setQuote(e.target.value)}
          placeholder="Enter your favorite Wiz Khalifa quote"
          style={{
            width: 400,
            padding: '12px 16px',
            borderRadius: 8,
            border: 'none',
            fontSize: 18,
            fontFamily: 'Inter, sans-serif',
            color: '#222',
            background: '#fff',
            outline: 'none',
          }}
        />
        <button
          type="submit"
          disabled={loading || !quote.trim()}
          style={{
            padding: '12px 24px',
            borderRadius: 8,
            border: 'none',
            background: loading ? '#e0e0e0' : '#fff',
            color: loading ? '#888' : 'rgb(2, 84, 236)',
            fontWeight: 700,
            fontSize: 18,
            cursor: loading ? 'not-allowed' : 'pointer',
            fontFamily: 'Inter, sans-serif',
            boxShadow: '0 2px 8px rgba(0,0,0,0.08)',
            transition: 'background 0.2s, color 0.2s',
          }}
          onMouseEnter={e => (e.currentTarget.style.background = '#f0f8ff')}
          onMouseLeave={e => (e.currentTarget.style.background = loading ? '#e0e0e0' : '#fff')}
        >
          {loading ? 'Submitting...' : 'Submit'}
        </button>
      </form>
      {error && (
        <div style={{ color: '#ffbaba', background: '#d8000c', padding: 12, borderRadius: 8, marginTop: 8, fontWeight: 500 }}>
          {error === 'Could not connect to backend or MongoDB.'
            ? 'Database connection unavailable. Please check your DB settings.'
            : error}
        </div>
      )}
      {submittedQuote && (
        <div style={{ marginTop: 24, background: 'rgba(255,255,255,0.08)', padding: 20, borderRadius: 12 }}>
          <h2 style={{ fontWeight: 700, fontSize: 28, marginBottom: 8 }}>Latest Quote</h2>
          <p style={{ fontSize: 22, fontWeight: 500 }}>{submittedQuote.quote}</p>
          <small>Timestamp: {submittedQuote.timestamp}</small><br/>
          <small>IP: {submittedQuote.ip}</small><br/>
          <small>Quote #: {submittedQuote.quoteNumber}</small>
        </div>
      )}
      {!submittedQuote && !latestQuote && !error && (
        <div style={{ marginTop: 24, color: '#fff', opacity: 0.7, fontSize: 20, fontWeight: 500 }}>
          Drop your first quote!
        </div>
      )}
      {latestQuote && !submittedQuote && (
        <div style={{ marginTop: 24, background: 'rgba(255,255,255,0.08)', padding: 20, borderRadius: 12 }}>
          <h2 style={{ fontWeight: 700, fontSize: 28, marginBottom: 8 }}>Latest Quote</h2>
          <p style={{ fontSize: 22, fontWeight: 500 }}>{latestQuote.quote}</p>
          <small>Timestamp: {latestQuote.timestamp}</small><br/>
          <small>IP: {latestQuote.ip}</small><br/>
          <small>Quote #: {latestQuote.quoteNumber}</small>
        </div>
      )}
      <div style={{ margin: '32px 0 16px 0' }}>
        <h4 style={{ fontWeight: 700, fontSize: 20, marginBottom: 10 }}>All Quotes</h4>
        <div style={{ display: 'flex', flexWrap: 'wrap', gap: 12 }}>
          {allQuotes.length === 0 ? (
            <span style={{ color: '#fff', opacity: 0.7 }}>No quotes found.</span>
          ) : (
            allQuotes.map(q => (
              <div key={q.id} style={{ background: 'rgba(255,255,255,0.08)', padding: 16, borderRadius: 10, minWidth: 220, position: 'relative' }}>
                <span style={{ position: 'absolute', top: 8, right: 12, cursor: 'pointer', color: '#ffbaba', fontWeight: 700, fontSize: 18 }} onClick={() => handleDeleteQuote(q.id)}>&times;</span>
                <div style={{ fontSize: 18, fontWeight: 500, marginBottom: 6 }}>{q.quote}</div>
                <div style={{ fontSize: 13, opacity: 0.8 }}>#{q.quoteNumber} | {q.timestamp}</div>
                <div style={{ fontSize: 12, opacity: 0.7 }}>IP: {q.ip}</div>
              </div>
            ))
          )}
        </div>
      </div>
      <footer style={{ marginTop: 48, borderTop: '1px solid #fff', paddingTop: 16, opacity: 0.95 }}>
        <h3 style={{ fontWeight: 700, fontSize: 22, marginBottom: 12 }}>Node/Application Info</h3>
        {dbStatus && (
          <div style={{ marginBottom: 12, color: dbStatus.connected === 'true' ? '#baffba' : '#ffbaba', fontWeight: 600 }}>
            <span>DB Status: </span>
            <span>{dbStatus.connected === 'true' ? 'Connected' : 'Not Connected'} ({dbStatus.type})</span>
            <span style={{ marginLeft: 8, fontWeight: 400, fontSize: 14, color: '#fff' }}>{dbStatus.message}</span>
            <div style={{ marginTop: 8 }}><strong>Connected DB:</strong> {dbStatus.type}</div>
          </div>
        )}
        <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fit, minmax(220px, 1fr))', gap: 8 }}>
          {nodeInfo ? (
            <>
              <div><strong>Hostname:</strong> {nodeInfo.hostname}</div>
              <div><strong>App:</strong> {nodeInfo.app}</div>
              <div><strong>OS:</strong> {nodeInfo['os.name']} {nodeInfo['os.version']} ({nodeInfo['os.arch']})</div>
              <div><strong>Available Processors:</strong> {nodeInfo.availableProcessors}</div>
              <div><strong>Max Memory:</strong> {nodeInfo.maxMemoryMB} MB</div>
              <div><strong>Total Memory:</strong> {nodeInfo.totalMemoryMB} MB</div>
              <div><strong>Free Memory:</strong> {nodeInfo.freeMemoryMB} MB</div>
              <div><strong>Timestamp:</strong> {nodeInfo.timestamp}</div>
            </>
          ) : (
            <div style={{ color: '#fff', opacity: 0.7 }}>
              Unable to load node info. Backend or database may be unavailable.<br />
              <span style={{ fontSize: 14, color: '#baffba' }}>
                Retrying every 3 seconds...
              </span>
            </div>
          )}
        </div>
        <div style={{ marginTop: 32, textAlign: 'center' }}>
          <a href="/api-docs.html" target="_blank" rel="noopener noreferrer" style={{ color: '#baffba', fontWeight: 700, fontSize: 18, textDecoration: 'underline' }}>
            API Docs
          </a>
        </div>
      </footer>
    </div>
  );
};

export default App;
