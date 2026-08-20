import { Link, NavLink, Outlet } from "react-router-dom";

import { Button } from "@/components/ui/button";
import { cn } from "@/lib/utils";

const navItems = [
  { to: "/dang-nhap", label: "Đăng nhập" },
  { to: "/dang-ky", label: "Đăng ký" },
];

export function PublicLayout() {
  return (
    <div className="min-h-screen bg-[radial-gradient(circle_at_top_left,#dbeafe_0,#f8fafc_30%,#f8fafc_100%)] px-4 py-4 sm:px-6 lg:px-8">
      <div className="mx-auto flex min-h-screen max-w-[1440px] flex-col gap-8">
        <header className="rounded-[30px] border border-white/70 bg-white/88 px-5 py-4 shadow-[0_18px_45px_rgba(15,23,42,0.08)] backdrop-blur sm:px-6">
          <div className="flex flex-col gap-5 lg:flex-row lg:items-center lg:justify-between">
            <Link to="/dang-nhap" className="min-w-[240px]">
              <div className="inline-flex rounded-full border border-primary/10 bg-primary/5 px-3 py-1 text-[11px] font-semibold uppercase tracking-[0.28em] text-primary">
                CT556 Internal PKI
              </div>
              <p className="mt-3 text-lg font-semibold tracking-tight text-slate-950">Cổng truy cập nội bộ</p>
              <p className="mt-1 max-w-md text-sm leading-6 text-slate-500">
                Không gian tập trung cho ký số, xác minh tài liệu và theo dõi nhật ký kiểm toán.
              </p>
            </Link>

            <nav className="flex flex-wrap items-center gap-2 rounded-full border border-slate-200 bg-slate-50/90 p-1.5 shadow-inner">
              {navItems.map((item) => (
                <NavLink
                  key={item.to}
                  to={item.to}
                  end
                  className={({ isActive }) =>
                    cn(
                      "rounded-full px-5 py-2.5 text-sm font-semibold transition-all duration-200",
                      isActive
                        ? "bg-white text-slate-950 shadow-[0_8px_18px_rgba(15,23,42,0.08)]"
                        : "text-slate-600 hover:bg-white/75 hover:text-slate-900",
                    )
                  }
                >
                  {item.label}
                </NavLink>
              ))}
            </nav>

            <div className="flex min-w-[240px] justify-start lg:justify-end">
              <Button asChild className="rounded-full px-6 shadow-[0_12px_24px_rgba(37,99,235,0.18)]" size="lg">
                <Link to="/dang-nhap">Vào cổng nội bộ</Link>
              </Button>
            </div>
          </div>
        </header>

        <main className="flex-1 pb-10">
          <Outlet />
        </main>
      </div>
    </div>
  );
}
