"use client";

import { useMemo } from "react";
import { taskTracker } from "@/lib/taskSync";
import { useSessionStore } from "@/lib/sessionStore";
import { useTaskStore } from "@/lib/taskStore";
import TaskStatusCard from "@/components/TaskStatusCard";
import styles from "./page.module.css";

/** 个人历史任务（R-09）：全部任务、创建时间、状态、成功预览/失败原因。 */
export default function HistoryPage() {
  const user = useSessionStore((s) => s.user);
  const booted = useSessionStore((s) => s.booted);
  const tasks = useTaskStore((s) => s.tasks);
  const order = useTaskStore((s) => s.order);
  const connectionError = useTaskStore((s) => s.connectionError);

  const list = useMemo(() => order.map((id) => tasks[id]).filter(Boolean), [order, tasks]);
  const activeCount = list.filter((t) => t.status === "WAITING" || t.status === "PROCESSING").length;

  return (
    <main className={styles.page}>
      <header className={styles.head}>
        <div>
          <h1>历史记录</h1>
          <p className={styles.sub}>
            {user ? `${user.nickname} 的全部制作任务` : "会话建立中…"}
            {list.length > 0 && ` · 共 ${list.length} 条`}
            {activeCount > 0 && ` · ${activeCount} 条进行中（自动跟踪）`}
          </p>
        </div>
        <button
          type="button"
          className={styles.refreshBtn}
          onClick={() => user && taskTracker.retry(user.id)}
          disabled={!user}
        >
          ⟳ 刷新
        </button>
      </header>

      {connectionError && (
        <div className={styles.connNote} role="alert">
          状态查询暂时不可用，已保留当前任务信息；点击「刷新」立即恢复查询。
        </div>
      )}

      {booted && !user && <div className={styles.empty}>会话未建立，请检查后端服务是否可用。</div>}
      {user && list.length === 0 && !connectionError && (
        <div className={styles.empty}>还没有任务记录，去模板列表开始第一次制作吧。</div>
      )}

      <div className={styles.grid}>
        {list.map((task) => (
          <TaskStatusCard key={task.id} task={task} showTemplate />
        ))}
      </div>
    </main>
  );
}
