import React, { useState, useCallback, useEffect } from 'react';
import './index.css';
import { Navigation } from './components/Navigation';
import { HeroSection } from './components/HeroSection';
import { PainPointsSection } from './components/PainPointsSection';
import { DataValueSection } from './components/DataValueSection';
import { CoreSystemSection } from './components/CoreSystemSection';
import { ProductShowcaseSection } from './components/ProductShowcaseSection';
import { ComplianceSection } from './components/ComplianceSection';
import { XinchuangPartnersSection } from './components/XinchuangPartnersSection';
import { Footer } from './components/Footer';

const ProductWebsite: React.FC = () => {

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
