$ErrorActionPreference = "Stop"

$baseUrl = "http://localhost:8080"
$testDir = Join-Path $PSScriptRoot "..\work\test-files"
New-Item -ItemType Directory -Force -Path $testDir | Out-Null

Set-Content -LiteralPath (Join-Path $testDir "ok.txt") -Value "ok"
Set-Content -LiteralPath (Join-Path $testDir ".env") -Value "SECRET=1"
Set-Content -LiteralPath (Join-Path $testDir ".profile.txt") -Value "hidden but has ext"
Set-Content -LiteralPath (Join-Path $testDir "noextension") -Value "no ext"
Set-Content -LiteralPath (Join-Path $testDir "base.txt") -Value "long name source"

try {
    Invoke-RestMethod `
        -Method Post `
        -Uri "$baseUrl/api/extensions/custom" `
        -ContentType "application/json" `
        -Body '{"extension":"env"}' | Out-Null
} catch {
    # An existing env policy is sufficient for the blocked .env upload case.
}

function Invoke-UploadCase {
    param(
        [string] $Title,
        [string] $FilePath,
        [string] $Filename
    )

    Write-Host ""
    Write-Host "=== $Title ==="

    if ($Filename) {
        curl.exe -s -X POST "$baseUrl/api/files" -F "file=@$FilePath;filename=$Filename"
    } else {
        curl.exe -s -X POST "$baseUrl/api/files" -F "file=@$FilePath"
    }
    Write-Host ""
}

Invoke-UploadCase "success expected: ok.txt" (Join-Path $testDir "ok.txt")
Invoke-UploadCase "success expected: .profile.txt" (Join-Path $testDir ".profile.txt")
Invoke-UploadCase "failure expected: .env is treated as blocked env extension" (Join-Path $testDir ".env")
Invoke-UploadCase "failure expected: no extension" (Join-Path $testDir "noextension")
Invoke-UploadCase "failure expected: filename over 255 chars" (Join-Path $testDir "base.txt") (("a" * 260) + ".txt")
