import { useMutation, useQuery } from "@tanstack/react-query";
import { useNavigate } from "react-router-dom";
import { useState } from "react";

import { PageHeader } from "@/components/common/PageHeader";
import { Button } from "@/components/ui/button";
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";
import { Label } from "@/components/ui/label";
import { Textarea } from "@/components/ui/textarea";
import { authService } from "@/services/auth-service";
import { documentsService } from "@/services/documents-service";
import { signingService } from "@/services/signing-service";
import { useAuthStore } from "@/store/auth-store";

export function CreateSignRequestPage() {
  const navigate = useNavigate();
  const currentUser = useAuthStore((state) => state.user);
  const [documentId, setDocumentId] = useState("");
  const [signerAccountId, setSignerAccountId] = useState("");
  const [note, setNote] = useState("");

  const documentsQuery = useQuery({ queryKey: ["documents", "mine"], queryFn: documentsService.listMine });
  const activeUsersQuery = useQuery({ queryKey: ["auth", "active-users"], queryFn: authService.listActiveUsers });

  const createMutation = useMutation({
    mutationFn: signingService.create,
    onSuccess: (request) => {
      navigate(`/app/sign-requests/${request.id}`);
    },
  });

  const availableSigners = (activeUsersQuery.data || []).filter((user) => user.id !== currentUser?.accountId);

  return (
    <div className="space-y-6">
      <PageHeader
        eyebrow="Quy trình ký"
        title="Tạo yêu cầu ký mới"
        description="Màn hình này kết nối trực tiếp tới signing-workflow-service và account-service để chọn tài liệu và người ký hợp lệ."
      />

      <Card>
        <CardHeader><CardTitle>Thông tin yêu cầu</CardTitle></CardHeader>
        <CardContent>
          <form
            className="grid gap-5"
            onSubmit={(event) => {
              event.preventDefault();
              createMutation.mutate({ documentId, signerAccountId, note });
            }}
          >
            <div className="space-y-2">
              <Label htmlFor="documentId">Tài liệu</Label>
              <select
                id="documentId"
                className="h-11 w-full rounded-2xl border border-input bg-white px-4 text-sm"
                value={documentId}
                onChange={(event) => setDocumentId(event.target.value)}
                required
              >
                <option value="">Chọn tài liệu của bạn</option>
                {documentsQuery.data?.map((document) => (
                  <option key={document.id} value={document.id}>
                    {document.title} ({document.currentVersion})
                  </option>
                ))}
              </select>
            </div>

            <div className="space-y-2">
              <Label htmlFor="signerAccountId">Người ký</Label>
              <select
                id="signerAccountId"
                className="h-11 w-full rounded-2xl border border-input bg-white px-4 text-sm"
                value={signerAccountId}
                onChange={(event) => setSignerAccountId(event.target.value)}
                required
              >
                <option value="">Chọn người dùng đang hoạt động</option>
                {availableSigners.map((user) => (
                  <option key={user.id} value={user.id}>
                    {user.username} - {user.email}
                  </option>
                ))}
              </select>
              <p className="text-xs text-muted-foreground">
                Danh sách này lấy từ account-service. Điều kiện chứng thư hợp lệ vẫn sẽ được backend kiểm tra ở bước duyệt và ký.
              </p>
            </div>

            <div className="space-y-2">
              <Label htmlFor="note">Ghi chú</Label>
              <Textarea id="note" value={note} onChange={(event) => setNote(event.target.value)} />
            </div>

            {createMutation.isError ? <p className="text-sm text-red-600">{createMutation.error.message}</p> : null}
            <div>
              <Button type="submit" disabled={!documentId || !signerAccountId || createMutation.isPending}>
                {createMutation.isPending ? "Đang tạo..." : "Tạo yêu cầu ký"}
              </Button>
            </div>
          </form>
        </CardContent>
      </Card>
    </div>
  );
}
