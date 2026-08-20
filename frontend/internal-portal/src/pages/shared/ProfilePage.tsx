import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { useEffect, useMemo, useState } from "react";

import { PageHeader } from "@/components/common/PageHeader";
import { StatusBadge } from "@/components/common/StatusBadge";
import { Button } from "@/components/ui/button";
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";
import { Input } from "@/components/ui/input";
import { authService } from "@/services/auth-service";
import { useAuthStore } from "@/store/auth-store";

export function ProfilePage({ title, description }: { title: string; description: string }) {
  const session = useAuthStore((state) => state.user);
  const queryClient = useQueryClient();
  const [selectedFile, setSelectedFile] = useState<File | null>(null);
  const [previewUrl, setPreviewUrl] = useState<string | null>(null);
  const [imageOnly, setImageOnly] = useState(false);
  const [xPercent, setXPercent] = useState(62);
  const [yPercent, setYPercent] = useState(4);
  const [widthPercent, setWidthPercent] = useState(34);
  const [heightPercent, setHeightPercent] = useState(16);
  const profileQuery = useQuery({
    queryKey: ["auth", "me"],
    queryFn: () => authService.getMe(),
  });
  const signatureImageQuery = useQuery({
    queryKey: ["auth", "signature-image"],
    queryFn: authService.getMySignatureImage,
    enabled: Boolean(profileQuery.data?.signatureImageConfigured),
  });

  const uploadSignatureImageMutation = useMutation({
    mutationFn: (file: File) => authService.uploadSignatureImage(file),
    onSuccess: async () => {
      setSelectedFile(null);
      await queryClient.invalidateQueries({ queryKey: ["auth", "me"] });
      queryClient.removeQueries({ queryKey: ["auth", "signature-image"] });
      await queryClient.invalidateQueries({ queryKey: ["auth", "signature-image"] });
    },
  });
  const deleteSignatureImageMutation = useMutation({
    mutationFn: authService.deleteMySignatureImage,
    onSuccess: async () => {
      setSelectedFile(null);
      await queryClient.invalidateQueries({ queryKey: ["auth", "me"] });
      queryClient.removeQueries({ queryKey: ["auth", "signature-image"] });
    },
  });
  const updateAppearanceMutation = useMutation({
    mutationFn: () =>
      authService.updateSignatureAppearance({
        imageOnly,
        xPercent,
        yPercent,
        widthPercent,
        heightPercent,
      }),
    onSuccess: async () => {
      await queryClient.invalidateQueries({ queryKey: ["auth", "me"] });
    },
  });

  const profile = profileQuery.data;
  const previewSource = useMemo(() => selectedFile ?? signatureImageQuery.data ?? null, [selectedFile, signatureImageQuery.data]);

  useEffect(() => {
    if (!previewSource) {
      setPreviewUrl(null);
      return;
    }

    const objectUrl = URL.createObjectURL(previewSource);
    setPreviewUrl(objectUrl);
    return () => URL.revokeObjectURL(objectUrl);
  }, [previewSource]);

  useEffect(() => {
    if (!profile) {
      return;
    }
    setImageOnly(profile.signatureImageOnly);
    setXPercent(profile.signatureBoxXPercent);
    setYPercent(profile.signatureBoxYPercent);
    setWidthPercent(profile.signatureBoxWidthPercent);
    setHeightPercent(profile.signatureBoxHeightPercent);
  }, [profile]);

  return (
    <div className="space-y-6">
      <PageHeader eyebrow="Tài khoản" title={title} description={description} />
      <Card>
        <CardHeader>
          <CardTitle>Thông tin tài khoản</CardTitle>
        </CardHeader>
        <CardContent className="grid gap-4 md:grid-cols-2">
          <div>
            <p className="text-xs uppercase tracking-[0.18em] text-muted-foreground">Tên đăng nhập</p>
            <p className="mt-1 text-base font-medium">{profile?.username ?? session?.username ?? "-"}</p>
          </div>
          <div>
            <p className="text-xs uppercase tracking-[0.18em] text-muted-foreground">Email</p>
            <p className="mt-1 text-base font-medium">{profile?.email ?? session?.email ?? "-"}</p>
          </div>
          <div>
            <p className="text-xs uppercase tracking-[0.18em] text-muted-foreground">Vai trò</p>
            <p className="mt-1 text-base font-medium">{profile?.role ?? session?.role ?? "-"}</p>
          </div>
          <div>
            <p className="text-xs uppercase tracking-[0.18em] text-muted-foreground">Trạng thái</p>
            <div className="mt-2">
              <StatusBadge status={profile?.status ?? session?.status ?? "-"} />
            </div>
          </div>
          <div className="md:col-span-2">
            <p className="text-xs uppercase tracking-[0.18em] text-muted-foreground">Mã tài khoản</p>
            <p className="mt-1 break-all font-mono text-sm">{profile?.id ?? session?.accountId ?? "-"}</p>
          </div>
        </CardContent>
      </Card>

      <Card>
        <CardHeader>
          <CardTitle>Ảnh chữ ký trên PDF</CardTitle>
        </CardHeader>
        <CardContent className="grid gap-6 xl:grid-cols-[1.05fr_0.95fr]">
          <div className="space-y-4">
            <p className="text-sm leading-6 text-muted-foreground">
              Ảnh này sẽ được nhúng vào khung hiển thị trên PDF sau khi bạn <span className="font-semibold text-slate-900">thực hiện ký số</span>.
              Đây chỉ là <span className="font-semibold text-slate-900">phần hiển thị trực quan</span>; giao dịch vẫn phải được ký bằng PKI/WebAuthn như bình thường.
              Các thông số bên dưới là <span className="font-semibold text-slate-900">thiết lập mặc định của hồ sơ</span> và có thể được thay đổi ngay trong bước ký PDF.
            </p>
            <div className="space-y-2">
              <label className="text-sm font-medium text-slate-900" htmlFor="signatureImage">
                Chọn ảnh chữ ký
              </label>
              <Input
                id="signatureImage"
                type="file"
                accept="image/png,image/jpeg"
                onChange={(event) => setSelectedFile(event.target.files?.[0] ?? null)}
              />
              <p className="text-xs leading-5 text-muted-foreground">
                Chấp nhận PNG/JPEG, tối đa 1MB. Nên dùng ảnh nền trong suốt để chữ ký hiển thị đẹp hơn trên PDF.
              </p>
            </div>
            <div className="flex flex-wrap gap-3">
              <Button
                onClick={() => selectedFile && uploadSignatureImageMutation.mutate(selectedFile)}
                disabled={!selectedFile || uploadSignatureImageMutation.isPending}
              >
                {uploadSignatureImageMutation.isPending ? "Đang tải lên..." : "Lưu ảnh chữ ký"}
              </Button>
              <Button
                variant="outline"
                onClick={() => deleteSignatureImageMutation.mutate()}
                disabled={!profile?.signatureImageConfigured || deleteSignatureImageMutation.isPending}
              >
                {deleteSignatureImageMutation.isPending ? "Đang xóa..." : "Xóa ảnh chữ ký"}
              </Button>
            </div>
            {uploadSignatureImageMutation.isError ? (
              <p className="text-sm text-red-600">{uploadSignatureImageMutation.error.message}</p>
            ) : null}
            {deleteSignatureImageMutation.isError ? (
              <p className="text-sm text-red-600">{deleteSignatureImageMutation.error.message}</p>
            ) : null}

            <div className="rounded-[24px] border border-slate-200/80 bg-slate-50/80 p-5">
              <h3 className="text-base font-semibold text-slate-950">Cấu hình chữ ký hiển thị</h3>
              <p className="mt-2 text-sm leading-6 text-muted-foreground">
                Các giá trị dưới đây được tính theo <span className="font-semibold text-slate-900">% kích thước trang PDF</span>.
                Gốc tọa độ là <span className="font-semibold text-slate-900">góc trái dưới</span>.
              </p>
              <div className="mt-5 grid gap-4 md:grid-cols-2">
                <label className="space-y-2 text-sm font-medium text-slate-900">
                  <span>Vị trí ngang X (%)</span>
                  <Input type="number" min={0} max={95} value={xPercent} onChange={(event) => setXPercent(Number(event.target.value))} />
                </label>
                <label className="space-y-2 text-sm font-medium text-slate-900">
                  <span>Vị trí dọc Y (%)</span>
                  <Input type="number" min={0} max={95} value={yPercent} onChange={(event) => setYPercent(Number(event.target.value))} />
                </label>
                <label className="space-y-2 text-sm font-medium text-slate-900">
                  <span>Chiều rộng khung (%)</span>
                  <Input type="number" min={5} max={100} value={widthPercent} onChange={(event) => setWidthPercent(Number(event.target.value))} />
                </label>
                <label className="space-y-2 text-sm font-medium text-slate-900">
                  <span>Chiều cao khung (%)</span>
                  <Input type="number" min={5} max={100} value={heightPercent} onChange={(event) => setHeightPercent(Number(event.target.value))} />
                </label>
              </div>

              <label className="mt-4 flex items-center gap-3 text-sm font-medium text-slate-900">
                <input
                  checked={imageOnly}
                  className="h-4 w-4 rounded border-slate-300 text-primary focus:ring-primary/30"
                  disabled={!profile?.signatureImageConfigured}
                  type="checkbox"
                  onChange={(event) => setImageOnly(event.target.checked)}
                />
                Chỉ hiển thị hình ảnh, không kèm thông tin người ký / serial / thời gian
              </label>
              {!profile?.signatureImageConfigured ? (
                <p className="mt-2 text-xs leading-5 text-amber-700">
                  Bạn chưa có ảnh chữ ký. Tùy chọn "chỉ hiển thị hình ảnh" sẽ chỉ có tác dụng sau khi tải ảnh chữ ký.
                </p>
              ) : null}

              <div className="mt-4 flex flex-wrap gap-3">
                <Button onClick={() => updateAppearanceMutation.mutate()} disabled={updateAppearanceMutation.isPending}>
                  {updateAppearanceMutation.isPending ? "Đang lưu vị trí..." : "Lưu cấu hình vị trí chữ ký"}
                </Button>
              </div>
              {updateAppearanceMutation.isError ? (
                <p className="mt-3 text-sm text-red-600">{updateAppearanceMutation.error.message}</p>
              ) : null}
            </div>
          </div>

          <div className="rounded-[24px] border border-slate-200/80 bg-slate-50/80 p-5">
            <p className="text-xs uppercase tracking-[0.18em] text-muted-foreground">Preview</p>
            <div className="mt-4 rounded-[20px] border border-dashed border-slate-300 bg-white p-4">
              <div className="relative mx-auto aspect-[1/1.414] w-full max-w-[280px] rounded-[14px] border border-slate-200 bg-[linear-gradient(180deg,#fff,#f8fafc)] shadow-sm">
                <div
                  className={`absolute overflow-hidden rounded-xl ${imageOnly ? "" : "border border-slate-300 bg-white shadow-sm"}`}
                  style={{
                    left: `${xPercent}%`,
                    bottom: `${yPercent}%`,
                    width: `${widthPercent}%`,
                    height: `${heightPercent}%`,
                  }}
                >
                  {previewUrl ? (
                    <div className="flex h-full w-full items-center justify-center p-2">
                      <img alt="Xem trước chữ ký" className="max-h-full w-auto object-contain" src={previewUrl} />
                    </div>
                  ) : (
                    <div className="flex h-full items-center justify-center px-2 text-center text-[11px] leading-4 text-muted-foreground">
                      {imageOnly ? "Ảnh chữ ký" : "Khung chữ ký"}
                    </div>
                  )}
                  {!imageOnly ? (
                    <div className="border-t border-slate-200/80 bg-slate-50/70 px-2 py-1 text-[9px] leading-3 text-slate-500">
                      người ký / serial / thời gian
                    </div>
                  ) : null}
                </div>
              </div>
              {!previewUrl ? (
                <p className="mx-auto mt-4 max-w-xs text-center text-sm leading-6 text-muted-foreground">
                  Chưa có ảnh chữ ký. Sau khi tải lên, phần xem trước sẽ hiển thị đúng khung ở vị trí đã cấu hình.
                </p>
              ) : null}
            </div>
            <div className="mt-4 flex items-center gap-2 text-sm">
              <span className="font-medium text-slate-900">Trạng thái:</span>
              <span className={profile?.signatureImageConfigured ? "text-emerald-700" : "text-slate-500"}>
                {profile?.signatureImageConfigured ? "Đã cấu hình" : "Chưa cấu hình"}
              </span>
            </div>
          </div>
        </CardContent>
      </Card>
    </div>
  );
}
