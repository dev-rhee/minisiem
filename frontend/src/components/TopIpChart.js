import React from 'react';
import {
    BarChart, Bar, XAxis, YAxis, Tooltip,
    ResponsiveContainer, Cell
} from 'recharts';

const COLORS = ['#ef4444', '#f97316', '#eab308', '#22c55e', '#3b82f6',
    '#8b5cf6', '#06b6d4', '#f43f5e', '#84cc16', '#a78bfa'];

function TopIpChart({ data }) {
    if (!data || data.length === 0) {
        return (
            <div className="chart-empty">수집된 데이터가 없습니다</div>
        );
    }

    return (
        <ResponsiveContainer width="100%" height={240}>
            <BarChart
                data={data}
                layout="vertical"
                margin={{ top: 4, right: 24, left: 8, bottom: 4 }}
            >
                <XAxis type="number" tick={{ fill: '#64748b', fontSize: 11 }} />
                <YAxis
                    type="category"
                    dataKey="ip"
                    width={110}
                    tick={{ fill: '#94a3b8', fontSize: 11 }}
                />
                <Tooltip
                    contentStyle={{ background: '#1e2130', border: '1px solid #2d3148', borderRadius: 6 }}
                    labelStyle={{ color: '#e2e8f0' }}
                    itemStyle={{ color: '#94a3b8' }}
                    formatter={(value) => [`${value}건`, '요청 수']}
                />
                <Bar dataKey="count" radius={[0, 4, 4, 0]}>
                    {data.map((_, idx) => (
                        <Cell key={idx} fill={COLORS[idx % COLORS.length]} />
                    ))}
                </Bar>
            </BarChart>
        </ResponsiveContainer>
    );
}

export default TopIpChart;