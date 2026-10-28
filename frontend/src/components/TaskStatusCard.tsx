"use client";

import type { TaskListItem } from "@/lib/types";
import { STAGE_LABELS, STAGE_ORDER } from "@/lib/types";
import styles from "./TaskStatusCard.module.css";

function formatTime(iso: string | null): string {
  if (!iso) return "";
  const d = new Date(iso);
  return d.toLocaleString("zh-CN", { hour12: false });
}

/** 单个任务卡片：区分 等待/处理中/成功/失败，展示阶段、失败原因或成功预览（R-07/R-09）。 */
export default function TaskStatusCard({ task, showTemplate }: { task: TaskListItem; showTemplate?: boolean }) {
  const stageIndex = task.currentStage ? STAGE_ORDER.indexOf(task.currentStage) : -1;

  return (
    <article className={`${styles.card} ${task.status === "FAILED" ? styles.failed : ""}`} data-status={task.status}>
      <header className={styles.head}>
        <span className={styles.title}>{showTemplate ? task.templateTitle : "制作任务"}</span>
        <span className={`${styles.badge} ${styles[task.status.toLowerCase()]}`}>
          {task.status === "WAITING" && "等待处理"}
          {task.status === "PROCESSING" && "处理中"}
          {task.status === "SUCCESS" && "成功"}
          {task.status === "FAILED" && "失败"}
        </span>
      </header>

      <div className={styles.body}>
        {task.status === "SUCCESS" && task.resultUrl && (
          <a href={task.resultUrl} target="_blank" rel="noreferrer" className={styles.resultLink} aria-label="查看结果大图">
            {/* eslint-disable-next-line @next/next/no-img-element */}
            <img src={task.resultUrl} alt={`${task.templateTitle} 的制作结果`} className={styles.result} />
          </a>
        )}

        {task.status === "FAILED" && (
          <div className={styles.failBox} role="alert">
            <div className={styles.failStage}>
              失败阶段：{task.failStage ? STAGE_LABELS[task.failStage] : "未知"}
            </div>
            <div className={styles.failReason}>{task.failReason ?? "未知原因"}</div>
          </div>
        )}

        {(task.status === "WAITING" || task.status === "PROCESSING") && (
          <div className={styles.progressBox}>
            {task.status === "WAITING" ? (
              <div className={styles.waitingText}>
                <span className={styles.spinner} aria-hidden />
                排队等待处理…
              </div>
            ) : (
              <>
                <div className={styles.stageText}>
                  <span className={styles.spinner} aria-hidden />
                  当前阶段：{task.currentStage ? STAGE_LABELS[task.currentStage] : "…"}
                </div>
                <ol className={styles.stageDots} aria-label="制作步骤">
                  {STAGE_ORDER.map((stage, i) => (
                    <li
                      key={stage}
                      className={`${i < stageIndex ? styles.done : ""} ${i === stageIndex ? styles.current : ""}`}
                      aria-current={i === stageIndex ? "step" : undefined}
                    >
                      {STAGE_LABELS[stage]}
                    </li>
                  ))}
                </ol>
              </>
            )}
          </div>
        )}
      </div>

      <footer className={styles.meta}>创建于 {formatTime(task.createdAt)}</footer>
    </article>
  );
}
