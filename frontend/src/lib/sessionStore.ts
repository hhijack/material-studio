"use client";

import { create } from "zustand";
import type { User } from "./types";

interface SessionStore {
  user: User | null;
  bootError: string | null;
  booted: boolean;
  setUser: (user: User) => void;
  setBootError: (message: string | null) => void;
}

export const useSessionStore = create<SessionStore>((set) => ({
  user: null,
  bootError: null,
  booted: false,
  setUser: (user) => set({ user, booted: true, bootError: null }),
  setBootError: (message) => set({ bootError: message, booted: true }),
}));
