// Input: ArrayBuffer、DOMParser、DecompressionStream
// Output: 轻量 OFD 解析器
// Pos: 通用复用组件 - OFD 解析层

export interface OfdTextObject {
  id: string;
  text: string;
  x: number;
  y: number;
  width: number;
  height: number;
  fontSize: number;
  fontFamily?: string;
}

export interface OfdImageObject {
  id: string;
  src: string;
  x: number;
  y: number;
  width: number;
  height: number;
}

export interface OfdPage {
  id: string;
  width: number;
  height: number;
  texts: OfdTextObject[];
  images: OfdImageObject[];
}

export interface ParsedOfdDocument {
  pages: OfdPage[];
}

interface ZipEntry {
  name: string;
  compressionMethod: number;
  compressedSize: number;
  uncompressedSize: number;
  dataOffset: number;
}

interface ResourceMapEntry {
  mediaFile: string;
  mimeType: string;
}

const ZIP_EOCD_SIGNATURE = 0x06054b50;
const ZIP_CENTRAL_HEADER_SIGNATURE = 0x02014b50;
const ZIP_LOCAL_HEADER_SIGNATURE = 0x04034b50;

export async function parseOfdDocument(buffer: ArrayBuffer): Promise<ParsedOfdDocument> {
  const entries = readZipEntries(buffer);
  const entryMap = new Map(entries.map((entry) => [normalizePath(entry.name), entry]));

  const ofdDocument = parseXml(await readEntryText(buffer, entryMap, 'OFD.xml'));
  const docRoot = getFirstText(ofdDocument, 'DocRoot');
  if (!docRoot) {
    throw new Error('OFD 缺少 DocRoot');
  }

  const normalizedDocRoot = normalizePath(docRoot);
  const documentXml = parseXml(await readEntryText(buffer, entryMap, normalizedDocRoot));
  const documentDir = dirname(normalizedDocRoot);
  const commonData = getFirstElement(documentXml, 'CommonData');

  const pageArea = commonData ? getFirstElement(commonData, 'PageArea') : null;
  const physicalBox = pageArea ? parseBox(getFirstText(pageArea, 'PhysicalBox')) : null;
  const applicationBox = pageArea ? parseBox(getFirstText(pageArea, 'ApplicationBox')) : null;
  const pageBox = applicationBox || physicalBox || { x: 0, y: 0, width: 210, height: 297 };

  const fontMap = new Map<string, string>();
  const resourceMap = new Map<string, ResourceMapEntry>();

  if (commonData) {
    const publicResPath = getFirstText(commonData, 'PublicRes');
    if (publicResPath) {
      await collectResources({
        buffer,
        entryMap,
        resourceMap,
        fontMap,
        resourceDocPath: resolvePath(documentDir, publicResPath),
      });
    }

    const documentResPath = getFirstText(commonData, 'DocumentRes');
    if (documentResPath) {
      await collectResources({
        buffer,
        entryMap,
        resourceMap,
        fontMap,
        resourceDocPath: resolvePath(documentDir, documentResPath),
      });
    }
  }

  const pageElements = getElementsByLocalName(documentXml, 'Page');
  const pages = await Promise.all(pageElements.map(async (pageElement) => {
    const pageId = pageElement.getAttribute('ID') || `page-${Math.random().toString(16).slice(2)}`;
    const pagePath = pageElement.getAttribute('BaseLoc');
    if (!pagePath) {
      throw new Error(`OFD 页面 ${pageId} 缺少 BaseLoc`);
    }

    const pageXmlPath = resolvePath(documentDir, pagePath);
    const pageXml = parseXml(await readEntryText(buffer, entryMap, pageXmlPath));
    const texts = collectTextObjects(pageXml, fontMap);
    const images = await collectImageObjects({
      buffer,
      entryMap,
      pageXml,
      pagePath: pageXmlPath,
      resourceMap,
    });

    return {
      id: pageId,
      width: pageBox.width,
      height: pageBox.height,
      texts,
      images,
    } satisfies OfdPage;
  }));

  return { pages };
}

