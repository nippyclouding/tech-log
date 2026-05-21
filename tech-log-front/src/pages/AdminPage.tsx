import { ExternalLink } from "lucide-react";
import { ADMIN_CONSOLE_URL } from "../lib/urls";

export function AdminPage() {
  return (
    <div className="container mx-auto px-4 py-24 max-w-2xl text-center">
      <h1 className="text-3xl font-bold text-slate-900 mb-4">Console</h1>
      <a
        href={ADMIN_CONSOLE_URL}
        className="inline-flex items-center gap-2 rounded-xl bg-slate-900 px-5 py-3 text-sm font-bold text-white hover:bg-slate-800"
      >
        Open
        <ExternalLink className="w-4 h-4" />
      </a>
    </div>
  );
}
