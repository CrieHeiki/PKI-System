# PKI System — Internal Portal

This React + TypeScript application is the frontend of the [PKI System](../../README.md). It is an **existing development frontend** connected to the Spring Boot backend through Nginx and API Gateway; it is not a standalone mocked demo.

## Technologies

- React, TypeScript, Vite
- React Router and route guards
- TanStack Query, Zustand
- React Hook Form, Zod
- Tailwind CSS and reusable UI primitives

See [frontend architecture](../../docs/architecture/frontend-architecture.md), [route inventory](../../docs/reference/frontend-routes.md) and [page inventory](../../docs/reference/frontend-pages.md).

## Existing feature areas

- Public: landing, sign-in and registration
- User: dashboard, documents and versions, certificate requests, signing requests, step-up, verification reports
- Admin: accounts/status, certificate approvals and revocation, document/audit views
- Blocked-account views and role-based route guards

This is an overview of existing pages, **not a guarantee that all workflows or UI states have been fully verified**. Consult [gap analysis](../../docs/project-status/gap-analysis.md).

## Local development (reference)

PowerShell:

```powershell
Copy-Item .env.example .env
npm install
npm run dev
```

The example `VITE_API_BASE_URL` points to `http://localhost:8080` (Nginx). Vite typically serves the frontend on `http://localhost:5173`. Backend services and infrastructure must be configured separately; read the [root README](../../README.md). This repository's publication-only documentation update does not claim a fresh end-to-end deployment was tested.

Existing commands: `npm run dev`, `npm run build`, `npm run lint`, `npm run typecheck`.
