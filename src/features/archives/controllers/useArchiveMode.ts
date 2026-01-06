/**
 * useArchiveMode - Mode Resolution Hook
 *
 * Handles route mode resolution and configuration
 */
import { useCallback } from 'react';
import { resolveRouteConfig, DEFAULT_ROUTE_CONFIG, ArchiveRouteMode } from '../routeConfigs';
import { ControllerMode, UseArchiveModeOptions } from './types';
import type { ModuleConfig } from '../../../types';

export function useArchiveMode(options: UseArchiveModeOptions): ControllerMode {
    const { routeConfig, title: propTitle, subTitle: propSubTitle, config: propConfig } = options;

    // Resolve configuration
    const resolvedConfig = routeConfig ? resolveRouteConfig(routeConfig as ArchiveRouteMode) : undefined;
    const title = resolvedConfig?.title || propTitle || DEFAULT_ROUTE_CONFIG.title;
    const subTitle = resolvedConfig?.subTitle || propSubTitle || DEFAULT_ROUTE_CONFIG.subTitle;
    const config: ModuleConfig = resolvedConfig?.config || propConfig || DEFAULT_ROUTE_CONFIG.config;

    const isPoolView = subTitle === '电子凭证池';
    const isLinkingView = subTitle === '凭证关联';

    const resolveCategoryCode = useCallback((): 'AC01' | 'AC02' | 'AC03' | 'AC04' | undefined => {
        switch (subTitle) {
            case '会计凭证':
            case '原始凭证':
            case '记账凭证':
            case '凭证关联':
                return 'AC01';
            case '会计账簿':
                return 'AC02';
            case '财务报告':
                return 'AC03';
            case '其他会计资料':
                return 'AC04';
            default:
                return undefined;
        }
    }, [subTitle]);

    const resolveDefaultStatus = useCallback(() => {
        if (subTitle === '凭证关联') return 'draft,MATCH_PENDING,MATCHED';
        if (['会计凭证', '会计账簿', '财务报告', '其他会计资料'].includes(subTitle || '')) {
            return 'archived';
        }
        return undefined;
    }, [subTitle]);

    return {
        routeKey: routeConfig as ArchiveRouteMode | undefined,
        title,
        subTitle,
        config,
        isPoolView,
        isLinkingView,
        categoryCode: resolveCategoryCode(),
        defaultStatus: resolveDefaultStatus(),
    };
}
