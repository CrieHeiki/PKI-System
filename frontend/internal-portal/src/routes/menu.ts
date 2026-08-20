import {
  BadgeCheck,
  ClipboardCheck,
  FileCheck2,
  FilePlus2,
  FileText,
  Fingerprint,
  FolderOpen,
  KeyRound,
  LayoutDashboard,
  ScrollText,
  Shield,
  Users,
  type LucideIcon,
} from "lucide-react";

import type { AppRole } from "@/types/auth";

export type SidebarMenuItem = {
  to: string;
  label: string;
  icon: LucideIcon;
  activePatterns?: string[];
  excludePatterns?: string[];
};

export type SidebarMenuGroup = {
  label: string;
  items: SidebarMenuItem[];
};

export function getMenuForRole(role: AppRole): SidebarMenuGroup[] {
  if (role === "ADMIN") {
    return [
      {
        label: "Tổng quan",
        items: [{ to: "/admin", label: "Bảng điều khiển quản trị", icon: LayoutDashboard }],
      },
      {
        label: "Quản lý tài khoản",
        items: [{ to: "/admin/accounts", label: "Danh sách tài khoản", icon: Users, activePatterns: ["/admin/accounts", "/admin/accounts/:id"] }],
      },
      {
        label: "Quản lý chứng thư",
        items: [
          { to: "/admin/certificates/requests", label: "Yêu cầu cấp", icon: BadgeCheck },
          { to: "/admin/certificates/renewals", label: "Yêu cầu gia hạn", icon: Fingerprint },
          { to: "/admin/certificates/revocations", label: "Yêu cầu thu hồi", icon: ClipboardCheck },
          { to: "/admin/certificates/manage", label: "Quản lý chứng thư", icon: Shield },
        ],
      },
      {
        label: "Quản lý tài liệu",
        items: [
          {
            to: "/admin/documents",
            label: "Tài liệu đã tải lên",
            icon: FolderOpen,
            activePatterns: ["/admin/documents", "/admin/documents/:id"],
          },
        ],
      },
      {
        label: "Kiểm toán",
        items: [{ to: "/admin/audit", label: "Nhật ký hệ thống", icon: ScrollText, activePatterns: ["/admin/audit", "/admin/audit/:id"] }],
      },
      {
        label: "Hồ sơ",
        items: [{ to: "/admin/profile", label: "Hồ sơ quản trị viên", icon: FileText }],
      },
    ];
  }

  return [
    {
      label: "Tổng quan",
      items: [{ to: "/app", label: "Bảng điều khiển", icon: LayoutDashboard }],
    },
    {
      label: "Tài liệu",
      items: [
        {
          to: "/app/documents",
          label: "Danh sách tài liệu",
          icon: FileText,
          activePatterns: ["/app/documents", "/app/documents/:id"],
          excludePatterns: ["/app/documents/upload"],
        },
        { to: "/app/documents/upload", label: "Tải lên tài liệu", icon: FilePlus2 },
      ],
    },
    {
      label: "Yêu cầu ký",
      items: [
        { to: "/app/sign-requests/create", label: "Tạo yêu cầu ký", icon: ClipboardCheck },
        { to: "/app/sign-requests/mine", label: "Yêu cầu tôi tạo", icon: FileCheck2 },
        { to: "/app/sign-requests/to-sign", label: "Yêu cầu cần tôi ký", icon: BadgeCheck },
      ],
    },
    {
      label: "Chứng thư và xác minh",
      items: [
        { to: "/app/certificate", label: "Chứng thư của tôi", icon: Fingerprint },
        { to: "/app/certificate/request", label: "Yêu cầu cấp chứng thư", icon: BadgeCheck },
        { to: "/app/certificate/renew", label: "Yêu cầu gia hạn", icon: Fingerprint },
        { to: "/app/certificate/revoke", label: "Yêu cầu thu hồi", icon: ClipboardCheck },
        { to: "/app/verify", label: "Xác minh tài liệu", icon: Shield },
        { to: "/app/verification-reports", label: "Báo cáo xác minh", icon: ScrollText },
      ],
    },
    {
      label: "Tài khoản",
      items: [
        { to: "/app/step-up", label: "Bảo mật và step-up", icon: KeyRound },
        { to: "/app/profile", label: "Hồ sơ cá nhân", icon: Users },
      ],
    },
  ];
}