async function collectResources(params: {
  buffer: ArrayBuffer;
  entryMap: Map<string, ZipEntry>;
  resourceMap: Map<string, ResourceMapEntry>;
  fontMap: Map<string, string>;
  resourceDocPath: string;
}) {
  const { buffer, entryMap, resourceMap, fontMap, resourceDocPath } = params;
  const normalizedPath = normalizePath(resourceDocPath);
  if (!entryMap.has(normalizedPath)) {
    return;
  }

  const resourceXml = parseXml(await readEntryText(buffer, entryMap, normalizedPath));
  const resourceDir = dirname(normalizedPath);
  const baseLoc = getDocumentRoot(resourceXml)?.getAttribute('BaseLoc') || '';
  const resourceBaseDir = resolvePath(resourceDir, baseLoc || '.');

  getElementsByLocalName(resourceXml, 'Font').forEach((fontElement) => {
    const id = fontElement.getAttribute('ID');
    if (!id) return;
    fontMap.set(
      id,
      fontElement.getAttribute('FamilyName')
      || fontElement.getAttribute('FontName')
      || 'sans-serif',
    );
  });

  getElementsByLocalName(resourceXml, 'MultiMedia').forEach((mediaElement) => {
    const id = mediaElement.getAttribute('ID');
    if (!id) return;

    const mediaFile = getFirstText(mediaElement, 'MediaFile');
    if (!mediaFile) return;

    resourceMap.set(id, {
      mediaFile: resolvePath(resourceBaseDir, mediaFile),
      mimeType: inferMimeType(mediaFile, mediaElement.getAttribute('Format') || undefined),
    });
  });
}

function collectTextObjects(pageXml: XMLDocument, fontMap: Map<string, string>): OfdTextObject[] {
  return getElementsByLocalName(pageXml, 'TextObject').map((textElement) => {
    const boundary = parseBox(textElement.getAttribute('Boundary'));
    const textCodes = getElementsByLocalName(textElement, 'TextCode');
    const text = textCodes.map((node) => node.textContent?.trim() || '').join('');
    const fontId = textElement.getAttribute('Font') || '';
    const fontSize = Number(textElement.getAttribute('Size') || '3') || 3;

    return {
      id: textElement.getAttribute('ID') || `text-${Math.random().toString(16).slice(2)}`,
      text,
      x: boundary.x,
      y: boundary.y,
      width: boundary.width,
      height: boundary.height,
      fontSize,
      fontFamily: fontMap.get(fontId),
    };
  }).filter((item) => item.text);
}

async function collectImageObjects(params: {
  buffer: ArrayBuffer;
  entryMap: Map<string, ZipEntry>;
  pageXml: XMLDocument;
  pagePath: string;
  resourceMap: Map<string, ResourceMapEntry>;
}): Promise<OfdImageObject[]> {
  const { buffer, entryMap, pageXml, resourceMap } = params;

  return Promise.all(getElementsByLocalName(pageXml, 'ImageObject').map(async (imageElement) => {
    const resourceId = imageElement.getAttribute('ResourceID');
    if (!resourceId) {
      throw new Error('OFD 图片对象缺少 ResourceID');
    }

    const resource = resourceMap.get(resourceId);
    if (!resource) {
      throw new Error(`OFD 图片资源不存在: ${resourceId}`);
    }

    const bytes = await readEntryBytes(buffer, entryMap, resource.mediaFile);
    const boundary = parseBox(imageElement.getAttribute('Boundary'));

    return {
      id: imageElement.getAttribute('ID') || `image-${Math.random().toString(16).slice(2)}`,
      src: toDataUrl(bytes, resource.mimeType),
      x: boundary.x,
      y: boundary.y,
      width: boundary.width,
      height: boundary.height,
    } satisfies OfdImageObject;
  }));
}

function readZipEntries(buffer: ArrayBuffer): ZipEntry[] {
  const view = new DataView(buffer);
  const eocdOffset = findEndOfCentralDirectory(view);
  const totalEntries = view.getUint16(eocdOffset + 10, true);
  const centralDirectoryOffset = view.getUint32(eocdOffset + 16, true);

  const decoder = new TextDecoder();
  const entries: ZipEntry[] = [];
  let offset = centralDirectoryOffset;

  for (let index = 0; index < totalEntries; index += 1) {
    const signature = view.getUint32(offset, true);
    if (signature !== ZIP_CENTRAL_HEADER_SIGNATURE) {
      throw new Error('OFD Zip 中央目录损坏');
    }

    const compressionMethod = view.getUint16(offset + 10, true);
    const compressedSize = view.getUint32(offset + 20, true);
    const uncompressedSize = view.getUint32(offset + 24, true);
    const nameLength = view.getUint16(offset + 28, true);
    const extraLength = view.getUint16(offset + 30, true);
    const commentLength = view.getUint16(offset + 32, true);
    const localHeaderOffset = view.getUint32(offset + 42, true);
    const nameBytes = new Uint8Array(buffer, offset + 46, nameLength);
    const name = decoder.decode(nameBytes);

    if (view.getUint32(localHeaderOffset, true) !== ZIP_LOCAL_HEADER_SIGNATURE) {
      throw new Error('OFD Zip 本地文件头损坏');
    }

    const localNameLength = view.getUint16(localHeaderOffset + 26, true);
    const localExtraLength = view.getUint16(localHeaderOffset + 28, true);
    const dataOffset = localHeaderOffset + 30 + localNameLength + localExtraLength;

    entries.push({
      name,
      compressionMethod,
      compressedSize,
      uncompressedSize,
      dataOffset,
    });

    offset += 46 + nameLength + extraLength + commentLength;
  }

  return entries;
}

