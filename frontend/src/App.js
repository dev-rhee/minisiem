import React, { useState, useEffect, useRef } from 'react';
import TopIpChart from './components/TopIpChart';
import AlertStats from './components/AlertStats';
import './App.css';

const API = 'http://localhost:8080';

function LoginForm({ onLogin }) {
    const [username, setUsername] = useState('');
    const [password, setPassword] = useState('');
    const [error, setError]       = useState('');
    const [loading, setLoading]   = useState(false);

    const handleSubmit = async (e) => {
        e.preventDefault();
        setLoading(true);
        setError('');

        const res = await fetch(`${API}/api/v1/alerts`, {
            headers: { 'Authorization': `Basic ${btoa(`${username}:${password}`)}` },
            credentials: 'include',
        }).catch(() => null);

        setLoading(false);

        if (res?.ok) {
            onLogin();
        } else {
            setError('아이디 또는 비밀번호가 틀렸습니다');
        }
    };

    return (
        <div className="login-wrap">
            <form className="login-form" onSubmit={handleSubmit}>
                <h1>🛡️ Mini SIEM</h1>
                <input
                    type="text"
                    placeholder="아이디"
                    value={username}
                    onChange={e => setUsername(e.target.value)}
                    autoFocus
                />
                <input
                    type="password"
                    placeholder="비밀번호"
                    value={password}
                    onChange={e => setPassword(e.target.value)}
                />
                {error && <p className="login-error">{error}</p>}
                <button type="submit" disabled={loading}>
                    {loading ? '로그인 중...' : '로그인'}
                </button>
            </form>
        </div>
    );
}

const METHOD_COLORS = {
    GET:    '#3b82f6',
    POST:   '#22c55e',
    PUT:    '#f59e0b',
    DELETE: '#ef4444',
    PATCH:  '#a855f7',
};

function statusClass(code) {
    if (!code) return '';
    if (code < 300) return 'status-2xx';
    if (code < 400) return 'status-3xx';
    if (code < 500) return 'status-4xx';
    return 'status-5xx';
}

function formatBytes(n) {
    if (!n) return '-';
    if (n < 1024) return `${n}B`;
    if (n < 1024 * 1024) return `${(n / 1024).toFixed(1)}KB`;
    return `${(n / 1024 / 1024).toFixed(1)}MB`;
}

function formatTime(s) {
    if (!s) return '-';
    const d = new Date(s);
    const pad = n => String(n).padStart(2, '0');
    return `${d.getFullYear()}.${pad(d.getMonth()+1)}.${pad(d.getDate())} ${pad(d.getHours())}:${pad(d.getMinutes())}:${pad(d.getSeconds())}`;
}

