function base64UrlToBuffer(value: string): ArrayBuffer {
  const base64 = value.replace(/-/g, "+").replace(/_/g, "/");
  const padded = base64.padEnd(Math.ceil(base64.length / 4) * 4, "=");
  const binary = window.atob(padded);
  const bytes = new Uint8Array(binary.length);
  for (let i = 0; i < binary.length; i += 1) {
    bytes[i] = binary.charCodeAt(i);
  }
  return bytes.buffer;
}

function parseOptionsJson(optionsJson: string) {
  const parsed = JSON.parse(optionsJson);
  if (parsed && typeof parsed === "object" && "publicKey" in parsed) {
    return (parsed as { publicKey: unknown }).publicKey;
  }
  return parsed;
}

function bufferToBase64Url(buffer: ArrayBuffer): string {
  const bytes = new Uint8Array(buffer);
  let binary = "";
  bytes.forEach((byte) => {
    binary += String.fromCharCode(byte);
  });
  return window.btoa(binary).replace(/\+/g, "-").replace(/\//g, "_").replace(/=+$/g, "");
}

function buildCreationOptionsFromJson(optionsJson: string): PublicKeyCredentialCreationOptions {
  const json = parseOptionsJson(optionsJson) as Record<string, any>;
  return {
    ...json,
    challenge: base64UrlToBuffer(json.challenge),
    user: {
      ...json.user,
      id: base64UrlToBuffer(json.user.id),
    },
    excludeCredentials: json.excludeCredentials?.map((item: Record<string, unknown>) => ({
      ...item,
      id: base64UrlToBuffer(String(item.id)),
    })),
  } as PublicKeyCredentialCreationOptions;
}

function buildRequestOptionsFromJson(optionsJson: string): PublicKeyCredentialRequestOptions {
  const json = parseOptionsJson(optionsJson) as Record<string, any>;
  return {
    ...json,
    challenge: base64UrlToBuffer(json.challenge),
    allowCredentials: json.allowCredentials?.map((item: Record<string, unknown>) => ({
      ...item,
      id: base64UrlToBuffer(String(item.id)),
    })),
  } as PublicKeyCredentialRequestOptions;
}

function serializeCredential(credential: PublicKeyCredential): string {
  const anyCredential = credential as PublicKeyCredential & { toJSON?: () => unknown };
  if (typeof anyCredential.toJSON === "function") {
    return JSON.stringify(anyCredential.toJSON());
  }

  const base = {
    id: credential.id,
    rawId: bufferToBase64Url(credential.rawId),
    type: credential.type,
    authenticatorAttachment: credential.authenticatorAttachment,
    clientExtensionResults: credential.getClientExtensionResults(),
  };

  if (credential.response instanceof AuthenticatorAttestationResponse) {
    return JSON.stringify({
      ...base,
      response: {
        clientDataJSON: bufferToBase64Url(credential.response.clientDataJSON),
        attestationObject: bufferToBase64Url(credential.response.attestationObject),
        transports: typeof credential.response.getTransports === "function" ? credential.response.getTransports() : undefined,
      },
    });
  }

  if (credential.response instanceof AuthenticatorAssertionResponse) {
    return JSON.stringify({
      ...base,
      response: {
        clientDataJSON: bufferToBase64Url(credential.response.clientDataJSON),
        authenticatorData: bufferToBase64Url(credential.response.authenticatorData),
        signature: bufferToBase64Url(credential.response.signature),
        userHandle: credential.response.userHandle ? bufferToBase64Url(credential.response.userHandle) : null,
      },
    });
  }

  throw new Error("Trinh duyet khong tra ve kieu credential WebAuthn hop le.");
}

export function isWebAuthnSupported() {
  return typeof window !== "undefined" && typeof window.PublicKeyCredential !== "undefined" && !!navigator.credentials;
}

export async function createWebAuthnCredential(optionsJson: string): Promise<string> {
  if (!isWebAuthnSupported()) {
    throw new Error("Trinh duyet hien tai khong ho tro WebAuthn.");
  }

  const parser = (PublicKeyCredential as unknown as {
    parseCreationOptionsFromJSON?: (value: unknown) => PublicKeyCredentialCreationOptions;
  }).parseCreationOptionsFromJSON;

  const normalizedJson = parseOptionsJson(optionsJson);
  const publicKey = parser ? parser(normalizedJson) : buildCreationOptionsFromJson(JSON.stringify(normalizedJson));
  const credential = await navigator.credentials.create({ publicKey });
  if (!(credential instanceof PublicKeyCredential)) {
    throw new Error("Khong tao duoc WebAuthn credential tu thiet bi.");
  }

  return serializeCredential(credential);
}

export async function getWebAuthnAssertion(optionsJson: string): Promise<string> {
  if (!isWebAuthnSupported()) {
    throw new Error("Trinh duyet hien tai khong ho tro WebAuthn.");
  }

  const parser = (PublicKeyCredential as unknown as {
    parseRequestOptionsFromJSON?: (value: unknown) => PublicKeyCredentialRequestOptions;
  }).parseRequestOptionsFromJSON;

  const normalizedJson = parseOptionsJson(optionsJson);
  const publicKey = parser ? parser(normalizedJson) : buildRequestOptionsFromJson(JSON.stringify(normalizedJson));
  const credential = await navigator.credentials.get({ publicKey });
  if (!(credential instanceof PublicKeyCredential)) {
    throw new Error("Khong lay duoc WebAuthn assertion tu thiet bi.");
  }

  return serializeCredential(credential);
}