function findEndOfCentralDirectory(view: DataView): number {
  const minOffset = Math.max(0, view.byteLength - 65557);

  for (let offset = view.byteLength - 22; offset >= minOffset; offset -= 1) {
    if (view.getUint32(offset, true) === ZIP_EOCD_SIGNATURE) {
      return offset;
    }
  }

  throw new Error('无效的 OFD Zip 文件');
}

async function readEntryBytes(
  buffer: ArrayBuffer,
  entryMap: Map<string, ZipEntry>,
  entryPath: string,
): Promise<Uint8Array> {
  const normalizedPath = normalizePath(entryPath);
  const entry = entryMap.get(normalizedPath);
  if (!entry) {
    throw new Error(`OFD 条目不存在: ${normalizedPath}`);
  }

  const compressed = new Uint8Array(buffer.slice(entry.dataOffset, entry.dataOffset + entry.compressedSize));

  if (entry.compressionMethod === 0) {
    return compressed;
  }

  if (entry.compressionMethod !== 8) {
    throw new Error(`暂不支持的 OFD 压缩方式: ${entry.compressionMethod}`);
  }

  if (typeof DecompressionStream === 'undefined') {
    throw new Error('当前浏览器不支持 OFD 在线解压');
  }

  const stream = new Response(compressed)
    .body
    ?.pipeThrough(new DecompressionStream('deflate-raw'));

  if (!stream) {
    throw new Error('OFD 解压失败');
  }

  const decompressed = new Uint8Array(await new Response(stream).arrayBuffer());
  if (entry.uncompressedSize > 0 && decompressed.byteLength !== entry.uncompressedSize) {
    return decompressed;
  }
  return decompressed;
}

async function readEntryText(
  buffer: ArrayBuffer,
  entryMap: Map<string, ZipEntry>,
  entryPath: string,
): Promise<string> {
  const bytes = await readEntryBytes(buffer, entryMap, entryPath);
  return new TextDecoder().decode(bytes);
}

function parseXml(xmlString: string): XMLDocument {
  const xml = new DOMParser().parseFromString(xmlString, 'application/xml');
  const parserError = xml.querySelector('parsererror');
  if (parserError) {
    throw new Error(`OFD XML 解析失败: ${parserError.textContent || ''}`);
  }
  return xml;
}

function getDocumentRoot(xml: XMLDocument): Element | null {
  return xml.documentElement || null;
}

function getElementsByLocalName(node: Document | Element, localName: string): Element[] {
  return Array.from(node.querySelectorAll('*')).filter(
    (element) => element.localName === localName,
  );
}

function getFirstElement(node: Document | Element, localName: string): Element | null {
  return getElementsByLocalName(node, localName)[0] || null;
}

function getFirstText(node: Document | Element, localName: string): string | null {
  return getFirstElement(node, localName)?.textContent?.trim() || null;
}

function parseBox(value: string | null): { x: number; y: number; width: number; height: number } {
  const [x = 0, y = 0, width = 0, height = 0] = (value || '')
    .trim()
    .split(/\s+/)
    .map((item) => Number(item));

  return { x, y, width, height };
}

function inferMimeType(fileName: string, format?: string): string {
  const normalized = (format || fileName.split('.').pop() || '').toLowerCase();
  if (normalized === 'png') return 'image/png';
  if (normalized === 'jpg' || normalized === 'jpeg') return 'image/jpeg';
  if (normalized === 'gif') return 'image/gif';
  if (normalized === 'webp') return 'image/webp';
  return 'application/octet-stream';
}

function toDataUrl(bytes: Uint8Array, mimeType: string): string {
  let binary = '';
  const chunkSize = 0x8000;

  for (let index = 0; index < bytes.length; index += chunkSize) {
    const chunk = bytes.subarray(index, index + chunkSize);
    binary += String.fromCharCode(...chunk);
  }

  return `data:${mimeType};base64,${btoa(binary)}`;
}

function normalizePath(path: string): string {
  return path.replace(/\\/g, '/').replace(/^\/+/, '');
}

function dirname(path: string): string {
  const normalized = normalizePath(path);
  const lastSlash = normalized.lastIndexOf('/');
  return lastSlash >= 0 ? normalized.slice(0, lastSlash) : '';
}

function resolvePath(basePath: string, relativePath: string): string {
  const normalizedRelative = normalizePath(relativePath);
  if (!basePath) {
    return normalizedRelative;
  }

  const parts = `${basePath}/${normalizedRelative}`
    .split('/')
    .filter(Boolean);
  const resolved: string[] = [];

  parts.forEach((part) => {
    if (part === '.') return;
    if (part === '..') {
      resolved.pop();
      return;
    }
    resolved.push(part);
  });

  return resolved.join('/');
}
