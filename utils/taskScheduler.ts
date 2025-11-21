import { ScheduledTask, TaskExecutionHistory } from '../types';

// LocalStorage keys
const STORAGE_KEYS = {
  TASKS: 'nexusarchive_scheduled_tasks',
  HISTORY: 'nexusarchive_task_history',
};

// Load tasks from localStorage
export const loadTasksFromStorage = (): ScheduledTask[] => {
  try {
    const stored = localStorage.getItem(STORAGE_KEYS.TASKS);
    if (stored) {
      return JSON.parse(stored);
    }
  } catch (e) {
    console.error('Failed to load tasks from storage:', e);
  }
  return [];
};

// Save tasks to localStorage
export const saveTasksToStorage = (tasks: ScheduledTask[]) => {
  try {
    localStorage.setItem(STORAGE_KEYS.TASKS, JSON.stringify(tasks));
  } catch (e) {
    console.error('Failed to save tasks to storage:', e);
  }
};

// Load execution history from localStorage
export const loadHistoryFromStorage = (): TaskExecutionHistory[] => {
  try {
    const stored = localStorage.getItem(STORAGE_KEYS.HISTORY);
    if (stored) {
      return JSON.parse(stored);
    }
  } catch (e) {
    console.error('Failed to load history from storage:', e);
  }
  return [];
};

// Save execution history to localStorage
export const saveHistoryToStorage = (history: TaskExecutionHistory[]) => {
  try {
    // Keep only last 100 records
    const limited = history.slice(-100);
    localStorage.setItem(STORAGE_KEYS.HISTORY, JSON.stringify(limited));
  } catch (e) {
    console.error('Failed to save history to storage:', e);
  }
};

// Add execution history
export const addExecutionHistory = (execution: TaskExecutionHistory) => {
  const history = loadHistoryFromStorage();
  history.push(execution);
  saveHistoryToStorage(history);
};

// Calculate next run time based on schedule
export const calculateNextRun = (schedule: ScheduledTask['schedule']): string | null => {
  const now = new Date();
  const next = new Date(now);

  switch (schedule.type) {
    case 'daily':
      if (schedule.time) {
        const [hours, minutes] = schedule.time.split(':').map(Number);
        next.setHours(hours, minutes, 0, 0);
        if (next <= now) {
          next.setDate(next.getDate() + 1);
        }
        return next.toISOString();
      }
      break;
    case 'interval':
      if (schedule.interval) {
        next.setMinutes(next.getMinutes() + schedule.interval);
        return next.toISOString();
      }
      break;
    case 'weekly':
      if (schedule.days && schedule.days.length > 0 && schedule.time) {
        const [hours, minutes] = schedule.time.split(':').map(Number);
        const currentDay = now.getDay();
        const sortedDays = [...schedule.days].sort((a, b) => a - b);
        const nextDay = sortedDays.find(d => d > currentDay) || sortedDays[0];
        const daysUntilNext = nextDay > currentDay ? nextDay - currentDay : 7 - currentDay + nextDay;
        next.setDate(next.getDate() + daysUntilNext);
        next.setHours(hours, minutes, 0, 0);
        return next.toISOString();
      }
      break;
    case 'monthly':
      if (schedule.days && schedule.days.length > 0 && schedule.time) {
        const [hours, minutes] = schedule.time.split(':').map(Number);
        const currentDate = now.getDate();
        const sortedDays = [...schedule.days].sort((a, b) => a - b);
        const nextDay = sortedDays.find(d => d >= currentDate) || sortedDays[0];
        if (nextDay >= currentDate) {
          next.setDate(nextDay);
        } else {
          next.setMonth(next.getMonth() + 1);
          next.setDate(sortedDays[0]);
        }
        next.setHours(hours, minutes, 0, 0);
        return next.toISOString();
      }
      break;
  }
  return null;
};

// Check if task should run now
export const shouldRunNow = (task: ScheduledTask): boolean => {
  if (!task.enabled) return false;
  
  const now = new Date();
  const nextRun = task.nextRun ? new Date(task.nextRun) : null;
  
  if (!nextRun) return false;
  
  // Allow 1 minute tolerance
  const diff = now.getTime() - nextRun.getTime();
  return diff >= 0 && diff < 60000;
};

// Task scheduler class
class TaskScheduler {
  private intervals: Map<string, NodeJS.Timeout> = new Map();
  private callbacks: Map<string, (task: ScheduledTask) => Promise<void>> = new Map();

  // Register a task execution callback
  registerCallback(taskId: string, callback: (task: ScheduledTask) => Promise<void>) {
    this.callbacks.set(taskId, callback);
  }

  // Unregister a task execution callback
  unregisterCallback(taskId: string) {
    this.callbacks.delete(taskId);
  }

  // Start scheduling a task
  scheduleTask(task: ScheduledTask) {
    if (!task.enabled) return;

    // Clear existing interval if any
    this.unscheduleTask(task.id);

    // For interval type, use setInterval
    if (task.schedule.type === 'interval' && task.schedule.interval) {
      const intervalMs = task.schedule.interval * 60 * 1000; // Convert minutes to ms
      const interval = setInterval(async () => {
        const callback = this.callbacks.get(task.id);
        if (callback) {
          await callback(task);
        }
      }, intervalMs);
      this.intervals.set(task.id, interval);
      return;
    }

    // For other types, check periodically (every minute)
    const checkInterval = setInterval(async () => {
      const tasks = loadTasksFromStorage();
      const currentTask = tasks.find(t => t.id === task.id);
      if (currentTask && shouldRunNow(currentTask)) {
        const callback = this.callbacks.get(task.id);
        if (callback) {
          await callback(currentTask);
          // Update next run time
          const updatedTasks = tasks.map(t => {
            if (t.id === task.id) {
              return {
                ...t,
                lastRun: new Date().toISOString(),
                nextRun: calculateNextRun(t.schedule) || undefined,
                runCount: t.runCount + 1
              };
            }
            return t;
          });
          saveTasksToStorage(updatedTasks);
        }
      }
    }, 60000); // Check every minute
    this.intervals.set(task.id, checkInterval);
  }

  // Stop scheduling a task
  unscheduleTask(taskId: string) {
    const interval = this.intervals.get(taskId);
    if (interval) {
      clearInterval(interval);
      this.intervals.delete(taskId);
    }
  }

  // Start all enabled tasks
  startAll(tasks: ScheduledTask[]) {
    tasks.forEach(task => {
      if (task.enabled) {
        this.scheduleTask(task);
      }
    });
  }

  // Stop all tasks
  stopAll() {
    this.intervals.forEach(interval => clearInterval(interval));
    this.intervals.clear();
  }
}

// Singleton instance
export const taskScheduler = new TaskScheduler();

