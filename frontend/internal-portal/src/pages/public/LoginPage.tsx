import { useMutation } from "@tanstack/react-query";
import { zodResolver } from "@hookform/resolvers/zod";
import { ArrowRight, ShieldCheck } from "lucide-react";
import { useForm } from "react-hook-form";
import { Link, useNavigate } from "react-router-dom";

import { Button } from "@/components/ui/button";
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from "@/components/ui/card";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import { loginSchema, type LoginSchema } from "@/features/auth/schema";
import { authService } from "@/services/auth-service";
import { useAuthStore } from "@/store/auth-store";

export function LoginPage() {
  const navigate = useNavigate();
  const setUser = useAuthStore((state) => state.setUser);
  const clearPendingRegistrationStatus = useAuthStore((state) => state.clearPendingRegistrationStatus);

  const form = useForm<LoginSchema>({
    resolver: zodResolver(loginSchema),
    defaultValues: {
      usernameOrEmail: "",
      password: "",
    },
  });

  const loginMutation = useMutation({
    mutationFn: authService.login,
    onSuccess: (user) => {
      setUser(user);
      clearPendingRegistrationStatus();
      navigate(user.role === "ADMIN" ? "/admin" : "/app", { replace: true });
    },
  });

  return (
    <div className="mx-auto grid max-w-[1240px] gap-6 xl:grid-cols-[1fr_460px]">
      <Card className="overflow-hidden border-slate-900 bg-[linear-gradient(145deg,#0f172a,#16233d)] text-white shadow-[0_20px_50px_rgba(15,23,42,0.22)]">
          <CardHeader className="space-y-5">
          <p className="text-xs font-semibold uppercase tracking-[0.24em] text-blue-200">Truy cập cổng nội bộ</p>
          <CardTitle className="max-w-2xl text-[2.7rem] leading-[1.03] text-white lg:text-[3.2rem]">
            Xây dựng hệ thống ký số và xác thực tính toàn vẹn tài liệu số theo kiến trúc microservices
          </CardTitle>
          {/* <CardDescription className="max-w-xl text-white/70">
            Xác thực, route guard và kiểm tra trạng thái tài khoản được áp dụng trước khi người dùng vào bất kỳ workflow nào được bảo vệ.
          </CardDescription> */}
        </CardHeader>
        {/* <CardContent className="grid gap-4 md:grid-cols-2">
          <div className="rounded-[24px] border border-white/10 bg-white/5 p-5">
            <ShieldCheck className="h-5 w-5 text-blue-200" />
            <p className="mt-4 font-semibold text-white">Route theo role</p>
            <p className="mt-2 text-sm leading-7 text-white/70">Admin và user được đưa vào workspace riêng, đồng thời các trang blocked state được hiển thị rõ ràng.</p>
          </div>
          <div className="rounded-[24px] border border-white/10 bg-white/5 p-5">
            <ShieldCheck className="h-5 w-5 text-blue-200" />
            <p className="mt-4 font-semibold text-white">Kiểm soát session rõ ràng</p>
            <p className="mt-2 text-sm leading-7 text-white/70">Cổng nội bộ giữ vắng vận hành được bảo vệ, nhưng trải nghiệm đăng nhập vẫn gọn và chuyên nghiệp.</p>
          </div>
        </CardContent> */}
      </Card>

      <Card>
        <CardHeader className="space-y-2">
          <p className="text-xs font-semibold uppercase tracking-[0.2em] text-accent">Đăng nhập</p>
          <CardTitle className="text-[1.9rem]">Chào mừng quay trở lại</CardTitle>
          {/* <CardDescription>
            Nhập tài khoản nội bộ để vào dashboard và các dịch vụ được cấp quyền cho vài trò của bạn.
          </CardDescription> */}
        </CardHeader>
        <CardContent>
          <form className="space-y-5" onSubmit={form.handleSubmit((values) => loginMutation.mutate(values))}>
            <div className="space-y-2">
              <Label htmlFor="usernameOrEmail">Username hoặc Email</Label>
              <Input id="usernameOrEmail" placeholder="Nhập username hoặc email" {...form.register("usernameOrEmail")} />
              {form.formState.errors.usernameOrEmail ? (
                <p className="text-sm text-red-600">{form.formState.errors.usernameOrEmail.message}</p>
              ) : null}
            </div>
            <div className="space-y-2">
              <Label htmlFor="password">Mật khẩu</Label>
              <Input id="password" type="password" placeholder="Nhập mật khẩu" {...form.register("password")} />
              {form.formState.errors.password ? (
                <p className="text-sm text-red-600">{form.formState.errors.password.message}</p>
              ) : null}
            </div>
            {loginMutation.isError ? <p className="text-sm text-red-600">{loginMutation.error.message}</p> : null}
            <Button className="w-full" size="lg" type="submit" disabled={loginMutation.isPending}>
              {loginMutation.isPending ? "Đang xử lý..." : "Đăng nhập"}
              <ArrowRight className="ml-2 h-4 w-4" />
            </Button>
          </form>

          <p className="mt-6 text-sm text-muted-foreground">
            Chưa có tài khoản?{" "}
            <Link className="font-semibold text-primary" to="/dang-ky">
              Đăng ký
            </Link>
          </p>
        </CardContent>
      </Card>
    </div>
  );
}
