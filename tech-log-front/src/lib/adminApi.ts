import { Category, PageResponse } from "./api";
import { Post } from "../types/blog";

export interface AdminComment {
  id: number;
  postId: number;
  postTitle: string;
  authorName: string;
  content: string;
  deleted: boolean;
  accessIp: string;
  date: string;
}

export interface AccessLog {
  id: number;
  ip: string;
  path: string;
  method: string;
  statusCode: number;
  timestamp: string;
  requestId?: string;
  errorType?: string;
  errorMessage?: string;
  stackTrace?: string;
}

export interface LoginLog {
  id: number;
  provider: string;
  loginId: string;
  ip: string;
  timestamp: string;
}

async function adminRequest<T>(url: string, init?: RequestInit): Promise<T> {
  const response = await fetch(url, {
    credentials: "include",
    ...init,
    headers: {
      ...(init?.body instanceof FormData ? {} : { "Content-Type": "application/json" }),
      ...init?.headers,
    },
  });

  if (!response.ok) {
    let message = `요청이 실패했습니다. (${response.status})`;
    try {
      const error = await response.json();
      message = error.message ?? message;
    } catch {
      // Keep the status based fallback when the response is not JSON.
    }
    throw new Error(message);
  }

  return response.status === 204 ? undefined as T : response.json();
}

export async function adminLogin(username: string, password: string): Promise<void> {
  const form = new URLSearchParams({ adminId: username, adminPassword: password });
  const response = await fetch("/api/admin/session/login", {
    method: "POST",
    credentials: "include",
    headers: { "Content-Type": "application/x-www-form-urlencoded" },
    body: form,
  });
  if (!response.ok) {
    throw new Error("ID 또는 PW가 올바르지 않습니다.");
  }
}

export function adminLogout() {
  return adminRequest<void>("/api/admin/session/logout", { method: "POST" });
}

export function savePost(id: string | number | null, form: FormData) {
  return adminRequest<Post>(id ? `/api/admin/posts/${id}` : "/api/admin/posts", {
    method: id ? "PUT" : "POST",
    body: form,
  });
}

export function deletePost(id: string | number) {
  return adminRequest<void>(`/api/admin/posts/${id}`, { method: "DELETE" });
}

export function saveCategory(id: number | null, name: string) {
  return adminRequest<Category>(id ? `/api/admin/categories/${id}` : "/api/admin/categories", {
    method: id ? "PUT" : "POST",
    body: JSON.stringify({ name }),
  });
}

export function deleteCategory(id: number) {
  return adminRequest<void>(`/api/admin/categories/${id}`, { method: "DELETE" });
}

export function fetchAdminComments(page: number) {
  return adminRequest<PageResponse<AdminComment>>(`/api/admin/comments?page=${page}&size=10`);
}

export function deleteComment(id: number) {
  return adminRequest<void>(`/api/admin/comments/${id}`, { method: "DELETE" });
}

export function fetchAccessLogs(page: number) {
  return adminRequest<PageResponse<AccessLog>>(`/api/admin/access-logs?page=${page}&size=20`);
}

export function fetchLoginLogs(page: number) {
  return adminRequest<PageResponse<LoginLog>>(`/api/admin/login-logs?page=${page}&size=20`);
}
