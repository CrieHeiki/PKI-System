import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { useNavigate } from "react-router-dom";
import { useState } from "react";

import { PageHeader } from "@/components/common/PageHeader";
import { Button } from "@/components/ui/button";
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import { Textarea } from "@/components/ui/textarea";
import { certificatesService } from "@/services/certificates-service";

export function RequestRenewalPage() {
  const navigate = useNavigate();
  const queryClient = useQueryClient();
  const [certificateId, setCertificateId] = useState("");
  const [ttl, setTtl] = useState("720h");
  const [organization, setOrganization] = useState("CT556");
  const [country, setCountry] = useState("VN");
  const [reason, setReason] = useState("Gia hạn chứng thư sắp hết hạn");

  const certificatesQuery = useQuery({ queryKey: ["certificates", "mine"], queryFn: certificatesService.listMine });
  const mutation = useMutation({
    mutationFn: (payload: { ttl?: string; organization?: string; country?: string; reason?: string }) =>
      certificatesService.createRenewalRequest(certificateId, payload),
    onSuccess: async () => {
      await queryClient.invalidateQueries({ queryKey: ["certificate-requests"] });
      navigate("/app/certificate");
    },
  });

  return (
    <div className="space-y-6">
      <PageHeader eyebrow="Gia hạn" title="Gửi yêu cầu gia hạn chứng thư" description="Chọn chứng thư hiện có để tạo yêu cầu gia hạn." />
      <Card>
        <CardHeader><CardTitle>Yêu cầu gia hạn</CardTitle></CardHeader>
        <CardContent>
          <form
            className="grid gap-5 md:grid-cols-2"
            onSubmit={(event) => {
              event.preventDefault();
              mutation.mutate({ ttl, organization, country, reason });
            }}
          >
            <div className="space-y-2 md:col-span-2">
              <Label htmlFor="certificateId">Chứng thư</Label>
              <select
                id="certificateId"
                className="h-11 w-full rounded-2xl border border-input bg-white px-4 text-sm"
                value={certificateId}
                onChange={(event) => setCertificateId(event.target.value)}
                required
              >
                <option value="">Chọn chứng thư</option>
                {certificatesQuery.data?.map((certificate) => (
                  <option key={certificate.id} value={certificate.id}>
                    {certificate.commonName} - {certificate.serialNumber}
                  </option>
                ))}
              </select>
            </div>
            <div className="space-y-2">
              <Label htmlFor="renew-ttl">TTL</Label>
              <Input id="renew-ttl" value={ttl} onChange={(event) => setTtl(event.target.value)} />
            </div>
            <div className="space-y-2">
              <Label htmlFor="renew-org">Organization</Label>
              <Input id="renew-org" value={organization} onChange={(event) => setOrganization(event.target.value)} />
            </div>
            <div className="space-y-2">
              <Label htmlFor="renew-country">Country</Label>
              <Input id="renew-country" value={country} onChange={(event) => setCountry(event.target.value)} />
            </div>
            <div className="space-y-2 md:col-span-2">
              <Label htmlFor="renew-reason">Lý do</Label>
              <Textarea id="renew-reason" value={reason} onChange={(event) => setReason(event.target.value)} />
            </div>
            {mutation.isError ? <p className="text-sm text-red-600 md:col-span-2">{mutation.error.message}</p> : null}
            <div className="md:col-span-2">
              <Button type="submit" disabled={!certificateId || mutation.isPending}>
                {mutation.isPending ? "Đang gửi..." : "Gửi yêu cầu gia hạn"}
              </Button>
            </div>
          </form>
        </CardContent>
      </Card>
    </div>
  );
}
