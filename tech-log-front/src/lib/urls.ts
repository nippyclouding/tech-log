const meta = import.meta as ImportMeta & { env?: Record<string, string | undefined> };

export const BACKEND_ORIGIN = meta.env?.VITE_BACKEND_ORIGIN ?? "";

export const ADMIN_CONSOLE_URL = `${BACKEND_ORIGIN}/admin-console`;
