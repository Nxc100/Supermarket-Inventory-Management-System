param(
  [string]$JavaHome = $env:JAVA_HOME,
  [switch]$TestDatabase
)
$ErrorActionPreference = 'Stop'
if (-not $JavaHome -or -not (Test-Path -LiteralPath (Join-Path $JavaHome 'bin/java.exe'))) {
  throw 'Set JAVA_HOME to a JDK 17 directory or pass -JavaHome.'
}
$env:JAVA_HOME = $JavaHome
$backendPath = Join-Path $PSScriptRoot '../backend'
Push-Location $backendPath
try {
  if ($TestDatabase) {
    if (-not $env:DB_PASSWORD) { throw 'Set DB_PASSWORD for the isolated test database.' }
    if (-not $env:JWT_SECRET) {
      $secretBytes = New-Object byte[] 48
      $generator = [Security.Cryptography.RandomNumberGenerator]::Create()
      try { $generator.GetBytes($secretBytes) } finally { $generator.Dispose() }
      $env:JWT_SECRET = [Convert]::ToBase64String($secretBytes)
    }
    $env:DB_URL = 'jdbc:mysql://127.0.0.1:3306/supermarket_inventory_test?connectionTimeZone=%2B08:00&forceConnectionTimeZoneToSession=true&characterEncoding=UTF-8'
  }
  & ./mvnw.cmd spring-boot:run
  if ($LASTEXITCODE -ne 0) { throw 'Backend exited with a failure.' }
} finally { Pop-Location }
