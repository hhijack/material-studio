import { describe, expect, it, vi } from "vitest";
import { TaskTracker, type TrackerDeps } from "@/lib/taskTracker";
import type { TaskListItem } from "@/lib/types";

function task(id: string, status: TaskListItem["status"]): TaskListItem {
  return {
    id,
    templateId: "t",
    templateTitle: "T",
    templateCoverUrl: "/c.svg",
    status,
    currentStage: status === "PROCESSING" ? "MATERIAL_CHECK" : null,
    failStage: status === "FAILED" ? "SIMULATE_GENERATE" : null,
    failReason: status === "FAILED" ? "x" : null,
    resultUrl: status === "SUCCESS" ? "/r.png" : null,
    createdAt: "2026-10-28T12:00:00",
    startedAt: null,
    finishedAt: null,
  };
}

interface Harness {
  deps: TrackerDeps;
  /** 当前仍然有效的排定轮询数（stop 取消后不再计入） */
  activeSchedules: () => number;
  fetchMock: ReturnType<typeof vi.fn>;
  applyMock: ReturnType<typeof vi.fn>;
  errorMock: ReturnType<typeof vi.fn>;
  idleMock: ReturnType<typeof vi.fn>;
  respondWith: (...results: Array<() => Promise<TaskListItem[]>>) => void;
  /** 触发一次已排定的轮询并等待其微任务完成 */
  runScheduledTick: () => Promise<void>;
}

function harness(): Harness {
  const entries: Array<{ fn: () => void; cancelled: boolean }> = [];
  const fetchMock = vi.fn();
  const applyMock = vi.fn();
  const errorMock = vi.fn();
  const idleMock = vi.fn();
  const responses: Array<() => Promise<TaskListItem[]>> = [];
  fetchMock.mockImplementation(() => {
    const next = responses.shift();
    return next ? next() : Promise.reject(new Error("no scripted response"));
  });
  const deps: TrackerDeps = {
    fetchMine: fetchMock,
    apply: applyMock,
    onError: errorMock,
    onIdle: idleMock,
    schedule: (fn) => {
      const entry = { fn, cancelled: false };
      entries.push(entry);
      return () => {
        entry.cancelled = true;
      };
    },
  };
  return {
    deps,
    activeSchedules: () => entries.filter((e) => !e.cancelled).length,
    fetchMock,
    applyMock,
    errorMock,
    idleMock,
    respondWith: (...results) => responses.push(...results),
    runScheduledTick: async () => {
      const entry = entries.find((e) => !e.cancelled);
      if (!entry) throw new Error("没有有效的已排定轮询");
      entry.fn();
      await flush();
    },
  };
}

const flush = async () => {
  await Promise.resolve();
  await Promise.resolve();
  await Promise.resolve();
};

