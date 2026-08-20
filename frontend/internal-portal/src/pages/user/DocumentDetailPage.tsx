import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { useParams } from "react-router-dom";
import { useState } from "react";

import { EmptyState } from "@/components/common/EmptyState";
import { PageHeader } from "@/components/common/PageHeader";
import { Button } from "@/components/ui/button";
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import { formatDateTime } from "@/lib/utils";
import { documentsService } from "@/services/documents-service";

const MAX_DOCUMENT_SIZE_BYTES = 20 * 1024 * 1024;

export function DocumentDetailPage() {
  const { id } = useParams();
  const queryClient = useQueryClient();
  const [versionFile, setVersionFile] = useState<File | null>(null);
  const [validationMessage, setValidationMessage] = useState<string | null>(null);

  const detailQuery = useQuery({
    queryKey: ["documents", "detail", id],
    queryFn: () => documentsService.get(id!),
    enabled: Boolean(id),
  });

  const uploadVersionMutation = useMutation({
    mutationFn: (payload: { file: File }) => documentsService.uploadVersion(id!, payload),
    onSuccess: async () => {
      setVersionFile(null);
      await queryClient.invalidateQueries({ queryKey: ["documents"] });
      await queryClient.invalidateQueries({ queryKey: ["documents", "detail", id] });
    },
  });

  const handleDownload = async () => {
    if (!id) return;
    const { fileName, blob } = await documentsService.download(id);
    const url = window.URL.createObjectURL(blob);
    const link = document.createElement("a");
    link.href = url;
    link.download = fileName;
    link.click();
    window.URL.revokeObjectURL(url);
  };

  return (
    <div className="space-y-6">
      <PageHeader eyebrow="Chi tiết tài liệu" title="Tài liệu và phiên bản hiện tại" description="Bạn có thể tải tệp hiện tại hoặc đẩy thêm phiên bản mới." />

      {detailQuery.isLoading ? <Card><CardContent className="pt-6 text-sm text-muted-foreground">Đang tải chi tiết tài liệu...</CardContent></Card> : null}
      {detailQuery.isError ? <Card><CardContent className="pt-6 text-sm text-red-600">{detailQuery.error.message}</CardContent></Card> : null}
      {!detailQuery.data && !detailQuery.isLoading && !detailQuery.isError ? (
        <EmptyState title="Không tìm thấy tài liệu" description="Kiểm tra lại documentId hoặc quay về danh sách." />
      ) : null}

      {detailQuery.data ? (
        <>
          <Card>
            <CardHeader><CardTitle>{detailQuery.data.title}</CardTitle></CardHeader>
            <CardContent className="grid gap-4 md:grid-cols-2">
              <div>
                <p className="text-xs uppercase tracking-[0.18em] text-muted-foreground">Mô tả</p>
                <p className="mt-1 text-sm">{detailQuery.data.description || "Không có mô tả."}</p>
              </div>
              <div>
                <p className="text-xs uppercase tracking-[0.18em] text-muted-foreground">Trạng thái</p>
                <p className="mt-1 text-sm">{detailQuery.data.status}</p>
              </div>
              <div>
                <p className="text-xs uppercase tracking-[0.18em] text-muted-foreground">Phiên bản hiện tại</p>
                <p className="mt-1 text-sm">{detailQuery.data.currentVersion}</p>
              </div>
              <div>
                <p className="text-xs uppercase tracking-[0.18em] text-muted-foreground">Cập nhật lúc</p>
                <p className="mt-1 text-sm">{formatDateTime(detailQuery.data.updatedAt)}</p>
              </div>
            </CardContent>
          </Card>

          <Card>
            <CardHeader><CardTitle>Chi tiết phiên bản hiện tại</CardTitle></CardHeader>
            <CardContent className="space-y-3 text-sm text-muted-foreground">
              <p>Tên tệp: {detailQuery.data.currentVersionDetail.fileName}</p>
              <p>Loại nội dung: {detailQuery.data.currentVersionDetail.contentType}</p>
              <p>Dung lượng: {detailQuery.data.currentVersionDetail.fileSize} byte</p>
              <p>SHA-256: <span className="break-all font-mono">{detailQuery.data.currentVersionDetail.sha256Hash}</span></p>
              <p>Tạo lúc: {formatDateTime(detailQuery.data.currentVersionDetail.createdAt)}</p>
              <Button onClick={handleDownload} variant="outline">Tải tệp hiện tại</Button>
            </CardContent>
          </Card>

          <Card>
            <CardHeader><CardTitle>Tải lên phiên bản mới</CardTitle></CardHeader>
            <CardContent>
              <form
                className="space-y-4"
                onSubmit={(event) => {
                  event.preventDefault();
                  if (!versionFile) return;
                  if (versionFile.size > MAX_DOCUMENT_SIZE_BYTES) {
                    setValidationMessage("Tệp phiên bản mới vượt quá giới hạn 20MB.");
                    return;
                  }
                  setValidationMessage(null);
                  uploadVersionMutation.mutate({ file: versionFile });
                }}
              >
                <div className="space-y-2">
                  <Label htmlFor="version-file">Tệp phiên bản mới</Label>
                  <Input
                    id="version-file"
                    type="file"
                    onChange={(event) => {
                      const nextFile = event.target.files?.[0] ?? null;
                      setVersionFile(nextFile);
                      if (nextFile && nextFile.size > MAX_DOCUMENT_SIZE_BYTES) {
                        setValidationMessage("Tệp phiên bản mới vượt quá giới hạn 20MB.");
                      } else {
                        setValidationMessage(null);
                      }
                    }}
                  />
                  <p className="text-xs text-muted-foreground">Kích thước tối đa: 20MB.</p>
                </div>
                {validationMessage ? <p className="text-sm text-red-600">{validationMessage}</p> : null}
                {uploadVersionMutation.isError ? <p className="text-sm text-red-600">{uploadVersionMutation.error.message}</p> : null}
                <Button type="submit" disabled={!versionFile || uploadVersionMutation.isPending || Boolean(validationMessage)}>
                  {uploadVersionMutation.isPending ? "Đang tải lên..." : "Đẩy phiên bản mới"}
                </Button>
              </form>
            </CardContent>
          </Card>
        </>
      ) : null}
    </div>
  );
}
