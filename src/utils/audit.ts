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
