import { Link, matchPath, useLocation } from "react-router-dom";

import { cn } from "@/lib/utils";
import { getMenuForRole, type SidebarMenuItem } from "@/routes/menu";
import { useAuthStore } from "@/store/auth-store";

function isMenuItemActive(pathname: string, item: SidebarMenuItem) {
  if (item.excludePatterns?.some((pattern) => Boolean(matchPath({ path: pattern, end: true }, pathname)))) {
    return false;
  }

  const patterns = item.activePatterns?.length ? item.activePatterns : [item.to];
  return patterns.some((pattern) => Boolean(matchPath({ path: pattern, end: true }, pathname)));
}

export function Sidebar() {
  const user = useAuthStore((state) => state.user);
  const menu = getMenuForRole(user?.role ?? "USER");
  const location = useLocation();

  return (
    <aside className="rounded-3xl border border-slate-200 bg-slate-50 p-4 text-slate-900 shadow-sm xl:sticky xl:top-6 xl:self-start">
      <div className="space-y-4">
        <div className="rounded-[22px] bg-slate-900 px-4 py-4 text-white">
          <p className="text-[11px] font-semibold uppercase tracking-[0.22em] text-slate-300">PKI Platform</p>
          <h2 className="mt-2 font-heading text-[1.2rem] font-semibold tracking-tight text-white">
            {user?.role === "ADMIN" ? "Khu vực quản trị" : "Khu vực người dùng"}
          </h2>
          <p className="mt-2 text-sm leading-6 text-white/70">
            Điều hướng theo nghiệp vụ và quyền hạn trong một không gian làm việc tập trung.
          </p>
        </div>

        <div className="rounded-2xl border border-slate-200 bg-white px-4 py-3">
          <p className="text-xs font-semibold uppercase tracking-[0.16em] text-slate-400">Tài khoản hiện tại</p>
          <p className="mt-2 text-base font-semibold text-slate-950">{user?.username}</p>
        </div>
      </div>

      <nav className="space-y-6 pt-6">
        {menu.map((group) => (
          <div key={group.label} className="space-y-2">
            <p className="px-2 text-[11px] font-semibold uppercase tracking-[0.18em] text-slate-400">{group.label}</p>
            <div className="space-y-1.5">
              {group.items.map((item) => {
                const isActive = isMenuItemActive(location.pathname, item);

                return (
                  <Link
                    key={`${group.label}-${item.to}-${item.label}`}
                    to={item.to}
                    aria-current={isActive ? "page" : undefined}
                    className={cn(
                      "group flex items-center gap-3 rounded-2xl px-3 py-3 text-sm font-medium transition-all",
                      isActive
                        ? "bg-white text-slate-950 shadow-sm ring-1 ring-slate-200"
                        : "text-slate-600 hover:bg-white hover:text-slate-950",
                    )}
                  >
                    <span
                      className={cn(
                        "flex h-9 w-9 shrink-0 items-center justify-center rounded-xl border bg-white shadow-sm",
                        isActive ? "border-primary/15 text-primary" : "border-slate-200 text-slate-600",
                      )}
                    >
                      <item.icon className="h-4 w-4 shrink-0" />
                    </span>
                    <span>{item.label}</span>
                  </Link>
                );
              })}
            </div>
          </div>
        ))}
      </nav>
    </aside>
  );
}
