import React, { useState, useEffect } from 'react';
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

function Dashboard() {
    const [alerts, setAlerts]     = useState([]);
    const [topIps, setTopIps]     = useState([]);
    const [connected, setConnected] = useState(false);

    const fetchTopIps = () => {
        fetch(`${API}/api/v1/logs/stats/top-ips`, { credentials: 'include' })
            .then(res => res.json())
            .then(setTopIps)
            .catch(() => {});
    };

    useEffect(() => {
        fetch(`${API}/api/v1/alerts`, { credentials: 'include' })
            .then(res => res.json())
            .then(data => setAlerts(data.slice(0, 50)))
            .catch(() => {});

        fetchTopIps();
        const interval = setInterval(fetchTopIps, 30_000);

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
            clearInterval(interval);
        };
    }, []);

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
                            <div key={alert.id ?? idx} className="alert-card">
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
                                    <span>상태: {alert.status}</span>
                                </div>
                            </div>
                        ))}
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
