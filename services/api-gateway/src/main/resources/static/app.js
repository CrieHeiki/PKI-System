const STORAGE_KEY = "ct556-pki-console-state";

const STATE_FIELDS = [
    "baseUrl",
    "operatorToken",
    "operatorAccountId",
    "operatorUsername",
    "operatorRole",
    "adminToken",
    "requesterToken",
    "signerToken",
    "requesterAccountId",
    "signerAccountId",
    "documentId",
    "certificateRequestId",
    "certificateId",
    "mfaRegistrationId",
    "stepUpChallengeId",
    "stepUpVerificationToken",
    "signingRequestId",
    "verificationReportId",
    "auditEventId",
    "activeModule"
];

const MODULE_META = {
    accounts: {
        title: "Accounts Workspace",
        description: "Tạo requester/signer demo, đăng nhập từng vai trò, và kích hoạt tài khoản khi cần."
    },
    documents: {
        title: "Document Service Workspace",
        description: "Upload tài liệu thật, đồng bộ documentId, và xem metadata của tài liệu hiện tại."
    },
    certificates: {
        title: "Certification Workspace",
        description: "Tạo yêu cầu chứng thư, duyệt bằng admin, và lấy certificate để signer sử dụng."
    },
    "step-up": {
        title: "Step-up Auth Workspace",
        description: "Đăng ký MFA, tạo challenge TTL trong Redis, và xác minh proof token cho hành động ký."
    },
    signing: {
        title: "Signing Workflow Workspace",
        description: "Tạo yêu cầu ký, approve bởi signer, execute-signing và theo dõi artifact kết quả."
    },
    verification: {
        title: "Verification Workspace",
        description: "Xác minh signed output theo request, lấy report mới nhất hoặc duyệt report gần đây."
    },
    audit: {
        title: "Audit Workspace",
        description: "Tra cứu audit event đã được ghi tự động hoặc tạo event bổ sung khi cần."
    },
    workspace: {
        title: "Workspace State",
        description: "Quản lý base URL, token, và các ID đang dùng xuyên suốt phiên demo hiện tại."
    }
};

const INPUTS = {
    baseUrl: document.getElementById("base-url"),
    adminToken: document.getElementById("admin-token"),
    requesterToken: document.getElementById("requester-token"),
    signerToken: document.getElementById("signer-token"),
    requesterAccountId: document.getElementById("requester-account-id"),
    signerAccountId: document.getElementById("signer-account-id"),
    documentId: document.getElementById("document-id"),
    certificateRequestId: document.getElementById("certificate-request-id"),
    certificateId: document.getElementById("certificate-id"),
    mfaRegistrationId: document.getElementById("mfa-registration-id"),
    stepUpChallengeId: document.getElementById("step-up-challenge-id"),
    stepUpVerificationToken: document.getElementById("step-up-verification-token"),
    signingRequestId: document.getElementById("signing-request-id"),
    verificationReportId: document.getElementById("verification-report-id"),
    auditEventId: document.getElementById("audit-event-id")
};

const authScreen = document.getElementById("auth-screen");
const appShell = document.getElementById("app-shell");
const tabLogin = document.getElementById("tab-login");
const tabRegister = document.getElementById("tab-register");
const loginPanel = document.getElementById("login-panel");
const registerPanel = document.getElementById("register-panel");
const authFeedback = document.getElementById("auth-feedback");
const operatorLoginForm = document.getElementById("operator-login-form");
const portalRegisterForm = document.getElementById("portal-register-form");
const operatorLoginUsername = document.getElementById("operator-login-username");
const operatorLoginPassword = document.getElementById("operator-login-password");
const portalRegisterUsername = document.getElementById("portal-register-username");
const portalRegisterEmail = document.getElementById("portal-register-email");
const portalRegisterPassword = document.getElementById("portal-register-password");
const authGatewayBadge = document.getElementById("auth-gateway-badge");
const baseUrlBadge = document.getElementById("base-url-badge");
const sessionRoleBadge = document.getElementById("session-role-badge");
const sessionSummary = document.getElementById("session-summary");
const operatorName = document.getElementById("operator-name");
const operatorRole = document.getElementById("operator-role");
const operatorAccountId = document.getElementById("operator-account-id");
const homeView = document.getElementById("home-view");
const moduleView = document.getElementById("module-view");
const moduleTitle = document.getElementById("module-title");
const moduleDescription = document.getElementById("module-description");
const moduleSections = Array.from(document.querySelectorAll(".module-section"));
const moduleCards = Array.from(document.querySelectorAll("[data-module-target]"));
const roleScopedElements = Array.from(document.querySelectorAll("[data-role-scope]"));
const backToDashboardButton = document.getElementById("back-to-dashboard");
const goHomeButton = document.getElementById("go-home");
const lockConsoleButton = document.getElementById("lock-console");
const logoutConsoleButton = document.getElementById("logout-console");
const consoleTitle = document.getElementById("console-title");
const consoleStatus = document.getElementById("console-status");
const consoleOutput = document.getElementById("console-output");

const state = loadState();
applyState();
bindEvents();

