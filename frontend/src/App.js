import React, { useState, useEffect, useRef, useCallback } from 'react';
import TopIpChart from './components/TopIpChart';
import AlertStats from './components/AlertStats';
import './App.css';

const API = 'http://localhost:8080';

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

// ── 로그인 폼 ────────────────────────────────────────────────────────────────

function LoginForm({ onLogin }) {
    const [username, setUsername] = useState('');
    const [password, setPassword] = useState('');
    const [error, setError]       = useState('');
    const [loading, setLoading]   = useState(false);

    const handleSubmit = async (e) => {
        e.preventDefault();
        setLoading(true);
        setError('');
        const ok = await onLogin(username, password);
        if (!ok) setError('아이디 또는 비밀번호가 틀렸습니다');
        setLoading(false);
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

// ── 대시보드 ─────────────────────────────────────────────────────────────────

function Dashboard({ apiFetch, tokenRef, onLogout }) {
    const [alerts, setAlerts]         = useState([]);
    const [topIps, setTopIps]         = useState([]);
    const [connected, setConnected]   = useState(false);
    const [recentLogs, setRecentLogs] = useState([]);
    const [newLogIds, setNewLogIds]   = useState(new Set());
    const [filter, setFilter] = useState({ method: '', status: '', from: '', to: '', url: '' });
    const prevLogIds = useRef(null);

    // Top IP 폴링
    const fetchTopIps = useCallback(() => {
        apiFetch(`${API}/api/v1/logs/stats/top-ips`)
            .then(r => r.json()).then(setTopIps).catch(() => {});
    }, [apiFetch]);

    // 최근 로그 폴링
    const fetchRecentLogs = useCallback(async () => {
        try {
            const res = await apiFetch(`${API}/api/v1/logs/recent?limit=100`);
            if (!res.ok) return;
            const data = await res.json();

            const ids = new Set(data.map(l => l.id));
            if (prevLogIds.current) {
                const fresh = new Set([...ids].filter(id => !prevLogIds.current.has(id)));
                if (fresh.size > 0) {
                    setNewLogIds(fresh);
                    setTimeout(() => setNewLogIds(new Set()), 3000);
                }
            }
            prevLogIds.current = ids;
            setRecentLogs(data);
        } catch {}
    }, [apiFetch]);

    // fetch 기반 SSE — tokenRef.current는 항상 최신 토큰을 참조
    useEffect(() => {
        const controller = new AbortController();
        let alive = true;

        const connect = async () => {
            try {
                const token = tokenRef.current;
                const res = await fetch(`${API}/api/v1/alerts/stream`, {
                    headers: token ? { 'Authorization': `Bearer ${token}` } : {},
                    credentials: 'include',
                    signal: controller.signal,
                });
                if (!res.ok || !alive) return;
                setConnected(true);

                const reader  = res.body.getReader();
                const decoder = new TextDecoder();
                let buffer    = '';
                let evtType   = 'message';

                while (alive) {
                    const { done, value } = await reader.read();
                    if (done) break;
                    buffer += decoder.decode(value, { stream: true });

                    const lines = buffer.split('\n');
                    buffer = lines.pop();

                    for (const line of lines) {
                        if (line.startsWith('event:')) {
                            evtType = line.slice(6).trim();
                        } else if (line.startsWith('data:')) {
                            const raw = line.slice(5).trim();
                            if (evtType === 'alert' && raw) {
                                const alert = JSON.parse(raw);
                                setAlerts(prev => [alert, ...prev].slice(0, 50));
                                fetchTopIps();
                            }
                            if (evtType === 'connect') setConnected(true);
                        } else if (line === '') {
                            evtType = 'message';
                        }
                    }
                }
            } catch (err) {
                if (!controller.signal.aborted && alive) {
                    setConnected(false);
                    setTimeout(connect, 5000);
                }
            }
        };

        connect();
        return () => { alive = false; controller.abort(); setConnected(false); };
    }, []); // tokenRef는 ref이므로 deps 불필요

    // 초기 데이터 + 폴링
    useEffect(() => {
        apiFetch(`${API}/api/v1/alerts`)
            .then(r => r.json()).then(d => setAlerts(d.slice(0, 50))).catch(() => {});

        fetchTopIps();
        fetchRecentLogs();

        const t1 = setInterval(fetchTopIps,     30_000);
        const t2 = setInterval(fetchRecentLogs,  5_000);
        return () => { clearInterval(t1); clearInterval(t2); };
    }, [apiFetch, fetchTopIps, fetchRecentLogs]);

    const severityColor = s => ({
        CRITICAL: '#ef4444', HIGH: '#f97316', MEDIUM: '#eab308', LOW: '#22c55e',
    }[s] || '#94a3b8');

    const setF = (key, val) => setFilter(f => ({ ...f, [key]: val }));

    const filteredLogs = recentLogs.filter(l => {
        if (filter.method && l.method !== filter.method) return false;
        if (filter.status) {
            const c = l.statusCode ?? 0;
            if (filter.status === '2xx' && (c < 200 || c >= 300)) return false;
            if (filter.status === '3xx' && (c < 300 || c >= 400)) return false;
            if (filter.status === '4xx' && (c < 400 || c >= 500)) return false;
            if (filter.status === '5xx' && c < 500) return false;
            if (/^\d+$/.test(filter.status) && c !== Number(filter.status)) return false;
        }
        if (filter.from && new Date(l.occurredAt) < new Date(filter.from)) return false;
        if (filter.to   && new Date(l.occurredAt) > new Date(filter.to))   return false;
        if (filter.url  && !l.requestUri?.toLowerCase().includes(filter.url.toLowerCase())) return false;
        return true;
    });
    const isFiltered = Object.values(filter).some(v => v !== '');

    return (
        <div className="app">
            <header className="header">
                <h1>🛡️ Mini SIEM</h1>
                <div style={{ display: 'flex', alignItems: 'center', gap: 16 }}>
                    <div className={`status ${connected ? 'connected' : 'disconnected'}`}>
                        {connected ? '● 실시간 연결됨' : '○ 연결 끊김'}
                    </div>
                    <button className="btn-logout" onClick={onLogout}>로그아웃</button>
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

                <section className="section">
                    <h2 className="section-title">
                        실시간 로그 스트림
                        <span className="log-count">
                            {filteredLogs.length}{isFiltered ? `/${recentLogs.length}` : ''}건
                        </span>
                    </h2>
                    <div className="log-filter-bar">
                        <div className="log-filter-item">
                            <label>메서드</label>
                            <select value={filter.method} onChange={e => setF('method', e.target.value)}>
                                <option value="">전체</option>
                                {['GET','POST','PUT','DELETE','PATCH'].map(m => (
                                    <option key={m} value={m}>{m}</option>
                                ))}
                            </select>
                        </div>
                        <div className="log-filter-item">
                            <label>상태코드</label>
                            <select value={filter.status} onChange={e => setF('status', e.target.value)}>
                                <option value="">전체</option>
                                <option value="2xx">2xx 성공</option>
                                <option value="3xx">3xx 리다이렉트</option>
                                <option value="4xx">4xx 클라이언트 오류</option>
                                <option value="5xx">5xx 서버 오류</option>
                                {[400,401,403,404,500,502,503].map(c => (
                                    <option key={c} value={String(c)}>{c}</option>
                                ))}
                            </select>
                        </div>
                        <div className="log-filter-item">
                            <label>시각 (시작)</label>
                            <input type="datetime-local" value={filter.from}
                                   onChange={e => setF('from', e.target.value)} />
                        </div>
                        <div className="log-filter-item">
                            <label>시각 (종료)</label>
                            <input type="datetime-local" value={filter.to}
                                   onChange={e => setF('to', e.target.value)} />
                        </div>
                        <div className="log-filter-item log-filter-url">
                            <label>URL</label>
                            <input type="text" placeholder="경로 검색..." value={filter.url}
                                   onChange={e => setF('url', e.target.value)} />
                        </div>
                        {isFiltered && (
                            <button className="log-filter-reset"
                                    onClick={() => setFilter({ method:'', status:'', from:'', to:'', url:'' })}>
                                초기화
                            </button>
                        )}
                    </div>
                    <div className="log-table-wrap">
                        {filteredLogs.length === 0
                            ? <div className="empty">
                                {isFiltered ? '검색 결과가 없습니다' : '수집된 로그가 없습니다'}
                              </div>
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
                                        {filteredLogs.map((log, idx) => (
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

// ── 루트 ─────────────────────────────────────────────────────────────────────

export default function App() {
    const [isLoggedIn, setIsLoggedIn] = useState(false);
    const [checking, setChecking]     = useState(true);
    const tokenRef = useRef(null);

    const forceLogout = useCallback(() => {
        tokenRef.current = null;
        setIsLoggedIn(false);
    }, []);

    const tryRefresh = useCallback(async () => {
        try {
            const res = await fetch(`${API}/auth/refresh`, {
                method: 'POST',
                credentials: 'include',
            });
            if (res.ok) {
                const { accessToken } = await res.json();
                tokenRef.current = accessToken;
                return true;
            }
        } catch {}
        return false;
    }, []);

    // 401 발생 시 자동 재발급 후 재시도
    const apiFetch = useCallback(async (url, opts = {}) => {
        const doFetch = (token) => fetch(url, {
            ...opts,
            headers: {
                ...opts.headers,
                ...(token ? { 'Authorization': `Bearer ${token}` } : {}),
            },
            credentials: 'include',
        });

        let res = await doFetch(tokenRef.current);
        if (res.status === 401) {
            const ok = await tryRefresh();
            if (ok) {
                res = await doFetch(tokenRef.current);
            } else {
                forceLogout();
            }
        }
        return res;
    }, [tryRefresh, forceLogout]);

    const handleLogin = useCallback(async (username, password) => {
        try {
            const res = await fetch(`${API}/auth/login`, {
                method: 'POST',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify({ username, password }),
                credentials: 'include',
            });
            if (res.ok) {
                const { accessToken } = await res.json();
                tokenRef.current = accessToken;
                setIsLoggedIn(true);
                return true;
            }
        } catch {}
        return false;
    }, []);

    const handleLogout = useCallback(async () => {
        try {
            await fetch(`${API}/auth/logout`, {
                method: 'POST',
                credentials: 'include',
            });
        } catch {}
        forceLogout();
    }, [forceLogout]);

    // 새로고침 시 refreshToken 쿠키로 자동 재인증
    useEffect(() => {
        tryRefresh()
            .then(ok => { if (ok) setIsLoggedIn(true); })
            .finally(() => setChecking(false));
    }, [tryRefresh]);

    if (checking) return null;
    return isLoggedIn
        ? <Dashboard apiFetch={apiFetch} tokenRef={tokenRef} onLogout={handleLogout} />
        : <LoginForm onLogin={handleLogin} />;
}
