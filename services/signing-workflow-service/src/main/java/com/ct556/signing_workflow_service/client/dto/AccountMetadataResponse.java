package com.ct556.signing_workflow_service.client.dto;

import java.util.UUID;

public class AccountMetadataResponse {
    private UUID id;
    private String username;
    private String email;
    private String role;
    private String status;
    private String signatureImageBase64;
    private String signatureImageContentType;
    private boolean signatureImageOnly;
    private int signatureBoxXPercent;
    private int signatureBoxYPercent;
    private int signatureBoxWidthPercent;
    private int signatureBoxHeightPercent;

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getRole() {
        return role;
    }

    public void setRole(String role) {
        this.role = role;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getSignatureImageBase64() {
        return signatureImageBase64;
    }

    public void setSignatureImageBase64(String signatureImageBase64) {
        this.signatureImageBase64 = signatureImageBase64;
    }

    public String getSignatureImageContentType() {
        return signatureImageContentType;
    }

    public void setSignatureImageContentType(String signatureImageContentType) {
        this.signatureImageContentType = signatureImageContentType;
    }

    public boolean isSignatureImageOnly() {
        return signatureImageOnly;
    }

    public void setSignatureImageOnly(boolean signatureImageOnly) {
        this.signatureImageOnly = signatureImageOnly;
    }

    public int getSignatureBoxXPercent() {
        return signatureBoxXPercent;
    }

    public void setSignatureBoxXPercent(int signatureBoxXPercent) {
        this.signatureBoxXPercent = signatureBoxXPercent;
    }

    public int getSignatureBoxYPercent() {
        return signatureBoxYPercent;
    }

    public void setSignatureBoxYPercent(int signatureBoxYPercent) {
        this.signatureBoxYPercent = signatureBoxYPercent;
    }

    public int getSignatureBoxWidthPercent() {
        return signatureBoxWidthPercent;
    }

    public void setSignatureBoxWidthPercent(int signatureBoxWidthPercent) {
        this.signatureBoxWidthPercent = signatureBoxWidthPercent;
    }

    public int getSignatureBoxHeightPercent() {
        return signatureBoxHeightPercent;
    }

    public void setSignatureBoxHeightPercent(int signatureBoxHeightPercent) {
        this.signatureBoxHeightPercent = signatureBoxHeightPercent;
    }
}
