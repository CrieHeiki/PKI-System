import { LogOut, ShieldCheck } from "lucide-react";
import { useNavigate } from "react-router-dom";

import { StatusBadge } from "@/components/common/StatusBadge";
import { Button } from "@/components/ui/button";
import { useAuthStore } from "@/store/auth-store";

export function Topbar() {
  const navigate = useNavigate();
  const user = useAuthStore((state) => state.user);
  const logout = useAuthStore((state) => state.logout);

  return (
    <header className="rounded-3xl border border-slate-200 bg-white px-5 py-4 shadow-sm lg:px-6">
      <div className="flex flex-col gap-5 xl:flex-row xl:items-center xl:justify-between">
        <div className="space-y-2">
          <div className="text-xs font-semibold uppercase tracking-[0.22em] text-slate-400">Hệ thống ký số nội bộ</div>
          <div>
            <h2 className="font-heading text-[1.5rem] font-semibold tracking-tight text-slate-950 lg:text-[1.75rem]">
              Hệ thống ký số nội bộ
            </h2>
            <p className="max-w-3xl text-sm leading-7 text-slate-500">
              Tài liệu, chứng thư, quy trình ký, xác minh và nhật ký kiểm toán được tập trung trong một cổng nội bộ thống nhất.
            </p>
          </div>
        </div>

        {user ? (
          <div className="flex flex-col gap-3 sm:flex-row sm:items-center">
            <div className="rounded-2xl border border-slate-200 bg-slate-50 px-4 py-3">
              <div className="flex items-center gap-2 text-xs font-semibold uppercase tracking-[0.18em] text-slate-500">
                <ShieldCheck className="h-4 w-4 text-primary" />
                Phiên hiện tại
              </div>
              <div className="mt-2 flex flex-wrap items-center gap-2">
                <span className="rounded-full border border-slate-200 bg-white px-3 py-1 text-sm font-semibold text-slate-900">{user.username}</span>
                <span className="rounded-full border border-slate-200 bg-white px-3 py-1 text-sm font-medium text-slate-700">{user.role}</span>
                <StatusBadge status={user.status} />
              </div>
            </div>
            <Button
              variant="outline"
              onClick={() => {
                logout();
                navigate("/dang-nhap", { replace: true });
              }}
            >
              <LogOut className="mr-2 h-4 w-4" />
              Đăng xuất
            </Button>
          </div>
        ) : null}
      </div>
    </header>
  );
}
