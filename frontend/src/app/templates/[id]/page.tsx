"use client";

import { useCallback, useEffect, useMemo, useState } from "react";
import Link from "next/link";
import { useParams } from "next/navigation";
import { api, ApiError } from "@/lib/api";
import { taskTracker } from "@/lib/taskSync";
import { useSessionStore } from "@/lib/sessionStore";
import { useTaskStore } from "@/lib/taskStore";
import type { Template } from "@/lib/types";
import UploadSlot, { type SlotState } from "@/components/UploadSlot";
import TaskStatusCard from "@/components/TaskStatusCard";
import styles from "./page.module.css";

const emptySlot = (): SlotState => ({
  key: Math.random().toString(36).slice(2),
  asset: null,
  uploading: false,
  progress: 0,
  error: null,
});

/** 模板详情与制作页（R-03/04/07/08，参考 UI-02/UI-03）：左制作、右历史。 */
export default function TemplateDetailPage() {
  const params = useParams<{ id: string }>();
  const templateId = params.id;

  const [template, setTemplate] = useState<Template | null>(null);
  const [loadState, setLoadState] = useState<"loading" | "ready" | "notfound" | "error">("loading");
  const [loadError, setLoadError] = useState<string | null>(null);

  const [slots, setSlots] = useState<SlotState[]>([]);
  const [submitError, setSubmitError] = useState<string | null>(null);

  const user = useSessionStore((s) => s.user);
  const connectionError = useTaskStore((s) => s.connectionError);
  const tasks = useTaskStore((s) => s.tasks);

  // 初始化：加载模板 + 生成 N 个上传位
  useEffect(() => {
    let alive = true;
    setLoadState("loading");
    api
      .template(templateId)
      .then((tpl) => {
        if (!alive) return;
        setTemplate(tpl);
        setSlots((prev) =>
          prev.length === tpl.requiredImageCount ? prev : Array.from({ length: tpl.requiredImageCount }, emptySlot)
        );
        setLoadState("ready");
      })
      .catch((e) => {
        if (!alive) return;
        if (e instanceof ApiError && e.httpStatus === 404) setLoadState("notfound");
        else {
          setLoadState("error");
          setLoadError(e instanceof Error ? e.message : "加载失败");
        }
      });
    return () => {
      alive = false;
    };
  }, [templateId]);

  const pickFile = useCallback(
    (index: number, file: File) => {
      if (!template) return;
      setSubmitError(null);
      setSlots((prev) => {
        const next = [...prev];
        // 客户端预校验：类型与大小（服务端仍会校验）
        if (!template.acceptedMimeTypes.includes(file.type)) {
          next[index] = { ...next[index], uploading: false, asset: null, progress: 0, error: `不支持的图片类型：${file.type || "未知"}` };
          return next;
        }
        if (file.size > template.maxFileSizeBytes) {
          next[index] = {
            ...next[index],
            uploading: false,
            asset: null,
            progress: 0,
            error: `文件超过大小限制（≤ ${Math.round(template.maxFileSizeBytes / 1024 / 1024)}MB）`,
          };
          return next;
        }
        next[index] = { ...next[index], uploading: true, progress: 0, asset: null, error: null };
        api
          .upload(template.id, file, (pct) => {
            setSlots((cur) => {
              const arr = [...cur];
              arr[index] = { ...arr[index], progress: pct };
              return arr;
            });
          })
          .then((asset) => {
            setSlots((cur) => {
              const arr = [...cur];
              arr[index] = { ...arr[index], uploading: false, asset, progress: 100, error: null };
              return arr;
            });
          })
          .catch((err) => {
            if (err instanceof ApiError && err.code === "ABORTED") return;
            setSlots((cur) => {
              const arr = [...cur];
              arr[index] = {
                ...arr[index],
                uploading: false,
                asset: null,
                error: err instanceof Error ? err.message : "上传失败",
              };
              return arr;
            });
          });
        return next;
      });
    },
    [template]
  );

  const removeAsset = useCallback((index: number) => {
    setSubmitError(null);
    setSlots((prev) => {
      const next = [...prev];
      next[index] = emptySlot();
      return next;
    });
  }, []);

  const submitting = useTaskStore((s) => s.submitting);
  const allFilled = slots.length > 0 && slots.every((s) => s.asset !== null);
  const canSubmit = Boolean(template && allFilled && !submitting);

  async function submit() {
    if (!template || !canSubmit || !user) return;
    useTaskStore.getState().setSubmitting(true);
    setSubmitError(null);
    try {
      const assetIds = slots.map((s) => s.asset!.id);
      await api.createTask(template.id, assetIds);
      // 受理成功：立即刷新一次任务列表并启动跟踪（按钮不再停留在“提交中”）
      const list = await api.myTasks();
      useTaskStore.getState().mergeTasks(list);
      taskTracker.start(user.id);
      setSlots(Array.from({ length: template.requiredImageCount }, emptySlot));
    } catch (e) {
      setSubmitError(e instanceof Error ? e.message : "提交失败");
    } finally {
      useTaskStore.getState().setSubmitting(false);
    }
  }

  // 右侧历史：本模板的任务（store 驱动，轮询自动更新）
  const order = useTaskStore((s) => s.order);
  const templateTasks = useMemo(
    () => order.map((id) => tasks[id]).filter((t) => t && t.templateId === templateId),
    [order, tasks, templateId]
  );

  if (loadState === "loading") return <main className={styles.state}>模板加载中…</main>;
  if (loadState === "notfound")
    return (
      <main className={styles.state}>
        模板不存在或已下架。
        <div style={{ marginTop: 12 }}>
          <Link className={styles.backLink} href="/">
            ← 返回模板列表
          </Link>
        </div>
      </main>
    );
  if (loadState === "error")
    return (
      <main className={styles.state} role="alert">
        模板加载失败：{loadError}
        <div style={{ marginTop: 12 }}>
          <Link className={styles.backLink} href="/">
            ← 返回模板列表
          </Link>
        </div>
      </main>
    );
  if (!template) return null;

  return (
    <main className={styles.page}>
      <section className={styles.left} aria-label="制作面板">
        <div className={styles.backRow}>
          <Link href="/" className={styles.backLink}>
            ← 返回
          </Link>
        </div>
        <h1 className={styles.title}>{template.title}</h1>
        <p className={styles.desc}>{template.description}</p>
        <div className={styles.coverWrap}>
          {/* eslint-disable-next-line @next/next/no-img-element */}
          <img src={template.coverUrl} alt={`${template.title} 示例封面`} className={styles.cover} />
        </div>

        <h2 className={styles.sectionTitle} id="upload-heading">
          上传素材（{slots.filter((s) => s.asset).length}/{slots.length}）
        </h2>
        <div className={styles.slotGrid} aria-labelledby="upload-heading">
          {slots.map((slot, i) => (
            <UploadSlot key={slot.key} index={i} template={template} slot={slot} onPickFile={pickFile} onRemove={removeAsset} />
          ))}
        </div>

        {submitError && (
          <p className={styles.submitError} role="alert">
            {submitError}
          </p>
        )}

        <button type="button" className={styles.submitBtn} disabled={!canSubmit} onClick={() => void submit()}>
          {!user ? "会话建立中…" : submitting ? "提交中…" : allFilled ? "开始制作" : `还需上传 ${slots.filter((s) => !s.asset).length} 张图片`}
        </button>
        {!allFilled && !submitting && (
          <p className={styles.tip}>提交后任务将在后台执行，你可以继续浏览其他模板。</p>
        )}
      </section>

      <section className={styles.right} aria-label="本模板的制作记录">
        <header className={styles.historyHead}>
          <h2>制作记录</h2>
          <button
            type="button"
            className={styles.refreshBtn}
            aria-label="刷新制作记录"
            title={connectionError ? "查询异常，点击重试" : "刷新"}
            onClick={() => user && taskTracker.retry(user.id)}
          >
            ⟳
          </button>
        </header>
        {connectionError && (
          <div className={styles.connNote} role="alert">
            状态查询暂时不可用，已保留当前任务信息。点击右上角 ⟳ 手动恢复。
          </div>
        )}
        {templateTasks.length === 0 ? (
          <div className={styles.emptyHistory}>还没有制作记录，上传图片并开始制作吧。</div>
        ) : (
          <div className={styles.taskList}>
            {templateTasks.map((task) => (
              <TaskStatusCard key={task.id} task={task} />
            ))}
          </div>
        )}
      </section>
    </main>
  );
}
