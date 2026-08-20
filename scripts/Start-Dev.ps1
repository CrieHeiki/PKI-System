param(
    [string[]]$Services = @(),
    [switch]$WithInfra,
    [int]$InfraWarmupSeconds = 8,
    [int]$StartupDelaySeconds = 3
)

$ErrorActionPreference = "Stop"

$repoRoot = Split-Path -Parent $PSScriptRoot
$runtimeDir = Join-Path $repoRoot ".runtime"
$pidDir = Join-Path $runtimeDir "pids"
$logDir = Join-Path $runtimeDir "logs"
$mavenOpts = '-Xms64m -Xmx256m -XX:MaxMetaspaceSize=192m -Duser.timezone=UTC'
$springBootJvmArgs = '-Xms128m -Xmx384m -XX:MaxMetaspaceSize=256m -Duser.timezone=UTC'
$javaToolOptions = '-Duser.timezone=UTC'

$serviceMap = [ordered]@{
    "audit-service" = @{ Path = "services/audit-service"; Port = 8088 }
    "account-service" = @{ Path = "services/account-service"; Port = 8081 }
    "document-service" = @{ Path = "services/document-service"; Port = 8082 }
    "step-up-auth-service" = @{ Path = "services/step-up-auth-service"; Port = 8085 }
    "certification-service" = @{ Path = "services/certification-service"; Port = 8083 }
    "signing-workflow-service" = @{ Path = "services/signing-workflow-service"; Port = 8084 }
    "verification-service" = @{ Path = "services/verification-service"; Port = 8087 }
    "api-gateway" = @{ Path = "services/api-gateway"; Port = 8090 }
}

function Ensure-Directory([string]$Path) {
    if (-not (Test-Path $Path)) {
        New-Item -ItemType Directory -Path $Path | Out-Null
    }
}

function Get-SelectedServices {
    param([string[]]$Requested)

    if (-not $Requested -or $Requested.Count -eq 0) {
        return [string[]]$serviceMap.Keys
    }

    $resolved = @()
    foreach ($name in $Requested) {
        if (-not $serviceMap.Contains($name)) {
            throw "Unknown service: $name"
        }
        $resolved += $name
    }
    return $resolved
}

function Get-PidFile([string]$ServiceName) {
    return (Join-Path $pidDir "$ServiceName.pid")
}

function Get-ServiceProcess([string]$ServiceName) {
    $pidFile = Get-PidFile $ServiceName
    if (-not (Test-Path $pidFile)) {
        return $null
    }

    $rawPid = Get-Content $pidFile -ErrorAction SilentlyContinue
    if (-not $rawPid) {
        Remove-Item $pidFile -ErrorAction SilentlyContinue
        return $null
    }

    $process = Get-Process -Id ([int]$rawPid) -ErrorAction SilentlyContinue
    if (-not $process) {
        Remove-Item $pidFile -ErrorAction SilentlyContinue
        return $null
    }

    return $process
}

Ensure-Directory $runtimeDir
Ensure-Directory $pidDir
Ensure-Directory $logDir

if ($WithInfra) {
    Write-Host "Starting infrastructure containers..." -ForegroundColor Cyan
    Push-Location (Join-Path $repoRoot "infra")
    try {
        docker compose up -d postgres redis minio minio-init vault immudb nginx
    } finally {
        Pop-Location
    }

    if ($InfraWarmupSeconds -gt 0) {
        Write-Host "Waiting $InfraWarmupSeconds seconds for infrastructure warm-up..." -ForegroundColor DarkCyan
        Start-Sleep -Seconds $InfraWarmupSeconds
    }
}

$selectedServices = Get-SelectedServices $Services

foreach ($serviceName in $selectedServices) {
    $existing = Get-ServiceProcess $serviceName
    if ($existing) {
        Write-Host "Skipping $serviceName (already running, PID $($existing.Id))" -ForegroundColor Yellow
        continue
    }

    $definition = $serviceMap[$serviceName]
    $servicePath = Join-Path $repoRoot $definition.Path
    $stdoutLog = Join-Path $logDir "$serviceName.out.log"
    $stderrLog = Join-Path $logDir "$serviceName.err.log"

    Remove-Item $stdoutLog, $stderrLog -ErrorAction SilentlyContinue

    Write-Host "Starting $serviceName..." -ForegroundColor Green
    $command = 'set "JAVA_TOOL_OPTIONS={0}" && set "MAVEN_OPTS={1}" && .\mvnw.cmd -q -Dspring-boot.run.jvmArguments="{2}" spring-boot:run' -f $javaToolOptions, $mavenOpts, $springBootJvmArgs
    $process = Start-Process `
        -FilePath "cmd.exe" `
        -ArgumentList "/c", $command `
        -WorkingDirectory $servicePath `
        -WindowStyle Minimized `
        -RedirectStandardOutput $stdoutLog `
        -RedirectStandardError $stderrLog `
        -PassThru

    Set-Content -Path (Get-PidFile $serviceName) -Value $process.Id
    Write-Host ("  PID {0}, port {1}, logs: {2}" -f $process.Id, $definition.Port, $stdoutLog)

    if ($StartupDelaySeconds -gt 0) {
        Start-Sleep -Seconds $StartupDelaySeconds
    }
}

Write-Host ""
Write-Host "Use scripts\\Status-Dev.ps1 to check service health." -ForegroundColor Cyan
