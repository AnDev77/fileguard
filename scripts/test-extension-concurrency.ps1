$ErrorActionPreference = "Stop"

$baseUrl = "http://localhost:8080"
$extension = "race" + (Get-Random -Minimum 1000 -Maximum 9999)

$jobs = 1..10 | ForEach-Object {
    Start-Job -ScriptBlock {
        param($baseUrl, $extension)

        try {
            $response = Invoke-WebRequest `
                -Uri "$baseUrl/api/extensions/custom" `
                -Method Post `
                -ContentType "application/json" `
                -Body "{`"extension`":`"$extension`"}" `
                -UseBasicParsing

            [pscustomobject]@{
                Status = $response.StatusCode
                Body = $response.Content
            }
        } catch {
            $status = if ($_.Exception.Response) { [int]$_.Exception.Response.StatusCode } else { -1 }
            $body = if ($_.ErrorDetails.Message) { $_.ErrorDetails.Message } else { $_.Exception.Message }

            [pscustomobject]@{
                Status = $status
                Body = $body
            }
        }
    } -ArgumentList $baseUrl, $extension
}

$results = $jobs | Wait-Job | Receive-Job
$jobs | Remove-Job

Write-Host "extension=$extension"
$results | Group-Object Status | Sort-Object Name | ForEach-Object {
    Write-Host "status=$($_.Name) count=$($_.Count)"
}

Write-Host ""
Write-Host "Expected: status=200 count=1, status=409 count=9"
