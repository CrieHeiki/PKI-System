# Existing Core Workflows

These diagrams summarize **repository-supported logic**, not a completed runtime integration test. Arrows show conceptual stages; not every arrow maps to one synchronous HTTP call. Refer to service implementations for exact routes and request bodies.

## 1. Certificate lifecycle

```mermaid
flowchart LR
    U[User certificate request] --> C[Certification Service]
    C --> A[Admin review / approval]
    A --> V[Vault PKI / Transit]
    V --> S[Certificate metadata and status]
    S --> R[Renewal / revocation workflows]
```

Existing code is organized under [`services/certification-service/`](../../services/certification-service/). Read its controllers and services for precise approval, issue, renewal and revoke transitions.

## 2. Document signing

```mermaid
flowchart TD
    D[Existing document / selected version] --> W[Signing request]
    W --> A[Signer review / approval]
    A --> U[Step-up verification, when required]
    U --> C[Certificate signing via Vault Transit]
    C --> O{Document format}
    O -->|PDF| P[PDF detached signature / optional visual appearance]
    O -->|Non-PDF| M[CMS signed output .p7m]
    P --> S[Store signed artifact]
    M --> S
    S --> V[Verification and audit pathways]
```

This represents the linked domain responsibilities of [`signing-workflow-service`](../../services/signing-workflow-service/), [`step-up-auth-service`](../../services/step-up-auth-service/), [`certification-service`](../../services/certification-service/) and [`document-service`](../../services/document-service/). The final verification step is an **available downstream workflow**, not a guarantee that verification runs automatically for each signature.

## 3. Signature verification

```mermaid
flowchart LR
    I[Signed artifact / signing request] --> F[Format parsing and cryptographic checks]
    F --> T{Trust source}
    T -->|Internal CA| N[Internal certificate metadata and status]
    T -->|External CA| E[User-supplied trust anchors / partial checks]
    N --> R[Verification report]
    E --> R
```

External CA trust validation, timestamps, and long-term revocation evidence are **not feature-complete**. Refer to [`VerificationService.java`](../../services/verification-service/src/main/java/com/ct556/verification_service/service/VerificationService.java) and [gap analysis](../project-status/gap-analysis.md).

## 4. Step-up and recovery (high-level)

A user may enroll an authenticator (subject to the existing first-enrollment ticket rules), obtain a WebAuthn challenge for sensitive actions, and submit an assertion. Existing recovery endpoints enable administrator-assisted recovery paths, but lost-session escalation and post-change session invalidation remain documented gaps.

Code links: [`WebAuthnStepUpService.java`](../../services/step-up-auth-service/src/main/java/com/ct556/step_up_auth_service/service/WebAuthnStepUpService.java) · [`WebAuthnRecoveryController.java`](../../services/step-up-auth-service/src/main/java/com/ct556/step_up_auth_service/controller/WebAuthnRecoveryController.java).
