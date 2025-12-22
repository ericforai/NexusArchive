// Input: 类型定义与 safeStorage
// Output: 通知规则与通知读写工具函数
// Pos: 前端通知服务工具
// 一旦我被更新，务必更新我的开头注释，以及所属的文件夹的 md。

import { EnhancedNotification, NotificationRule } from '../types';
import { safeStorage } from './storage';

// LocalStorage keys
const STORAGE_KEYS = {
  NOTIFICATIONS: 'nexusarchive_notifications',
  NOTIFICATION_RULES: 'nexusarchive_notification_rules',
};

// Load notifications from localStorage
export const loadNotificationsFromStorage = (): EnhancedNotification[] => {
  try {
    const stored = safeStorage.getItem(STORAGE_KEYS.NOTIFICATIONS);
    if (stored) {
      return JSON.parse(stored);
    }
  } catch (e) {
    console.error('Failed to load notifications from storage:', e);
  }
  return [];
};

// Save notifications to localStorage
export const saveNotificationsToStorage = (notifications: EnhancedNotification[]) => {
  try {
    // Keep only last 200 notifications
    const limited = notifications.slice(-200);
    safeStorage.setItem(STORAGE_KEYS.NOTIFICATIONS, JSON.stringify(limited));
  } catch (e) {
    console.error('Failed to save notifications to storage:', e);
  }
};

// Load notification rules from localStorage
export const loadNotificationRulesFromStorage = (): NotificationRule[] => {
  try {
    const stored = safeStorage.getItem(STORAGE_KEYS.NOTIFICATION_RULES);
    if (stored) {
      return JSON.parse(stored);
    }
  } catch (e) {
    console.error('Failed to load notification rules from storage:', e);
  }
  return [];
};

// Save notification rules to localStorage
export const saveNotificationRulesToStorage = (rules: NotificationRule[]) => {
  try {
    safeStorage.setItem(STORAGE_KEYS.NOTIFICATION_RULES, JSON.stringify(rules));
  } catch (e) {
    console.error('Failed to save notification rules to storage:', e);
  }
};

// Create a notification
export const createNotification = (
  title: string,
  type: 'info' | 'warning' | 'success' = 'info',
  category: EnhancedNotification['category'] = 'system',
  metadata?: Record<string, any>
): EnhancedNotification => {
  return {
    id: `notif_${Date.now()}_${Math.random().toString(36).substr(2, 9)}`,
    title,
    time: '刚刚',
    type,
    read: false,
    category,
    metadata
  };
};

// Add a notification
export const addNotification = (notification: EnhancedNotification) => {
  const notifications = loadNotificationsFromStorage();
  notifications.push(notification);
  saveNotificationsToStorage(notifications);
  return notification;
};

// Mark notification as read
export const markNotificationAsRead = (id: string) => {
  const notifications = loadNotificationsFromStorage();
  const updated = notifications.map(n => n.id === id ? { ...n, read: true } : n);
  saveNotificationsToStorage(updated);
};

// Mark all notifications as read
export const markAllNotificationsAsRead = () => {
  const notifications = loadNotificationsFromStorage();
  const updated = notifications.map(n => ({ ...n, read: true }));
  saveNotificationsToStorage(updated);
};

// Delete a notification
export const deleteNotification = (id: string) => {
  const notifications = loadNotificationsFromStorage();
  const updated = notifications.filter(n => n.id !== id);
  saveNotificationsToStorage(updated);
};

// Get unread count
export const getUnreadCount = (): number => {
  const notifications = loadNotificationsFromStorage();
  return notifications.filter(n => !n.read).length;
};

// Check notification rules and create notifications
export const checkNotificationRules = (
  trigger: NotificationRule['trigger'],
  data: Record<string, any>
) => {
  const rules = loadNotificationRulesFromStorage();
  const activeRules = rules.filter(r => r.enabled && r.trigger === trigger);

  activeRules.forEach(rule => {
    // Check conditions
    let shouldNotify = true;
    if (rule.conditions) {
      // Simple condition checking (can be extended)
      Object.keys(rule.conditions).forEach(key => {
        if (data[key] !== rule.conditions[key]) {
          shouldNotify = false;
        }
      });
    }

    if (shouldNotify) {
      let title = '';
      let type: 'info' | 'warning' | 'success' = 'info';
      let category: EnhancedNotification['category'] = 'system';

      switch (trigger) {
        case 'task_complete':
          title = `定时任务执行完成: ${data.taskName || '未知任务'}`;
          if (data.matchedCount !== undefined) {
            title += `，匹配 ${data.matchedCount} 条凭证`;
          }
          type = 'success';
          category = 'task';
          break;
        case 'task_failed':
          title = `定时任务执行失败: ${data.taskName || '未知任务'}`;
          if (data.errorMessage) {
            title += ` - ${data.errorMessage}`;
          }
          type = 'warning';
          category = 'task';
          break;
        case 'match_threshold':
          title = `匹配任务完成: 共关联 ${data.matchedCount || 0} 条凭证`;
          type = 'success';
          category = 'match';
          break;
        case 'compliance_alert':
          title = `四性检测预警: ${data.message || '检测到异常'}`;
          type = 'warning';
          category = 'compliance';
          break;
        default:
          title = rule.template || '系统通知';
      }

      const notification = createNotification(title, type, category, data);
      addNotification(notification);
    }
  });
};

// Notification service class
class NotificationService {
  private listeners: Set<(notification: EnhancedNotification) => void> = new Set();

  // Subscribe to new notifications
  subscribe(callback: (notification: EnhancedNotification) => void) {
    this.listeners.add(callback);
    return () => this.listeners.delete(callback);
  }

  // Notify all listeners
  notify(notification: EnhancedNotification) {
    this.listeners.forEach(listener => listener(notification));
  }

  // Send notification (creates and notifies)
  send(
    title: string,
    type: 'info' | 'warning' | 'success' = 'info',
    category: EnhancedNotification['category'] = 'system',
    metadata?: Record<string, any>
  ) {
    const notification = createNotification(title, type, category, metadata);
    addNotification(notification);
    this.notify(notification);
    return notification;
  }
}

// Singleton instance
export const notificationService = new NotificationService();

