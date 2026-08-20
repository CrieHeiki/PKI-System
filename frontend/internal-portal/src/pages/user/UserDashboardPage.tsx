import { useQuery } from "@tanstack/react-query";
import { ArrowRight, FileText, Fingerprint, PenTool, ShieldCheck } from "lucide-react";
import { Link } from "react-router-dom";

import { PageHeader } from "@/components/common/PageHeader";
import { Button } from "@/components/ui/button";
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";
import { certificatesService } from "@/services/certificates-service";
import { documentsService } from "@/services/documents-service";
import { signingService } from "@/services/signing-service";
import { verificationService } from "@/services/verification-service";
import { useAuthStore } from "@/store/auth-store";

export function UserDashboardPage() {
  const user = useAuthStore((state) => state.user);
  const documentsQuery = useQuery({ queryKey: ["documents", "mine"], queryFn: documentsService.listMine });
  const mineSigningQuery = useQuery({ queryKey: ["signing", "mine"], queryFn: signingService.listMine });
  const toSignQuery = useQuery({ queryKey: ["signing", "to-sign"], queryFn: signingService.listPendingForMe });
  const certificatesQuery = useQuery({ queryKey: ["certificates", "mine"], queryFn: certificatesService.listMine });
  const reportsQuery = useQuery({ queryKey: ["verification", "recent"], queryFn: verificationService.listRecent });

  const summaryCards = [
    {
      label: "Tài liệu của tôi",
      value: documentsQuery.data?.length ?? 0,
      description: "Tổng số tài liệu bạn đang sở hữu trong hệ thống.",
      to: "/app/documents",
      action: "Mở danh sách tài liệu",
      icon: FileText,
      tone: "border-emerald-200/70 bg-emerald-50/75",
      iconTone: "bg-white text-emerald-700",
    },
    {
      label: "Quy trình ký",
      value: mineSigningQuery.data?.length ?? 0,
      description: `${toSignQuery.data?.length ?? 0} yêu cầu đang chờ bạn xử lý.`,
      to: "/app/sign-requests/mine",
      action: "Mở quy trình ký",
      icon: PenTool,
      tone: "border-sky-200/70 bg-sky-50/75",
      iconTone: "bg-white text-sky-700",
    },
    {
      label: "Chứng thư và xác minh",
      value: certificatesQuery.data?.length ?? 0,
      description: `${reportsQuery.data?.length ?? 0} báo cáo xác minh gần đây.`,
      to: "/app/certificate",
      action: "Mở khu chứng thư",
      icon: ShieldCheck,
      tone: "border-amber-200/70 bg-amber-50/75",
      iconTone: "bg-white text-amber-700",
    },
  ];

  return (
    <div className="space-y-7">
      <PageHeader
        eyebrow="Bảng điều khiển người dùng"
        title="Không gian làm việc của người dùng"
        description="Tài liệu, yêu cầu ký, chứng thư và báo cáo xác minh được sắp lại theo nghiệp vụ để bạn đi từng bước dễ hơn."
        actions={
          <div className="flex flex-wrap gap-3">
            <Button asChild>
              <Link to="/app/sign-requests/create">Tạo yêu cầu ký</Link>
            </Button>
            <Button asChild variant="outline">
              <Link to="/app/documents/upload">Tải lên tài liệu</Link>
            </Button>
          </div>
        }
      />

      <section className="grid gap-5 xl:grid-cols-[1.35fr_0.95fr]">
        <Card className="overflow-hidden bg-[linear-gradient(135deg,rgba(255,252,247,0.98),rgba(242,250,248,0.94))]">
          <CardHeader className="relative space-y-4">
            <div className="absolute -right-6 top-0 h-40 w-40 rounded-full bg-emerald-400/10 blur-3xl" />
            <div className="absolute left-12 top-8 h-24 w-24 rounded-full bg-sky-300/10 blur-3xl" />
            <p className="text-xs font-semibold uppercase tracking-[0.22em] text-accent">Không gian làm việc</p>
            <CardTitle className="max-w-2xl text-[2.35rem] leading-[1.05] text-slate-950">Một bảng điều khiển để đi từ tài liệu đến ký, xác minh và lưu dấu vết.</CardTitle>
            <p className="max-w-2xl text-sm leading-7 text-slate-600">
              Cổng nội bộ này ưu tiên sự rõ ràng: bạn thấy ngay tài liệu của mình, yêu cầu cần xử lý, chứng thư hiện có và các báo cáo xác minh đã tạo.
            </p>
            <div className="flex flex-wrap gap-2">
              <span className="rounded-full border border-emerald-200 bg-white/90 px-3 py-1 text-[11px] font-semibold uppercase tracking-[0.16em] text-emerald-700">
                {documentsQuery.data?.length ?? 0} tài liệu
              </span>
              <span className="rounded-full border border-sky-200 bg-white/90 px-3 py-1 text-[11px] font-semibold uppercase tracking-[0.16em] text-sky-700">
                {toSignQuery.data?.length ?? 0} yêu cầu chờ ký
              </span>
              <span className="rounded-full border border-amber-200 bg-white/90 px-3 py-1 text-[11px] font-semibold uppercase tracking-[0.16em] text-amber-700">
                {certificatesQuery.data?.length ?? 0} chứng thư đang theo dõi
              </span>
            </div>
          </CardHeader>
          <CardContent className="grid gap-4 md:grid-cols-3">
            {summaryCards.map((card) => (
              <div key={card.label} className={`rounded-[24px] border p-6 backdrop-blur ${card.tone}`}>
                <div className="flex items-center gap-3">
                  <span className={`flex h-11 w-11 items-center justify-center rounded-2xl shadow-sm ${card.iconTone}`}>
                    <card.icon className="h-5 w-5" />
                  </span>
                  <div>
                    <p className="text-xs uppercase tracking-[0.18em] text-slate-500">{card.label}</p>
                    <p className="text-3xl font-semibold text-slate-950">{card.value}</p>
                  </div>
                </div>
                <p className="mt-4 text-sm leading-6 text-slate-700">{card.description}</p>
                <Link className="mt-4 inline-flex items-center gap-2 text-sm font-medium text-slate-900" to={card.to}>
                  {card.action}
                  <ArrowRight className="h-4 w-4" />
                </Link>
              </div>
            ))}
          </CardContent>
        </Card>

        <Card className="bg-[linear-gradient(180deg,rgba(255,252,247,0.98),rgba(249,244,236,0.94))]">
          <CardHeader>
            <p className="text-xs font-semibold uppercase tracking-[0.2em] text-accent">Phiên làm việc</p>
            <CardTitle className="text-[2rem]">Thông tin người dùng</CardTitle>
          </CardHeader>
          <CardContent className="space-y-3 text-sm text-slate-600">
            {[
              ["Tên đăng nhập", user?.username],
              ["Email", user?.email],
              ["Vai trò", user?.role],
              ["Trạng thái", user?.status],
            ].map(([label, value]) => (
              <div key={label} className="flex items-center justify-between rounded-2xl border border-slate-200/80 bg-white/82 px-4 py-3">
                <span className="text-slate-500">{label}</span>
                <span className="font-medium text-slate-900">{value}</span>
              </div>
            ))}
            <div className="rounded-[22px] border border-slate-200/80 bg-white/82 p-4">
              <p className="text-xs uppercase tracking-[0.18em] text-slate-400">Mã tài khoản</p>
              <p className="mt-2 break-all text-sm font-medium text-slate-900">{user?.accountId}</p>
            </div>
            <div className="rounded-[22px] border border-sky-200/70 bg-sky-50/80 p-4 text-sm leading-6 text-sky-950">
              Nếu bạn là người ký, hãy ưu tiên đăng ký WebAuthn để thao tác ký số chỉ diễn ra khi có sự hiện diện thật sự trên thiết bị của bạn.
            </div>
          </CardContent>
        </Card>
      </section>

      <section className="grid gap-5 lg:grid-cols-3">
        <Card>
          <CardHeader>
            <CardTitle className="text-[1.65rem]">Bước tiếp theo</CardTitle>
          </CardHeader>
          <CardContent className="space-y-3">
            <Link className="flex items-center justify-between rounded-[22px] border border-slate-200/80 bg-white/82 px-4 py-3 text-sm font-medium text-slate-900 transition hover:border-primary/30 hover:bg-white" to="/app/documents/upload">
              Tải lên tài liệu mới
              <ArrowRight className="h-4 w-4 text-slate-400" />
            </Link>
            <Link className="flex items-center justify-between rounded-[22px] border border-slate-200/80 bg-white/82 px-4 py-3 text-sm font-medium text-slate-900 transition hover:border-primary/30 hover:bg-white" to="/app/sign-requests/create">
              Tạo yêu cầu ký
              <ArrowRight className="h-4 w-4 text-slate-400" />
            </Link>
            <Link className="flex items-center justify-between rounded-[22px] border border-slate-200/80 bg-white/82 px-4 py-3 text-sm font-medium text-slate-900 transition hover:border-primary/30 hover:bg-white" to="/app/verify">
              Xác minh tài liệu đã ký
              <ArrowRight className="h-4 w-4 text-slate-400" />
            </Link>
          </CardContent>
        </Card>

        <Card className="lg:col-span-2">
          <CardHeader className="flex flex-row items-start justify-between gap-4">
            <div>
              <p className="text-xs font-semibold uppercase tracking-[0.2em] text-accent">Bảo mật giao dịch</p>
              <CardTitle className="text-[1.65rem]">Step-up cho người ký</CardTitle>
            </div>
            <Fingerprint className="h-5 w-5 text-accent" />
          </CardHeader>
          <CardContent className="grid gap-4 md:grid-cols-3">
            <div className="rounded-[22px] border border-slate-200/80 bg-white/82 p-5 text-sm leading-6 text-slate-600">
              <p className="font-semibold text-slate-950">1. Duyệt yêu cầu</p>
              <p className="mt-2">Người ký chọn chứng thư đang hoạt động và duyệt yêu cầu trong trang chi tiết.</p>
            </div>
            <div className="rounded-[22px] border border-slate-200/80 bg-white/82 p-5 text-sm leading-6 text-slate-600">
              <p className="font-semibold text-slate-950">2. Step-up bằng WebAuthn</p>
              <p className="mt-2">Trình duyệt gọi authenticator của thiết bị để xác minh sự hiện diện thật sự của người dùng.</p>
            </div>
            <div className="rounded-[22px] border border-slate-200/80 bg-white/82 p-5 text-sm leading-6 text-slate-600">
              <p className="font-semibold text-slate-950">3. Thực hiện ký số</p>
              <p className="mt-2">Sau khi có proof token, backend mới cho phép ký và sinh đầu ra đã ký trên MinIO.</p>
            </div>
          </CardContent>
        </Card>
      </section>
    </div>
  );
}
