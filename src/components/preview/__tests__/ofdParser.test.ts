// Input: vitest、Node fs、本地 OFD 解析器
// Output: OFD 解析测试
// Pos: 预览组件测试

import { readFile } from 'node:fs/promises';
import { resolve } from 'node:path';
import { describe, expect, it } from 'vitest';
import { parseOfdDocument } from '../ofdParser';

async function readFixture(relativePath: string): Promise<ArrayBuffer> {
  const file = await readFile(resolve(process.cwd(), relativePath));
  return file.buffer.slice(file.byteOffset, file.byteOffset + file.byteLength);
}

describe('parseOfdDocument', () => {
  it('parses text-based ofd pages from fixture', async () => {
    const buffer = await readFixture('nexusarchive-java/data/temp/uploads/test_upload.ofd');

    const document = await parseOfdDocument(buffer);

    expect(document.pages).toHaveLength(1);
    expect(document.pages[0].texts).toHaveLength(1);
    expect(document.pages[0].texts[0].text).toContain('PDF Content Placeholder');
  });

  it('parses image-based ofd pages from fixture', async () => {
    const buffer = await readFixture('nexusarchive-java/data/temp/uploads/dzfp_25312000000361691112_上海市徐汇区晓旻餐饮店_20251107223428.ofd');

    const document = await parseOfdDocument(buffer);

    expect(document.pages).toHaveLength(1);
    expect(document.pages[0].images).toHaveLength(1);
    expect(document.pages[0].images[0].src).toMatch(/^data:image\/png;base64,/);
  });

  it('merges template page content for real invoice ofd files', async () => {
    const buffer = await readFixture(
      'nexusarchive-java/data/archives/original-vouchers/1970e13f-7653-4740-9aaf-d0e873b91b7e/5fe0368e-7f42-42da-9aa6-97009bd889dc.ofd',
    );

    const document = await parseOfdDocument(buffer);

    expect(document.pages).toHaveLength(1);
    expect(document.pages[0].height).toBe(154);
    expect(document.pages[0].texts.some((text) => text.text.includes('电子发票（普通发票）'))).toBe(true);
    expect(document.pages[0].texts.some((text) => text.text.includes('发票号码：'))).toBe(true);
  });
});
