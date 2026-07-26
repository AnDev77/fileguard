$ErrorActionPreference = "Stop"

$baseUrl = "http://localhost:8080"
$testDir = Join-Path $PSScriptRoot "..\work\test-files"
New-Item -ItemType Directory -Force -Path $testDir | Out-Null

$smallFile = Join-Path $testDir "size-ok.bin"
$largeFile = Join-Path $testDir "size-too-large.bin"

[System.IO.File]::WriteAllBytes($smallFile, (New-Object byte[] (1MB)))
[System.IO.File]::WriteAllBytes($largeFile, (New-Object byte[] (11MB)))

function Convert-ToCurlPath {
    param([string] $Path)
    return (Resolve-Path $Path).Path -replace "\\", "/"
}

function Invoke-SizeCase {
    param(
        [string] $Title,
        [string] $FilePath,
        [string] $Filename
    )

    Write-Host ""
    Write-Host "=== $Title ==="
    $curlPath = Convert-ToCurlPath $FilePath
    curl.exe -s -w "`nHTTP_STATUS:%{http_code}`n" `
        -X POST "$baseUrl/api/files" `
        -F "file=@$curlPath;filename=$Filename"
}

Invoke-SizeCase "success expected: 1MB file" $smallFile "size-ok.bin"
Invoke-SizeCase "failure expected: 11MB file" $largeFile "size-too-large.bin"
