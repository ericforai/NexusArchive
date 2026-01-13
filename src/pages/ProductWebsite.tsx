import React from 'react';
import './ProductWebsite.css';
import { Navigation } from './product-website/components/Navigation';
import { HeroSection } from './product-website/components/HeroSection';
import { PainPointsSection } from './product-website/components/PainPointsSection';
import { DataValueSection } from './product-website/components/DataValueSection';
import { CoreSystemSection } from './product-website/components/CoreSystemSection';
import { ProductShowcaseSection } from './product-website/components/ProductShowcaseSection';
import { ComplianceSection } from './product-website/components/ComplianceSection';
import { XinchuangPartnersSection } from './product-website/components/XinchuangPartnersSection';
import { Footer } from './product-website/components/Footer';

export const ProductWebsite: React.FC = () => {

  const [scrolled, setScrolled] = React.useState(false);

  // 使用原生导航替代 useNavigate，避免 React 19 + Router v7 兼容性问题
  const handleNavigate = React.useCallback((path: string) => {
    window.location.href = path;
  }, []);

  React.useEffect(() => {
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
