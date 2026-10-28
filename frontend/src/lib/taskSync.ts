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
      // 查询恢复：清除「查询异常」标记（查询异常 ≠ 任务失败，恢复后不再误提示）
      useTaskStore.getState().setConnectionError(null);
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

/**
 * 受理制作任务（R-04/R-07 的受理路径）：
 * - `createTask` 成功即视为受理成功：立即启动跟踪（避免此前空闲时新任务失联），并清空查询异常标记；
 * - 受理成功后的「任务列表刷新」失败 ≠ 提交失败：只走查询异常通道，等轮询恢复后新任务自然出现；
 * - 仅 `createTask` 本身失败才返回受理失败（调用方保留上传位供重试，不得清空）。
 */
export async function createAndTrackTask(
  templateId: string,
  assetIds: string[],
  userId: string
): Promise<{ accepted: true } | { accepted: false; error: string }> {
  try {
    await api.createTask(templateId, assetIds);
  } catch (e) {
    return { accepted: false, error: e instanceof Error ? e.message : "提交失败" };
  }
  taskTracker.start(userId);
  try {
    const list = await api.myTasks();
    if (useSessionStore.getState().user?.id === userId) {
      useTaskStore.getState().mergeTasks(list);
      useTaskStore.getState().setConnectionError(null);
    }
  } catch (e) {
    useTaskStore.getState().setConnectionError(e instanceof Error ? e.message : "任务列表暂时不可用");
  }
  return { accepted: true };
}
