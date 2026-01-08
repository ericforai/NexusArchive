// Input: React、状态管理、路由跳转
// Output: ProductWebsite 营落地页主组件
// Pos: src/pages/ProductWebsite.tsx
// 一旦我被更新，务必更新我的开头注释，以及所属的文件夹的 md。

import React, { useState, useEffect } from 'react';
import { useNavigate } from 'react-router-dom';
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
  const navigate = useNavigate();
  const [scrolled, setScrolled] = useState(false);

  useEffect(() => {
    const handleScroll = () => {
      setScrolled(window.scrollY > 50);
    };
    window.addEventListener('scroll', handleScroll);
    return () => window.removeEventListener('scroll', handleScroll);
  }, []);

  return (
    <div className="min-h-screen bg-[#0B1120] text-slate-300 font-sans selection:bg-cyan-500/30">
      <Navigation scrolled={scrolled} onNavigate={navigate} />
      <HeroSection onNavigate={navigate} />
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
