param(
    [string[]]$Services = @()
)

$ErrorActionPreference = "Stop"

$repoRoot = Split-Path -Parent $PSScriptRoot
$runtimeDir = Join-Path $repoRoot ".runtime"
$pidDir = Join-Path $runtimeDir "pids"

$serviceMap = [ordered]@{
    "audit-service" = @{ Port = 8088 }
    "account-service" = @{ Port = 8081 }
    "document-service" = @{ Port = 8082 }
    "step-up-auth-service" = @{ Port = 8085 }
    "certification-service" = @{ Port = 8083 }
    "signing-workflow-service" = @{ Port = 8084 }
    "verification-service" = @{ Port = 8087 }
    "api-gateway" = @{ Port = 8090 }
}

function Get-SelectedServices {
    param([string[]]$Requested)
    if (-not $Requested -or $Requested.Count -eq 0) {
        return [string[]]$serviceMap.Keys
    }

    foreach ($name in $Requested) {
        if (-not $serviceMap.Contains($name)) {
            throw "Unknown service: $name"
        }
    }
    return $Requested
}

function Test-ServiceHealth([int]$Port) {
    try {
        $response = Invoke-RestMethod -Uri "http://localhost:$Port/actuator/health" -TimeoutSec 3
        return $response.status
    } catch {
        return "DOWN"
    }
}

foreach ($serviceName in (Get-SelectedServices $Services)) {
    $pidFile = Join-Path $pidDir "$serviceName.pid"
    $processId = $null
    $processStatus = "STOPPED"

    if (Test-Path $pidFile) {
        $rawPid = Get-Content $pidFile -ErrorAction SilentlyContinue
        if ($rawPid) {
            $processId = [int]$rawPid
            if (Get-Process -Id $processId -ErrorAction SilentlyContinue) {
                $processStatus = "RUNNING"
            } else {
                $processStatus = "STALE_PID"
            }
        }
    }

    $port = $serviceMap[$serviceName].Port
    $health = Test-ServiceHealth $port
    $pidDisplay = if ($null -eq $processId) { "-" } else { $processId }

    "{0,-28} pid={1,-8} process={2,-10} port={3,-5} health={4}" -f $serviceName, $pidDisplay, $processStatus, $port, $health
}
