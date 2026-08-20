import { useMutation, useQueryClient } from "@tanstack/react-query";
import { useNavigate } from "react-router-dom";
import { useState } from "react";

import { PageHeader } from "@/components/common/PageHeader";
import { Button } from "@/components/ui/button";
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import { Textarea } from "@/components/ui/textarea";
import { documentsService } from "@/services/documents-service";

const MAX_DOCUMENT_SIZE_BYTES = 20 * 1024 * 1024;

export function UploadDocumentPage() {
  const navigate = useNavigate();
  const queryClient = useQueryClient();
  const [title, setTitle] = useState("");
  const [description, setDescription] = useState("");
  const [file, setFile] = useState<File | null>(null);
  const [validationMessage, setValidationMessage] = useState<string | null>(null);

  const uploadMutation = useMutation({
    mutationFn: documentsService.create,
    onSuccess: async (document) => {
      await queryClient.invalidateQueries({ queryKey: ["documents"] });
      navigate(`/app/documents/${document.id}`);
    },
  });

  return (
    <div className="space-y-6">
      <PageHeader
        eyebrow="Tải lên"
        title="Tải lên tài liệu mới"
        description="Tệp được đẩy lên MinIO, metadata được lưu vào document-service và hiển thị lại ngay trong danh sách."
      />
      <Card>
        <CardHeader><CardTitle>Thông tin tài liệu</CardTitle></CardHeader>
        <CardContent>
          <form
            className="grid gap-5"
            onSubmit={(event) => {
              event.preventDefault();
              if (!file) {
                return;
              }
              if (file.size > MAX_DOCUMENT_SIZE_BYTES) {
                setValidationMessage("Tệp vượt quá giới hạn 20MB. Hãy chọn tệp nhỏ hơn.");
                return;
              }
              setValidationMessage(null);
              uploadMutation.mutate({ title, description, file });
            }}
          >
            <div className="space-y-2">
              <Label htmlFor="title">Tiêu đề</Label>
              <Input id="title" value={title} onChange={(event) => setTitle(event.target.value)} required />
            </div>
            <div className="space-y-2">
              <Label htmlFor="description">Mô tả</Label>
              <Textarea id="description" value={description} onChange={(event) => setDescription(event.target.value)} />
            </div>
            <div className="space-y-2">
              <Label htmlFor="file">Tệp tài liệu</Label>
              <Input
                id="file"
                type="file"
                onChange={(event) => {
                  const nextFile = event.target.files?.[0] ?? null;
                  setFile(nextFile);
                  if (nextFile && nextFile.size > MAX_DOCUMENT_SIZE_BYTES) {
                    setValidationMessage("Tệp vượt quá giới hạn 20MB. Hãy chọn tệp nhỏ hơn.");
                  } else {
                    setValidationMessage(null);
                  }
                }}
                required
              />
              {file ? <p className="text-sm text-muted-foreground">{file.name}</p> : null}
              <p className="text-xs text-muted-foreground">Kích thước tối đa: 20MB.</p>
            </div>
            {validationMessage ? <p className="text-sm text-red-600">{validationMessage}</p> : null}
            {uploadMutation.isError ? <p className="text-sm text-red-600">{uploadMutation.error.message}</p> : null}
            <div>
              <Button disabled={uploadMutation.isPending || !file || !title.trim() || Boolean(validationMessage)} type="submit">
                {uploadMutation.isPending ? "Đang tải lên..." : "Tạo tài liệu"}
              </Button>
            </div>
          </form>
        </CardContent>
      </Card>
    </div>
  );
}
