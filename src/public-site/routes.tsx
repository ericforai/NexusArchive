// Input: React Router、官网页面组件
// Output: publicRoutes 官网公开路由配置
// Pos: 官网公开路由配置中心

import React, { lazy, Suspense } from 'react';
import type { RouteObject } from 'react-router-dom';

const ProductWebsite = lazy(() => import('../pages/product-website'));
const BlogIndex = lazy(() => import('../pages/product-website/blog/index'));
const LawStandardInterpretation = lazy(() => import('../pages/product-website/blog/LawStandardInterpretation'));
const SingleSetImplementation = lazy(() => import('../pages/product-website/blog/SingleSetImplementation'));
const DAT94Interpretation = lazy(() => import('../pages/product-website/blog/DAT94Interpretation'));
const DAT92Interpretation = lazy(() => import('../pages/product-website/blog/DAT92Interpretation'));
const DAT95Interpretation = lazy(() => import('../pages/product-website/blog/DAT95Interpretation'));
const ERP104Interpretation = lazy(() => import('../pages/product-website/blog/ERP104Interpretation'));
const GBT18894Interpretation = lazy(() => import('../pages/product-website/blog/GBT18894Interpretation'));
const GBT39784Interpretation = lazy(() => import('../pages/product-website/blog/GBT39784Interpretation'));
const FinanceSolution = lazy(() => import('../pages/product-website/solutions/FinanceSolution'));
const RegulationsIndex = lazy(() => import('../pages/product-website/regulations/index'));
const ERPArchiveSpec = lazy(() => import('../pages/product-website/regulations/ERPArchiveSpec'));
const DAT94Spec = lazy(() => import('../pages/product-website/regulations/DAT94Spec'));
const DAT95Spec = lazy(() => import('../pages/product-website/regulations/DAT95Spec'));
const SystemFunctionalReq = lazy(() => import('../pages/product-website/regulations/SystemFunctionalReq'));
const DAT92Spec = lazy(() => import('../pages/product-website/regulations/DAT92Spec'));
const GBT18894Spec = lazy(() => import('../pages/product-website/regulations/GBT18894Spec'));

const PublicLoadingFallback = () => (
  <div className="flex min-h-screen items-center justify-center bg-[#0B1120] text-sm text-slate-300">
    页面加载中...
  </div>
);

function withSuspense<P extends object>(
  Component: React.LazyExoticComponent<React.ComponentType<P>>,
  props?: P,
): React.ReactElement {
  return (
    <Suspense fallback={<PublicLoadingFallback />}>
      <Component {...((props ?? {}) as any)} />
    </Suspense>
  );
}

export const publicRoutes: RouteObject[] = [
  { path: '/', element: withSuspense(ProductWebsite), handle: { title: 'DigiVoucher 数凭官网' } },
  { path: '/blog', element: withSuspense(BlogIndex), handle: { title: '知识库中心 - DigiVoucher' } },
  { path: '/blog/electronic-accounting-archives-law-standard-interpretation', element: withSuspense(LawStandardInterpretation), handle: { title: '电子会计档案法律法规与国标解读 | DigiVoucher' } },
  { path: '/blog/single-set-system-implementation', element: withSuspense(SingleSetImplementation), handle: { title: '企业单套制归档实施指南 | DigiVoucher' } },
  { path: '/blog/dat-94-2022-interpretation', element: withSuspense(DAT94Interpretation), handle: { title: '标准解读: DA/T 94-2022 会计档案管理规范 | DigiVoucher' } },
  { path: '/blog/dat-92-interpretation', element: withSuspense(DAT92Interpretation), handle: { title: '标准解读: DA/T 92-2022 电子档案单套制 | DigiVoucher' } },
  { path: '/blog/dat-95-interpretation', element: withSuspense(DAT95Interpretation), handle: { title: '标准解读: DA/T 95-2022 电子会计凭证入账 | DigiVoucher' } },
  { path: '/blog/dat-104-interpretation', element: withSuspense(ERP104Interpretation), handle: { title: '标准解读: DA/T 104-2024 ERP归档接口规范 | DigiVoucher' } },
  { path: '/blog/gbt-18894-interpretation', element: withSuspense(GBT18894Interpretation), handle: { title: '标准解读: GB/T 18894 电子档案管理基本要求 | DigiVoucher' } },
  { path: '/blog/gbt-39784-interpretation', element: withSuspense(GBT39784Interpretation), handle: { title: '标准解读: GB/T 39784 电子档案系统工程要求 | DigiVoucher' } },
  { path: '/solutions/finance', element: withSuspense(FinanceSolution), handle: { title: '金融行业电子档案方案 | DigiVoucher' } },
  { path: '/regulations', element: withSuspense(RegulationsIndex), handle: { title: '电子会计档案法律法规库 | DigiVoucher' } },
  { path: '/regulations/erp-archive-spec', element: withSuspense(ERPArchiveSpec), handle: { title: 'DA/T 104-2024 ERP系统归档规范 | DigiVoucher' } },
  { path: '/regulations/dat-94-spec', element: withSuspense(DAT94Spec), handle: { title: 'DA/T 94-2022 电子会计档案管理规范 | DigiVoucher' } },
  { path: '/regulations/dat-95-spec', element: withSuspense(DAT95Spec), handle: { title: 'DA/T 95-2022 财务报销技术规范 | DigiVoucher' } },
  { path: '/regulations/system-functional-req', element: withSuspense(SystemFunctionalReq), handle: { title: 'GB/T 39784 电子档案系统要求 | DigiVoucher' } },
  { path: '/regulations/dat-92-spec', element: withSuspense(DAT92Spec), handle: { title: 'DA/T 92-2022 电子档案单套管理要求 | DigiVoucher' } },
  { path: '/regulations/gbt-18894-spec', element: withSuspense(GBT18894Spec), handle: { title: 'GB/T 18894 电子档案管理规范 | DigiVoucher' } },
];
