import "@testing-library/jest-dom/vitest";
import { afterEach } from "vitest";
import { cleanup } from "@testing-library/react";

// vitest 不开 globals 时 RTL 不会自动清理，手动保证用例间 DOM 隔离
afterEach(() => {
  cleanup();
});
