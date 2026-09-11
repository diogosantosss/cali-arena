import { getErrorDescription } from "./error-messages";

const API_BASE_URL = "/api";

export class ApiError extends Error {
  readonly status: number;

  constructor(status: number, message: string) {
    super(message);
    this.status = status;
  }
}

/**
 * Reads the auth token from localStorage.
 * Used by the API client (Authorization header) and by the spectator SSE URL.
 */
export function getStoredToken(): string | null {
  return localStorage.getItem("token");
}

/**
 * Clears the stored auth token. Called when the backend rejects the token
 * with 401 so the app can send the user back to the login page.
 */
function clearStoredToken(): void {
  localStorage.removeItem("token");
}

/**
 * Forwards to the login page. Used when a request fails with 401 so an expired
 * or invalidated token (e.g. after the backend restarts) does not leave the
 * user stuck on a dashboard page.
 */
function redirectToLogin(): void {
  if (window.location.pathname !== "/") {
    window.location.href = "/";
  }
}

async function request<T>(endpoint: string, options: RequestInit = {}): Promise<T> {
  const isLoginRequest = endpoint === "/users/token";
  const token = getStoredToken();

  let response: Response;
  try {
    response = await fetch(`${API_BASE_URL}${endpoint}`, {
      ...options,
      headers: {
        ...(options.body !== undefined ? { "Content-Type": "application/json" } : {}),
        ...(token ? { Authorization: `Bearer ${token}` } : {}),
        ...options.headers,
      },
    });
  } catch {
    // Network failure — the backend is unreachable (e.g. it went down).
    // If we were authenticated, clear the session and go back to login.
    if (!isLoginRequest) {
      clearStoredToken();
      redirectToLogin();
    }
    throw new ApiError(0, "The server is unreachable. Please try again later.");
  }

  if (response.status === 401) {
    if (!isLoginRequest) {
      // An authenticated request was rejected: the token is invalid or expired.
      clearStoredToken();
      redirectToLogin();
    }
  }

  if (!response.ok) {
    const problem = await response.json().catch(() => ({ title: "Unknown error" }));
    const message = problem.title ? getErrorDescription(problem.title) : response.statusText;
    throw new ApiError(response.status, message);
  }

  if (response.status === 204) {
    return undefined as T;
  }

  return response.json();
}

export const apiClient = {
  get<T>(endpoint: string) {
    return request<T>(endpoint, { method: "GET" });
  },

  post<T>(endpoint: string, body?: unknown) {
    return request<T>(endpoint, {
      method: "POST",
      body: body === undefined ? undefined : JSON.stringify(body),
    });
  },

  put<T>(endpoint: string, body?: unknown) {
    return request<T>(endpoint, {
      method: "PUT",
      body: body === undefined ? undefined : JSON.stringify(body),
    });
  },

  patch<T>(endpoint: string, body: unknown) {
    return request<T>(endpoint, { method: "PATCH", body: JSON.stringify(body) });
  },

  delete<T = void>(endpoint: string) {
    return request<T>(endpoint, { method: "DELETE" });
  },
};