function bindEvents() {
    tabLogin.addEventListener("click", () => switchAuthTab("login"));
    tabRegister.addEventListener("click", () => switchAuthTab("register"));

    operatorLoginForm.addEventListener("submit", async (event) => {
        event.preventDefault();
        await runAuthAction("Portal Login", loginOperator);
    });

    portalRegisterForm.addEventListener("submit", async (event) => {
        event.preventDefault();
        await runAuthAction("Portal Register", registerPortalAccount);
    });

    moduleCards.forEach((card) => {
        card.addEventListener("click", (event) => {
            const trigger = event.target.closest("[data-module-target]") || card;
            const moduleKey = trigger.getAttribute("data-module-target");
            openModule(moduleKey);
        });
    });

    backToDashboardButton.addEventListener("click", showHomeView);
    goHomeButton.addEventListener("click", showHomeView);

    lockConsoleButton.addEventListener("click", () => {
        clearPortalSession();
        applyState();
        persistState();
        setConsole("Portal Locked", "idle", {
            message: "Portal session removed. Login is required to continue."
        });
    });

    logoutConsoleButton.addEventListener("click", () => {
        resetAllState();
        applyState();
        persistState();
        setConsole("Signed Out", "idle", {
            message: "Portal session and local workspace state were cleared."
        });
    });

    document.getElementById("save-state").addEventListener("click", () => {
        readStateFromInputs();
        persistState();
        setConsole("State Saved", "success", state);
    });

    document.getElementById("reset-state").addEventListener("click", () => {
        resetWorkspaceState();
        applyState();
        persistState();
        setConsole("State Reset", "idle", state);
    });

    const mapRequesterButton = document.getElementById("map-operator-to-requester");
    if (mapRequesterButton) {
        mapRequesterButton.addEventListener("click", () => runAction("Map Current User To Requester", () => useCurrentOperatorAs("requester")));
    }

    const mapSignerButton = document.getElementById("map-operator-to-signer");
    if (mapSignerButton) {
        mapSignerButton.addEventListener("click", () => runAction("Map Current User To Signer", () => useCurrentOperatorAs("signer")));
    }

    document.getElementById("register-user").addEventListener("click", () => runAction("Register User", registerUser));
    document.getElementById("login-user").addEventListener("click", () => runAction("Login", loginUser));
    document.getElementById("activate-requester").addEventListener("click", () => runAction("Activate Requester", () => activateAccount("requester")));
    document.getElementById("activate-signer").addEventListener("click", () => runAction("Activate Signer", () => activateAccount("signer")));
    document.getElementById("lock-requester")?.addEventListener("click", () => runAction("Lock Requester", () => lockAccount("requester")));
    document.getElementById("lock-signer")?.addEventListener("click", () => runAction("Lock Signer", () => lockAccount("signer")));
    document.getElementById("disable-requester")?.addEventListener("click", () => runAction("Disable Requester", () => disableAccount("requester")));
    document.getElementById("disable-signer")?.addEventListener("click", () => runAction("Disable Signer", () => disableAccount("signer")));
    document.getElementById("get-requester-account")?.addEventListener("click", () => runAction("Get Requester Detail", () => getManagedAccount("requester")));
    document.getElementById("get-signer-account")?.addEventListener("click", () => runAction("Get Signer Detail", () => getManagedAccount("signer")));
    document.getElementById("change-requester-role")?.addEventListener("click", () => runAction("Change Requester Role", () => changeManagedRole("requester")));
    document.getElementById("change-signer-role")?.addEventListener("click", () => runAction("Change Signer Role", () => changeManagedRole("signer")));
    document.getElementById("use-current-as-requester")?.addEventListener("click", () => runAction("Use Current User As Requester", () => useCurrentOperatorAs("requester")));
    document.getElementById("use-current-as-signer")?.addEventListener("click", () => runAction("Use Current User As Signer", () => useCurrentOperatorAs("signer")));

    document.getElementById("upload-document").addEventListener("click", () => runAction("Upload Document", uploadDocument));
    document.getElementById("upload-document-version")?.addEventListener("click", () => runAction("Upload Document Version", uploadDocumentVersion));
    document.getElementById("list-my-documents").addEventListener("click", () => runAction("List My Documents", listMyDocuments));
    document.getElementById("get-document-detail").addEventListener("click", () => runAction("Get Document Detail", getDocumentDetail));
    document.getElementById("download-document")?.addEventListener("click", () => runAction("Download Document", downloadDocument));
    document.getElementById("list-all-documents")?.addEventListener("click", () => runAction("List All Documents", listAllDocuments));

    document.getElementById("create-certificate-request").addEventListener("click", () => runAction("Create Certificate Request", createCertificateRequest));
    document.getElementById("list-my-certificate-requests")?.addEventListener("click", () => runAction("List My Certificate Requests", listMyCertificateRequests));
    document.getElementById("get-certificate-request")?.addEventListener("click", () => runAction("Get Certificate Request Detail", getCertificateRequest));
    document.getElementById("approve-certificate-request").addEventListener("click", () => runAction("Approve Certificate Request", approveCertificateRequest));
    document.getElementById("list-my-certificates").addEventListener("click", () => runAction("List My Certificates", listMyCertificates));
    document.getElementById("get-certificate-detail")?.addEventListener("click", () => runAction("Get Certificate Detail", getCertificateDetail));
    document.getElementById("create-renewal-request")?.addEventListener("click", () => runAction("Create Renewal Request", createRenewalRequest));
    document.getElementById("revoke-my-certificate")?.addEventListener("click", () => runAction("Revoke My Certificate", revokeMyCertificate));
    document.getElementById("reject-certificate-request")?.addEventListener("click", () => runAction("Reject Certificate Request", rejectCertificateRequest));
    document.getElementById("list-all-certificate-requests")?.addEventListener("click", () => runAction("List All Certificate Requests", listAllCertificateRequests));
    document.getElementById("list-all-certificates")?.addEventListener("click", () => runAction("List All Certificates", listAllCertificates));
    document.getElementById("revoke-admin-certificate")?.addEventListener("click", () => runAction("Admin Revoke Certificate", revokeAdminCertificate));

    document.getElementById("create-mfa-registration").addEventListener("click", () => runAction("Create MFA Registration", createMfaRegistration));
    document.getElementById("list-mfa-registrations").addEventListener("click", () => runAction("List My MFA Registrations", listMfaRegistrations));
    document.getElementById("disable-mfa")?.addEventListener("click", () => runAction("Disable MFA Registration", disableMfa));
    document.getElementById("create-step-up-challenge").addEventListener("click", () => runAction("Create Step-up Challenge", createStepUpChallenge));
    document.getElementById("verify-step-up-challenge").addEventListener("click", () => runAction("Verify Step-up Challenge", verifyStepUpChallenge));

    document.getElementById("create-signing-request").addEventListener("click", () => runAction("Create Signing Request", createSigningRequest));
    document.getElementById("list-requested-by-me")?.addEventListener("click", () => runAction("List Requested By Me", listRequestedByMe));
    document.getElementById("list-pending-for-me")?.addEventListener("click", () => runAction("List Pending For Me", listPendingForMe));
    document.getElementById("approve-signing-request").addEventListener("click", () => runAction("Approve Signing Request", approveSigningRequest));
    document.getElementById("reject-signing-request")?.addEventListener("click", () => runAction("Reject Signing Request", rejectSigningRequest));
    document.getElementById("cancel-signing-request")?.addEventListener("click", () => runAction("Cancel Signing Request", cancelSigningRequest));
    document.getElementById("execute-signing-request").addEventListener("click", () => runAction("Execute Signing", executeSigningRequest));
    document.getElementById("get-signing-request").addEventListener("click", () => runAction("Get Signing Detail", getSigningRequest));
    document.getElementById("list-all-signing-requests")?.addEventListener("click", () => runAction("List All Signing Requests", listAllSigningRequests));

    document.getElementById("verify-signing-request").addEventListener("click", () => runAction("Verify Signing Request", verifySigningRequest));
    document.getElementById("load-latest-verification").addEventListener("click", () => runAction("Load Latest Verification", getLatestVerification));
    document.getElementById("load-recent-verifications").addEventListener("click", () => runAction("Load Recent Verification Reports", getRecentVerifications));
    document.getElementById("get-verification-report")?.addEventListener("click", () => runAction("Get Verification Report", getVerificationReport));

    document.getElementById("create-audit-event").addEventListener("click", () => runAction("Create Audit Event", createAuditEvent));
    document.getElementById("search-audit-events").addEventListener("click", () => runAction("Search Audit Events", searchAuditEvents));
    document.getElementById("get-audit-event").addEventListener("click", () => runAction("Get Audit Event", getAuditEvent));
}

