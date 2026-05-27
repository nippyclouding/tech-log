import { Post } from "../types/blog";

export interface PageResponse<T> {
  content: T[];
  page: number;
  size: number;
  totalElements: number;
  totalPages: number;
  last: boolean;
}

export function requirePageResponse<T>(value: unknown): PageResponse<T> {
  if (
    !value ||
    typeof value !== "object" ||
    !Array.isArray((value as PageResponse<T>).content) ||
    !Number.isInteger((value as PageResponse<T>).page) ||
    !Number.isInteger((value as PageResponse<T>).size) ||
    typeof (value as PageResponse<T>).totalElements !== "number" ||
    !Number.isInteger((value as PageResponse<T>).totalPages) ||
    typeof (value as PageResponse<T>).last !== "boolean"
  ) {
    throw new Error("페이지 응답 형식이 올바르지 않습니다.");
  }
  return value as PageResponse<T>;
}

export interface Category {
  id: number;
  name: string;
  slug: string;
}

export interface Comment {
  id: number;
  postId: number;
  authorName: string;
  authorAvatar: string;
  authorGithubUrl: string;
  content: string;
  date: string;
}

export interface CurrentUser {
  authenticated: boolean;
  admin: boolean;
  provider?: string;
  name?: string;
  avatar?: string;
}

export interface MessageResponse {
  message: string;
}

async function request<T>(input: RequestInfo | URL, init?: RequestInit): Promise<T> {
  const response = await fetch(input, {
    credentials: "include",
    ...init,
    headers: {
      ...(init?.body instanceof FormData ? {} : { "Content-Type": "application/json" }),
      ...init?.headers,
    },
  });

  if (!response.ok) {
    const message = await response.text();
    throw new Error(message || `Request failed with ${response.status}`);
  }

  if (response.status === 204) {
    return undefined as T;
  }

  return response.json();
}

export async function fetchPosts(params: { page: number; size?: number; category?: string | null; q?: string }) {
  const searchParams = new URLSearchParams();
  searchParams.set("page", String(params.page));
  searchParams.set("size", String(params.size ?? 5));
  if (params.category) searchParams.set("category", params.category);
  if (params.q) searchParams.set("q", params.q);
  return requirePageResponse<Post>(await request<unknown>(`/api/posts?${searchParams.toString()}`));
}

export function fetchPost(id: string | number) {
  return request<Post>(`/api/posts/${id}`);
}

export function fetchCategories() {
  return request<Category[]>("/api/categories");
}

export function fetchComments(postId: string | number) {
  return request<Comment[]>(`/api/posts/${postId}/comments`);
}

export function createComment(postId: string | number, content: string) {
  return request<Comment>(`/api/posts/${postId}/comments`, {
    method: "POST",
    body: JSON.stringify({ content }),
  });
}

export function fetchCurrentUser() {
  return request<CurrentUser>("/api/auth/me");
}

export function subscribeToNewsletter(email: string) {
  return request<MessageResponse>("/api/subscriptions", {
    method: "POST",
    body: JSON.stringify({ email }),
  });
}

export function unsubscribeFromNewsletter(email: string) {
  return request<MessageResponse>("/api/subscriptions/unsubscribe-request", {
    method: "POST",
    body: JSON.stringify({ email }),
  });
}
