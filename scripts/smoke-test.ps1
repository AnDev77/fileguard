param(
    [string] $BaseUrl = "http://localhost"
)

$ErrorActionPreference = "Stop"
$base = $BaseUrl.TrimEnd("/")

function Assert-HttpOk {
    param(
        [string] $Name,
        [string] $Url
    )

    $response = Invoke-WebRequest -Uri $Url -Method Get -UseBasicParsing
    if ($response.StatusCode -ne 200) {
        throw "$Name failed with HTTP $($response.StatusCode)."
    }
    Write-Host "[OK] $Name -> HTTP $($response.StatusCode)"
}

Assert-HttpOk "Frontend" "$base/"
Assert-HttpOk "Health" "$base/health"
Assert-HttpOk "Extension API" "$base/api/extensions"

Write-Host "Single-address smoke test completed: $base"