async function runAuthAction(title, fn) {
    hideAuthFeedback();

    try {
        const result = await fn();
        applyState();
        persistState();
        setConsole(title, "success", result);
    } catch (error) {
        const formatted = formatError(error);
        showAuthFeedback(extractErrorMessage(formatted), "error");
        setConsole(title, "error", formatted);
    }
}

async function runAction(title, fn) {
    if (!hasPortalSession()) {
        const payload = { message: "Portal is locked. Sign in again to continue." };
        showAuthFeedback(payload.message, "error");
        setConsole(title, "error", payload);
        return;
    }

    readStateFromInputs();
    persistState();

    try {
        const result = await fn();
        applyState();
        persistState();
        setConsole(title, "success", result);
    } catch (error) {
        setConsole(title, "error", formatError(error));
    }
}

async function loginOperator() {
    const usernameOrEmail = operatorLoginUsername.value.trim();
    const password = operatorLoginPassword.value;

    if (!usernameOrEmail || !password) {
        throw new Error("Username/email and password are required");
    }

    const response = await apiRequest("POST", "/api/v1/auth/login", {
        body: { usernameOrEmail, password }
    });

    const accessToken = response.data.accessToken ?? "";
    if (!accessToken) {
        throw new Error("Access token was not returned by the login API");
    }

    const claims = decodeJwtPayload(accessToken);
    const role = normalizeRole(claims.role ?? claims.roles ?? "");
    const accountId = claims.accountId ?? "";
    const username = claims.username ?? claims.sub ?? usernameOrEmail;

    state.operatorToken = accessToken;
    state.operatorAccountId = accountId;
    state.operatorUsername = username;
    state.operatorRole = role;

    if (role === "ADMIN") {
        state.adminToken = accessToken;
    }

    operatorLoginPassword.value = "";
    state.activeModule = "";
    showAuthFeedback("Login successful. Entering the internal dashboard.", "success");

    return {
        operatorUsername: username,
        operatorRole: role,
        operatorAccountId: accountId
    };
}

async function registerPortalAccount() {
    const payload = {
        username: portalRegisterUsername.value.trim(),
        email: portalRegisterEmail.value.trim(),
        password: portalRegisterPassword.value
    };

    if (!payload.username || !payload.email || !payload.password) {
        throw new Error("Username, email, and password are required");
    }

    const response = await apiRequest("POST", "/api/v1/auth/register", { body: payload });

    operatorLoginUsername.value = payload.username;
    operatorLoginPassword.value = "";
    switchAuthTab("login");
    showAuthFeedback("Registration successful. The new account stays in PENDING status until an admin activates it.", "success");

    return response.data;
}

