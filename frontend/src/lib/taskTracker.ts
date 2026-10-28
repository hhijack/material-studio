"use client";

import type { TaskListItem } from "./types";
import { isActive } from "./types";

/**
 * 任务轮询跟踪器（R-08 的生命周期管理）：
 * - 只要有 WAITING/PROCESSING 任务就按间隔轮询；
 * - 全部任务结束后自动停止（不再发任何请求）；
 * - 新任务出现时可再次 start；
 * - 切换用户（start 传入新 userId）会中止旧请求并拒绝应用过期响应，
 *   避免旧用户数据覆盖当前用户；
 * - 单次查询失败只回调 onError，不影响已保存的任务状态（查询异常 ≠ 任务失败）。
 */
export interface TrackerDeps {
  fetchMine: () => Promise<TaskListItem[]>;
  /** 应用一次成功的查询结果（实现方需做用户一致性校验时可用 userId） */
  apply: (list: TaskListItem[], userId: string | null) => void;
  onError: (message: string) => void;
  /** 本轮查询后没有待处理任务（轮询已停止） */
  onIdle?: () => void;
  intervalMs?: number;
  /** 可注入定时器（测试用）；默认 setInterval/setTimeout */
  schedule?: (fn: () => void, ms: number) => () => void;
}

export class TaskTracker {
  private deps: TrackerDeps;
  private timerCancel: (() => void) | null = null;
  private userId: string | null = null;
  private epoch = 0;

  constructor(deps: TrackerDeps) {
    this.deps = deps;
  }

  start(userId: string) {
    this.userId = userId;
    this.epoch += 1;
    this.scheduleNext();
    // 立即拉一次，不等待首个间隔
    void this.pollOnce();
  }

  stop() {
    this.epoch += 1; // 使在途请求的结果失效
    this.userId = null;
    if (this.timerCancel) {
      this.timerCancel();
      this.timerCancel = null;
    }
  }

  get running() {
    return this.timerCancel !== null || this.userId !== null;
  }

  /** 手动恢复查询（查询异常时界面上的「重试」按钮）。 */
  retry(userId: string) {
    if (this.userId === userId) {
      void this.pollOnce();
    } else {
      this.start(userId);
    }
  }

  private scheduleNext() {
    if (this.timerCancel) return;
    const schedule = this.deps.schedule ?? ((fn, ms) => {
      const t = setInterval(fn, ms);
      return () => clearInterval(t);
    });
    this.timerCancel = schedule(() => void this.pollOnce(), this.deps.intervalMs ?? 2000);
  }

  private async pollOnce() {
    if (!this.userId) return;
    const { fetchMine, apply, onError, onIdle } = this.deps;
    const requestedUserId = this.userId;
    const requestedEpoch = this.epoch;
    try {
      const list = await fetchMine();
      // 过期响应（已切换用户/已停止）不落地
      if (this.userId !== requestedUserId || this.epoch !== requestedEpoch) return;
      apply(list, requestedUserId);
      const hasActive = list.some((t) => isActive(t.status));
      if (hasActive) {
        this.scheduleNext();
      } else {
        this.stop();
        onIdle?.();
      }
    } catch (e) {
      if (this.userId !== requestedUserId || this.epoch !== requestedEpoch) return;
      onError(e instanceof Error ? e.message : "查询失败");
      // 查询异常时保持轮询（自动恢复），下次成功会清除错误
      this.scheduleNext();
    }
  }
}
