// Input: React、React Router、Ant Design、scanApi
// Output: MobileUploadPage 组件 - 移动端扫描上传页面
// Pos: src/pages/scan/MobileUploadPage.tsx

import React, { useState, useEffect } from 'react';
import { useParams } from 'react-router-dom';
import { Button, Upload, message, Spin, Alert, Card, Typography } from 'antd';
import { CameraOutlined, UploadOutlined, CheckCircleOutlined } from '@ant-design/icons';
import type { UploadProps } from 'antd';
import { scanApi } from '@api/scan';

const { Title } = Typography;

export const MobileUploadPage: React.FC = () => {
  const { sessionId } = useParams<{ sessionId: string }>();

  const [loading, setLoading] = useState(true);
  const [sessionValid, setSessionValid] = useState(false);
  const [uploadedFiles, setUploadedFiles] = useState<string[]>([]);

  useEffect(() => {
    const validateSession = async () => {
      if (!sessionId) {
        setLoading(false);
        return;
      }
      try {
        const result = await scanApi.validateSession(sessionId);
        setSessionValid(result.valid);
      } catch {
        setSessionValid(false);
      } finally {
        setLoading(false);
      }
    };
    validateSession();
  }, [sessionId]);

  if (loading) {
    return (
      <div style={{ display: 'flex', justifyContent: 'center', alignItems: 'center', height: '100vh' }}>
        <Spin size="large" />
      </div>
    );
  }

  if (!sessionValid) {
    return (
      <div style={{ padding: 24, maxWidth: 400, margin: '0 auto', marginTop: 100 }}>
        <Alert
          type="error"
          message="会话无效或已过期"
          description="请返回扫描页面重新获取二维码"
          showIcon
        />
      </div>
    );
  }

  const uploadProps: UploadProps = {
    name: 'file',
    multiple: true,
    accept: 'image/*,.pdf',
    showUploadList: true,
    customRequest: async (options) => {
      const { file, onSuccess, onError } = options;
      const fileObj = file as unknown as File;
      const fileName = fileObj.name || 'unknown';

      // 显式检查 sessionId（防止 TypeScript 非空断言问题）
      if (!sessionId) {
        message.error('会话ID无效，请重新扫码');
        onError?.(new Error('会话ID无效'));
        return;
      }

      try {
        // 使用移动端专用上传端点（无需 JWT 认证）
        await scanApi.mobileUpload(fileObj, sessionId);
        setUploadedFiles([...uploadedFiles, fileName]);
        onSuccess?.({});
        message.success(`${fileName} 上传成功`);
      } catch (error) {
        onError?.(error as Error);
        message.error(`${fileName} 上传失败`);
      }
    },
  };

  return (
    <div
      style={{
        padding: 24,
        maxWidth: 600,
        margin: '0 auto',
        minHeight: '100vh',
        background: '#f5f5f5',
      }}
    >
      <Card>
        <Title level={4}>📱 扫描上传</Title>
        {uploadedFiles.length > 0 && (
          <Alert
            type="success"
            icon={<CheckCircleOutlined />}
            message={`已上传 ${uploadedFiles.length} 个文件`}
            style={{ marginBottom: 16 }}
          />
        )}
        <div style={{ display: 'flex', flexDirection: 'column', gap: 16 }}>
          <Upload {...uploadProps}>
            <Button icon={<UploadOutlined />} size="large" block>
              选择照片
            </Button>
          </Upload>
          <Button type="primary" icon={<CameraOutlined />} size="large" block>
            拍照上传
          </Button>
        </div>
      </Card>
    </div>
  );
};

export default MobileUploadPage;