function switchAuthTab(tab) {
    const isLogin = tab === "login";
    tabLogin.classList.toggle("active", isLogin);
    tabRegister.classList.toggle("active", !isLogin);
    loginPanel.classList.toggle("active", isLogin);
    registerPanel.classList.toggle("active", !isLogin);
}

function showHomeView() {
    state.activeModule = "";
    applyState();
    persistState();
}

function openModule(moduleKey) {
    if (!MODULE_META[moduleKey]) {
        return;
    }

    state.activeModule = moduleKey;
    applyState();
    persistState();
    window.scrollTo({ top: 0, behavior: "smooth" });
}

function clearPortalSession() {
    state.operatorToken = "";
    state.operatorAccountId = "";
    state.operatorUsername = "";
    state.operatorRole = "";
    state.activeModule = "";
}

function resetWorkspaceState() {
    const baseUrl = state.baseUrl || detectBaseUrl();
    const sessionSnapshot = {
        operatorToken: state.operatorToken,
        operatorAccountId: state.operatorAccountId,
        operatorUsername: state.operatorUsername,
        operatorRole: state.operatorRole
    };

    Object.keys(state).forEach((key) => {
        state[key] = key === "baseUrl" ? baseUrl : "";
    });

    Object.assign(state, sessionSnapshot);
}

function resetAllState() {
    const baseUrl = state.baseUrl || detectBaseUrl();
    Object.keys(state).forEach((key) => {
        state[key] = key === "baseUrl" ? baseUrl : "";
    });

    operatorLoginUsername.value = "";
    operatorLoginPassword.value = "";
    portalRegisterUsername.value = "";
    portalRegisterEmail.value = "";
    portalRegisterPassword.value = "";
    hideAuthFeedback();
    switchAuthTab("login");
}

async function registerUser() {
    const target = valueOf("register-target");
    const payload = {
        username: valueOf("register-username"),
        email: valueOf("register-email"),
        password: valueOf("register-password")
    };

    const response = await apiRequest("POST", "/api/v1/auth/register", { body: payload });
    if (target === "requester") {
        state.requesterAccountId = response.data.id ?? "";
    } else {
        state.signerAccountId = response.data.id ?? "";
    }
    return response.data;
}

async function loginUser() {
    const target = valueOf("login-target");
    const response = await apiRequest("POST", "/api/v1/auth/login", {
        body: {
            usernameOrEmail: valueOf("login-username"),
            password: valueOf("login-password")
        }
    });

    const token = response.data.accessToken ?? "";
    const claims = decodeJwtPayload(token);
    const accountId = claims.accountId ?? "";
    if (target === "admin") {
        state.adminToken = token;
    } else if (target === "requester") {
        state.requesterToken = token;
        state.requesterAccountId = accountId || state.requesterAccountId;
    } else {
        state.signerToken = token;
        state.signerAccountId = accountId || state.signerAccountId;
    }
    return response.data;
}

async function activateAccount(target) {
    const accountId = target === "requester" ? state.requesterAccountId : state.signerAccountId;
    ensureValue(accountId, `${target}AccountId`);

    const response = await apiRequest("PATCH", `/api/v1/admin/accounts/${accountId}/activate`, {
        token: adminTokenValue()
    });
    return response.data;
}

async function lockAccount(target) {
    const accountId = target === "requester" ? state.requesterAccountId : state.signerAccountId;
    ensureValue(accountId, `${target}AccountId`);

    const response = await apiRequest("PATCH", `/api/v1/admin/accounts/${accountId}/lock`, {
        token: adminTokenValue()
    });
    return response.data;
}

async function disableAccount(target) {
    const accountId = target === "requester" ? state.requesterAccountId : state.signerAccountId;
    ensureValue(accountId, `${target}AccountId`);

    const response = await apiRequest("PATCH", `/api/v1/admin/accounts/${accountId}/disable`, {
        token: adminTokenValue()
    });
    return response.data;
}

async function getManagedAccount(target) {
    const accountId = target === "requester" ? state.requesterAccountId : state.signerAccountId;
    ensureValue(accountId, `${target}AccountId`);

    const response = await apiRequest("GET", `/api/v1/admin/accounts/${accountId}`, {
        token: adminTokenValue()
    });
    return response.data;
}

async function changeManagedRole(target) {
    const accountId = target === "requester" ? state.requesterAccountId : state.signerAccountId;
    const roleInputId = target === "requester" ? "requester-role" : "signer-role";
    ensureValue(accountId, `${target}AccountId`);

    const response = await apiRequest("PATCH", `/api/v1/admin/accounts/${accountId}/role`, {
        token: adminTokenValue(),
        body: {
            role: valueOf(roleInputId)
        }
    });
    return response.data;
}

function useCurrentOperatorAs(target) {
    if (normalizeRole(state.operatorRole) !== "USER") {
        throw new Error("Current portal session is not a USER account");
    }

    if (target === "requester") {
        state.requesterToken = state.operatorToken;
        state.requesterAccountId = state.operatorAccountId;
    } else {
        state.signerToken = state.operatorToken;
        state.signerAccountId = state.operatorAccountId;
    }

    return {
        mappedTarget: target,
        operatorUsername: state.operatorUsername,
        operatorAccountId: state.operatorAccountId
    };
}

