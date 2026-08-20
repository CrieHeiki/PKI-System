import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { useEffect, useState } from "react";
import { useNavigate, useSearchParams } from "react-router-dom";

import { PageHeader } from "@/components/common/PageHeader";
import { Button } from "@/components/ui/button";
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";
import { Label } from "@/components/ui/label";
import { Textarea } from "@/components/ui/textarea";
import { certificatesService } from "@/services/certificates-service";

export function RequestRevocationPage() {
  const navigate = useNavigate();
  const queryClient = useQueryClient();
  const [searchParams] = useSearchParams();
  const [certificateId, setCertificateId] = useState(searchParams.get("certificateId") || "");
  const [reason, setReason] = useState("Không còn nhu cầu sử dụng chứng thư này.");

  const certificatesQuery = useQuery({ queryKey: ["certificates", "mine"], queryFn: certificatesService.listMine });

  useEffect(() => {
    const nextCertificateId = searchParams.get("certificateId");
    if (nextCertificateId) {
      setCertificateId(nextCertificateId);
    }
  }, [searchParams]);

  const mutation = useMutation({
    mutationFn: () => certificatesService.createRevocationRequest(certificateId, { reason }),
    onSuccess: async () => {
      await queryClient.invalidateQueries({ queryKey: ["certificate-requests"] });
      await queryClient.invalidateQueries({ queryKey: ["certificates"] });
      navigate("/app/certificate");
    },
  });

  return (
    <div className="space-y-6">
      <PageHeader eyebrow="Thu hồi" title="Gửi yêu cầu thu hồi chứng thư" description="Người dùng gửi lý do thu hồi, sau đó quản trị viên sẽ chấp thuận hoặc từ chối yêu cầu này." />
      <Card>
        <CardHeader><CardTitle>Yêu cầu thu hồi</CardTitle></CardHeader>
        <CardContent>
          <form
            className="grid gap-5"
            onSubmit={(event) => {
              event.preventDefault();
              mutation.mutate();
            }}
          >
            <div className="space-y-2">
              <Label htmlFor="certificateId">Chứng thư</Label>
              <select
                id="certificateId"
                className="h-11 w-full rounded-2xl border border-input bg-white px-4 text-sm"
                value={certificateId}
                onChange={(event) => setCertificateId(event.target.value)}
                required
              >
                <option value="">Chọn chứng thư</option>
                {certificatesQuery.data
                  ?.filter((certificate) => certificate.status !== "REVOKED")
                  .map((certificate) => (
                    <option key={certificate.id} value={certificate.id}>
                      {certificate.commonName} - {certificate.serialNumber}
                    </option>
                  ))}
              </select>
            </div>
            <div className="space-y-2">
              <Label htmlFor="revoke-reason">Lý do thu hồi</Label>
              <Textarea id="revoke-reason" value={reason} onChange={(event) => setReason(event.target.value)} required />
            </div>
            {mutation.isError ? <p className="text-sm text-red-600">{mutation.error.message}</p> : null}
            <div>
              <Button type="submit" disabled={!certificateId || !reason.trim() || mutation.isPending}>
                {mutation.isPending ? "Đang gửi..." : "Gửi yêu cầu thu hồi"}
              </Button>
            </div>
          </form>
        </CardContent>
      </Card>
    </div>
  );
}
