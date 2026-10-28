"use client";

import { useEffect, useState } from "react";
import { api, ApiError } from "@/lib/api";
import { switchIdentity } from "@/lib/taskSync";
import { useSessionStore } from "@/lib/sessionStore";
import styles from "./AccountDialog.module.css";

/** 账号设置（对应 R-10 参考图）：查看/修改昵称、切换身份。 */
export default function AccountDialog({ onClose }: { onClose: () => void }) {
  const user = useSessionStore((s) => s.user);
  const setUser = useSessionStore((s) => s.setUser);
  const [nickname, setNickname] = useState(user?.nickname ?? "");
  const [saving, setSaving] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [switching, setSwitching] = useState(false);

  useEffect(() => {
    const onKey = (e: KeyboardEvent) => {
      if (e.key === "Escape") onClose();
    };
    window.addEventListener("keydown", onKey);
    return () => window.removeEventListener("keydown", onKey);
  }, [onClose]);

  async function save() {
    if (!nickname.trim()) {
      setError("昵称不能为空");
      return;
    }
    setSaving(true);
    setError(null);
    try {
      const updated = await api.updateNickname(nickname.trim());
      setUser(updated);
      onClose();
    } catch (e) {
      setError(e instanceof ApiError ? e.message : "保存失败");
    } finally {
      setSaving(false);
    }
  }

  async function switchUser() {
    setSwitching(true);
    setError(null);
    try {
      await switchIdentity();
      onClose();
    } catch (e) {
      setError(e instanceof ApiError ? e.message : "切换身份失败");
    } finally {
      setSwitching(false);
    }
  }

  return (
    <div className={styles.overlay} onClick={onClose}>
      <div
        className={styles.dialog}
        role="dialog"
        aria-modal="true"
        aria-label="账号设置"
        onClick={(e) => e.stopPropagation()}
      >
        <div className={styles.head}>
          <h2>账号设置</h2>
          <button type="button" className={styles.close} onClick={onClose} aria-label="关闭">
            ✕
          </button>
        </div>

        <div className={styles.card}>
          <span className={styles.avatar} aria-hidden>
            {user ? user.nickname.slice(0, 1) : "?"}
          </span>
          <div>
            <div className={styles.uid}>UID {user ? `${user.id.slice(0, 8)}…` : "—"}</div>
            <div className={styles.dim}>
              本站采用匿名身份：无需注册，会话 Cookie 自动记录你的素材与任务
            </div>
          </div>
        </div>

        <label className={styles.field} htmlFor="nickname-input">
          <span className={styles.dim}>昵称</span>
          <div className={styles.row}>
            <input
              id="nickname-input"
              type="text"
              value={nickname}
              maxLength={24}
              onChange={(e) => setNickname(e.target.value)}
              onKeyDown={(e) => {
                if (e.key === "Enter") void save();
              }}
            />
            <button type="button" onClick={() => void save()} disabled={saving}>
              {saving ? "保存中…" : "保存"}
            </button>
          </div>
        </label>

        {error && (
          <p className={styles.error} role="alert">
            {error}
          </p>
        )}

        <div className={styles.footer}>
          <button type="button" className={styles.switchBtn} onClick={() => void switchUser()} disabled={switching}>
            {switching ? "切换中…" : "切换身份（演示记录隔离）"}
          </button>
        </div>
      </div>
    </div>
  );
}
