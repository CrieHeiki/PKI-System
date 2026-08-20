import { Suspense, lazy, type ComponentType } from "react";
import { Navigate, createBrowserRouter } from "react-router-dom";

import { AppShell } from "@/layouts/AppShell";
import { BlockedLayout } from "@/layouts/BlockedLayout";
import { PublicLayout } from "@/layouts/PublicLayout";
import { ActiveAccountRoute } from "@/routes/guards/ActiveAccountRoute";
import { AuthenticatedRoute } from "@/routes/guards/AuthenticatedRoute";
import { PublicRoute } from "@/routes/guards/PublicRoute";
import { RoleBasedRoute } from "@/routes/guards/RoleBasedRoute";

function lazyPage<TModule extends Record<string, ComponentType<any>>, TName extends keyof TModule>(
  loader: () => Promise<TModule>,
  exportName: TName,
) {
  return lazy(async () => {
    const module = await loader();
    return { default: module[exportName] as ComponentType<any> };
  });
}

function RouteFallback() {
  return (
    <div className="rounded-[28px] border border-white/70 bg-white/85 px-6 py-10 shadow-[0_16px_35px_rgba(15,23,42,0.06)]">
      <div className="h-3 w-28 animate-pulse rounded-full bg-slate-200" />
      <div className="mt-5 h-7 w-64 animate-pulse rounded-full bg-slate-200" />
      <div className="mt-8 grid gap-3">
        <div className="h-24 animate-pulse rounded-[24px] bg-slate-100" />
        <div className="h-24 animate-pulse rounded-[24px] bg-slate-100" />
      </div>
    </div>
  );
}

function renderLazy(Component: ReturnType<typeof lazy>) {
  return (
    <Suspense fallback={<RouteFallback />}>
      <Component />
    </Suspense>
  );
}

const AccountDetailPage = lazyPage(() => import("@/pages/admin/AccountDetailPage"), "AccountDetailPage");
const AccountManagementPage = lazyPage(() => import("@/pages/admin/AccountManagementPage"), "AccountManagementPage");
const AdminDocumentDetailPage = lazyPage(() => import("@/pages/admin/AdminDocumentDetailPage"), "AdminDocumentDetailPage");
const AdminDocumentsPage = lazyPage(() => import("@/pages/admin/AdminDocumentsPage"), "AdminDocumentsPage");
const AdminDashboardPage = lazyPage(() => import("@/pages/admin/AdminDashboardPage"), "AdminDashboardPage");
const AdminProfilePage = lazyPage(() => import("@/pages/admin/AdminProfilePage"), "AdminProfilePage");
const AuditLogDetailPage = lazyPage(() => import("@/pages/admin/AuditLogDetailPage"), "AuditLogDetailPage");
const AuditLogPage = lazyPage(() => import("@/pages/admin/AuditLogPage"), "AuditLogPage");
const CertificateManagementPage = lazyPage(() => import("@/pages/admin/CertificateManagementPage"), "CertificateManagementPage");
const CertificateRequestManagementPage = lazyPage(() => import("@/pages/admin/CertificateRequestManagementPage"), "CertificateRequestManagementPage");
const RevocationRequestManagementPage = lazyPage(() => import("@/pages/admin/RevocationRequestManagementPage"), "RevocationRequestManagementPage");
const RenewalRequestManagementPage = lazyPage(() => import("@/pages/admin/RenewalRequestManagementPage"), "RenewalRequestManagementPage");

const DisabledAccountPage = lazyPage(() => import("@/pages/blocked/DisabledAccountPage"), "DisabledAccountPage");
const LockedAccountPage = lazyPage(() => import("@/pages/blocked/LockedAccountPage"), "LockedAccountPage");
const PendingActivationPage = lazyPage(() => import("@/pages/blocked/PendingActivationPage"), "PendingActivationPage");

const LoginPage = lazyPage(() => import("@/pages/public/LoginPage"), "LoginPage");
const RegisterPage = lazyPage(() => import("@/pages/public/RegisterPage"), "RegisterPage");

const NotFoundPage = lazyPage(() => import("@/pages/system/NotFoundPage"), "NotFoundPage");
const UnauthorizedPage = lazyPage(() => import("@/pages/system/UnauthorizedPage"), "UnauthorizedPage");

