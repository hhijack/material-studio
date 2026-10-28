"use client";

import { useEffect } from "react";
import { bootstrapSession } from "@/lib/taskSync";

/** 应用启动：建立匿名会话、恢复历史任务并按需启动轮询（刷新恢复）。 */
export default function AppBoot() {
  useEffect(() => {
    void bootstrapSession();
  }, []);
  return null;
}
