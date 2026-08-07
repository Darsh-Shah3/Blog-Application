# Run unit tests for every Spring service. Exit non-zero if any suite fails.
# Use before pushing to GitHub when you do not have cloud CI.

$ErrorActionPreference = "Stop"
$Root = Split-Path -Parent $PSScriptRoot
if (-not $Root) { $Root = (Resolve-Path (Join-Path $PSScriptRoot "..")).Path }

$Services = @(
  "user-auth-service",
  "community-service",
  "post-service",
  "comment-service",
  "vote-service",
  "media-service",
  "api-gateway"
)

Write-Host "=== Threadly unit tests (local pre-commit gate) ==="

$mvn = Get-Command mvn -ErrorAction SilentlyContinue
if (-not $mvn) {
  Write-Host "ERROR: Maven (mvn) not found on PATH."
  Write-Host "Install Maven 3.9+ and JDK 17, then re-run."
  exit 1
}

$failed = 0
foreach ($svc in $Services) {
  $dir = Join-Path $Root "services\$svc"
  $pom = Join-Path $dir "pom.xml"
  if (-not (Test-Path $pom)) {
    Write-Host "SKIP $svc (no pom.xml)"
    continue
  }
  Write-Host ""
  Write-Host ">>> Testing $svc"
  Push-Location $dir
  try {
    & mvn -q -DskipITs test
    if ($LASTEXITCODE -ne 0) {
      Write-Host "FAIL $svc"
      $failed = 1
    } else {
      Write-Host "OK   $svc"
    }
  } catch {
    Write-Host "FAIL $svc : $_"
    $failed = 1
  } finally {
    Pop-Location
  }
}

Write-Host ""
if ($failed -ne 0) {
  Write-Host "=== TESTS FAILED — do not commit/push until green ==="
  exit 1
}
Write-Host "=== ALL TESTS PASSED — safe to commit ==="
exit 0