const CreateSignRequestPage = lazyPage(() => import("@/pages/user/CreateSignRequestPage"), "CreateSignRequestPage");
const DocumentDetailPage = lazyPage(() => import("@/pages/user/DocumentDetailPage"), "DocumentDetailPage");
const MyCertificatePage = lazyPage(() => import("@/pages/user/MyCertificatePage"), "MyCertificatePage");
const MyCreatedSignRequestsPage = lazyPage(() => import("@/pages/user/MyCreatedSignRequestsPage"), "MyCreatedSignRequestsPage");
const MyDocumentsPage = lazyPage(() => import("@/pages/user/MyDocumentsPage"), "MyDocumentsPage");
const ProfileUserPage = lazyPage(() => import("@/pages/user/ProfileUserPage"), "ProfileUserPage");
const RequestCertificatePage = lazyPage(() => import("@/pages/user/RequestCertificatePage"), "RequestCertificatePage");
const RequestRevocationPage = lazyPage(() => import("@/pages/user/RequestRevocationPage"), "RequestRevocationPage");
const RequestRenewalPage = lazyPage(() => import("@/pages/user/RequestRenewalPage"), "RequestRenewalPage");
const RequestsToSignPage = lazyPage(() => import("@/pages/user/RequestsToSignPage"), "RequestsToSignPage");
const SignRequestDetailPage = lazyPage(() => import("@/pages/user/SignRequestDetailPage"), "SignRequestDetailPage");
const StepUpManagementPage = lazyPage(() => import("@/pages/user/StepUpManagementPage"), "StepUpManagementPage");
const UploadDocumentPage = lazyPage(() => import("@/pages/user/UploadDocumentPage"), "UploadDocumentPage");
const UserDashboardPage = lazyPage(() => import("@/pages/user/UserDashboardPage"), "UserDashboardPage");
const VerificationReportsPage = lazyPage(() => import("@/pages/user/VerificationReportsPage"), "VerificationReportsPage");
const VerifyDocumentPage = lazyPage(() => import("@/pages/user/VerifyDocumentPage"), "VerifyDocumentPage");

export const router = createBrowserRouter([
  {
    element: <PublicRoute />,
    children: [
      {
        element: <PublicLayout />,
        children: [
          { path: "/", element: <Navigate to="/dang-nhap" replace /> },
          { path: "/dang-nhap", element: renderLazy(LoginPage) },
          { path: "/dang-ky", element: renderLazy(RegisterPage) },
        ],
      },
    ],
  },
  {
    element: <BlockedLayout />,
    children: [
      { path: "/blocked/pending", element: renderLazy(PendingActivationPage) },
      { path: "/blocked/locked", element: renderLazy(LockedAccountPage) },
      { path: "/blocked/disabled", element: renderLazy(DisabledAccountPage) },
      { path: "/403", element: renderLazy(UnauthorizedPage) },
    ],
  },
  {
    element: <AuthenticatedRoute />,
    children: [
      {
        element: <ActiveAccountRoute />,
        children: [
          {
            element: <RoleBasedRoute role="USER" />,
            children: [
              {
                path: "/app",
                element: <AppShell />,
                children: [
                  { index: true, element: renderLazy(UserDashboardPage) },
                  { path: "documents", element: renderLazy(MyDocumentsPage) },
                  { path: "documents/upload", element: renderLazy(UploadDocumentPage) },
                  { path: "documents/:id", element: renderLazy(DocumentDetailPage) },
                  { path: "sign-requests/create", element: renderLazy(CreateSignRequestPage) },
                  { path: "sign-requests/mine", element: renderLazy(MyCreatedSignRequestsPage) },
                  { path: "sign-requests/to-sign", element: renderLazy(RequestsToSignPage) },
                  { path: "sign-requests/:id", element: renderLazy(SignRequestDetailPage) },
                  { path: "certificate", element: renderLazy(MyCertificatePage) },
                  { path: "certificate/request", element: renderLazy(RequestCertificatePage) },
                  { path: "certificate/renew", element: renderLazy(RequestRenewalPage) },
                  { path: "certificate/revoke", element: renderLazy(RequestRevocationPage) },
                  { path: "verify", element: renderLazy(VerifyDocumentPage) },
                  { path: "verification-reports", element: renderLazy(VerificationReportsPage) },
                  { path: "step-up", element: renderLazy(StepUpManagementPage) },
                  { path: "profile", element: renderLazy(ProfileUserPage) },
                ],
              },
            ],
          },
          {
            element: <RoleBasedRoute role="ADMIN" />,
            children: [
              {
                path: "/admin",
                element: <AppShell />,
                children: [
                  { index: true, element: renderLazy(AdminDashboardPage) },
                  { path: "accounts", element: renderLazy(AccountManagementPage) },
                  { path: "accounts/:id", element: renderLazy(AccountDetailPage) },
                  { path: "accounts/demo", element: renderLazy(AccountManagementPage) },
                  { path: "documents", element: renderLazy(AdminDocumentsPage) },
                  { path: "documents/:id", element: renderLazy(AdminDocumentDetailPage) },
                  { path: "certificates/requests", element: renderLazy(CertificateRequestManagementPage) },
                  { path: "certificates/renewals", element: renderLazy(RenewalRequestManagementPage) },
                  { path: "certificates/revocations", element: renderLazy(RevocationRequestManagementPage) },
                  { path: "certificates/manage", element: renderLazy(CertificateManagementPage) },
                  { path: "audit", element: renderLazy(AuditLogPage) },
                  { path: "audit/:id", element: renderLazy(AuditLogDetailPage) },
                  { path: "profile", element: renderLazy(AdminProfilePage) },
                ],
              },
            ],
          },
        ],
      },
    ],
  },
  {
    path: "*",
    element: renderLazy(NotFoundPage),
  },
]);
