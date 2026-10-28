"use client";

import Link from "next/link";
import { usePathname } from "next/navigation";
import { useState } from "react";
import { useSessionStore } from "@/lib/sessionStore";
import { useTaskStore } from "@/lib/taskStore";
import { bootstrapSession } from "@/lib/taskSync";
import AccountDialog from "./AccountDialog";
import styles from "./TopBar.module.css";

export default function TopBar() {
  const [open, setOpen] = useState(false);
  const pathname = usePathname();
  const user = useSessionStore((s) => s.user);
  const bootError = useSessionStore((s) => s.bootError);
  const connectionError = useTaskStore((s) => s.connectionError);

  return (
    <header className={styles.header}>
      <div className={styles.inner}>
        <Link href="/" className={styles.logo} aria-label="Material Studio 首页">
          <span className={styles.logoMark} aria-hidden>
            M
          </span>
          <span className={styles.logoText}>Material Studio</span>
        </Link>
        <nav className={styles.nav} aria-label="主导航">
          <Link href="/" className={pathname === "/" ? styles.active : ""}>
            模板
          </Link>
          <Link href="/history" className={pathname === "/history" ? styles.active : ""}>
            历史记录
          </Link>
        </nav>
        <button
          type="button"
          className={styles.accountBtn}
          onClick={() => setOpen(true)}
          aria-haspopup="dialog"
          aria-label="账号设置"
        >
          <span className={styles.avatar} aria-hidden>
            {user ? user.nickname.slice(0, 1) : "·"}
          </span>
          <span>{user ? user.nickname : "未连接"}</span>
        </button>
      </div>
      {bootError && !user && (
        <div className={styles.errorBar} role="alert">
          <span>连接服务失败：{bootError}</span>
          <button type="button" className={styles.retryLink} onClick={() => void bootstrapSession()}>
            重试
          </button>
        </div>
      )}
      {connectionError && (
        <div className={styles.connBar} role="alert">
          状态查询暂时不可用（{connectionError}），任务信息已保留，正在自动重试…
        </div>
      )}
      {open && <AccountDialog onClose={() => setOpen(false)} />}
    </header>
  );
}
