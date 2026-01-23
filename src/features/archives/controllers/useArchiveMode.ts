/**
 * useArchiveMode - Mode Resolution Hook
 *
 * Handles route mode resolution and configuration
 * 修复：使用 useMemo 稳定返回值引用
 */
import { useCallback, useMemo } from 'react';
import { resolveRouteConfig, DEFAULT_ROUTE_CONFIG, ArchiveRouteMode } from '../routeConfigs';
import { ControllerMode, UseArchiveModeOptions } from './types';
import type { ModuleConfig } from '../../../types';

export function useArchiveMode(options: UseArchiveModeOptions): ControllerMode {
    const { routeConfig, title: propTitle, subTitle: propSubTitle, config: propConfig } = options;

    // Resolve configuration
    const resolvedConfig = routeConfig ? resolveRouteConfig(routeConfig as ArchiveRouteMode) : undefined;
    const title = propTitle || resolvedConfig?.title || DEFAULT_ROUTE_CONFIG.title;
    const subTitle = propSubTitle || resolvedConfig?.subTitle || DEFAULT_ROUTE_CONFIG.subTitle;
    const config: ModuleConfig = propConfig || resolvedConfig?.config || DEFAULT_ROUTE_CONFIG.config;

    const isPoolView = routeConfig === 'pool' || subTitle === '记账凭证库';
    const isLinkingView = subTitle === '凭证关联';

    const resolveCategoryCode = useCallback((): 'AC01' | 'AC02' | 'AC03' | 'AC04' | undefined => {
        switch (subTitle) {
            case '会计凭证':
            case '原始凭证':
            case '记账凭证':
            case '凭证关联':
                return 'AC01';
            case '会计账簿':
            case '会计账簿库':
                return 'AC02';
            case '财务报告':
            case '财务报告库':
                return 'AC03';
            case '其他会计资料':
            case '其他会计资料库':
                return 'AC04';
            default:
                // Handle fuzzy match for robustness
                if ((subTitle || '').includes('会计账簿')) return 'AC02';
                if ((subTitle || '').includes('财务报告')) return 'AC03';
                if ((subTitle || '').includes('其他会计资料')) return 'AC04';
                return undefined;
        }
    }, [subTitle]);

    const resolveDefaultStatus = useCallback(() => {
        if (subTitle === '凭证关联') return 'draft,MATCH_PENDING,MATCHED';

        // For Pool views, we typically want "All" (undefined) or PENDING.
        // If we force 'archived', we hide new uploads.
        if (isPoolView) return undefined;

        // 会计档案相关 routeConfig 应只显示已归档档案
        // 待处理状态的档案应在预归档库中显示
        const archivedRouteConfigs: ArchiveRouteMode[] = ['view', 'voucher', 'ledger', 'report', 'other'];
        if (routeConfig && archivedRouteConfigs.includes(routeConfig as ArchiveRouteMode)) {
            return 'archived';
        }

        // subTitle 匹配（兜底逻辑）
        if (['会计凭证', '会计账簿', '财务报告', '其他会计资料', '归档查看'].some(kw => (subTitle || '').includes(kw))) {
            return 'archived';
        }

        return undefined;
    }, [subTitle, isPoolView, routeConfig]);

    // 使用 useMemo 稳定返回值引用
    return useMemo(() => ({
        routeKey: routeConfig as ArchiveRouteMode | undefined,
        title,
        subTitle,
        config,
        isPoolView,
        isLinkingView,
        categoryCode: resolveCategoryCode(),
        defaultStatus: resolveDefaultStatus(),
    }), [routeConfig, title, subTitle, config, isPoolView, isLinkingView, resolveCategoryCode, resolveDefaultStatus]);
}