describe("taskTracker：查询生命周期（R-08）", () => {
  it("没有待处理任务时不进入轮询（结束即停止跟踪）", async () => {
    const h = harness();
    h.respondWith(() => Promise.resolve([task("a", "SUCCESS")]));
    const tracker = new TaskTracker(h.deps);
    tracker.start("u1");
    await flush();

    expect(h.applyMock).toHaveBeenCalledTimes(1);
    expect(h.idleMock).toHaveBeenCalledTimes(1);
    expect(h.activeSchedules()).toBe(0); // 没有继续排定轮询
    expect(tracker.running).toBe(false);
  });

  it("有未完成任务时持续轮询，全部结束后自动停止", async () => {
    const h = harness();
    h.respondWith(
      () => Promise.resolve([task("a", "PROCESSING"), task("b", "WAITING")]),
      () => Promise.resolve([task("a", "PROCESSING"), task("b", "FAILED")]),
      () => Promise.resolve([task("a", "SUCCESS"), task("b", "FAILED")])
    );
    const tracker = new TaskTracker(h.deps);
    tracker.start("u1");
    await flush();
    expect(h.applyMock).toHaveBeenCalledTimes(1);
    expect(h.activeSchedules()).toBe(1);

    await h.runScheduledTick(); // 第 2 次查询：仍有 PROCESSING
    expect(h.applyMock).toHaveBeenCalledTimes(2);
    expect(h.activeSchedules()).toBe(1);

    await h.runScheduledTick(); // 第 3 次查询：全部结束
    expect(h.applyMock).toHaveBeenCalledTimes(3);
    expect(h.activeSchedules()).toBe(0);
    expect(h.idleMock).toHaveBeenCalledTimes(1);
    expect(tracker.running).toBe(false);
  });

  it("单次查询异常不清空任务、不视为任务失败，并保持自动重试", async () => {
    const h = harness();
    h.respondWith(
      () => Promise.resolve([task("a", "PROCESSING")]),
      () => Promise.reject(new Error("网络错误")),
      () => Promise.resolve([task("a", "SUCCESS")])
    );
    const tracker = new TaskTracker(h.deps);
    tracker.start("u1");
    await flush();
    expect(h.applyMock).toHaveBeenCalledTimes(1);

    await h.runScheduledTick(); // 这次查询失败
    expect(h.errorMock).toHaveBeenCalledWith("网络错误");
    expect(h.applyMock).toHaveBeenCalledTimes(1); // 没有用失败结果覆盖任务
    expect(h.idleMock).not.toHaveBeenCalled(); // 不会误判为空闲
    expect(h.activeSchedules()).toBe(1); // 仍保持自动重试

    tracker.retry("u1"); // 手动/自动的下一次查询成功
    await flush();
    expect(h.applyMock).toHaveBeenCalledTimes(2);
    expect(h.errorMock).toHaveBeenCalledTimes(1); // 恢复后不再有新错误
    expect(h.activeSchedules()).toBe(0); // 任务结束，轮询停止
  });

  it("切换用户后，旧用户的在途响应不会落地（避免串数据）", async () => {
    const h = harness();
    let resolveOld!: (v: TaskListItem[]) => void;
    h.respondWith(() => new Promise((resolve) => (resolveOld = resolve)));

    const tracker = new TaskTracker(h.deps);
    tracker.start("user-A");
    tracker.start("user-B"); // 立即切换：旧请求作废
    resolveOld([task("old-task", "PROCESSING")]);
    await flush();

    expect(h.applyMock).not.toHaveBeenCalled(); // 旧用户数据未应用
    tracker.stop();
  });

  it("stop 后在途响应同样不落地", async () => {
    const h = harness();
    let resolve!: (v: TaskListItem[]) => void;
    h.respondWith(() => new Promise((r) => (resolve = r)));
    const tracker = new TaskTracker(h.deps);
    tracker.start("u1");
    tracker.stop();
    resolve([task("a", "PROCESSING")]);
    await flush();
    expect(h.applyMock).not.toHaveBeenCalled();
  });

  it("手动 retry 触发一次即时查询（查询异常时的恢复途径）", async () => {
    const h = harness();
    h.respondWith(
      () => Promise.reject(new Error("查询失败")),
      () => Promise.resolve([task("a", "SUCCESS")])
    );
    const tracker = new TaskTracker(h.deps);
    tracker.start("u1"); // 第一次查询失败 → onError + 保持重试
    await flush();
    expect(h.errorMock).toHaveBeenCalledTimes(1);
    expect(h.applyMock).not.toHaveBeenCalled();

    tracker.retry("u1"); // 手动恢复
    await flush();
    expect(h.applyMock).toHaveBeenCalledTimes(1);
    expect(h.errorMock).toHaveBeenCalledTimes(1);
  });

  it("上一轮轮询未返回时跳过新 tick（in-flight 互斥，防请求堆积）", async () => {
    const h = harness();
    let resolveSlow!: (v: TaskListItem[]) => void;
    h.respondWith(() => new Promise((resolve) => (resolveSlow = resolve)));
    const tracker = new TaskTracker(h.deps);
    tracker.start("u1"); // 第 1 次查询发出（慢）
    await flush();
    expect(h.fetchMock).toHaveBeenCalledTimes(1);

    await h.runScheduledTick(); // 上一轮未返回 → 跳过，不发新请求
    expect(h.fetchMock).toHaveBeenCalledTimes(1);
    expect(h.errorMock).not.toHaveBeenCalled();

    resolveSlow([task("a", "SUCCESS")]);
    await flush();
    expect(h.applyMock).toHaveBeenCalledTimes(1);
    expect(h.activeSchedules()).toBe(0); // 全部终态 → 停止轮询
  });
});
