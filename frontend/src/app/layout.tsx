import type { Metadata } from "next";
import "./globals.css";
import AppBoot from "@/components/AppBoot";
import TopBar from "@/components/TopBar";

export const metadata: Metadata = {
  title: "Material Studio · 素材制作",
  description: "浏览模板、上传图片、提交制作任务并查看结果",
};

export default function RootLayout({
  children,
}: Readonly<{
  children: React.ReactNode;
}>) {
  return (
    <html lang="zh-CN">
      <body>
        <AppBoot />
        <TopBar />
        {children}
      </body>
    </html>
  );
}
