#Requires -Version 5.1
<#
.SYNOPSIS
  smoke-test.ps1 - automated regression for docs/testing/manual-verification.md

.DESCRIPTION
  One-shot: start Docker infra (Redis + RabbitMQ) -> build bootJars -> start the 4
  services in background -> run J-1/J-2/J-3 + negative cases -> print PASS/FAIL ->
  cleanup (stop services + docker down).

  Maps to journeys.md J-1/J-2/J-3. Test data is ASCII-only to avoid PowerShell 5.1
  UTF-8 (no-BOM) encoding issues.

.PARAMETER JavaHome
  JDK 21 home (default ~/.jdks/corretto-21.0.5).

.EXAMPLE
  powershell -ExecutionPolicy Bypass -File scripts/smoke-test.ps1
#>

param(
    [string]$JavaHome = "$env:USERPROFILE\.jdks\corretto-21.0.5"
)

$ErrorActionPreference = "Continue"   # native stderr (docker/gradle) 不應中止腳本
$script:Failed = 0

$env:JAVA_HOME = $JavaHome   # gradlew build needs JAVA_HOME

$Java    = Join-Path $JavaHome "bin\java.exe"
$Root    = Split-Path $PSScriptRoot -Parent
$App     = Join-Path $Root "app"
$Base    = "http://localhost:8080/api/v1"
$Tmp     = Join-Path ([System.IO.Path]::GetTempPath()) "lucky-draw-smoke"
$Compose = Join-Path $Root "docker\docker-compose.yml"
New-Item -ItemType Directory -Force -Path $Tmp | Out-Null

$Services = @(
    @{ Name = "auth";      Module = "auth-service";      Port = 8081 },
    @{ Name = "campaign";  Module = "campaign-service";  Port = 8082 },
    @{ Name = "inventory"; Module = "inventory-service"; Port = 8083 },
    @{ Name = "gateway";   Module = "api-gateway";       Port = 8080 }
)
$Pids = @()

# ---------------------------------------------------------------- helpers

function Write-JsonFile([string]$name, $obj) {
    $obj | ConvertTo-Json -Compress | Set-Content -Path (Join-Path $Tmp $name) -Encoding ascii -NoNewline
}

function Invoke-Post([string]$path, [string]$jsonFile, [hashtable]$headers = @{}, [string]$method = "POST") {
    $curlArgs = @("-s", "-X", $method, "$Base$path", "-H", "Content-Type: application/json")
    foreach ($k in $headers.Keys) { $curlArgs += @("-H", "$($k): $($headers[$k])") }
    $curlArgs += @("--data-binary", "@$(Join-Path $Tmp $jsonFile)")
    return (& curl.exe @curlArgs 2>$null)
}

function Invoke-Get([string]$path, [hashtable]$headers = @{}) {
    $curlArgs = @("-s", "$Base$path")
    foreach ($k in $headers.Keys) { $curlArgs += @("-H", "$($k): $($headers[$k])") }
    return (& curl.exe @curlArgs 2>$null)
}

function Assert-Code([string]$body, [string]$expectedCode, [string]$label) {
    # extract code via regex (avoid ConvertFrom-Json which breaks on non-ASCII messages under PS 5.1)
    if ($body -match '"code"\s*:\s*"([A-Za-z0-9]+)"') {
        $code = $Matches[1]
        if ($code -eq $expectedCode) {
            Write-Host "  [PASS] $label" -ForegroundColor Green
        } else {
            Write-Host "  [FAIL] $label (expect $expectedCode, got $code)" -ForegroundColor Red
            $script:Failed++
        }
    } else {
        Write-Host "  [FAIL] $label (no code field in response)" -ForegroundColor Red
        $script:Failed++
    }
}

function Assert-True([bool]$cond, [string]$label) {
    if ($cond) { Write-Host "  [PASS] $label" -ForegroundColor Green }
    else { Write-Host "  [FAIL] $label" -ForegroundColor Red; $script:Failed++ }
}

