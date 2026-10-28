/** 与 Java 后端接口对应的类型定义。 */

export type TaskStatus = "WAITING" | "PROCESSING" | "SUCCESS" | "FAILED";

export type StageCode = "MATERIAL_CHECK" | "SIMULATE_GENERATE" | "RESULT_FINALIZE";

export const STAGE_LABELS: Record<StageCode, string> = {
  MATERIAL_CHECK: "素材检查",
  SIMULATE_GENERATE: "模拟生成",
  RESULT_FINALIZE: "结果整理",
};

export const STAGE_ORDER: StageCode[] = ["MATERIAL_CHECK", "SIMULATE_GENERATE", "RESULT_FINALIZE"];

export interface Template {
  id: string;
  title: string;
  category: string;
  coverUrl: string;
  description: string;
  requiredImageCount: number;
  acceptedMimeTypes: string[];
  maxFileSizeBytes: number;
  sortOrder: number;
}

export interface CategoryInfo {
  category: string;
  templateCount: number;
}

export interface User {
  id: string;
  nickname: string;
  created: boolean;
}

/** 任务（历史列表条目，含模板标题/封面）。 */
export interface TaskListItem {
  id: string;
  templateId: string;
  templateTitle: string;
  templateCoverUrl: string;
  status: TaskStatus;
  currentStage: StageCode | null;
  failStage: StageCode | null;
  failReason: string | null;
  resultUrl: string | null;
  createdAt: string;
  startedAt: string | null;
  finishedAt: string | null;
}

export interface UploadedAsset {
  id: string;
  url: string;
  originalName: string;
  mimeType: string;
  sizeBytes: number;
}

/** 任务步骤（详情接口返回，展示独立执行边界）。 */
export interface TaskStep {
  id: number;
  taskId: string;
  stepIndex: number;
  stage: StageCode;
  status: "PENDING" | "RUNNING" | "PASSED" | "FAILED";
  message: string | null;
  startedAt: string | null;
  finishedAt: string | null;
}

export interface TaskDetail {
  task: TaskListItem;
  template: Template;
  steps: TaskStep[];
  assets: UploadedAsset[];
}

export interface SimConfig {
  materialCheckMs: number;
  simulateGenerateMs: number;
  resultFinalizeMs: number;
  failStage: StageCode | null;
}

export const isTerminal = (status: TaskStatus) => status === "SUCCESS" || status === "FAILED";
export const isActive = (status: TaskStatus) => status === "WAITING" || status === "PROCESSING";
