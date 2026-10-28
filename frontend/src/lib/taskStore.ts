"use client";

import { create } from "zustand";
import type { TaskListItem } from "./types";
import { isActive } from "./types";

/**
 * 任务状态仓库：前端异步状态管理的核心。
 * - mergeTasks 只做增量合并：查询异常时保留已有任务信息（查询失败 ≠ 任务失败）；
 * - 每个任务按 id 独立更新，互不串扰；
 * - connectionError 只代表「查询通道异常」，绝不改动任务自身状态。
 */
export interface TaskStore {
  tasks: Record<string, TaskListItem>;
  /** 任务 id 顺序（按创建时间倒序，由后端排序保证） */
  order: string[];
  connectionError: string | null;
  submitting: boolean;
  mergeTasks: (list: TaskListItem[]) => void;
  setConnectionError: (message: string | null) => void;
  setSubmitting: (value: boolean) => void;
  clear: () => void;
}

export const useTaskStore = create<TaskStore>((set) => ({
  tasks: {},
  order: [],
  connectionError: null,
  submitting: false,

  mergeTasks: (list) =>
    set((state) => {
      const tasks = { ...state.tasks };
      for (const task of list) {
        tasks[task.id] = task; // 整条覆盖：以服务端记录为准
      }
      const order = [...state.order];
      for (const task of list) {
        if (!order.includes(task.id)) order.unshift(task.id); // 新任务放最前
      }
      return { tasks, order };
    }),

  setConnectionError: (message) => set({ connectionError: message }),
  setSubmitting: (value) => set({ submitting: value }),

  clear: () => set({ tasks: {}, order: [], connectionError: null, submitting: false }),
}));

export function selectActiveTaskIds(state: TaskStore): string[] {
  return state.order.filter((id) => {
    const task = state.tasks[id];
    return task && isActive(task.status);
  });
}
