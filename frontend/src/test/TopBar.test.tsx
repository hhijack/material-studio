import { beforeEach, describe, expect, it, vi, type Mock } from "vitest";
import { render, screen, fireEvent, waitFor } from "@testing-library/react";
import { useSessionStore } from "@/lib/sessionStore";

vi.mock("next/navigation", () => ({
  usePathname: () => "/",
}));

vi.mock("next/link", () => ({
  default: ({ href, children, ...rest }: { href: string; children: React.ReactNode }) => (
    <a href={href} {...rest}>
      {children}
    </a>
  ),
}));

vi.mock("@/lib/api", () => ({
  ApiError: class ApiError extends Error {},
  api: {
    session: vi.fn(),
    updateNickname: vi.fn(),
    switchIdentity: vi.fn(),
    myTasks: vi.fn(),
  },
}));

import { api } from "@/lib/api";
import TopBar from "@/components/TopBar";

describe("TopBar 会话建立失败的全局提示（R2-P2-2）", () => {
  beforeEach(() => {
    vi.clearAllMocks();
    useSessionStore.setState({ user: null, booted: true, bootError: null });
    (api.myTasks as Mock).mockResolvedValue([]);
  });

  it("bootError 时显示「连接服务失败 + 重试」；重试成功后横幅消失并建立会话", async () => {
    useSessionStore.setState({ user: null, booted: true, bootError: "无法连接服务器" });
    (api.session as Mock).mockResolvedValue({ id: "u9", nickname: "访客TEST", created: false });

    render(<TopBar />);
    expect(screen.getByRole("alert")).toHaveTextContent("连接服务失败：无法连接服务器");
    expect(screen.getByText("未连接")).toBeInTheDocument();

    fireEvent.click(screen.getByRole("button", { name: "重试" }));
    await waitFor(() => expect(useSessionStore.getState().user?.nickname).toBe("访客TEST"));
    expect(screen.queryByText(/连接服务失败/)).not.toBeInTheDocument();
  });

  it("会话正常时（有 user）不显示错误横幅", () => {
    useSessionStore.setState({
      user: { id: "u1", nickname: "访客A1B2", created: false },
      booted: true,
      bootError: null,
    });
    render(<TopBar />);
    expect(screen.queryByRole("alert")).not.toBeInTheDocument();
    expect(screen.getByText("访客A1B2")).toBeInTheDocument();
  });
});
