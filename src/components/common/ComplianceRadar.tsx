import React from 'react';
import {
    Radar,
    RadarChart,
    PolarGrid,
    PolarAngleAxis,
    PolarRadiusAxis,
    ResponsiveContainer,
    Tooltip
} from 'recharts';

interface ComplianceRadarProps {
    data?: {
        authenticity: number;
        integrity: number;
        usability: number;
        security: number;
    };
}

/**
 * 档案“四性检测”合规雷达图
 * 展示真实性、完整性、可用性、安全性通过率
 */
export const ComplianceRadar: React.FC<ComplianceRadarProps> = ({ data }) => {
    // 默认数据 (如果传入为空)
    const defaultData = [
        { subject: '真实性', A: (data?.authenticity || 0) * 100, fullMark: 100 },
        { subject: '完整性', A: (data?.integrity || 0) * 100, fullMark: 100 },
        { subject: '可用性', A: (data?.usability || 0) * 100, fullMark: 100 },
        { subject: '安全性', A: (data?.security || 0) * 100, fullMark: 100 },
    ];

    const isEmpty = !data || (data.authenticity === 0 && data.integrity === 0 && data.usability === 0 && data.security === 0);

    return (
        <div className="w-full h-64 bg-slate-900/50 rounded-lg p-4 border border-slate-700/50 backdrop-blur-sm relative">
            <div className="text-sm font-medium text-slate-300 mb-2 flex items-center justify-between">
                <span>四性合规雷达图</span>
                <span className="text-xs text-slate-500 font-normal">基于 GB/T 39362 规范</span>
            </div>
            {isEmpty ? (
                <div className="w-full h-[calc(100%-2rem)] flex flex-col items-center justify-center text-slate-500">
                    <div className="text-2xl mb-2">📊</div>
                    <div className="text-xs">该批次暂无四性检测数据</div>
                    <div className="text-[10px] mt-1 opacity-50">点击“执行检测”后生成</div>
                </div>
            ) : (
                <div className="w-full h-[calc(100%-2rem)]">
                    <ResponsiveContainer width="100%" height="100%">
                        <RadarChart cx="50%" cy="50%" outerRadius="70%" data={defaultData}>
                            <PolarGrid stroke="#334155" />
                            <PolarAngleAxis
                                dataKey="subject"
                                tick={{ fill: '#94a3b8', fontSize: 12 }}
                            />
                            <PolarRadiusAxis
                                angle={30}
                                domain={[0, 100]}
                                tick={false}
                                axisLine={false}
                            />
                            <Tooltip
                                contentStyle={{
                                    backgroundColor: '#0f172a',
                                    border: '1px solid #334155',
                                    borderRadius: '6px',
                                    fontSize: '12px'
                                }}
                                formatter={(value: any) => [`${value}%`, '通过率']}
                            />
                            <Radar
                                name="通过率"
                                dataKey="A"
                                stroke="#6366f1"
                                fill="#6366f1"
                                fillOpacity={0.5}
                            />
                        </RadarChart>
                    </ResponsiveContainer>
                </div>
            )}
        </div>
    );
};