async function uploadDocument() {
    const fileInput = document.getElementById("document-file");
    if (!fileInput.files || fileInput.files.length === 0) {
        throw new Error("Choose a file before uploading");
    }

    const formData = new FormData();
    formData.append("title", valueOf("document-title"));
    formData.append("description", valueOf("document-description"));
    formData.append("file", fileInput.files[0]);

    const response = await apiRequest("POST", "/api/v1/documents", {
        token: requesterTokenValue(),
        formData
    });

    state.documentId = response.data.id ?? "";
    return response.data;
}

async function uploadDocumentVersion() {
    ensureValue(state.documentId, "documentId");

    const fileInput = document.getElementById("document-file");
    if (!fileInput.files || fileInput.files.length === 0) {
        throw new Error("Choose a file before uploading a new version");
    }

    const formData = new FormData();
    formData.append("file", fileInput.files[0]);

    const response = await apiRequest("POST", `/api/v1/documents/${state.documentId}/versions`, {
        token: requesterTokenValue(),
        formData
    });

    return response.data;
}

async function listMyDocuments() {
    const response = await apiRequest("GET", "/api/v1/documents/my", {
        token: requesterTokenValue()
    });
    if (Array.isArray(response.data) && response.data[0]?.id) {
        state.documentId = response.data[0].id;
    }
    return response.data;
}

async function getDocumentDetail() {
    ensureValue(state.documentId, "documentId");
    const response = await apiRequest("GET", `/api/v1/documents/${state.documentId}`, {
        token: requesterTokenValue()
    });
    return response.data;
}

async function downloadDocument() {
    ensureValue(state.documentId, "documentId");
    const baseUrl = (state.baseUrl || detectBaseUrl()).replace(/\/+$/, "");

    const response = await fetch(`${baseUrl}/api/v1/documents/${state.documentId}/download`, {
        method: "GET",
        headers: {
            Authorization: `Bearer ${requesterTokenValue()}`
        }
    });

    if (!response.ok) {
        const contentType = response.headers.get("content-type") || "";
        const payload = contentType.includes("application/json")
            ? await response.json()
            : await response.text();
        throw {
            status: response.status,
            payload
        };
    }

    const blob = await response.blob();
    const url = window.URL.createObjectURL(blob);
    const link = document.createElement("a");
    link.href = url;
    link.download = `document-${state.documentId}`;
    document.body.appendChild(link);
    link.click();
    link.remove();
    window.URL.revokeObjectURL(url);

    return {
        message: "Download triggered in browser",
        documentId: state.documentId,
        size: blob.size
    };
}

async function listAllDocuments() {
    const response = await apiRequest("GET", "/api/v1/admin/documents", {
        token: adminTokenValue()
    });
    return response.data;
}

async function createCertificateRequest() {
    const response = await apiRequest("POST", "/api/v1/certificate-requests", {
        token: signerTokenValue(),
        body: {
            commonName: valueOf("certificate-common-name"),
            organization: valueOf("certificate-organization"),
            country: valueOf("certificate-country"),
            ttl: valueOf("certificate-ttl")
        }
    });

    state.certificateRequestId = response.data.id ?? "";
    return response.data;
}

async function listMyCertificateRequests() {
    const response = await apiRequest("GET", "/api/v1/certificate-requests/my", {
        token: signerTokenValue()
    });

    if (Array.isArray(response.data) && response.data[0]?.id) {
        state.certificateRequestId = response.data[0].id;
    }
    return response.data;
}

async function getCertificateRequest() {
    ensureValue(state.certificateRequestId, "certificateRequestId");
    const response = await apiRequest("GET", `/api/v1/certificate-requests/${state.certificateRequestId}`, {
        token: signerTokenValue()
    });
    return response.data;
}

async function approveCertificateRequest() {
    ensureValue(state.certificateRequestId, "certificateRequestId");
    const response = await apiRequest("POST", `/api/v1/admin/certificate-requests/${state.certificateRequestId}/approve`, {
        token: adminTokenValue()
    });

    state.certificateId = response.data.certificate?.id ?? state.certificateId;
    return response.data;
}

async function rejectCertificateRequest() {
    ensureValue(state.certificateRequestId, "certificateRequestId");
    const response = await apiRequest("POST", `/api/v1/admin/certificate-requests/${state.certificateRequestId}/reject`, {
        token: adminTokenValue(),
        body: {
            reason: valueOf("certificate-reason")
        }
    });
    return response.data;
}

async function listMyCertificates() {
    const response = await apiRequest("GET", "/api/v1/certificates/my", {
        token: signerTokenValue()
    });

    if (Array.isArray(response.data) && response.data[0]?.id) {
        state.certificateId = response.data[0].id;
    }
    return response.data;
}

async function getCertificateDetail() {
    ensureValue(state.certificateId, "certificateId");
    const response = await apiRequest("GET", `/api/v1/certificates/${state.certificateId}`, {
        token: signerTokenValue()
    });
    return response.data;
}

async function createRenewalRequest() {
    ensureValue(state.certificateId, "certificateId");
    const response = await apiRequest("POST", `/api/v1/certificates/${state.certificateId}/renewal-requests`, {
        token: signerTokenValue(),
        body: {
            ttl: valueOf("certificate-renewal-ttl") || null,
            organization: valueOf("certificate-organization") || null,
            country: valueOf("certificate-country") || null,
            reason: valueOf("certificate-reason") || null
        }
    });
    state.certificateRequestId = response.data?.id ?? state.certificateRequestId;
    return response.data;
}

