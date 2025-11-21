import React, { useEffect, useState } from 'react';
import axios from 'axios';

const Admin: React.FC = () => {
  const [dbType, setDbType] = useState<string>('h2');
  const [conn, setConn] = useState<string>('');
  const [username, setUsername] = useState<string>('');
  const [password, setPassword] = useState<string>('');
  const [sqlQuery, setSqlQuery] = useState<string>('SELECT id, quote, timestamp FROM quote_postgres ORDER BY quoteNumber DESC LIMIT 25');
  const [sqlResult, setSqlResult] = useState<any>(null);
  const [mongoDb, setMongoDb] = useState<string>('kendrickquotes');
  const [mongoCollection, setMongoCollection] = useState<string>('quotes');
  const [mongoDocs, setMongoDocs] = useState<string[] | null>(null);
  const [h2Rows, setH2Rows] = useState<any[] | null>(null);
  const [message, setMessage] = useState<string | null>(null);
  const [loading, setLoading] = useState<boolean>(false);
  const [nodeInfo, setNodeInfo] = useState<any>(null);
  const [dbStatus, setDbStatus] = useState<any>(null);

  const setType = async () => {
    setLoading(true);
    setMessage(null);
    try {
      const res = await axios.post('/api/admin/set-db-type', { dbType });
      setMessage(res.data?.message || 'Requested DB change; restart to apply');
    } catch (err: any) {
      setMessage(err?.response?.data?.error || String(err));
    }
    setLoading(false);
  };

  const testSql = async () => {
    setLoading(true);
    setSqlResult(null);
    setMessage(null);
    try {
      await axios.post('/api/admin/test-sql-connection', { connectionString: conn, username, password });
      setMessage('SQL connection OK');
    } catch (err: any) {
      setMessage(err?.response?.data?.error || String(err));
    }
    setLoading(false);
  };

  const runSql = async () => {
    setLoading(true);
    setSqlResult(null);
    setMessage(null);
    try {
      const res = await axios.post('/api/admin/exec-sql', { connectionString: conn, username, password, query: sqlQuery });
      setSqlResult(res.data?.rows ?? res.data);
    } catch (err: any) {
      setMessage(err?.response?.data?.error || String(err));
    }
    setLoading(false);
  };

  const testMongo = async () => {
    setLoading(true);
    setMessage(null);
    try {
      const res = await axios.post('/api/admin/test-mongo-connection', { connectionString: conn });
      setMessage('Mongo OK: ' + (res.data?.result ?? 'ping ok'));
    } catch (err: any) {
      setMessage(err?.response?.data?.error || String(err));
    }
    setLoading(false);
  };

  const exploreMongo = async () => {
    setLoading(true);
    setMongoDocs(null);
    setMessage(null);
    try {
      const res = await axios.post('/api/admin/mongo-explore', { connectionString: conn, database: mongoDb, collection: mongoCollection, limit: 25 });
      setMongoDocs(res.data?.documents ?? []);
    } catch (err: any) {
      setMessage(err?.response?.data?.error || String(err));
    }
    setLoading(false);
  };

  useEffect(() => {
    let mounted = true;
    const fetch = async () => {
      try {
        const [n, d] = await Promise.all([axios.get('/api/nodeinfo'), axios.get('/api/dbstatus')]);
        if (!mounted) return;
        setNodeInfo(n.data);
        setDbStatus(d.data);
      } catch (_) {
        if (mounted) {
          setNodeInfo(null);
          setDbStatus(null);
        }
      }
    };
    fetch();
    const t = setInterval(fetch, 3000);
    return () => { mounted = false; clearInterval(t); };
  }, []);

  return (
    <div style={{ marginTop: 32, padding: 20, borderRadius: 12, background: 'rgba(255,255,255,0.04)' }}>
      <h2 style={{ fontWeight: 800, marginBottom: 12 }}>Admin</h2>

      {/* Primary connection string area */}
      <div style={{ background: 'rgba(255,255,255,0.02)', padding: 14, borderRadius: 10 }}>
        <label style={{ display: 'block', fontWeight: 700 }}>Set Connection String</label>
        <input value={conn} onChange={e => setConn(e.target.value)} placeholder="jdbc:postgresql://... or mongodb://..." style={{ width: '100%', padding: 10, marginTop: 8 }} />
        <div style={{ display: 'flex', gap: 8, marginTop: 8 }}>
          <input value={username} onChange={e => setUsername(e.target.value)} placeholder="username" style={{ flex: 1, padding: 8 }} />
          <input value={password} onChange={e => setPassword(e.target.value)} placeholder="password" style={{ flex: 1, padding: 8 }} />
        </div>
        <div style={{ marginTop: 10, display: 'flex', gap: 8 }}>
          <select value={dbType} onChange={e => setDbType(e.target.value)} style={{ padding: 8 }}>
            <option value="h2">h2</option>
            <option value="mongo">mongo</option>
            <option value="postgres">postgres</option>
          </select>
          <button onClick={setType} disabled={loading}>Set DB Type</button>
          <div style={{ flex: 1 }} />
          <button onClick={testSql} disabled={loading}>Test PostGres</button>
          <button onClick={testMongo} disabled={loading}>Test Mongo</button>
        </div>
      </div>

      {/* Explorers */}
      <div style={{ marginTop: 18 }}>
        <h3 style={{ fontWeight: 800 }}>Database Explorers</h3>

        {/* Postgres Explorer */}
        <section style={{ marginTop: 12, padding: 12, borderRadius: 8, background: 'rgba(0,0,0,0.08)' }}>
          <h4 style={{ margin: 0, fontWeight: 700 }}>Postgres Explorer</h4>
          <div style={{ display: 'flex', gap: 8, marginTop: 8 }}>
            <button onClick={runSql} disabled={loading}>Run SQL (SELECT only)</button>
            <input value={sqlQuery} onChange={e => setSqlQuery(e.target.value)} style={{ flex: 1, padding: 8 }} />
          </div>
          {sqlResult && (
            <div style={{ marginTop: 12, maxHeight: 240, overflow: 'auto', background: '#021627', padding: 12, borderRadius: 8 }}>
              <pre style={{ color: '#baffba' }}>{JSON.stringify(sqlResult, null, 2)}</pre>
            </div>
          )}
        </section>

        {/* H2 Explorer */}
        <section style={{ marginTop: 12, padding: 12, borderRadius: 8, background: 'rgba(0,0,0,0.06)' }}>
          <h4 style={{ margin: 0, fontWeight: 700 }}>H2 Explorer</h4>
          <div style={{ marginTop: 8, display: 'flex', gap: 8 }}>
            <button onClick={async () => {
              setLoading(true); setMessage(null);
              try { const r = await axios.get('/api/quotes'); setH2Rows(r.data || []); } catch (err:any) { setMessage(err?.response?.data?.error || String(err)); }
              setLoading(false);
            }}>Fetch Quotes (via app API)</button>
            <button onClick={() => { setH2Rows(null); setMessage(null); }}>Clear</button>
          </div>
          {h2Rows && (
            <div style={{ marginTop: 12, maxHeight: 240, overflow: 'auto', background: '#021627', padding: 12, borderRadius: 8 }}>
              <pre style={{ color: '#baffba' }}>{JSON.stringify(h2Rows, null, 2)}</pre>
            </div>
          )}
        </section>

        {/* Mongo Explorer */}
        <section style={{ marginTop: 12, padding: 12, borderRadius: 8, background: 'rgba(0,0,0,0.06)' }}>
          <h4 style={{ margin: 0, fontWeight: 700 }}>Mongo Explorer</h4>
          <div style={{ display: 'flex', gap: 8, marginTop: 8 }}>
            <input value={mongoDb} onChange={e => setMongoDb(e.target.value)} placeholder="database (e.g. kendrickquotes)" style={{ padding: 8 }} />
            <input value={mongoCollection} onChange={e => setMongoCollection(e.target.value)} placeholder="collection (e.g. quotes)" style={{ padding: 8 }} />
            <button onClick={exploreMongo} disabled={loading}>Explore Collection (first 25)</button>
          </div>
          {mongoDocs && (
            <div style={{ marginTop: 12, maxHeight: 240, overflow: 'auto', background: '#021627', padding: 12, borderRadius: 8 }}>
              {mongoDocs.map((d, i) => <pre key={i} style={{ color: '#baffba' }}>{d}</pre>)}
            </div>
          )}
        </section>
      </div>

      {message && <div style={{ marginTop: 12, padding: 10, borderRadius: 8, background: 'rgba(0,0,0,0.22)' }}>{message}</div>}

      <div style={{ marginTop: 28 }}>
        <h3 style={{ fontWeight: 700, marginBottom: 12 }}>Node/Application Info</h3>
        {dbStatus && (
          <div style={{ marginBottom: 12, color: dbStatus?.connected ? '#baffba' : '#ffbaba', fontWeight: 600 }}>
            <span>DB Status: </span>
            <span>{dbStatus?.connected ? 'Connected' : 'Not Connected'} ({dbStatus?.type})</span>
            <div style={{ marginTop: 8 }}><strong>Message:</strong> {dbStatus?.message}</div>
          </div>
        )}

        <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fit, minmax(220px, 1fr))', gap: 8 }}>
          {nodeInfo ? (
            <>
              <div><strong>Hostname:</strong> {nodeInfo.hostname}</div>
              <div><strong>App:</strong> {nodeInfo.app}</div>
              <div><strong>OS:</strong> {nodeInfo['os.name']} {nodeInfo['os.version']}</div>
              <div><strong>Processors:</strong> {nodeInfo.availableProcessors}</div>
              <div><strong>Max Memory:</strong> {nodeInfo.maxMemoryMB} MB</div>
              <div><strong>Total Memory:</strong> {nodeInfo.totalMemoryMB} MB</div>
            </>
          ) : (
            <div style={{ color: '#fff', opacity: 0.8 }}>Unable to load node info. Retrying...</div>
          )}
        </div>
      </div>
    </div>
  );
};

export default Admin;
