// Input: React、状态管理、路由跳转
// Output: ProductWebsite 营落地页主组件 + React 实例诊断
// Pos: src/pages/ProductWebsite.tsx
// 一旦我被更新，务必更新我的开头注释，以及所属的文件夹的 md。

import React, { useState, useEffect, useCallback } from 'react';
import './ProductWebsite.css';
import {
  Navigation,
  HeroSection,
  PainPointsSection,
  DataValueSection,
  CoreSystemSection,
  ProductShowcaseSection,
  ComplianceSection,
  XinchuangPartnersSection,
  Footer,
} from './product-website/components';

export const ProductWebsite: React.FC = () => {
  // 开发环境 React 诊断（带安全检查）
  if (import.meta.env.DEV && typeof window !== 'undefined' && React?.useState) {
    const win = window as any;
    win.__REACT_PAGE_LOG__ = win.__REACT_PAGE_LOG__ || [];
    try {
      const dispatcher =
        (React as any).__SECRET_INTERNALS_DO_NOT_USE_OR_YOU_WILL_BE_FIRED
          ?.ReactCurrentDispatcher?.current ?? null;
      const entry = {
        page: 'ProductWebsite',
        time: new Date().toISOString(),
        version: React.version || 'unknown',
        module: import.meta.url,
        sameRoot: win.__REACT_ROOT_REF__ ? win.__REACT_ROOT_REF__ === React : null,
        sameUseState: win.__REACT_ROOT_USESTATE__ ? win.__REACT_ROOT_USESTATE__ === React.useState : null,
        dispatcher: dispatcher ? 'set' : 'null',
      };
      win.__REACT_PAGE_LOG__.push(entry);
      if (entry.sameRoot === false || entry.sameUseState === false || entry.dispatcher === 'null') {
        console.warn('[ReactDebug] ProductWebsite hook context check', entry);
      }
    } catch (e) {
      console.warn('[ReactDebug] Diagnostic error:', e);
    }
  }
  const [scrolled, setScrolled] = useState(false);

  // 使用原生导航替代 useNavigate，避免 React 19 + Router v7 兼容性问题
  const handleNavigate = useCallback((path: string) => {
    window.location.href = path;
  }, []);

  useEffect(() => {
    const handleScroll = () => {
      setScrolled(window.scrollY > 50);
    };
    window.addEventListener('scroll', handleScroll);
    return () => window.removeEventListener('scroll', handleScroll);
  }, []);

  return (
    <div className="min-h-screen bg-[#0B1120] text-slate-300 font-sans selection:bg-cyan-500/30">
      <Navigation scrolled={scrolled} onNavigate={handleNavigate} />
      <HeroSection onNavigate={handleNavigate} />
      <PainPointsSection />
      <DataValueSection />
      <CoreSystemSection />
      <ProductShowcaseSection />
      <ComplianceSection />
      <XinchuangPartnersSection />
      <Footer />
    </div>
  );
};

export default ProductWebsite;