function Dashboard() {
    const [alerts, setAlerts]       = useState([]);
    const [topIps, setTopIps]       = useState([]);
    const [connected, setConnected] = useState(false);
    const [recentLogs, setRecentLogs] = useState([]);
    const [newLogIds, setNewLogIds]   = useState(new Set());
    const lastFetchRef = useRef(null);

    const fetchTopIps = () => {
        fetch(`${API}/api/v1/logs/stats/top-ips`, { credentials: 'include' })
            .then(res => res.json())
            .then(setTopIps)
            .catch(() => {});
    };

    const fetchRecentLogs = async () => {
        try {
            const res = await fetch(`${API}/api/v1/logs/recent?limit=100`, { credentials: 'include' });
            if (!res.ok) return;
            const data = await res.json(); // occurred_at DESC 정렬

            if (lastFetchRef.current) {
                const prevIds = lastFetchRef.current;
                const fresh = new Set(data.filter(l => !prevIds.has(l.id)).map(l => l.id));
                if (fresh.size > 0) {
                    setNewLogIds(fresh);
                    setTimeout(() => setNewLogIds(new Set()), 3000);
                }
            }
            lastFetchRef.current = new Set(data.map(l => l.id));
            setRecentLogs(data);
        } catch {}
    };

    useEffect(() => {
        fetch(`${API}/api/v1/alerts`, { credentials: 'include' })
            .then(res => res.json())
            .then(data => setAlerts(data.slice(0, 50)))
            .catch(() => {});

        fetchTopIps();
        fetchRecentLogs();
        const topIpInterval  = setInterval(fetchTopIps, 30_000);
        const logInterval    = setInterval(fetchRecentLogs, 5_000);

        const es = new EventSource(`${API}/api/v1/alerts/stream`, { withCredentials: true });
        es.addEventListener('connect', () => setConnected(true));
        es.addEventListener('alert', (e) => {
            const alert = JSON.parse(e.data);
            setAlerts(prev => [alert, ...prev].slice(0, 50));
            fetchTopIps();
        });
        es.onerror = () => setConnected(false);

        return () => {
            es.close();
            clearInterval(topIpInterval);
            clearInterval(logInterval);
        };
    }, []);

    const updateAlertStatus = async (id, newStatus) => {
        try {
            const res = await fetch(
                `${API}/api/v1/alerts/${id}/status?status=${newStatus}`,
                { method: 'PATCH', credentials: 'include' }
            );
            if (!res.ok) return;
            const updated = await res.json();
            setAlerts(prev => prev.map(a => a.id === id ? updated : a));
        } catch {}
    };

    const severityColor = (s) => ({
        CRITICAL: '#ef4444',
        HIGH:     '#f97316',
        MEDIUM:   '#eab308',
        LOW:      '#22c55e',
    }[s] || '#94a3b8');

    return (
        <div className="app">
            <header className="header">
                <h1>🛡️ Mini SIEM</h1>
                <div className={`status ${connected ? 'connected' : 'disconnected'}`}>
                    {connected ? '● 실시간 연결됨' : '○ 연결 끊김'}
                </div>
            </header>

            <main className="main">
                <section className="section">
                    <h2 className="section-title">경보 요약</h2>
                    <AlertStats alerts={alerts} />
                </section>

                <section className="section">
                    <h2 className="section-title">Top 10 IP (요청 수)</h2>
                    <div className="chart-box">
                        <TopIpChart data={topIps} />
                    </div>
                </section>

                <section className="section">
                    <h2 className="section-title">경보 목록 ({alerts.length})</h2>
                    <div className="alert-list">
                        {alerts.length === 0 && <div className="empty">경보가 없습니다</div>}
                        {alerts.map((alert, idx) => (
                            <div key={alert.id ?? idx}
                                 className={`alert-card${alert.status === 'RESOLVED' ? ' alert-resolved' : ''}`}>
                                <div className="alert-header">
                                    <span className="severity-badge"
                                          style={{ background: severityColor(alert.severity) }}>
                                        {alert.severity}
                                    </span>
                                    <span className="rule-name">{alert.ruleName}</span>
                                    <span className="alert-time">
                                        {alert.createdAt
                                            ? new Date(alert.createdAt).toLocaleString('ko-KR')
                                            : '-'}
                                    </span>
                                </div>
                                <div className="alert-body">
                                    <span>IP: {alert.srcIp ?? '-'}</span>
                                    <span>발생 건수: {alert.occurredCount}</span>
                                    <span className={`alert-status alert-status-${alert.status}`}>
                                        {alert.status}
                                    </span>
                                    <div className="alert-actions">
                                        {alert.status === 'OPEN' && (
                                            <button className="btn-ack"
                                                    onClick={() => updateAlertStatus(alert.id, 'ACKNOWLEDGED')}>
                                                확인
                                            </button>
                                        )}
                                        {alert.status === 'ACKNOWLEDGED' && (
                                            <button className="btn-resolve"
                                                    onClick={() => updateAlertStatus(alert.id, 'RESOLVED')}>
                                                해결
                                            </button>
                                        )}
                                    </div>
                                </div>
                            </div>
                        ))}
                    </div>
                </section>

                <section className="section">
                    <h2 className="section-title">
                        실시간 로그 스트림
                        <span className="log-count">최근 {recentLogs.length}건</span>
                    </h2>
                    <div className="log-table-wrap">
                        {recentLogs.length === 0
                            ? <div className="empty">수집된 로그가 없습니다</div>
                            : (
                                <table className="log-table">
                                    <thead>
                                        <tr>
                                            <th>시각</th>
                                            <th>소스 IP</th>
                                            <th>메서드</th>
                                            <th>경로</th>
                                            <th>상태</th>
                                            <th>크기</th>
                                            <th>User Agent</th>
                                        </tr>
                                    </thead>
                                    <tbody>
                                        {recentLogs.map((log, idx) => (
                                            <tr key={log.id ?? idx}
                                                className={newLogIds.has(log.id) ? 'log-row-new' : ''}>
                                                <td className="log-time">{formatTime(log.occurredAt)}</td>
                                                <td className="log-ip">{log.srcIp ?? '-'}</td>
                                                <td>
                                                    <span className="method-badge"
                                                          style={{ background: METHOD_COLORS[log.method] ?? '#475569' }}>
                                                        {log.method ?? '-'}
                                                    </span>
                                                </td>
                                                <td className="log-uri" title={log.requestUri}>
                                                    {log.requestUri ?? '-'}
                                                </td>
                                                <td>
                                                    <span className={`status-badge ${statusClass(log.statusCode)}`}>
                                                        {log.statusCode ?? '-'}
                                                    </span>
                                                </td>
                                                <td className="log-bytes">{formatBytes(log.responseBytes)}</td>
                                                <td className="log-ua" title={log.userAgent}>
                                                    {log.userAgent ?? '-'}
                                                </td>
                                            </tr>
                                        ))}
                                    </tbody>
                                </table>
                            )
                        }
                    </div>
                </section>
            </main>
        </div>
    );
}

export default function App() {
    const [isLoggedIn, setIsLoggedIn] = useState(false);
    const [checking, setChecking]     = useState(true);

    // 새로고침해도 세션이 살아있으면 로그인 유지
    useEffect(() => {
        fetch(`${API}/api/v1/alerts`, { credentials: 'include' })
            .then(res => {
                if (res.ok) setIsLoggedIn(true);
            })
            .catch(() => {})
            .finally(() => setChecking(false));
    }, []);

    if (checking) return null;

    return isLoggedIn
        ? <Dashboard />
        : <LoginForm onLogin={() => setIsLoggedIn(true)} />;
}