async function revokeMyCertificate() {
    ensureValue(state.certificateId, "certificateId");
    const response = await apiRequest("POST", `/api/v1/certificates/${state.certificateId}/revoke`, {
        token: signerTokenValue()
    });
    return response.data;
}

async function listAllCertificateRequests() {
    const response = await apiRequest("GET", "/api/v1/admin/certificate-requests", {
        token: adminTokenValue()
    });
    if (Array.isArray(response.data) && response.data[0]?.id) {
        state.certificateRequestId = response.data[0].id;
    }
    return response.data;
}

async function listAllCertificates() {
    const response = await apiRequest("GET", "/api/v1/admin/certificates", {
        token: adminTokenValue()
    });
    if (Array.isArray(response.data) && response.data[0]?.id) {
        state.certificateId = response.data[0].id;
    }
    return response.data;
}

async function revokeAdminCertificate() {
    ensureValue(state.certificateId, "certificateId");
    const response = await apiRequest("POST", `/api/v1/admin/certificates/${state.certificateId}/revoke`, {
        token: adminTokenValue()
    });
    return response.data;
}

async function createMfaRegistration() {
    const response = await apiRequest("POST", "/api/v1/step-up/mfa-registrations", {
        token: stepUpActorToken(),
        body: {
            mfaType: valueOf("step-up-mfa-type"),
            issuer: valueOf("step-up-issuer") || "CT556 PKI",
            label: valueOf("step-up-label")
        }
    });

    state.mfaRegistrationId = response.data?.id ?? "";
    return response.data;
}

async function listMfaRegistrations() {
    const response = await apiRequest("GET", "/api/v1/step-up/mfa-registrations/me", {
        token: stepUpActorToken()
    });

    if (Array.isArray(response.data) && response.data[0]?.id) {
        state.mfaRegistrationId = response.data[0].id;
    }
    return response.data;
}

async function disableMfa() {
    ensureValue(state.mfaRegistrationId, "mfaRegistrationId");
    const response = await apiRequest("POST", `/api/v1/step-up/mfa-registrations/${state.mfaRegistrationId}/disable`, {
        token: stepUpActorToken()
    });
    return response.data;
}

async function createStepUpChallenge() {
    const targetId = valueOf("step-up-target-id") || state.signingRequestId || null;
    const response = await apiRequest("POST", "/api/v1/step-up/challenges", {
        token: stepUpActorToken(),
        body: {
            registrationId: state.mfaRegistrationId || null,
            action: valueOf("step-up-action"),
            targetType: valueOf("step-up-target-type") || null,
            targetId
        }
    });

    state.stepUpChallengeId = response.data?.challengeId ?? "";
    if (response.data?.challengeCode) {
        document.getElementById("step-up-otp-code").value = response.data.challengeCode;
    }
    return response.data;
}

async function verifyStepUpChallenge() {
    ensureValue(state.stepUpChallengeId, "stepUpChallengeId");
    const response = await apiRequest("POST", `/api/v1/step-up/challenges/${state.stepUpChallengeId}/verify`, {
        token: stepUpActorToken(),
        body: {
            otpCode: valueOf("step-up-otp-code")
        }
    });

    state.stepUpVerificationToken = response.data?.verificationToken ?? "";
    return response.data;
}

async function createSigningRequest() {
    ensureValue(state.documentId, "documentId");
    ensureValue(state.signerAccountId, "signerAccountId");

    const response = await apiRequest("POST", "/api/v1/signing-requests", {
        token: requesterTokenValue(),
        body: {
            documentId: state.documentId,
            signerAccountId: state.signerAccountId,
            note: valueOf("signing-note")
        }
    });

    state.signingRequestId = response.data?.id ?? "";
    return response.data;
}

async function listRequestedByMe() {
    const response = await apiRequest("GET", "/api/v1/signing-requests/requested-by-me", {
        token: requesterTokenValue()
    });
    if (Array.isArray(response.data) && response.data[0]?.id) {
        state.signingRequestId = response.data[0].id;
    }
    return response.data;
}

async function listPendingForMe() {
    const response = await apiRequest("GET", "/api/v1/signing-requests/pending-for-me", {
        token: signerTokenValue()
    });
    if (Array.isArray(response.data) && response.data[0]?.id) {
        state.signingRequestId = response.data[0].id;
    }
    return response.data;
}

async function approveSigningRequest() {
    ensureValue(state.signingRequestId, "signingRequestId");
    ensureValue(state.certificateId, "certificateId");

    const response = await apiRequest("POST", `/api/v1/signing-requests/${state.signingRequestId}/approve`, {
        token: signerTokenValue(),
        body: {
            certificateId: state.certificateId,
            note: "Approved from internal portal"
        }
    });

    return response.data;
}

async function rejectSigningRequest() {
    ensureValue(state.signingRequestId, "signingRequestId");
    const response = await apiRequest("POST", `/api/v1/signing-requests/${state.signingRequestId}/reject`, {
        token: signerTokenValue(),
        body: {
            note: valueOf("signing-reject-note")
        }
    });
    return response.data;
}

async function cancelSigningRequest() {
    ensureValue(state.signingRequestId, "signingRequestId");
    const response = await apiRequest("POST", `/api/v1/signing-requests/${state.signingRequestId}/cancel`, {
        token: requesterTokenValue() || adminTokenValue()
    });
    return response.data;
}