function Get-RedisStock([string]$key) {
    $v = docker exec lucky-draw-redis redis-cli GET $key 2>$null
    $n = 0
    if ($v -and $v -match '^-?\d+$') { $n = [int]$v }
    return $n
}

function Wait-Healthy([string]$url, [int]$timeoutSec) {
    $deadline = (Get-Date).AddSeconds($timeoutSec)
    while ((Get-Date) -lt $deadline) {
        $r = curl.exe -s -m 2 "$url" 2>$null
        if ($r -like '*UP*') { return $true }
        Start-Sleep -Seconds 2
    }
    return $false
}

function Stop-Services {
    foreach ($p in $Pids) { Stop-Process -Id $p -Force -ErrorAction SilentlyContinue }
    docker compose -f $Compose down 2>&1 | Out-Null
}

# ---------------------------------------------------------------- main

Write-Host "== [1/5] start infra (Redis + RabbitMQ) =="
docker compose -f $Compose up -d redis rabbitmq 2>&1 | Out-Null

Write-Host "== [2/5] build bootJars =="
Push-Location $App
& .\gradlew.bat auth-service:bootJar campaign-service:bootJar inventory-service:bootJar api-gateway:bootJar --console=plain 2>&1 | Out-Null
if ($LASTEXITCODE -ne 0) { Pop-Location; throw "bootJar build failed" }
Pop-Location

Write-Host "== [3/5] start services (background) =="
foreach ($s in $Services) {
    $jar = Get-ChildItem (Join-Path $App "$($s.Module)\build\libs") -Filter "*.jar" |
           Where-Object { $_.Name -notmatch 'plain' } | Select-Object -First 1
    if (-not $jar) { throw "bootJar not found for $($s.Module)" }
    $log = Join-Path $Tmp "$($s.Name).log"
    $p = Start-Process -FilePath $Java -ArgumentList "-jar", $jar.FullName, "--logging.file.name=$log" -WindowStyle Hidden -PassThru
    $Pids += $p.Id
    Write-Host "  $($s.Name) PID=$($p.Id)"
}

Write-Host "== [4/5] wait for health =="
foreach ($s in $Services) {
    $ok = Wait-Healthy "http://localhost:$($s.Port)/actuator/health" 90
    Assert-True $ok "$($s.Name) healthy"
}

