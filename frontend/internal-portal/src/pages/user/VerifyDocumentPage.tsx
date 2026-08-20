import { useMutation } from "@tanstack/react-query";
import { useState } from "react";

import { EmptyState } from "@/components/common/EmptyState";
import { PageHeader } from "@/components/common/PageHeader";
import { StatusBadge } from "@/components/common/StatusBadge";
import { Badge } from "@/components/ui/badge";
import { Button } from "@/components/ui/button";
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import { Select } from "@/components/ui/select";
import { formatDateTime } from "@/lib/utils";
import { buildVerificationSignals, describeVerificationCheck } from "@/lib/verification-presenter";
import { verificationService } from "@/services/verification-service";
import { useAuthStore } from "@/store/auth-store";

function VerificationCheckBadge({ passed }: { passed: boolean }) {
  return <Badge variant={passed ? "success" : "destructive"}>{passed ? "ĐẠT" : "KHÔNG ĐẠT"}</Badge>;
}

export function VerifyDocumentPage() {
  const [selectedFile, setSelectedFile] = useState<File | null>(null);
  const [trustSource, setTrustSource] = useState<"INTERNAL_CA" | "EXTERNAL_CA">("INTERNAL_CA");
  const [trustAnchorFiles, setTrustAnchorFiles] = useState<File[]>([]);
  const [validationMessage, setValidationMessage] = useState<string | null>(null);
  const currentUser = useAuthStore((state) => state.user);
  const verifyMutation = useMutation({
    mutationFn: (file: File) =>
      verificationService.verifyUploadedFile(file, {
        verifiedBy: currentUser?.accountId,
        trustSource,
        trustAnchorFiles,
      }),
  });

  const report = verifyMutation.data;
  const verificationSignals = report ? buildVerificationSignals(report) : [];

  return (
    <div className="space-y-6">
      <PageHeader eyebrow="Xác minh" title="Xác minh tệp đã ký" description="Tải tệp đã ký từ máy của bạn lên để kiểm tra chữ ký, tính toàn vẹn và chuỗi tin cậy." />
      <Card>
        <CardHeader><CardTitle>Tải tệp để xác minh</CardTitle></CardHeader>
        <CardContent>
          <form
            className="grid gap-5"
            onSubmit={(event) => {
              event.preventDefault();
              if (!selectedFile) {
                setValidationMessage("Hãy chọn tệp cần xác minh.");
                return;
              }
              if (selectedFile.size > 20 * 1024 * 1024) {
                setValidationMessage("Tệp vượt quá giới hạn 20MB. Hãy chọn tệp nhỏ hơn.");
                return;
              }
              setValidationMessage(null);
              verifyMutation.mutate(selectedFile);
            }}
          >
            <div className="space-y-2">
              <Label htmlFor="trustSource">Nguồn tin cậy</Label>
              <Select
                id="trustSource"
                value={trustSource}
                onChange={(event) => {
                  const nextValue = event.target.value as "INTERNAL_CA" | "EXTERNAL_CA";
                  setTrustSource(nextValue);
                  if (nextValue === "INTERNAL_CA") {
                    setTrustAnchorFiles([]);
                  }
                }}
              >
                <option value="INTERNAL_CA">CA nội bộ</option>
                <option value="EXTERNAL_CA">CA bên ngoài</option>
              </Select>
              <p className="text-xs leading-5 text-muted-foreground">
                CA nội bộ sẽ đối chiếu với certification-service. CA bên ngoài sẽ dùng trust store của JVM và có thể nhận thêm PEM/CRT gốc hoặc intermediate do bạn cung cấp.
              </p>
            </div>
            <div className="space-y-2">
              <Label htmlFor="signedFile">Tệp cần xác minh</Label>
              <Input
                id="signedFile"
                accept=".pdf,.p7m,.zip,application/pdf,application/pkcs7-mime,application/zip"
                type="file"
                onChange={(event) => {
                  const file = event.target.files?.[0] ?? null;
                  setSelectedFile(file);
                  if (file && file.size > 20 * 1024 * 1024) {
                    setValidationMessage("Tệp vượt quá giới hạn 20MB. Hãy chọn tệp nhỏ hơn.");
                    return;
                  }
                  setValidationMessage(null);
                }}
              />
              <p className="text-xs leading-5 text-muted-foreground">
                Chấp nhận PDF đã ký, CMS/CAdES <span className="font-semibold text-slate-900">.p7m</span> và cả ZIP đã ký theo định dạng cũ để đối chiếu.
              </p>
              <p className="text-xs text-muted-foreground">Kích thước tối đa: 20MB.</p>
            </div>
            {trustSource === "EXTERNAL_CA" ? (
              <div className="space-y-2">
                <Label htmlFor="trustAnchors">Trust anchor / chuỗi bổ sung</Label>
                <Input
                  id="trustAnchors"
                  accept=".pem,.crt,.cer,.p7b,application/x-pem-file,application/pkix-cert"
                  multiple
                  type="file"
                  onChange={(event) => {
                    const files = Array.from(event.target.files ?? []);
                    setTrustAnchorFiles(files);
                  }}
                />
                <p className="text-xs leading-5 text-muted-foreground">
                  Nếu tài liệu được cấp bởi CA công cộng, hệ thống sẽ thử xác minh bằng trust store mặc định của JVM. Nếu là CA doanh nghiệp hoặc CA tự cài, hãy tải lên root/intermediate PEM/CRT để hệ thống dựng chuỗi chính xác hơn.
                </p>
                {trustAnchorFiles.length > 0 ? (
                  <p className="text-xs text-muted-foreground">Đã chọn {trustAnchorFiles.length} tệp trust anchor / chain.</p>
                ) : null}
              </div>
            ) : null}
            {validationMessage ? <p className="text-sm text-red-600">{validationMessage}</p> : null}
            {verifyMutation.isError ? <p className="text-sm text-red-600">{verifyMutation.error.message}</p> : null}
            <div>
              <Button type="submit" disabled={verifyMutation.isPending || !selectedFile}>
                {verifyMutation.isPending ? "Đang xác minh..." : "Xác minh tệp đã tải lên"}
              </Button>
            </div>
          </form>
        </CardContent>
      </Card>

      {report ? (
        <Card>
          <CardHeader><CardTitle>Kết quả xác minh</CardTitle></CardHeader>
          <CardContent className="space-y-4">
            <div className="flex items-center gap-3">
              <StatusBadge status={report.verificationStatus} />
              <span className="text-sm text-muted-foreground">Xác minh lúc {formatDateTime(report.verifiedAt)}</span>
            </div>
            <p className="text-sm text-muted-foreground">{report.overallMessage || "Không có thông điệp tổng quan."}</p>
            <div className="grid gap-3 md:grid-cols-2">
              {verificationSignals.map((signal) => (
                <div key={signal.key} className="rounded-2xl border border-border p-4">
                  <div className="flex items-center gap-3">
                    <VerificationCheckBadge passed={signal.result} />
                    <p className="font-medium">{signal.title}</p>
                  </div>
                  <p className="mt-2 text-sm text-slate-700">{signal.message}</p>
                </div>
              ))}
            </div>
            <div className="space-y-3">
              {report.checks.map((check) => {
                const presentation = describeVerificationCheck(check);
                return (
                <div key={check.checkName} className="rounded-2xl border border-border p-4">
                  <div className="flex items-center gap-3">
                    <VerificationCheckBadge passed={check.result} />
                    <p className="font-medium">{presentation.title}</p>
                  </div>
                  <p className="mt-2 text-sm text-slate-700">{presentation.summary}</p>
                  {presentation.technicalDetail ? (
                    <p className="mt-2 text-sm text-muted-foreground">Chi tiết kỹ thuật: {presentation.technicalDetail}</p>
                  ) : null}
                  <div className="mt-3 rounded-2xl border border-dashed border-slate-300 bg-slate-50/80 p-3 text-sm">
                    <p className="font-medium text-slate-900">Thông tin debug</p>
                    <div className="mt-2 space-y-2 text-slate-700">
                      <p><span className="font-medium">Điều kiện pass:</span> {presentation.passCondition}</p>
                      <p><span className="font-medium">Điều kiện fail:</span> {presentation.failCondition}</p>
                      <p><span className="font-medium">Dịch vụ quyết định:</span> {presentation.decidingService}</p>
                      <p className="text-xs text-slate-500"><span className="font-medium">Khóa kỹ thuật:</span> {presentation.technicalKey}</p>
                    </div>
                  </div>
                </div>
              )})}
            </div>
          </CardContent>
        </Card>
      ) : null}

      {!report && !verifyMutation.isPending ? <EmptyState title="Chưa có báo cáo xác minh" description="Tải một tệp đã ký từ máy của bạn lên để tạo báo cáo xác minh." /> : null}
    </div>
  );
}
