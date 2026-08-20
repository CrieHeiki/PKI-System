import { Outlet } from "react-router-dom";

export function BlockedLayout() {
  return (
    <div className="min-h-screen px-6 py-8 lg:px-10">
      <div className="mx-auto flex min-h-[calc(100vh-4rem)] max-w-4xl items-center justify-center">
        <Outlet />
      </div>
    </div>
  );
}
