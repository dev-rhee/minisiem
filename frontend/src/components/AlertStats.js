import React from 'react';

function AlertStats({ alerts }) {
    const counts = alerts.reduce((acc, alert) => {
        acc[alert.severity] = (acc[alert.severity] || 0) + 1;
        return acc;
    }, {});

    const stats = [
        { label: 'CRITICAL', color: '#ef4444', bg: '#2d1a1a' },
        { label: 'HIGH',     color: '#f97316', bg: '#2d1f1a' },
        { label: 'MEDIUM',   color: '#eab308', bg: '#2d2a1a' },
        { label: 'LOW',      color: '#22c55e', bg: '#1a2d1e' },
    ];

    return (
        <div className="stats-grid">
            {stats.map(({ label, color, bg }) => (
                <div key={label} className="stat-card" style={{ background: bg, borderColor: color + '44' }}>
                    <div className="stat-label" style={{ color }}>{label}</div>
                    <div className="stat-count">{counts[label] ?? 0}</div>
                </div>
            ))}
        </div>
    );
}

export default AlertStats;