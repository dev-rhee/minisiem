import React, { useState, useEffect } from 'react';
import './App.css';

function App() {
    const [alerts, setAlerts] = useState([]);
    const [connected, setConnected] = useState(false);

    // SSE 연결
    useEffect(() => {
        const eventSource = new EventSource('http://localhost:8080/api/v1/alerts/stream');

        eventSource.addEventListener('connect', () => {
            setConnected(true);
        });

        eventSource.addEventListener('alert', (e) => {
            const alert = JSON.parse(e.data);
            setAlerts(prev => [alert, ...prev].slice(0, 50)); // 최근 50개만 유지
        });

        eventSource.onerror = () => {
            setConnected(false);
        };

        // 기존 경보 목록도 REST로 가져오기
        fetch('http://localhost:8080/api/v1/alerts')
            .then(res => res.json())
            .then(data => setAlerts(data.slice(0, 50)));

        return () => eventSource.close();
    }, []);

    const severityColor = (severity) => {
        const colors = {
            CRITICAL: '#ef4444',
            HIGH: '#f97316',
            MEDIUM: '#eab308',
            LOW: '#22c55e',
        };
        return colors[severity] || '#94a3b8';
    };

    return (
        <div className="app">
            <header className="header">
                <h1>🛡️ Mini SIEM</h1>
                <div className={`status ${connected ? 'connected' : 'disconnected'}`}>
                    {connected ? '● 실시간 연결됨' : '○ 연결 끊김'}
                </div>
            </header>

            <main className="main">
                <section className="alert-section">
                    <h2>경보 목록 ({alerts.length})</h2>
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
                    {new Date(alert.createdAt).toLocaleString('ko-KR')}
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