"use client";

import { useEffect, useState } from "react";
import Link from "next/link";
import { api } from "@/lib/api";
import type { CategoryInfo, Template } from "@/lib/types";
import styles from "./page.module.css";

/** 模板列表（R-02 / UI-01）：分类 chips + 卡片网格，数据来自 Java 接口。 */
export default function TemplateListPage() {
  const [templates, setTemplates] = useState<Template[] | null>(null);
  const [categories, setCategories] = useState<CategoryInfo[]>([]);
  const [active, setActive] = useState<string>("全部");
  const [error, setError] = useState<string | null>(null);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    let alive = true;
    Promise.all([api.templates(), api.categories()])
      .then(([tpls, cats]) => {
        if (!alive) return;
        setTemplates(tpls);
        setCategories(cats);
      })
      .catch((e) => {
        if (alive) setError(e instanceof Error ? e.message : "加载失败");
      })
      .finally(() => {
        if (alive) setLoading(false);
      });
    return () => {
      alive = false; // 卸载后丢弃响应
    };
  }, []);

  const shown = templates?.filter((t) => active === "全部" || t.category === active) ?? [];

  return (
    <main className={styles.page}>
      <div className={styles.filterRow} role="tablist" aria-label="模板分类筛选">
        {["全部", ...categories.map((c) => c.category)].map((cat) => (
          <button
            key={cat}
            role="tab"
            aria-selected={active === cat}
            className={`${styles.chip} ${active === cat ? styles.chipActive : ""}`}
            onClick={() => setActive(cat)}
          >
            {cat}
          </button>
        ))}
      </div>

      {loading && <div className={styles.state}>模板加载中…</div>}
      {!loading && error && (
        <div className={styles.state} role="alert">
          模板加载失败：{error}
          <button className={styles.retryBtn} onClick={() => location.reload()}>
            重试
          </button>
        </div>
      )}
      {!loading && !error && shown.length === 0 && (
        <div className={styles.state}>该分类下暂无模板</div>
      )}

      <div className={styles.grid}>
        {shown.map((tpl) => (
          <Link
            key={tpl.id}
            href={`/templates/${tpl.id}`}
            className={styles.card}
            aria-label={`模板：${tpl.title}`}
          >
            <div className={styles.coverWrap}>
              {/* eslint-disable-next-line @next/next/no-img-element */}
              <img src={tpl.coverUrl} alt={`${tpl.title} 封面`} className={styles.cover} loading="lazy" />
              <span className={styles.categoryTag}>{tpl.category}</span>
            </div>
            <div className={styles.cardBody}>
              <div className={styles.title}>{tpl.title}</div>
              <div className={styles.desc}>{tpl.description}</div>
              <div className={styles.meta}>
                需 {tpl.requiredImageCount} 张图片 · 单张 ≤ {Math.round(tpl.maxFileSizeBytes / 1024 / 1024)}MB
              </div>
            </div>
          </Link>
        ))}
      </div>
    </main>
  );
}
