param(
    [string[]]$Services = @(),
    [switch]$WithInfra
)

$ErrorActionPreference = "Stop"

$repoRoot = Split-Path -Parent $PSScriptRoot
$runtimeDir = Join-Path $repoRoot ".runtime"
$pidDir = Join-Path $runtimeDir "pids"

$serviceNames = @(
    "audit-service",
    "account-service",
    "document-service",
    "step-up-auth-service",
    "certification-service",
    "signing-workflow-service",
    "verification-service",
    "api-gateway"
)

function Get-SelectedServices {
    param([string[]]$Requested)

    if (-not $Requested -or $Requested.Count -eq 0) {
        return $serviceNames
    }

    foreach ($name in $Requested) {
        if ($serviceNames -notcontains $name) {
            throw "Unknown service: $name"
        }
    }

    return $Requested
}

foreach ($serviceName in (Get-SelectedServices $Services)) {
    $pidFile = Join-Path $pidDir "$serviceName.pid"
    if (-not (Test-Path $pidFile)) {
        Write-Host "Skipping $serviceName (no PID file)" -ForegroundColor Yellow
        continue
    }

    $rawPid = Get-Content $pidFile -ErrorAction SilentlyContinue
    if (-not $rawPid) {
        Remove-Item $pidFile -ErrorAction SilentlyContinue
        Write-Host "Removed stale PID file for $serviceName" -ForegroundColor Yellow
        continue
    }

    $processId = [int]$rawPid
    $process = Get-Process -Id $processId -ErrorAction SilentlyContinue
    if (-not $process) {
        Remove-Item $pidFile -ErrorAction SilentlyContinue
        Write-Host "Removed stale PID file for $serviceName" -ForegroundColor Yellow
        continue
    }

    Write-Host "Stopping $serviceName (PID $processId)..." -ForegroundColor Green
    & taskkill /PID $processId /T /F | Out-Null
    Remove-Item $pidFile -ErrorAction SilentlyContinue
}

if ($WithInfra) {
    Write-Host "Stopping infrastructure containers..." -ForegroundColor Cyan
    Push-Location (Join-Path $repoRoot "infra")
    try {
        docker compose stop nginx immudb vault minio minio-init redis postgres
    } finally {
        Pop-Location
    }
}
