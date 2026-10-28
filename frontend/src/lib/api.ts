import type {
  CategoryInfo,
  SimConfig,
  TaskDetail,
  TaskListItem,
  Template,
  UploadedAsset,
  User,
} from "./types";

export class ApiError extends Error {
  code: string;
  httpStatus: number;

  constructor(httpStatus: number, code: string, message: string) {
    super(message);
    this.httpStatus = httpStatus;
    this.code = code;
  }
}

/** 普通请求超时：防止后端悬挂时轮询请求长期 pending（上传走 XHR，不受此限）。 */
const REQUEST_TIMEOUT_MS = 8000;

async function request<T>(path: string, init?: RequestInit): Promise<T> {
  let res: Response;
  try {
    // 同源请求（Next.js rewrites 反代到 Java），携带会话 Cookie
    res = await fetch(path, {
      credentials: "include",
      ...init,
      signal: init?.signal ?? AbortSignal.timeout(REQUEST_TIMEOUT_MS),
    });
  } catch {
    // 网络层失败（后端不可达、超时等）：与业务失败区分开
    throw new ApiError(0, "NETWORK_ERROR", "无法连接服务器");
  }
  if (!res.ok) {
    let code = "HTTP_" + res.status;
    let message = `请求失败（HTTP ${res.status}）`;
    try {
      const body = (await res.json()) as { code?: string; message?: string };
      if (body?.code) code = body.code;
      if (body?.message) message = body.message;
    } catch {
      // 非 JSON 响应，保留默认消息
    }
    throw new ApiError(res.status, code, message);
  }
  return (await res.json()) as T;
}

export const api = {
  session: () => request<User>("/api/session"),
  updateNickname: (nickname: string) =>
    request<User>("/api/session", {
      method: "PATCH",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify({ nickname }),
    }),
  switchIdentity: () => request<User>("/api/session", { method: "DELETE" }),

  templates: () => request<Template[]>("/api/templates"),
  template: (id: string) => request<Template>(`/api/templates/${id}`),
  categories: () => request<CategoryInfo[]>("/api/categories"),

  upload: (templateId: string, file: File, onProgress?: (pct: number) => void, signal?: AbortSignal) =>
    new Promise<UploadedAsset>((resolve, reject) => {
      // fetch 不支持上传进度，使用 XHR
      const xhr = new XMLHttpRequest();
      xhr.open("POST", "/api/uploads");
      xhr.responseType = "json";
      xhr.withCredentials = true;
      xhr.upload.onprogress = (e) => {
        if (e.lengthComputable && onProgress) onProgress(Math.round((e.loaded / e.total) * 100));
      };
      xhr.onload = () => {
        const body = xhr.response as UploadedAsset | { code?: string; message?: string };
        if (xhr.status >= 200 && xhr.status < 300) {
          resolve(body as UploadedAsset);
        } else {
          const b = body as { code?: string; message?: string };
          reject(new ApiError(xhr.status, b?.code ?? "HTTP_" + xhr.status, b?.message ?? "上传失败"));
        }
      };
      xhr.onerror = () => reject(new ApiError(0, "NETWORK_ERROR", "上传失败：网络异常"));
      xhr.onabort = () => reject(new ApiError(0, "ABORTED", "上传已取消"));
      signal?.addEventListener("abort", () => xhr.abort());
      const form = new FormData();
      form.append("templateId", templateId);
      form.append("file", file);
      xhr.send(form);
    }),

  createTask: (templateId: string, assetIds: string[]) =>
    request<{ taskId: string }>("/api/tasks", {
      method: "POST",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify({ templateId, assetIds }),
    }),

  myTasks: () => request<TaskListItem[]>("/api/tasks/mine"),
  taskDetail: (taskId: string) => request<TaskDetail>(`/api/tasks/${taskId}`),

  simConfig: () => request<SimConfig>("/api/dev/sim-config"),
  updateSimConfig: (config: SimConfig) =>
    request<SimConfig>("/api/dev/sim-config", {
      method: "PUT",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify(config),
    }),
};
