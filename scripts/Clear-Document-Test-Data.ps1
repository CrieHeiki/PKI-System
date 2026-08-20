param(
    [string]$PostgresContainer = "pki-postgres",
    [string]$PostgresUser = "postgres"
)

$ErrorActionPreference = "Stop"

function Invoke-PostgresSql {
    param(
        [string]$Database,
        [string]$Sql
    )

    Write-Host "Cleaning $Database..." -ForegroundColor Cyan
    docker exec $PostgresContainer psql -U $PostgresUser -d $Database -v ON_ERROR_STOP=1 -c $Sql
}

$documentSql = @"
TRUNCATE TABLE
    document_versions,
    documents
CASCADE;
"@

$signingSql = @"
TRUNCATE TABLE
    signing_artifacts,
    signing_events,
    signing_requests
CASCADE;
"@

$verificationSql = @"
TRUNCATE TABLE
    verification_report_details,
    verification_reports
CASCADE;
"@

Invoke-PostgresSql -Database "verification_db" -Sql $verificationSql
Invoke-PostgresSql -Database "signing_db" -Sql $signingSql
Invoke-PostgresSql -Database "document_db" -Sql $documentSql

Write-Host "Document, signing, and verification test data cleared. account_db and certification_db were not changed." -ForegroundColor Green
