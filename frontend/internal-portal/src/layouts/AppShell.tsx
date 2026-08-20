import { Outlet } from "react-router-dom";

import { Sidebar } from "@/components/common/Sidebar";
import { Topbar } from "@/components/common/Topbar";

export function AppShell() {
  return (
    <div className="min-h-screen px-4 py-4 sm:px-6 lg:px-8 lg:py-6 2xl:px-10">
      <div className="mx-auto flex max-w-[1600px] flex-col gap-6">
        <Topbar />
        <div className="grid items-start gap-6 xl:grid-cols-[300px_minmax(0,1fr)] 2xl:gap-8">
          <Sidebar />
          <div className="min-w-0 space-y-7 pb-12">
            <Outlet />
          </div>
        </div>
      </div>
    </div>
  );
}
