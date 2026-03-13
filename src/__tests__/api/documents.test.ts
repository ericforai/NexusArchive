// Input: vitest、本地模块 api/documents、api/client
// Output: 测试用例与断言
// Pos: 前端单元测试
// 一旦我被更新，务必更新我的开头注释，以及所属的文件夹的 md。

import { afterEach, beforeEach, describe, expect, it } from 'vitest';

import { client } from '../../api/client';
import { documentsApi } from '../../api/documents';

describe('documentsApi', () => {
  let originalAdapter: any;

  beforeEach(() => {
    originalAdapter = client.defaults.adapter;
  });

  afterEach(() => {
    client.defaults.adapter = originalAdapter;
  });

  it('should request a document section by project and section id', async () => {
    let requestConfig: any;

    client.defaults.adapter = async (config) => {
      requestConfig = config;
      return {
        data: { data: { id: 'sec-1', projectId: 'project-1', title: '第一章' } },
        status: 200,
        statusText: 'OK',
        headers: {},
        config,
      };
    };

    const response = await documentsApi.getSection('project-1', 'sec-1');

    expect(requestConfig?.url).toBe('/documents/project-1/editor/sections/sec-1');
    expect(requestConfig?.method).toBe('get');
    expect(response.data.data.id).toBe('sec-1');
  });

  it('should update a document section', async () => {
    let requestConfig: any;

    client.defaults.adapter = async (config) => {
      requestConfig = config;
      return {
        data: { data: { id: 'sec-1', title: '更新后的章节' } },
        status: 200,
        statusText: 'OK',
        headers: {},
        config,
      };
    };

    await documentsApi.updateSection('project-1', 'sec-1', {
      title: '更新后的章节',
      content: '新内容',
      sortOrder: 2,
    });

    expect(requestConfig?.url).toBe('/documents/project-1/editor/sections/sec-1');
    expect(requestConfig?.method).toBe('put');
    expect(requestConfig?.data).toContain('更新后的章节');
    expect(requestConfig?.data).toContain('新内容');
  });

  it('should create assignment, lock, reminder, version, and rollback requests', async () => {
    const requests: Array<{ url?: string; method?: string }> = [];

    client.defaults.adapter = async (config) => {
      requests.push({ url: config.url, method: config.method });
      return {
        data: { data: { ok: true } },
        status: 200,
        statusText: 'OK',
        headers: {},
        config,
      };
    };

    await documentsApi.createAssignment('project-1', {
      sectionId: 'sec-1',
      assigneeId: 'user-2',
      assigneeName: '张三',
    });
    await documentsApi.createLock('project-1', {
      sectionId: 'sec-1',
      reason: '正在编辑',
      active: true,
    });
    await documentsApi.createReminder('project-1', {
      sectionId: 'sec-1',
      message: '请在今天完成',
      remindAt: '2026-03-11T18:00:00',
      recipientId: 'user-2',
      recipientName: '张三',
    });
    await documentsApi.createVersion('project-1', {
      versionName: 'v1',
      description: '初版',
    });
    await documentsApi.rollbackVersion('project-1', 'ver-1');

    expect(requests).toEqual([
      { url: '/documents/project-1/editor/assignments', method: 'post' },
      { url: '/documents/project-1/editor/locks', method: 'post' },
      { url: '/documents/project-1/editor/reminders', method: 'post' },
      { url: '/documents/project-1/versions', method: 'post' },
      { url: '/documents/project-1/versions/ver-1/rollback', method: 'post' },
    ]);
  });

  it('should list versions for a project', async () => {
    let requestConfig: any;

    client.defaults.adapter = async (config) => {
      requestConfig = config;
      return {
        data: { data: [{ id: 'ver-1', versionName: 'v1' }] },
        status: 200,
        statusText: 'OK',
        headers: {},
        config,
      };
    };

    const response = await documentsApi.listVersions('project-1');

    expect(requestConfig?.url).toBe('/documents/project-1/versions');
    expect(requestConfig?.method).toBe('get');
    expect(response.data.data[0].id).toBe('ver-1');
  });
});
