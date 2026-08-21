import { apiClient } from "@/lib/api/client";
import type { LoginInput, LoginOutput, User } from "../types";

export const authService = {
  createToken(input: LoginInput): Promise<LoginOutput> {
    return apiClient.post("/users/token", input);
  },

  logout(): Promise<void> {
    return apiClient.post("/users/logout");
  },

  getMe(): Promise<User> {
    return apiClient.get("/users/me");
  },
};
