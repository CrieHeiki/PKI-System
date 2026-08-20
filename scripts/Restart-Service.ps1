param(
    [Parameter(Mandatory = $true)]
    [string[]]$Services,
    [switch]$WithInfra
)

$ErrorActionPreference = "Stop"

& (Join-Path $PSScriptRoot "Stop-Dev.ps1") -Services $Services
Start-Sleep -Seconds 2
& (Join-Path $PSScriptRoot "Start-Dev.ps1") -Services $Services -WithInfra:$WithInfra
