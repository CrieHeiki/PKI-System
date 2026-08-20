import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";

export function IntroPage() {
  return (
    <div className="grid gap-6 lg:grid-cols-[1.05fr_0.95fr]">
      <Card className="border-slate-900 bg-[linear-gradient(145deg,#0f172a,#16233d)] text-white shadow-[0_20px_50px_rgba(15,23,42,0.22)]">
        <CardHeader className="space-y-4">
          <p className="text-xs font-semibold uppercase tracking-[0.2em] text-blue-200">Tổng quan hệ thống</p>
          <CardTitle className="max-w-4xl text-[2.7rem] leading-[1.05] text-white lg:text-[3.25rem]">
            Hệ thống gồm nhiều microservice được tổ chức theo luồng nghiệp vụ rõ ràng.
          </CardTitle>
        </CardHeader>
        <CardContent className="space-y-4 text-sm leading-7 text-white/74">
          <div className="rounded-[24px] border border-white/10 bg-white/5 p-6">Nginx và API Gateway dùng làm điểm vào thống nhất cho người dùng nội bộ, còn frontend mới tập trung sắp xếp trải nghiệm theo nghiệp vụ.</div>
          <div className="rounded-[24px] border border-white/10 bg-white/5 p-6">USER đi qua các bước: upload document, request certificate, tạo yêu cầu ký, step-up, execute signing, verify report.</div>
          <div className="rounded-[24px] border border-white/10 bg-white/5 p-6">ADMIN quản lý account, certificate lifecycle và audit trail, không chen vào nghiệp vụ ký như user thông thường.</div>
        </CardContent>
      </Card>

      <div className="grid gap-4">
        <Card>
          <CardHeader>
            <p className="text-xs font-semibold uppercase tracking-[0.2em] text-accent">Architecture</p>
            <CardTitle className="text-[1.8rem]">Ranh giới service</CardTitle>
          </CardHeader>
          <CardContent className="text-sm leading-7 text-slate-600">
            Account, Document, Certification, Signing Workflow, Step-up, Verification và Audit được tách theo ranh giới rõ ràng để dễ mở rộng và truy vết.
          </CardContent>
        </Card>
        <Card>
          <CardHeader>
            <p className="text-xs font-semibold uppercase tracking-[0.2em] text-accent">Operations</p>
            <CardTitle className="text-[1.8rem]">Quản trị nội bộ</CardTitle>
          </CardHeader>
          <CardContent className="text-sm leading-7 text-slate-600">
            Portal mới sắp lại trải nghiệm theo nghiệp vụ thực tế: upload document, certificate lifecycle, signing, verification và audit evidence.
          </CardContent>
        </Card>
      </div>
    </div>
  );
}
