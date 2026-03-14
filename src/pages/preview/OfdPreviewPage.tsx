// Input: React、react-router-dom、UnifiedOfdPreview
// Output: OfdPreviewPage 独立预览页
// Pos: src/pages/preview/OfdPreviewPage.tsx

import React from 'react';
import { useParams, useSearchParams } from 'react-router-dom';
import { UnifiedOfdPreview } from '@/components/preview';
import { ChevronLeft, FileText } from 'lucide-react';

export const OfdPreviewPage: React.FC = () => {
  const { fileId } = useParams<{ fileId: string }>();
  const [searchParams] = useSearchParams();
  const fileName = searchParams.get('fileName') || 'document.ofd';
  const sourceType = searchParams.get('sourceType') as 'ARCHIVE' | 'ORIGINAL' | null;

  if (!fileId) {
    return (
      <div className="flex h-screen items-center justify-center bg-slate-100 text-slate-500">
        无效的文件 ID
      </div>
    );
  }

  return (
    <div className="flex h-screen flex-col bg-slate-900">
      {/* 顶部工具栏 */}
      <div className="flex h-12 items-center justify-between border-b border-slate-700 bg-slate-800 px-4 text-white shadow-md">
        <div className="flex items-center gap-3">
          <button 
            onClick={() => window.close()} 
            className="flex items-center gap-1 rounded-md px-2 py-1 text-sm text-slate-300 hover:bg-slate-700 hover:text-white"
          >
            <ChevronLeft size={16} />
            关闭
          </button>
          <div className="h-4 w-px bg-slate-600" />
          <div className="flex items-center gap-2 truncate">
            <FileText size={16} className="text-blue-400" />
            <span className="text-sm font-medium truncate max-w-md">{fileName}</span>
          </div>
        </div>
        <div className="text-xs text-slate-400">
          OFD 核心渲染引擎 (liteofd)
        </div>
      </div>

      {/* 预览区域 */}
      <div className="flex-1 overflow-hidden">
        <UnifiedOfdPreview
          fileId={fileId}
          fileName={fileName}
          sourceType={sourceType}
          className="h-full border-0"
        />
      </div>
    </div>
  );
};

export default OfdPreviewPage;
