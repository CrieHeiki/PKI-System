import { useQuery } from "@tanstack/react-query";
import { ArrowRight, BadgeCheck, Files, Shield, ShieldAlert, ShieldCheck, Users } from "lucide-react";
import { Link } from "react-router-dom";

import { PageHeader } from "@/components/common/PageHeader";
import { StatusBadge } from "@/components/common/StatusBadge";
import { Button } from "@/components/ui/button";
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";
import { accountsService } from "@/services/accounts-service";
import { auditService } from "@/services/audit-service";
import { certificatesService } from "@/services/certificates-service";
import { useAuthStore } from "@/store/auth-store";

function formatDateTime(value?: string | null) {
  if (!value) return "Chưa có dữ liệu";
  return new Date(value).toLocaleString("vi-VN");
}

export function AdminDashboardPage() {
  const user = useAuthStore((state) => state.user);
  const accountsQuery = useQuery({ queryKey: ["admin", "accounts"], queryFn: () => accountsService.list() });
  const certRequestsQuery = useQuery({
    queryKey: ["admin", "certificate-requests", "all"],
    queryFn: () => certificatesService.listAdminRequests(),
  });
  const certificatesQuery = useQuery({ queryKey: ["admin", "certificates"], queryFn: certificatesService.listAll });
  const auditQuery = useQuery({ queryKey: ["admin", "audit"], queryFn: () => auditService.search({ limit: 10 }) });

  const accounts = accountsQuery.data ?? [];
  const certificateRequests = certRequestsQuery.data ?? [];
  const certificates = certificatesQuery.data ?? [];
  const auditEvents = auditQuery.data ?? [];

  const activeAccounts = accounts.filter((item) => item.status === "ACTIVE").length;
  const pendingAccounts = accounts.filter((item) => item.status === "PENDING").length;
  const revokedCertificates = certificates.filter((item) => item.status === "REVOKED").length;
  const renewalRequests = certificateRequests.filter((item) => item.requestType === "RENEWAL").length;
  const latestAudit = auditEvents.slice(0, 4);
  const latestCertificateRequests = certificateRequests.slice(0, 4);

  return (
    <div className="space-y-7">
      <PageHeader
        eyebrow="Quản trị"
        title="Trung tâm điều phối hệ thống"
        description="Theo dõi tài khoản, vòng đời chứng thư và nhật ký kiểm toán trong một không gian quản trị gọn, rõ và ưu tiên hành động."
        actions={
          <div className="flex flex-wrap gap-3">
            <Button asChild>
              <Link to="/admin/accounts">Mở quản lý tài khoản</Link>
            </Button>
            <Button asChild variant="secondary">
              <Link to="/admin/audit">Xem nhật ký kiểm toán</Link>
            </Button>
          </div>
        }
      />

      <section className="grid gap-5 xl:grid-cols-[1.35fr_0.95fr]">
        <Card className="bg-slate-950 text-white shadow-[0_18px_45px_rgba(15,23,42,0.18)]">
          <CardHeader className="space-y-4">
            <p className="text-xs font-semibold uppercase tracking-[0.2em] text-blue-200">Toàn cảnh vận hành</p>
            <CardTitle className="max-w-2xl text-[2.2rem] leading-[1.04] text-white">
              Ưu tiên kiểm soát, đồng thời nhìn nhanh vào những quyết định đang chờ xử lý.
            </CardTitle>
            <p className="max-w-2xl text-sm leading-7 text-white/70">
              Màn hình này ưu tiên hiển thị những điểm quản trị viên cần xử lý ngay: tài khoản chờ kích hoạt, hàng đợi cấp chứng thư, yêu cầu gia hạn và dòng sự kiện kiểm toán gần nhất.
            </p>
          </CardHeader>
          <CardContent className="grid gap-4 md:grid-cols-3">
            <div className="rounded-[22px] border border-white/10 bg-white/5 p-5">
              <Users className="h-5 w-5 text-blue-200" />
              <p className="mt-4 text-xs font-semibold uppercase tracking-[0.16em] text-white/58">Tài khoản</p>
              <p className="mt-2 text-3xl font-bold text-white">{accounts.length}</p>
              <p className="mt-2 text-sm leading-6 text-white/70">{activeAccounts} đang hoạt động, {pendingAccounts} chờ kích hoạt.</p>
            </div>
            <div className="rounded-[22px] border border-white/10 bg-white/5 p-5">
              <BadgeCheck className="h-5 w-5 text-blue-200" />
              <p className="mt-4 text-xs font-semibold uppercase tracking-[0.16em] text-white/58">Chứng thư</p>
              <p className="mt-2 text-3xl font-bold text-white">{certificates.length}</p>
              <p className="mt-2 text-sm leading-6 text-white/70">{certificateRequests.length} yêu cầu, {renewalRequests} yêu cầu gia hạn.</p>
            </div>
            <div className="rounded-[22px] border border-white/10 bg-white/5 p-5">
              <ShieldCheck className="h-5 w-5 text-blue-200" />
              <p className="mt-4 text-xs font-semibold uppercase tracking-[0.16em] text-white/58">Kiểm toán</p>
              <p className="mt-2 text-3xl font-bold text-white">{auditEvents.length}</p>
              <p className="mt-2 text-sm leading-6 text-white/70">Các sự kiện mới nhất sẵn sàng để rà soát.</p>
            </div>
          </CardContent>
        </Card>

        <Card>
          <CardHeader className="space-y-2">
            <p className="text-xs font-semibold uppercase tracking-[0.18em] text-accent">Quản trị viên hiện tại</p>
            <CardTitle className="text-[1.6rem]">Hồ sơ phiên làm việc</CardTitle>
          </CardHeader>
          <CardContent className="space-y-3">
            <div className="grid gap-3">
              <div className="rounded-[18px] border border-slate-200 bg-slate-50 px-4 py-3">
                <p className="text-xs uppercase tracking-[0.16em] text-slate-400">Tên đăng nhập</p>
                <p className="mt-1 font-semibold text-slate-900">{user?.username}</p>
              </div>
              <div className="rounded-[18px] border border-slate-200 bg-slate-50 px-4 py-3">
                <p className="text-xs uppercase tracking-[0.16em] text-slate-400">Email</p>
                <p className="mt-1 font-medium text-slate-900">{user?.email}</p>
              </div>
              <div className="flex items-center justify-between rounded-[18px] border border-slate-200 bg-slate-50 px-4 py-3">
                <span className="text-sm text-slate-500">Vai trò</span>
                <span className="rounded-full border border-slate-200 bg-white px-3 py-1 text-xs font-semibold uppercase tracking-[0.16em] text-slate-700">
                  {user?.role}
                </span>
              </div>
              <div className="flex items-center justify-between rounded-[18px] border border-slate-200 bg-slate-50 px-4 py-3">
                <span className="text-sm text-slate-500">Trạng thái</span>
                {user?.status ? <StatusBadge status={user.status} /> : null}
              </div>
            </div>
          </CardContent>
        </Card>
      </section>

      <section className="grid gap-5 lg:grid-cols-3">
        <Card>
          <CardHeader className="flex flex-row items-start justify-between gap-4">
            <div>
              <p className="text-xs font-semibold uppercase tracking-[0.18em] text-accent">Thao tác nhanh</p>
              <CardTitle className="text-[1.45rem]">Truy cập nhanh</CardTitle>
            </div>
            <Shield className="h-5 w-5 text-primary" />
          </CardHeader>
          <CardContent className="space-y-3">
            <Link className="flex items-center justify-between rounded-[18px] border border-slate-200 bg-slate-50 px-4 py-3 text-sm font-medium text-slate-900 transition hover:border-primary/30 hover:bg-white" to="/admin/accounts">
              Kích hoạt và quản lý tài khoản
              <ArrowRight className="h-4 w-4 text-slate-400" />
            </Link>
            <Link className="flex items-center justify-between rounded-[18px] border border-slate-200 bg-slate-50 px-4 py-3 text-sm font-medium text-slate-900 transition hover:border-primary/30 hover:bg-white" to="/admin/certificates/requests">
              Xử lý yêu cầu cấp chứng thư
              <ArrowRight className="h-4 w-4 text-slate-400" />
            </Link>
            <Link className="flex items-center justify-between rounded-[18px] border border-slate-200 bg-slate-50 px-4 py-3 text-sm font-medium text-slate-900 transition hover:border-primary/30 hover:bg-white" to="/admin/audit">
              Theo dõi nhật ký kiểm toán
              <ArrowRight className="h-4 w-4 text-slate-400" />
            </Link>
          </CardContent>
        </Card>

        <Card className="lg:col-span-2">
          <CardHeader className="flex flex-row items-start justify-between gap-4">
            <div>
              <p className="text-xs font-semibold uppercase tracking-[0.18em] text-accent">Hàng đợi chứng thư</p>
              <CardTitle className="text-[1.45rem]">Yêu cầu gần đây</CardTitle>
            </div>
            <Files className="h-5 w-5 text-primary" />
          </CardHeader>
          <CardContent className="space-y-3">
            {latestCertificateRequests.length ? (
              latestCertificateRequests.map((request) => (
                <div key={request.id} className="flex flex-col gap-3 rounded-[18px] border border-slate-200 bg-slate-50 px-5 py-4 md:flex-row md:items-center md:justify-between">
                  <div className="space-y-1">
                    <p className="font-semibold text-slate-900">{request.commonName}</p>
                    <p className="text-sm text-slate-500">
                      Loại {request.requestType} • trạng thái {request.status} • gửi lúc {formatDateTime(request.requestedAt)}
                    </p>
                  </div>
                  <Button asChild variant="outline">
                    <Link to={request.requestType === "RENEWAL" ? "/admin/certificates/renewals" : "/admin/certificates/requests"}>Mở phân hệ</Link>
                  </Button>
                </div>
              ))
            ) : (
              <Card className="border-dashed bg-slate-50/70">
                <CardContent className="pt-7 text-sm text-slate-500">Chưa có yêu cầu chứng thư mới trong hàng đợi.</CardContent>
              </Card>
            )}
          </CardContent>
        </Card>
      </section>

      <section className="grid gap-5 xl:grid-cols-[1.05fr_0.95fr]">
        <Card>
          <CardHeader>
            <p className="text-xs font-semibold uppercase tracking-[0.18em] text-accent">Ảnh chụp kiểm toán</p>
            <CardTitle className="text-[1.45rem]">Sự kiện mới nhất</CardTitle>
          </CardHeader>
          <CardContent className="space-y-3">
            {latestAudit.length ? (
              latestAudit.map((event) => (
                <div key={event.id} className="rounded-[18px] border border-slate-200 bg-slate-50 px-5 py-4">
                  <div className="flex flex-wrap items-center gap-2">
                    <span className="rounded-full border border-blue-200 bg-blue-50 px-3 py-1 text-[11px] font-semibold uppercase tracking-[0.16em] text-blue-700">
                      {event.eventType}
                    </span>
                    {event.resourceType ? (
                      <span className="rounded-full border border-slate-200 bg-white px-3 py-1 text-[11px] font-semibold uppercase tracking-[0.16em] text-slate-600">
                        {event.resourceType}
                      </span>
                    ) : null}
                  </div>
                  <p className="mt-3 font-medium text-slate-900">{event.summary ?? "Không có tóm tắt"}</p>
                  <p className="mt-1 text-sm text-slate-500">{formatDateTime(event.createdAt)}</p>
                </div>
              ))
            ) : (
              <Card className="border-dashed bg-slate-50/70">
                <CardContent className="pt-7 text-sm text-slate-500">Chưa có sự kiện kiểm toán để hiển thị trên bảng điều khiển.</CardContent>
              </Card>
            )}
          </CardContent>
        </Card>

        <Card>
          <CardHeader className="flex flex-row items-start justify-between gap-4">
            <div>
              <p className="text-xs font-semibold uppercase tracking-[0.18em] text-accent">Cảnh báo vận hành</p>
              <CardTitle className="text-[1.45rem]">Cảnh báo cần lưu ý</CardTitle>
            </div>
            <ShieldAlert className="h-5 w-5 text-primary" />
          </CardHeader>
          <CardContent className="space-y-3 text-sm text-slate-600">
            <div className="rounded-[18px] border border-amber-200 bg-amber-50 px-5 py-4">
              <p className="font-semibold text-amber-950">{pendingAccounts} tài khoản đang chờ kích hoạt</p>
              <p className="mt-1 leading-6 text-amber-900/80">Hãy kích hoạt sớm để người yêu cầu và người ký tiếp tục đi qua các bước nghiệp vụ.</p>
            </div>
            <div className="rounded-[18px] border border-blue-200 bg-blue-50 px-5 py-4">
              <p className="font-semibold text-blue-950">{renewalRequests} yêu cầu gia hạn đang chờ xử lý</p>
              <p className="mt-1 leading-6 text-blue-900/80">Phân hệ gia hạn nên được kiểm tra định kỳ để tránh chứng thư hết hạn.</p>
            </div>
            <div className="rounded-[18px] border border-red-200 bg-red-50 px-5 py-4">
              <p className="font-semibold text-red-950">{revokedCertificates} chứng thư đã bị thu hồi</p>
              <p className="mt-1 leading-6 text-red-900/80">Kiểm toán và xác minh cần nhất quán với danh sách chứng thư bị thu hồi.</p>
            </div>
          </CardContent>
        </Card>
      </section>
    </div>
  );
}
