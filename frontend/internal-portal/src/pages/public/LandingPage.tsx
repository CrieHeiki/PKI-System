import { ArrowRight, BadgeCheck, Fingerprint, ShieldCheck, Workflow } from "lucide-react";
import { Link } from "react-router-dom";

import { Button } from "@/components/ui/button";
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";

const stats = [
  { value: "7", label: "Microservice" },
  { value: "1", label: "Công gateway thống nhất" },
  { value: "Level 1", label: "Luồng ký thật" },
  { value: "WebAuthn", label: "Lựa chọn step-up" },
];

export function LandingPage() {
  return (
    <div className="space-y-8">
      <section className="grid gap-6 xl:grid-cols-[1.08fr_0.92fr]">
        <Card className="overflow-hidden border-slate-900 bg-[linear-gradient(145deg,#0f172a,#16233d)] text-white shadow-[0_20px_50px_rgba(15,23,42,0.22)]">
          <CardHeader className="space-y-5">
            <div className="inline-flex rounded-full border border-white/10 bg-white/8 px-4 py-2 text-[11px] font-semibold uppercase tracking-[0.28em] text-blue-100">
              Nền tảng PKI nội bộ
            </div>
            <CardTitle className="max-w-3xl text-[2.9rem] leading-[1.02] text-white lg:text-[4rem]">
              Cổng nội bộ cho PKI được tổ chức theo luồng vận hành rõ ràng.
            </CardTitle>
            <p className="max-w-2xl text-base leading-8 text-white/70">
              Một workspace tập trung cho tài liệu, chứng thư, step-up, quy trình ký, xác minh và audit mà vẫn giữ được khả năng đọc và kiểm soát rõ ràng.
            </p>
          </CardHeader>
          <CardContent className="grid gap-4 md:grid-cols-3">
            <div className="rounded-[24px] border border-white/10 bg-white/5 p-5">
              <Workflow className="h-5 w-5 text-blue-200" />
              <p className="mt-4 font-semibold text-white">Ưu tiên quy trình</p>
              <p className="mt-2 text-sm leading-6 text-white/70">Luồng user và admin được tách rõ nhưng vẫn đi qua một cổng được quản trị tập trung.</p>
            </div>
            <div className="rounded-[24px] border border-white/10 bg-white/5 p-5">
              <ShieldCheck className="h-5 w-5 text-blue-200" />
              <p className="mt-4 font-semibold text-white">Vận hành dễ đọc</p>
              <p className="mt-2 text-sm leading-6 text-white/70">Nội dung được đặt trên các bề mặt ổn định, tương phản cao thay vì nền nhiều chi tiết.</p>
            </div>
            <div className="rounded-[24px] border border-white/10 bg-white/5 p-5">
              <Fingerprint className="h-5 w-5 text-blue-200" />
              <p className="mt-4 font-semibold text-white">Đảm bảo giao dịch</p>
              <p className="mt-2 text-sm leading-6 text-white/70">Step-up rằng buộc hành động ký nhạy cảm với sự hiện diện thực sự của người dùng trên thiết bị.</p>
            </div>
          </CardContent>
        </Card>

        <div className="grid gap-6">
          <Card>
            <CardHeader className="space-y-3">
              <p className="text-xs font-semibold uppercase tracking-[0.2em] text-accent">Portal entry</p>
              <CardTitle className="text-[2.1rem]">Công khai trước, bảo vệ sau</CardTitle>
            </CardHeader>
            <CardContent className="space-y-5 text-sm leading-7 text-slate-600">
              <div className="rounded-[22px] border border-slate-200 bg-slate-50 p-5">Khách chỉ thấy landing, đăng nhập, đăng ký và giới thiệu hệ thống.</div>
              <div className="rounded-[22px] border border-slate-200 bg-slate-50 p-5">Sau khi đăng nhập, workspace sẽ tự điều chỉnh theo role và trạng thái tài khoản, đồng thời chặn rõ các blocked state.</div>
              <div className="flex flex-wrap gap-3">
                <Button asChild>
                  <Link to="/dang-nhap">
                    Đăng nhập
                    <ArrowRight className="ml-2 h-4 w-4" />
                  </Link>
                </Button>
                <Button asChild variant="secondary">
                  <Link to="/dang-ky">Đăng ký</Link>
                </Button>
              </div>
            </CardContent>
          </Card>

          <div className="grid gap-4 sm:grid-cols-2">
            {stats.map((item) => (
              <Card key={item.label} className="bg-white/96">
                <CardContent className="p-6">
                  <p className="text-[2rem] font-extrabold tracking-tight text-primary">{item.value}</p>
                  <p className="mt-1 text-sm font-medium text-slate-600">{item.label}</p>
                </CardContent>
              </Card>
            ))}
          </div>
        </div>
      </section>

      <section className="grid gap-6 lg:grid-cols-3">
        <Card>
          <CardHeader>
            <BadgeCheck className="h-5 w-5 text-primary" />
            <CardTitle className="text-[1.55rem]">Từ tài liệu đến chữ ký</CardTitle>
          </CardHeader>
          <CardContent className="text-sm leading-7 text-slate-600">
            Upload tài liệu, tạo signing request, approve bằng certificate còn hiệu lực, thực hiện step-up và sinh signed output trong MinIO.
          </CardContent>
        </Card>
        <Card>
          <CardHeader>
            <ShieldCheck className="h-5 w-5 text-primary" />
            <CardTitle className="text-[1.55rem]">Vòng đời chứng thư</CardTitle>
          </CardHeader>
          <CardContent className="text-sm leading-7 text-slate-600">
            Yêu cầu cấp, gia hạn hoặc thu hồi theo luồng admin kiểm soát, được hỗ trợ bởi các PKI service nội bộ.
          </CardContent>
        </Card>
        <Card>
          <CardHeader>
            <Fingerprint className="h-5 w-5 text-primary" />
            <CardTitle className="text-[1.55rem]">Xác minh và audit</CardTitle>
          </CardHeader>
          <CardContent className="text-sm leading-7 text-slate-600">
            Xem verification report, truy vết sự kiện vận hành và giữ bằng chứng đủ rõ để demo, quản trị và đưa vào báo cáo.
          </CardContent>
        </Card>
      </section>
    </div>
  );
}
