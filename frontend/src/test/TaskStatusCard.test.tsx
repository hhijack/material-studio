import { describe, expect, it } from "vitest";
import { render, screen } from "@testing-library/react";
import TaskStatusCard from "@/components/TaskStatusCard";
import type { TaskListItem } from "@/lib/types";

function task(overrides: Partial<TaskListItem>): TaskListItem {
  return {
    id: "task-1",
    templateId: "travel-postcard",
    templateTitle: "旅行明信片",
    templateCoverUrl: "/demo/covers/travel-postcard.svg",
    status: "PROCESSING",
    currentStage: "SIMULATE_GENERATE",
    failStage: null,
    failReason: null,
    resultUrl: null,
    createdAt: "2026-10-28T12:00:00",
    startedAt: null,
    finishedAt: null,
    ...overrides,
  };
}

describe("TaskStatusCard：状态表达（R-07）", () => {
  it("处理中：展示当前阶段，且不渲染失败/结果内容", () => {
    render(<TaskStatusCard task={task({ status: "PROCESSING", currentStage: "SIMULATE_GENERATE" })} />);
    expect(screen.getByText("处理中")).toBeInTheDocument();
    expect(screen.getByText(/当前阶段：模拟生成/)).toBeInTheDocument();
    expect(screen.queryByText("失败阶段")).not.toBeInTheDocument();
    expect(screen.queryByRole("img")).not.toBeInTheDocument();
  });

  it("等待处理：明确展示排队文案", () => {
    render(<TaskStatusCard task={task({ status: "WAITING", currentStage: null })} />);
    expect(screen.getByText("等待处理")).toBeInTheDocument();
    expect(screen.getByText(/排队等待处理/)).toBeInTheDocument();
  });

  it("成功：渲染结果预览图", () => {
    render(<TaskStatusCard task={task({ status: "SUCCESS", resultUrl: "/api/files/results/x.png" })} />);
    expect(screen.getByText("成功")).toBeInTheDocument();
    const img = screen.getByRole("img", { name: /制作结果/ });
    expect(img).toHaveAttribute("src", "/api/files/results/x.png");
  });

  it("失败：展示失败阶段与原因，不展示结果图", () => {
    render(
      <TaskStatusCard
        task={task({
          status: "FAILED",
          failStage: "SIMULATE_GENERATE",
          failReason: "模拟故障注入：本次任务被配置为在「模拟生成」阶段失败",
          currentStage: "SIMULATE_GENERATE",
        })}
      />
    );
    expect(screen.getByText("失败")).toBeInTheDocument();
    expect(screen.getByText(/失败阶段：模拟生成/)).toBeInTheDocument();
    expect(screen.getByText(/模拟故障注入/)).toBeInTheDocument();
    expect(screen.queryByRole("img")).not.toBeInTheDocument();
  });
});
