import { beforeEach, describe, expect, it, vi, type Mock, type MockInstance } from "vitest";
import { render, screen, fireEvent, waitFor } from "@testing-library/react";
import { useSessionStore } from "@/lib/sessionStore";
import { useTaskStore } from "@/lib/taskStore";
import { taskTracker } from "@/lib/taskSync";
import type { Template } from "@/lib/types";

vi.mock("next/navigation", () => ({
  useParams: () => ({ id: "art-sketch" }),
}));

// 只替换 api 方法，保留 ApiError 类（页面用 instanceof 判断）
vi.mock("@/lib/api", async (importActual) => {
  const actual = await importActual<typeof import("@/lib/api")>();
  return {
    ...actual,
    api: {
      ...actual.api,
      template: vi.fn(),
      upload: vi.fn(),
      createTask: vi.fn(),
      myTasks: vi.fn(),
    },
  };
});

import { api } from "@/lib/api";
import TemplateDetailPage from "@/app/templates/[id]/page";

const template: Template = {
  id: "art-sketch",
  title: "速写画框",
  category: "创意图片",
  coverUrl: "/demo/covers/art-sketch.svg",
  description: "d",
  requiredImageCount: 1,
  acceptedMimeTypes: ["image/png"],
  maxFileSizeBytes: 3 * 1024 * 1024,
  sortOrder: 2,
};

/** 渲染详情页并完成一次上传（mock api.upload），返回「开始制作」按钮的点击器 */
async function setupPageWithUploadedAsset() {
  render(<TemplateDetailPage />);
  await screen.findByRole("heading", { name: /速写画框/ });
  const input = document.querySelector('input[type="file"]') as HTMLInputElement;
  fireEvent.change(input, {
    target: { files: [new File(["png-bytes"], "p.png", { type: "image/png" })] },
  });
  await screen.findByAltText(/已上传素材 1/);
}

describe("详情页受理路径（R2-P1：受理成功后的列表刷新失败不得误报提交失败）", () => {
  let startSpy: MockInstance;

  beforeEach(() => {
    vi.clearAllMocks();
    useTaskStore.getState().clear();
    useSessionStore.setState({
      user: { id: "u1", nickname: "测试用户", created: false },
      booted: true,
      bootError: null,
    });
    (api.template as Mock).mockResolvedValue(template);
    (api.upload as Mock).mockResolvedValue({
      id: "asset-1",
      url: "/api/files/uploads/asset-1",
      originalName: "p.png",
      mimeType: "image/png",
      sizeBytes: 10,
    });
    (api.createTask as Mock).mockResolvedValue({ taskId: "task-1" });
    (api.myTasks as Mock).mockRejectedValue(new Error("网络断开"));
    startSpy = vi.spyOn(taskTracker, "start").mockImplementation(() => {});
  });

  it("createTask 成功 + myTasks 失败：不显示提交错误、tracker 已启动、connectionError 已设置、上传位已清空", async () => {
    await setupPageWithUploadedAsset();
    fireEvent.click(screen.getByRole("button", { name: "开始制作" }));

    await waitFor(() => expect(useTaskStore.getState().connectionError).toBeTruthy());
    // 受理成功：不得出现「提交失败」类错误提示
    expect(screen.queryByText(/提交失败/)).not.toBeInTheDocument();
    // 跟踪器已启动（新任务不失联）
    expect(startSpy).toHaveBeenCalledWith("u1");
    // 上传位已清空，可继续制作
    expect(screen.getByRole("heading", { name: /上传素材（0\/1）/ })).toBeInTheDocument();
    expect(useTaskStore.getState().submitting).toBe(false);
  });

  it("createTask 失败：显示提交错误、上传位保留、不启动 tracker、不误设 connectionError", async () => {
    (api.createTask as Mock).mockRejectedValue(new Error("受理被拒绝"));
    await setupPageWithUploadedAsset();
    fireEvent.click(screen.getByRole("button", { name: "开始制作" }));

    await waitFor(() => expect(screen.getByText("受理被拒绝")).toBeInTheDocument());
    expect(startSpy).not.toHaveBeenCalled();
    expect(useTaskStore.getState().connectionError).toBeNull();
    // 上传位保留（用户可直接重试）
    expect(screen.getByRole("heading", { name: /上传素材（1\/1）/ })).toBeInTheDocument();
  });
});
