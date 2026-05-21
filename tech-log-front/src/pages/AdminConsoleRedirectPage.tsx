import { useEffect } from "react";
import { ADMIN_CONSOLE_URL } from "../lib/urls";

export function AdminConsoleRedirectPage() {
  useEffect(() => {
    window.location.replace(ADMIN_CONSOLE_URL);
  }, []);

  return (
    <div className="container mx-auto px-4 py-24 max-w-2xl text-center">
      <p className="text-slate-500">관리자 콘솔로 이동하는 중입니다.</p>
    </div>
  );
}
