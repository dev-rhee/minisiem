import React, { useState, useEffect } from 'react';
import TopIpChart from './components/TopIpChart';
import AlertStats from './components/AlertStats';
import './App.css';

function App() {
    const [alerts, setAlerts]   = useState([]);
    const [topIps, setTopIps]   = useState([]);
    const [connected, setConnected] = useState(false);

    // Top IP 주기적으로 갱신
    const fetchTopIps = () => {
        fetch('http://localhost:8080/api/v1/logs/stats/top-ips')
            .then(res => res.json())
            .then(setTopIps)
            .catch(() => {});
    };

    useEffect(() => {
        // 기존 경보 불러오기
        fetch('http://localhost:8080/api/v1/alerts')
            .then(res => res.json())
            .then(data => setAlerts(data.slice(0, 50)))
            .catch(() => {});

        // Top IP 초기 로드 + 30초마다 갱신
        fetchTopIps();
        const interval = setInterval(fetchTopIps, 30_000);

        // SSE 연결
        const eventSource = new EventSource('http://localhost:8080/api/v1/alerts/stream');

        eventSource.addEventListener('connect', () => setConnected(true));
        eventSource.addEventListener('alert', (e) => {
            const alert = JSON.parse(e.data);
            setAlerts(prev => [alert, ...prev].slice(0, 50));
            fetchTopIps(); // 새 경보 올 때마다 Top IP도 갱신
        });
        eventSource.onerror = () => setConnected(false);

        return () => {
            eventSource.close();
            clearInterval(interval);
        };
    }, []);

    const severityColor = (severity) => ({
        CRITICAL: '#ef4444',
        HIGH:     '#f97316',
        MEDIUM:   '#eab308',
        LOW:      '#22c55e',
    }[severity] || '#94a3b8');

    return (
        <div className="app">
            <header className="header">
                <h1>🛡️ Mini SIEM</h1>
                <div className={`status ${connected ? 'connected' : 'disconnected'}`}>
                    {connected ? '● 실시간 연결됨' : '○ 연결 끊김'}
                </div>
            </header>

            <main className="main">

                {/* 심각도별 요약 */}
                <section className="section">
                    <h2 className="section-title">경보 요약</h2>
                    <AlertStats alerts={alerts} />
                </section>

                {/* Top IP 차트 */}
                <section className="section">
                    <h2 className="section-title">Top 10 IP (요청 수)</h2>
                    <div className="chart-box">
                        <TopIpChart data={topIps} />
                    </div>
                </section>

                {/* 경보 목록 */}
                <section className="section">
                    <h2 className="section-title">경보 목록 ({alerts.length})</h2>
                    <div className="alert-list">
                        {alerts.length === 0 && (
                            <div className="empty">경보가 없습니다</div>
                        )}
                        {alerts.map((alert, idx) => (
                            <div key={alert.id ?? idx} className="alert-card">
                                <div className="alert-header">
                  <span
                      className="severity-badge"
                      style={{ background: severityColor(alert.severity) }}
                  >
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

export default App;