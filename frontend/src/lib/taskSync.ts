"use client";

import { api } from "./api";
import { useSessionStore } from "./sessionStore";
import { useTaskStore } from "./taskStore";
import { TaskTracker } from "./taskTracker";
import type { TaskListItem } from "./types";

/**
 * 应用级轮询单例：把 TaskTracker 与 zustand store、会话 store 接线。
 * 页面挂载/卸载不启停轮询；仅在「切换用户」时重置，以满足 R-08 的订阅清理要求。
 */
function makeTracker() {
  return new TaskTracker({
    fetchMine: () => api.myTasks(),
    apply: (list: TaskListItem[], userId) => {
      const session = useSessionStore.getState();
      if (!session.user || session.user.id !== userId) return; // 旧用户响应不落地
      useTaskStore.getState().mergeTasks(list);
    },
    onError: (message) => {
      // 查询异常 ≠ 任务失败：只标记连接状态，任务信息原样保留
      useTaskStore.getState().setConnectionError(message);
    },
    intervalMs: 1500,
  });
}

export const taskTracker = makeTracker();

/** 应用启动：建立会话 → 恢复历史任务 → 需要时启动轮询（R-09 刷新恢复）。 */
export async function bootstrapSession() {
  try {
    const user = await api.session();
    useSessionStore.getState().setUser(user);
    const tasks = await api.myTasks();
    if (useSessionStore.getState().user?.id !== user.id) return; // 竞态保护
    useTaskStore.getState().mergeTasks(tasks);
    useTaskStore.getState().setConnectionError(null);
    if (tasks.some((t) => t.status === "WAITING" || t.status === "PROCESSING")) {
      taskTracker.start(user.id);
    }
  } catch (e) {
    useSessionStore.getState().setBootError(e instanceof Error ? e.message : "会话建立失败");
  }
}

/** 切换身份：重置会话与任务状态，重建轮询订阅。 */
export async function switchIdentity() {
  taskTracker.stop();
  useTaskStore.getState().clear();
  const user = await api.switchIdentity();
  useSessionStore.getState().setUser(user);
  taskTracker.start(user.id);
  return user;
}