async function executeSigningRequest() {
    ensureValue(state.signingRequestId, "signingRequestId");
    ensureValue(state.stepUpVerificationToken, "stepUpVerificationToken");

    const response = await apiRequest("POST", `/api/v1/signing-requests/${state.signingRequestId}/execute-signing`, {
        token: signerTokenValue(),
        body: {
            stepUpVerificationToken: state.stepUpVerificationToken
        }
    });
    return response.data;
}

async function getSigningRequest() {
    ensureValue(state.signingRequestId, "signingRequestId");
    const response = await apiRequest("GET", `/api/v1/signing-requests/${state.signingRequestId}`, {
        token: state.requesterToken || state.signerToken || state.operatorToken || state.adminToken
    });
    return response.data;
}

async function listAllSigningRequests() {
    const response = await apiRequest("GET", "/api/v1/admin/signing-requests", {
        token: adminTokenValue()
    });
    if (Array.isArray(response.data) && response.data[0]?.id) {
        state.signingRequestId = response.data[0].id;
    }
    return response.data;
}

async function verifySigningRequest() {
    ensureValue(state.signingRequestId, "signingRequestId");
    const response = await apiRequest("POST", `/api/v1/verifications/signing-requests/${state.signingRequestId}`, {
        body: {
            trustSource: valueOf("verification-trust-source") || "INTERNAL_CA"
        }
    });

    state.verificationReportId = response.data?.id ?? "";
    return response.data;
}

async function getLatestVerification() {
    ensureValue(state.signingRequestId, "signingRequestId");
    const response = await apiRequest("GET", `/api/v1/verifications/signing-requests/${state.signingRequestId}/latest`);
    state.verificationReportId = response.data?.id ?? state.verificationReportId;
    return response.data;
}

async function getRecentVerifications() {
    const response = await apiRequest("GET", "/api/v1/verifications/reports");
    if (Array.isArray(response.data) && response.data[0]?.id) {
        state.verificationReportId = response.data[0].id;
    }
    return response.data;
}

async function getVerificationReport() {
    ensureValue(state.verificationReportId, "verificationReportId");
    const response = await apiRequest("GET", `/api/v1/verifications/reports/${state.verificationReportId}`);
    return response.data;
}

async function createAuditEvent() {
    const response = await apiRequest("POST", "/internal/v1/audit/events", {
        body: {
            eventType: valueOf("audit-event-type"),
            actorId: state.signerAccountId || state.requesterAccountId || state.operatorAccountId || null,
            actorRole: state.signerAccountId ? "SIGNER" : (state.requesterAccountId ? "REQUESTER" : state.operatorRole || null),
            resourceType: valueOf("audit-resource-type"),
            resourceId: state.signingRequestId || null,
            correlationId: valueOf("audit-correlation-id"),
            summary: valueOf("audit-summary"),
            payload: {
                documentId: state.documentId || null,
                certificateId: state.certificateId || null,
                signingRequestId: state.signingRequestId || null,
                verificationReportId: state.verificationReportId || null
            }
        }
    });

    state.auditEventId = response.data?.id ?? "";
    return response.data;
}

async function searchAuditEvents() {
    const params = new URLSearchParams();
    const eventType = valueOf("audit-event-type");
    const resourceType = valueOf("audit-resource-type");
    const correlationId = valueOf("audit-correlation-id");

    if (eventType) params.set("eventType", eventType);
    if (resourceType) params.set("resourceType", resourceType);
    if (state.signingRequestId) params.set("resourceId", state.signingRequestId);
    if (correlationId) params.set("correlationId", correlationId);

    const query = params.toString();
    const response = await apiRequest("GET", `/api/v1/admin/audit/events${query ? `?${query}` : ""}`, {
        token: adminTokenValue()
    });
    if (Array.isArray(response.data) && response.data[0]?.id) {
        state.auditEventId = response.data[0].id;
    }
    return response.data;
}

async function getAuditEvent() {
    ensureValue(state.auditEventId, "auditEventId");
    const response = await apiRequest("GET", `/api/v1/admin/audit/events/${state.auditEventId}`, {
        token: adminTokenValue()
    });
    return response.data;
}

async function apiRequest(method, path, options = {}) {
    const baseUrl = (state.baseUrl || detectBaseUrl()).replace(/\/+$/, "");
    const headers = {};

    if (options.token) {
        headers.Authorization = `Bearer ${options.token}`;
    }

    let body;
    if (options.formData) {
        body = options.formData;
    } else if (options.body !== undefined) {
        headers["Content-Type"] = "application/json";
        body = JSON.stringify(options.body);
    }

    const response = await fetch(`${baseUrl}${path}`, {
        method,
        headers,
        body
    });

    const contentType = response.headers.get("content-type") || "";
    const payload = contentType.includes("application/json")
        ? await response.json()
        : await response.text();

    if (!response.ok) {
        throw {
            status: response.status,
            payload
        };
    }

    return {
        status: response.status,
        data: unwrap(payload)
    };
}

function unwrap(payload) {
    if (payload && typeof payload === "object" && "success" in payload && "data" in payload) {
        return payload.data;
    }
    return payload;
}

function loadState() {
    const saved = localStorage.getItem(STORAGE_KEY);
    const initial = Object.fromEntries(STATE_FIELDS.map((field) => [field, ""]));
    initial.baseUrl = detectBaseUrl();

    if (!saved) {
        return initial;
    }

    try {
        return {
            ...initial,
            ...JSON.parse(saved)
        };
    } catch {
        return initial;
    }
}