try {
    Write-Host "== [5/5] E2E scenarios =="

    # ---- J-2 USER ----
    Write-Host "-- J-2 register/login/browse/draw --"
    Write-JsonFile "reg.json" @{ username = "alice"; email = "alice@example.com"; password = "S3cure!Pass" }
    $r = Invoke-Post "/auth/register" "reg.json"
    Assert-Code $r "00000" "register"

    Write-JsonFile "login.json" @{ username = "alice"; password = "S3cure!Pass" }
    $r = Invoke-Post "/auth/login" "login.json"
    Assert-Code $r "00000" "login"
    $userToken = ($r | ConvertFrom-Json).data.accessToken

    $r = Invoke-Get "/campaigns"
    Assert-Code $r "00000" "list campaigns"

    # ---- J-3 replay ----
    Write-Host "-- J-3 replay --"
    Write-JsonFile "draw.json" @{ count = 1 }
    $key = "aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa"
    $draw1 = Invoke-Post "/campaigns/1/draw" "draw.json" @{ Authorization = "Bearer $userToken"; "Idempotency-Key" = $key }
    $draw2 = Invoke-Post "/campaigns/1/draw" "draw.json" @{ Authorization = "Bearer $userToken"; "Idempotency-Key" = $key }
    Assert-Code $draw1 "00000" "draw"
    Assert-True ($draw1 -eq $draw2) "replay byte-identical"

    # 抽獎後庫存扣減（批次抽獎 → Redis 預扣遞減；DB 真相扣減由 integration 測試驗證）
    # seed 活動庫存真相 = 1 + 10 + 100 = 111；抽獎結果隨機，故「有中獎才斷言 < 111、全銘謝惠顧則 skip」
    Write-JsonFile "batch.json" @{ count = 9 }
    $batch = Invoke-Post "/campaigns/1/draw" "batch.json" @{ Authorization = "Bearer $userToken"; "Idempotency-Key" = "dddddddd-dddd-dddd-dddd-dddddddddddd" }
    Assert-Code $batch "00000" "batch draw"
    $after = (Get-RedisStock "stock:1") + (Get-RedisStock "stock:2") + (Get-RedisStock "stock:3")
    if ($batch -match '"resultType"\s*:\s*"WIN"') {
        Assert-True ($after -lt 111) "stock deducted after draw (Redis pre-deduct, seed sum 111)"
    } else {
        Write-Host "  [SKIP] batch all THANK_YOU (no WIN), deduction assert skipped" -ForegroundColor Yellow
    }

    # ---- J-1 ADMIN ----
    Write-Host "-- J-1 admin create/configure/activate --"
    Write-JsonFile "adminlogin.json" @{ username = "admin"; password = "admin123" }
    $r = Invoke-Post "/auth/login" "adminlogin.json"
    Assert-Code $r "00000" "admin login"
    $adminToken = ($r | ConvertFrom-Json).data.accessToken

    Write-JsonFile "campaign.json" @{ name = "smoke"; startTime = "2026-09-01T00:00:00Z"; endTime = "2026-10-01T00:00:00Z"; drawLimit = 5 }
    $r = Invoke-Post "/campaigns" "campaign.json" @{ Authorization = "Bearer $adminToken" }
    Assert-Code $r "00000" "create campaign"
    $campaignId = ($r | ConvertFrom-Json).data.id

    Write-JsonFile "prizes.json" @{ prizes = @(
        @{ name = "iPhone"; type = "PRIZE"; probability = 5; quantity = 1 },
        @{ name = "Thanks"; type = "THANK_YOU"; probability = 95; quantity = 0 }
    ) }
    $r = Invoke-Post "/campaigns/$campaignId/prizes" "prizes.json" @{ Authorization = "Bearer $adminToken" } "PUT"
    Assert-Code $r "00000" "configure prizes"

    Write-JsonFile "status.json" @{ status = "ACTIVE" }
    $r = Invoke-Post "/campaigns/$campaignId/status" "status.json" @{ Authorization = "Bearer $adminToken" } "PATCH"
    Assert-Code $r "00000" "activate campaign"

    # ---- negative ----
    Write-Host "-- negative cases --"
    Write-JsonFile "draw2.json" @{ count = 1 }
    $r = Invoke-Post "/campaigns/1/draw" "draw2.json" @{ "Idempotency-Key" = "bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb" }
    Assert-Code $r "A0203" "no-token draw -> 401/A0203"

    $r = Invoke-Post "/campaigns" "campaign.json" @{ Authorization = "Bearer $userToken" }
    Assert-Code $r "A0400" "USER create campaign -> 403/A0400"

    $r = Invoke-Post "/campaigns/1/draw" "draw2.json" @{ Authorization = "Bearer $userToken" }
    Assert-Code $r "A0501" "missing Idempotency-Key -> 400/A0501"

    & curl.exe -s -X POST "$Base/auth/logout" -H "Authorization: Bearer $userToken" 2>$null | Out-Null
    $r = Invoke-Post "/campaigns/1/draw" "draw2.json" @{ Authorization = "Bearer $userToken"; "Idempotency-Key" = "cccccccc-cccc-cccc-cccc-cccccccccccc" }
    Assert-Code $r "A0203" "after-logout draw -> 401/A0203"
}
finally {
    Write-Host "== cleanup =="
    Stop-Services
}

Write-Host ""
if ($script:Failed -eq 0) {
    Write-Host "[OK] smoke test passed" -ForegroundColor Green
    exit 0
} else {
    Write-Host "[FAIL] $($script:Failed) failure(s)" -ForegroundColor Red
    exit 1
}
