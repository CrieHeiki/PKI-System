import { useMutation } from "@tanstack/react-query";
import { zodResolver } from "@hookform/resolvers/zod";
import { ArrowRight, UserPlus } from "lucide-react";
import { useForm } from "react-hook-form";
import { Link, useNavigate } from "react-router-dom";

import { Button } from "@/components/ui/button";
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from "@/components/ui/card";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import { registerSchema, type RegisterSchema } from "@/features/auth/schema";
import { authService } from "@/services/auth-service";
import { useAuthStore } from "@/store/auth-store";

export function RegisterPage() {
  const navigate = useNavigate();
  const setPendingRegistrationStatus = useAuthStore((state) => state.setPendingRegistrationStatus);

  const form = useForm<RegisterSchema>({
    resolver: zodResolver(registerSchema),
    defaultValues: {
      username: "",
      email: "",
      password: "",
    },
  });

  const registerMutation = useMutation({
    mutationFn: authService.register,
    onSuccess: (result) => {
      setPendingRegistrationStatus(result.status);
      if (result.status === "PENDING") {
        navigate("/blocked/pending", { replace: true });
        return;
      }
      navigate("/dang-nhap", { replace: true });
    },
  });

  return (
    <div className="mx-auto grid max-w-[1240px] gap-6 xl:grid-cols-[0.95fr_1.05fr]">
      <Card className="overflow-hidden border-slate-900 bg-[linear-gradient(145deg,#0f172a,#16233d)] text-white shadow-[0_20px_50px_rgba(15,23,42,0.22)]">
        <CardHeader className="space-y-5">
          <p className="text-xs font-semibold uppercase tracking-[0.24em] text-blue-200">Khởi tạo tài khoản nội bộ</p>
          <CardTitle className="text-[2.7rem] leading-[1.03] text-white lg:text-[3.2rem]">
            Tạo tài khoản nội bộ mới cho không gian làm việc PKI.
          </CardTitle>
          {/* <CardDescription className="max-w-xl text-white/70">
            Tài khoản mới bắt đầu ở trạng thái chờ kích hoạt và chỉ được vào nghiệp vụ sau khi quản trị viên phê duyệt.
          </CardDescription> */}
        </CardHeader>
        {/* <CardContent className="space-y-4">
          <div className="rounded-[24px] border border-white/10 bg-white/5 p-5">
            <UserPlus className="h-5 w-5 text-blue-200" />
            <p className="mt-4 font-semibold text-white">Phù hợp cho người dùng nội bộ</p>
            <p className="mt-2 text-sm leading-7 text-white/70">Người yêu cầu ký, người ký và các vai trò hỗ trợ có thể bắt đầu từ luồng đăng ký được kiểm soát này.</p>
          </div>
          <div className="rounded-[24px] border border-white/10 bg-white/5 p-5 text-sm leading-7 text-white/70">
            Sau khi được kích hoạt, người dùng có thể tải lên tài liệu, gửi yêu cầu chứng thư, tạo yêu cầu ký và xem kết quả xác minh.
          </div>
        </CardContent> */}
      </Card>

      <Card>
        <CardHeader className="space-y-2">
          <p className="text-xs font-semibold uppercase tracking-[0.2em] text-accent">Đăng ký</p>
          <CardTitle className="text-[1.9rem]">Tạo tài khoản nội bộ</CardTitle>
          <CardDescription>Sau khi tạo thành công, tài khoản mới sẽ chờ kích hoạt nếu hệ thống trả về trạng thái chờ duyệt.</CardDescription>
        </CardHeader>
        <CardContent>
          <form className="grid gap-5 md:grid-cols-2" onSubmit={form.handleSubmit((values) => registerMutation.mutate(values))}>
            <div className="space-y-2">
              <Label htmlFor="username">Tên đăng nhập</Label>
              <Input id="username" placeholder="nguoi-dung-noi-bo" {...form.register("username")} />
              {form.formState.errors.username ? <p className="text-sm text-red-600">{form.formState.errors.username.message}</p> : null}
            </div>
            <div className="space-y-2">
              <Label htmlFor="email">Email</Label>
              <Input id="email" type="email" placeholder="user@company.local" {...form.register("email")} />
              {form.formState.errors.email ? <p className="text-sm text-red-600">{form.formState.errors.email.message}</p> : null}
            </div>
            <div className="space-y-2 md:col-span-2">
              <Label htmlFor="password">Mật khẩu</Label>
              <Input id="password" type="password" placeholder="Nhập mật khẩu" {...form.register("password")} />
              {form.formState.errors.password ? <p className="text-sm text-red-600">{form.formState.errors.password.message}</p> : null}
            </div>
            {registerMutation.isError ? <p className="text-sm text-red-600 md:col-span-2">{registerMutation.error.message}</p> : null}
            <div className="md:col-span-2">
              <Button size="lg" type="submit" disabled={registerMutation.isPending}>
                {registerMutation.isPending ? "Đang tạo tài khoản..." : "Tạo tài khoản"}
                <ArrowRight className="ml-2 h-4 w-4" />
              </Button>
            </div>
          </form>

          <p className="mt-6 text-sm text-muted-foreground">
            Đã có tài khoản?{" "}
            <Link className="font-semibold text-primary" to="/dang-nhap">
              Đăng nhập
            </Link>
          </p>
        </CardContent>
      </Card>
    </div>
  );
}
