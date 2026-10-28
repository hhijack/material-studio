import { beforeEach, describe, expect, it } from "vitest";
import { useTaskStore } from "@/lib/taskStore";
import type { TaskListItem } from "@/lib/types";

function task(overrides: Partial<TaskListItem> & { id: string }): TaskListItem {
  return {
    templateId: "travel-postcard",
    templateTitle: "旅行明信片",
    templateCoverUrl: "/demo/covers/travel-postcard.svg",
    status: "PROCESSING",
    currentStage: "MATERIAL_CHECK",
    failStage: null,
    failReason: null,
    resultUrl: null,
    createdAt: "2026-10-28T12:00:00",
    startedAt: null,
    finishedAt: null,
    ...overrides,
  };
}

describe("taskStore：多任务与查询异常（R-07/R-08）", () => {
  beforeEach(() => {
    useTaskStore.getState().clear();
  });

  it("多条任务按 id 独立合并，互不串扰", () => {
    const { mergeTasks } = useTaskStore.getState();
    mergeTasks([task({ id: "a", status: "PROCESSING", currentStage: "MATERIAL_CHECK" })]);
    mergeTasks([task({ id: "b", status: "WAITING", currentStage: null })]);

    let state = useTaskStore.getState();
    expect(state.tasks.a.status).toBe("PROCESSING");
    expect(state.tasks.b.status).toBe("WAITING");

    // b 失败不影响 a
    mergeTasks([task({ id: "b", status: "FAILED", failStage: "SIMULATE_GENERATE", failReason: "注入失败" })]);
    state = useTaskStore.getState();
    expect(state.tasks.a.status).toBe("PROCESSING");
    expect(state.tasks.b.status).toBe("FAILED");
    expect(state.order).toEqual(["b", "a"]); // 新任务在列表最前
  });

  it("查询异常仅记录 connectionError，已保存的任务信息原样保留", () => {
    const { mergeTasks, setConnectionError } = useTaskStore.getState();
    mergeTasks([task({ id: "a", status: "SUCCESS", resultUrl: "/api/files/results/a.png" })]);

    setConnectionError("无法连接服务器");

    const state = useTaskStore.getState();
    expect(state.connectionError).toBe("无法连接服务器");
    // 任务未被标记为失败，结果预览仍在
    expect(state.tasks.a.status).toBe("SUCCESS");
    expect(state.tasks.a.resultUrl).toBe("/api/files/results/a.png");
  });

  it("查询恢复后清除 connectionError", () => {
    const { setConnectionError } = useTaskStore.getState();
    setConnectionError("网络异常");
    setConnectionError(null);
    expect(useTaskStore.getState().connectionError).toBeNull();
  });

  it("clear 清空全部任务与错误态（切换用户用）", () => {
    const { mergeTasks, setConnectionError, clear } = useTaskStore.getState();
    mergeTasks([task({ id: "a" })]);
    setConnectionError("x");
    clear();
    const state = useTaskStore.getState();
    expect(state.order).toEqual([]);
    expect(Object.keys(state.tasks)).toHaveLength(0);
    expect(state.connectionError).toBeNull();
  });
});
