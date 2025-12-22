// Input: 浏览器 window 事件系统
// Output: 审计刷新触发与订阅函数
// Pos: 审计事件工具模块
// 一旦我被更新，务必更新我的开头注释，以及所属的文件夹的 md。

const EVENT_NAME = 'audit:refresh';

export const triggerAuditRefresh = () => {
  if (typeof window === 'undefined') return;
  window.dispatchEvent(new Event(EVENT_NAME));
};

export const subscribeAuditRefresh = (handler: () => void) => {
  if (typeof window === 'undefined') {
    return () => {};
  }
  window.addEventListener(EVENT_NAME, handler);
  return () => window.removeEventListener(EVENT_NAME, handler);
};
