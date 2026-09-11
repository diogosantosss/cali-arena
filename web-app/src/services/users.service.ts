import { apiClient } from "@/api/client";
import type { CreateUserInput, UpdateUserRoleInput, User } from "@/data/users";

export const usersService = {
  getUsers(): Promise<User[]> {
    return apiClient.get("/users");
  },

  createUser(input: CreateUserInput): Promise<User> {
    return apiClient.post("/users", input);
  },

  updateUserRole(input: UpdateUserRoleInput): Promise<void> {
    return apiClient.put("/users/update/role", input);
  },
};
