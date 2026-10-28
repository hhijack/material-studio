"use client";

import { useRef } from "react";
import type { Template, UploadedAsset } from "@/lib/types";
import styles from "./UploadSlot.module.css";

export interface SlotState {
  key: string;
  asset: UploadedAsset | null;
  uploading: boolean;
  progress: number;
  error: string | null;
}

/** 单个图片上传位（R-03）：选择/预览/替换/移除，上传中进度与失败反馈。 */
export default function UploadSlot({
  index,
  template,
  slot,
  onPickFile,
  onRemove,
}: {
  index: number;
  template: Template;
  slot: SlotState;
  onPickFile: (index: number, file: File) => void;
  onRemove: (index: number) => void;
}) {
  const inputRef = useRef<HTMLInputElement>(null);
  const accept = template.acceptedMimeTypes.join(",");
  const maxSizeMb = (template.maxFileSizeBytes / 1024 / 1024).toFixed(1);

  return (
    <div className={styles.slot} aria-label={`上传位 ${index + 1}`}>
      <input
        ref={inputRef}
        type="file"
        accept={accept}
        className={styles.fileInput}
        onChange={(e) => {
          const file = e.target.files?.[0];
          e.target.value = ""; // 允许重复选择同一文件
          if (file) onPickFile(index, file);
        }}
        aria-label={`为上传位 ${index + 1} 选择图片`}
      />

      {slot.asset ? (
        <div className={styles.previewBox}>
          {/* eslint-disable-next-line @next/next/no-img-element */}
          <img src={slot.asset.url} alt={`已上传素材 ${index + 1}：${slot.asset.originalName}`} className={styles.preview} />
          <div className={styles.previewActions}>
            <button type="button" onClick={() => inputRef.current?.click()}>
              替换
            </button>
            <button type="button" className={styles.dangerBtn} onClick={() => onRemove(index)}>
              移除
            </button>
          </div>
          <div className={styles.fileName} title={slot.asset.originalName}>
            {slot.asset.originalName}
          </div>
        </div>
      ) : slot.uploading ? (
        <div className={styles.dropzone} data-state="uploading">
          <div className={styles.uploadingText}>上传中 {slot.progress}%</div>
          <div
            className={styles.progressTrack}
            role="progressbar"
            aria-valuenow={slot.progress}
            aria-valuemin={0}
            aria-valuemax={100}
          >
            <div className={styles.progressFill} style={{ width: `${slot.progress}%` }} />
          </div>
        </div>
      ) : (
        <button
          type="button"
          className={styles.dropzone}
          data-state="empty"
          onClick={() => inputRef.current?.click()}
        >
          <span className={styles.plusIcon} aria-hidden>
            ＋
          </span>
          <span>点击上传图片</span>
          <span className={styles.hint}>
            支持 {accept.replaceAll("image/", "").toUpperCase()} · 单张 ≤ {maxSizeMb}MB
          </span>
        </button>
      )}

      {slot.error && (
        <p className={styles.error} role="alert">
          {slot.error}
          <button type="button" onClick={() => inputRef.current?.click()}>
            重新选择
          </button>
        </p>
      )}
    </div>
  );
}
