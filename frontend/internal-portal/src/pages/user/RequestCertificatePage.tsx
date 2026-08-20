import { useMutation, useQueryClient } from "@tanstack/react-query";
import { useNavigate } from "react-router-dom";
import { useState } from "react";

import { PageHeader } from "@/components/common/PageHeader";
import { Button } from "@/components/ui/button";
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import { certificatesService } from "@/services/certificates-service";

export function RequestCertificatePage() {
  const navigate = useNavigate();
  const queryClient = useQueryClient();
  const [commonName, setCommonName] = useState("");
  const [ttl, setTtl] = useState("720h");
  const [organization, setOrganization] = useState("CT556");
  const [country, setCountry] = useState("VN");

  const mutation = useMutation({
    mutationFn: certificatesService.createRequest,
    onSuccess: async () => {
      await queryClient.invalidateQueries({ queryKey: ["certificate-requests"] });
      navigate("/app/certificate");
    },
  });

  return (
    <div className="space-y-6">
      <PageHeader eyebrow="Cấp chứng thư" title="Gửi yêu cầu cấp chứng thư số" description="Yêu cầu này sẽ vào hàng đợi để quản trị viên xét duyệt trong certification-service." />
      <Card>
        <CardHeader><CardTitle>Thông tin yêu cầu chứng thư</CardTitle></CardHeader>
        <CardContent>
          <form
            className="grid gap-5 md:grid-cols-2"
            onSubmit={(event) => {
              event.preventDefault();
              mutation.mutate({ commonName, ttl, organization, country });
            }}
          >
            <div className="space-y-2">
              <Label htmlFor="commonName">Common Name</Label>
              <Input id="commonName" value={commonName} onChange={(event) => setCommonName(event.target.value)} required />
            </div>
            <div className="space-y-2">
              <Label htmlFor="ttl">TTL</Label>
              <Input id="ttl" value={ttl} onChange={(event) => setTtl(event.target.value)} />
            </div>
            <div className="space-y-2">
              <Label htmlFor="organization">Organization</Label>
              <Input id="organization" value={organization} onChange={(event) => setOrganization(event.target.value)} />
            </div>
            <div className="space-y-2">
              <Label htmlFor="country">Quốc gia</Label>
              <Input id="country" value={country} onChange={(event) => setCountry(event.target.value)} />
            </div>
            {mutation.isError ? <p className="text-sm text-red-600 md:col-span-2">{mutation.error.message}</p> : null}
            <div className="md:col-span-2">
              <Button type="submit" disabled={mutation.isPending || !commonName.trim()}>
                {mutation.isPending ? "Đang gửi..." : "Gửi yêu cầu cấp chứng thư"}
              </Button>
            </div>
          </form>
        </CardContent>
      </Card>
    </div>
  );
}
