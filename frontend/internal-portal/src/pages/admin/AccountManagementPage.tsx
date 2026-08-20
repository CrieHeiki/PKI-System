import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { useState } from "react";
import { Link } from "react-router-dom";

import { EmptyState } from "@/components/common/EmptyState";
import { PageHeader } from "@/components/common/PageHeader";
import { StatusBadge } from "@/components/common/StatusBadge";
import { Button } from "@/components/ui/button";
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import { Select } from "@/components/ui/select";
import { Table, TableWrapper, TBody, TD, TH, THead, TR } from "@/components/ui/table";
import { accountsService } from "@/services/accounts-service";
import { useAuthStore } from "@/store/auth-store";

export function AccountManagementPage() {
  const queryClient = useQueryClient();
  const currentUser = useAuthStore((state) => state.user);
  const [q, setQ] = useState("");
  const [role, setRole] = useState("");
  const [status, setStatus] = useState("");

  const accountsQuery = useQuery({
    queryKey: ["admin", "accounts", { q, role, status }],
    queryFn: () => accountsService.list({ q: q || undefined, role: role || undefined, status: status || undefined }),
  });

  const refresh = () => queryClient.invalidateQueries({ queryKey: ["admin", "accounts"] });
  const activateMutation = useMutation({ mutationFn: accountsService.activate, onSuccess: refresh });
  const lockMutation = useMutation({ mutationFn: accountsService.lock, onSuccess: refresh });
  const unlockMutation = useMutation({ mutationFn: accountsService.unlock, onSuccess: refresh });
  const disableMutation = useMutation({ mutationFn: accountsService.disable, onSuccess: refresh });
  const restoreMutation = useMutation({ mutationFn: accountsService.restore, onSuccess: refresh });

  return (
    <div className="space-y-7">
      <PageHeader eyebrow="Quản lý tài khoản" title="Danh sách tài khoản" description="Tìm kiếm, lọc và thực hiện các thao tác quản trị trên tài khoản từ account-service." />

      <Card>
        <CardHeader><CardTitle className="text-[1.35rem]">Bộ lọc</CardTitle></CardHeader>
        <CardContent className="grid gap-4 md:grid-cols-4">
          <div className="space-y-2 md:col-span-2">
            <Label htmlFor="q">Tìm theo username/email</Label>
            <Input id="q" value={q} onChange={(event) => setQ(event.target.value)} placeholder="Nhập username hoặc email" />
          </div>
          <div className="space-y-2">
            <Label htmlFor="role">Role</Label>
            <Select id="role" value={role} onChange={(event) => setRole(event.target.value)}>
              <option value="">Tat ca</option>
              <option value="USER">USER</option>
              <option value="ADMIN">ADMIN</option>
            </Select>
          </div>
          <div className="space-y-2">
            <Label htmlFor="status">Status</Label>
            <Select id="status" value={status} onChange={(event) => setStatus(event.target.value)}>
              <option value="">Tat ca</option>
              <option value="PENDING">PENDING</option>
              <option value="ACTIVE">ACTIVE</option>
              <option value="LOCKED">LOCKED</option>
              <option value="DISABLED">DISABLED</option>
            </Select>
          </div>
        </CardContent>
      </Card>

      <Card>
        <CardHeader><CardTitle className="text-[1.35rem]">Kết quả</CardTitle></CardHeader>
        <CardContent className="space-y-4">
          {accountsQuery.isLoading ? <p className="text-sm text-muted-foreground">Đang tải danh sách tài khoản...</p> : null}
          {accountsQuery.isError ? <p className="text-sm text-red-600">{accountsQuery.error.message}</p> : null}
          {accountsQuery.data?.length === 0 ? <EmptyState title="Không có account phù hợp" description="Thử đổi bộ lọc hoặc tạo account mới từ trang public." /> : null}
          {accountsQuery.data?.length ? (
            <TableWrapper>
              <Table>
                <THead>
                  <TR>
                    <TH>Account</TH>
                    <TH>Role</TH>
                    <TH>Status</TH>
                    <TH>Chi tiết</TH>
                    <TH className="text-right">Thao tac</TH>
                  </TR>
                </THead>
                <TBody>
                  {accountsQuery.data?.map((account) => {
                    const isSelfAccount = account.id === currentUser?.accountId;
                    return (
                    <TR key={account.id}>
                      <TD>
                        <div className="space-y-1">
                          <Link className="font-semibold text-slate-900 transition hover:text-primary" to={`/admin/accounts/${account.id}`}>
                            {account.username}
                          </Link>
                          <p className="text-xs text-slate-500">{account.email}</p>
                        </div>
                      </TD>
                      <TD>
                        <span className="rounded-full border border-slate-200 bg-white px-3 py-1 text-xs font-semibold uppercase tracking-[0.16em] text-slate-700">
                          {account.role}
                        </span>
                      </TD>
                      <TD><StatusBadge status={account.status} /></TD>
                      <TD>
                        <span className="text-xs text-slate-500">{account.id}</span>
                      </TD>
                      <TD className="text-right">
                        <div className="flex flex-wrap justify-end gap-2">
                          {account.status === "PENDING" ? (
                            <Button size="sm" variant="secondary" onClick={() => activateMutation.mutate(account.id)}>Activate</Button>
                          ) : null}
                          {account.status === "ACTIVE" ? (
                            <>
                              <Button size="sm" variant="outline" onClick={() => lockMutation.mutate(account.id)} disabled={isSelfAccount}>Lock</Button>
                              <Button size="sm" variant="danger" onClick={() => disableMutation.mutate(account.id)} disabled={isSelfAccount}>Disable</Button>
                            </>
                          ) : null}
                          {account.status === "LOCKED" ? (
                            <>
                              <Button size="sm" variant="secondary" onClick={() => unlockMutation.mutate(account.id)}>Unlock</Button>
                              <Button size="sm" variant="danger" onClick={() => disableMutation.mutate(account.id)} disabled={isSelfAccount}>Disable</Button>
                            </>
                          ) : null}
                          {account.status === "DISABLED" ? (
                            <Button size="sm" variant="secondary" onClick={() => restoreMutation.mutate(account.id)}>Restore</Button>
                          ) : null}
                        </div>
                      </TD>
                    </TR>
                    );
                  })}
                </TBody>
              </Table>
            </TableWrapper>
          ) : null}
        </CardContent>
      </Card>
    </div>
  );
}
