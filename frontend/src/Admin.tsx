import React, { useState, useEffect } from 'react';
import axios from 'axios';

const Admin: React.FC = () => {
  const [dbType, setDbType] = useState('h2');
  const [conn, setConn] = useState('');
  const [username, setUsername] = useState('');
  const [password, setPassword] = useState('');
  const [sqlQuery, setSqlQuery] = useState('SELECT 1');
  const [sqlResult, setSqlResult] = useState<any>(null);
  const [mongoDb, setMongoDb] = useState('test');
  const [mongoCollection, setMongoCollection] = useState('');
  const [mongoDocs, setMongoDocs] = useState<string[] | null>(null);
  const [message, setMessage] = useState<string | null>(null);
  const [loading, setLoading] = useState(false);
  const [nodeInfo, setNodeInfo] = useState<any>(null);
  const [dbStatus, setDbStatus] = useState<{connected: string, type: string, message: string} | null>(null);

  const setType = async () => {
    setLoading(true);
    setMessage(null);
    try {
      const res = await axios.post('/api/admin/set-db-type', { dbType });
      setMessage(res.data.message || 'Requested');
    } catch (e: any) {
      setMessage(e?.response?.data?.error || e.message);
    }
    setLoading(false);
  };

  const testSql = async () => {
    setLoading(true);
    setSqlResult(null);
    setMessage(null);
    try {
      const res = await axios.post('/api/admin/test-sql-connection', { connectionString: conn, username, password });
      setMessage('SQL connection OK');
    } catch (e: any) {
      setMessage(e?.response?.data?.error || e.message);
    }
    setLoading(false);
  };

  const runSql = async () => {
    setLoading(true);
    setSqlResult(null);
    setMessage(null);
    try {
      const res = await axios.post('/api/admin/exec-sql', { connectionString: conn, username, password, query: sqlQuery });
      setSqlResult(res.data.rows || []);
    } catch (e: any) {
      setMessage(e?.response?.data?.error || e.message);
    }
    setLoading(false);
  };

  const testMongo = async () => {
    setLoading(true);
    setMessage(null);
    try {
      const res = await axios.post('/api/admin/test-mongo-connection', { connectionString: conn });
      setMessage('Mongo OK: ' + (res.data.result || 'ping ok'));
    } catch (e: any) {
      setMessage(e?.response?.data?.error || e.message);
    }
    setLoading(false);
  };

  const exploreMongo = async () => {
    setLoading(true);
    setMongoDocs(null);
    setMessage(null);
    try {
      const res = await axios.post('/api/admin/mongo-explore', { connectionString: conn, database: mongoDb, collection: mongoCollection, limit: 25 });
      setMongoDocs(res.data.documents || []);
    } catch (e: any) {
      setMessage(e?.response?.data?.error || e.message);
    }
    setLoading(false);
  };

  useEffect(() => {
    let mounted = true;
    const fetchAll = () => {
      axios.get('/api/nodeinfo')
        .then((res: { data: any }) => { if (mounted) setNodeInfo(res.data); })
        .catch(() => { if (mounted) setNodeInfo(null); });
      axios.get('/api/dbstatus')
        .then((res: { data: any }) => { if (mounted) setDbStatus(res.data); })
        .catch(() => { if (mounted) setDbStatus(null); });
    };
    fetchAll();
    const t = setInterval(fetchAll, 3000);
    return () => { mounted = false; clearInterval(t); };
  }, []);

  return (
    <div style={{ marginTop: 32, padding: 20, borderRadius: 12, background: 'rgba(255,255,255,0.04)' }}>
      <h3 style={{ fontWeight: 700 }}>Admin / DB Explorer</h3>
      <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: 12 }}>
        <div>
          <label><strong>DB Type</strong></label>
          <select value={dbType} onChange={e => setDbType(e.target.value)} style={{ width: '100%', padding: 8, marginTop: 6 }}>
            <option value="h2">H2 (in-memory)</option>
            <option value="postgres">Postgres</option>
            <option value="mongo">MongoDB</option>
          </select>
          <button onClick={setType} style={{ marginTop: 10, padding: '8px 12px' }} disabled={loading}>Set DB Type</button>
        </div>
        <div>
          <label><strong>Connection String</strong></label>
          <input value={conn} onChange={e => setConn(e.target.value)} placeholder="jdbc:postgresql://... or mongodb://..." style={{ width: '100%', padding: 8, marginTop: 6 }} />
          <div style={{ display: 'flex', gap: 8, marginTop: 8 }}>
            <input value={username} onChange={e => setUsername(e.target.value)} placeholder="username" style={{ flex: 1, padding: 8 }} />
            <input value={password} onChange={e => setPassword(e.target.value)} placeholder="password" style={{ flex: 1, padding: 8 }} />
          </div>
        </div>
      </div>

      <div style={{ marginTop: 16 }}>
        <h4>SQL Explorer</h4>
        <div style={{ display: 'flex', gap: 8 }}>
          <button onClick={testSql} disabled={loading}>Test SQL Connection</button>
          <button onClick={runSql} disabled={loading}>Run SQL (SELECT only)</button>
          <input value={sqlQuery} onChange={e => setSqlQuery(e.target.value)} style={{ flex: 1, padding: 8 }} />
        </div>
        {sqlResult && (
          <div style={{ marginTop: 12, maxHeight: 220, overflow: 'auto', background: '#021627', padding: 12, borderRadius: 8 }}>
            <pre style={{ color: '#baffba' }}>{JSON.stringify(sqlResult, null, 2)}</pre>
          </div>
        )}
      </div>

      <div style={{ marginTop: 16 }}>
        <h4>Mongo Explorer</h4>
        <div style={{ display: 'flex', gap: 8 }}>
          <input value={mongoDb} onChange={e => setMongoDb(e.target.value)} placeholder="database" style={{ padding: 8 }} />
          <input value={mongoCollection} onChange={e => setMongoCollection(e.target.value)} placeholder="collection" style={{ padding: 8 }} />
          <button onClick={testMongo} disabled={loading}>Test Mongo Ping</button>
          <button onClick={exploreMongo} disabled={loading}>Explore Collection</button>
        </div>
        {mongoDocs && (
          <div style={{ marginTop: 12, maxHeight: 220, overflow: 'auto', background: '#021627', padding: 12, borderRadius: 8 }}>
            {mongoDocs.map((d, i) => (
              <pre key={i} style={{ color: '#baffba' }}>{d}</pre>
            ))}
          </div>
        )}
      </div>

      {message && (
        <div style={{ marginTop: 12, padding: 10, borderRadius: 8, background: 'rgba(0,0,0,0.25)' }}>{message}</div>
      )}

      <div style={{ marginTop: 28 }}>
        <h3 style={{ fontWeight: 700, marginBottom: 12 }}>Node/Application Info</h3>
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
      </div>

    </div>
  );
};

export default Admin;