function applyState() {
    Object.entries(INPUTS).forEach(([key, input]) => {
        if (input) {
            input.value = state[key] || "";
        }
    });

    authGatewayBadge.textContent = state.baseUrl || detectBaseUrl();
    baseUrlBadge.textContent = state.baseUrl || detectBaseUrl();
    operatorName.textContent = state.operatorUsername || "-";
    operatorRole.textContent = state.operatorRole || "-";
    operatorAccountId.textContent = state.operatorAccountId || "-";
    sessionRoleBadge.textContent = state.operatorRole || "locked";
    sessionSummary.textContent = hasPortalSession()
        ? `${state.operatorUsername || "Unknown user"} da dang nhap. Tu day ban co the vao dashboard va chon module dich vu can dung.`
        : "Hay dang nhap de truy cap phan gioi thieu va dashboard dich vu.";

    authScreen.classList.toggle("hidden", hasPortalSession());
    appShell.classList.toggle("hidden", !hasPortalSession());

    const showingHome = !state.activeModule;
    homeView.classList.toggle("view-active", showingHome);
    homeView.classList.toggle("hidden", !showingHome);
    moduleView.classList.toggle("view-active", !showingHome);
    moduleView.classList.toggle("hidden", showingHome);

    moduleSections.forEach((section) => section.classList.remove("active"));
    if (!showingHome && MODULE_META[state.activeModule]) {
        moduleTitle.textContent = MODULE_META[state.activeModule].title;
        moduleDescription.textContent = MODULE_META[state.activeModule].description;
        const section = document.getElementById(`module-${state.activeModule}`);
        section?.classList.add("active");
        section?.classList.remove("hidden");
    }

    moduleSections.forEach((section) => {
        if (section.id !== `module-${state.activeModule}`) {
            section.classList.add("hidden");
        }
    });

    applyRoleScopedVisibility();
}

function readStateFromInputs() {
    Object.entries(INPUTS).forEach(([key, input]) => {
        if (input) {
            state[key] = (input.value || "").trim();
        }
    });
}

function applyRoleScopedVisibility() {
    const role = normalizeRole(state.operatorRole);

    roleScopedElements.forEach((element) => {
        const scopes = String(element.getAttribute("data-role-scope") || "")
            .split(",")
            .map((entry) => entry.trim().toUpperCase())
            .filter(Boolean);

        if (scopes.length === 0) {
            element.classList.remove("hidden");
            return;
        }

        const allowed = scopes.includes(role);
        element.classList.toggle("hidden", !allowed);
    });
}

function persistState() {
    localStorage.setItem(STORAGE_KEY, JSON.stringify(state));
}

function detectBaseUrl() {
    return window.location.origin || "http://localhost:8080";
}

function hasPortalSession() {
    return Boolean(state.operatorToken);
}

function valueOf(id) {
    return (document.getElementById(id)?.value || "").trim();
}

function adminTokenValue() {
    return state.adminToken || (normalizeRole(state.operatorRole) === "ADMIN" ? state.operatorToken : "");
}

function requesterTokenValue() {
    return state.requesterToken || (normalizeRole(state.operatorRole) === "USER" ? state.operatorToken : "");
}

function signerTokenValue() {
    return state.signerToken || (normalizeRole(state.operatorRole) === "USER" ? state.operatorToken : "");
}

function stepUpActorToken() {
    const actor = valueOf("step-up-actor") || "signer";
    return actor === "requester" ? requesterTokenValue() : signerTokenValue();
}

function ensureValue(value, label) {
    if (!value) {
        throw new Error(`${label} is required`);
    }
}

function setConsole(title, kind, payload) {
    consoleTitle.textContent = title;
    consoleStatus.textContent = kind === "success"
        ? "Request completed"
        : kind === "error"
            ? "Request failed"
            : "Ready";
    consoleStatus.className = `console-status ${kind}`;
    consoleOutput.textContent = typeof payload === "string"
        ? payload
        : JSON.stringify(payload, null, 2);
}

function showAuthFeedback(message, kind) {
    authFeedback.textContent = message;
    authFeedback.className = `auth-feedback ${kind}`;
    authFeedback.classList.remove("hidden");
}

function hideAuthFeedback() {
    authFeedback.textContent = "";
    authFeedback.className = "auth-feedback hidden";
}

function formatError(error) {
    if (error?.payload) {
        return {
            status: error.status,
            error: error.payload
        };
    }
    if (error instanceof Error) {
        return {
            message: error.message
        };
    }
    return error;
}

function extractErrorMessage(payload) {
    if (!payload) {
        return "Unknown error";
    }
    if (typeof payload.message === "string") {
        return payload.message;
    }
    if (payload.error?.message) {
        return payload.error.message;
    }
    if (typeof payload.error === "string") {
        return payload.error;
    }
    return JSON.stringify(payload);
}

function decodeJwtPayload(token) {
    const parts = token.split(".");
    if (parts.length < 2) {
        return {};
    }

    const base64 = parts[1]
        .replace(/-/g, "+")
        .replace(/_/g, "/")
        .padEnd(Math.ceil(parts[1].length / 4) * 4, "=");

    try {
        return JSON.parse(window.atob(base64));
    } catch {
        return {};
    }
}

function normalizeRole(roleClaim) {
    if (Array.isArray(roleClaim)) {
        return normalizeRole(roleClaim[0] || "");
    }

    const raw = String(roleClaim || "").trim();
    if (!raw) {
        return "";
    }

    return raw.startsWith("ROLE_") ? raw.slice(5).toUpperCase() : raw.toUpperCase();
}
