$ErrorActionPreference = "Stop"
try {
    $response = Invoke-RestMethod -Uri "http://localhost:8080/challenges/f3510104-690c-4c79-8a1e-b873c9d4418f" -Method Get
    "=== Challenge Info ===" | Out-File -FilePath result.txt -Encoding utf8
    "ID: $($response.data.challengeId)" | Out-File -FilePath result.txt -Append -Encoding utf8
    "Name: $($response.data.name)" | Out-File -FilePath result.txt -Append -Encoding utf8
    "MonthlyFee: $($response.data.monthlyFee)" | Out-File -FilePath result.txt -Append -Encoding utf8
    "DepositAmount: $($response.data.depositAmount)" | Out-File -FilePath result.txt -Append -Encoding utf8
    "SupportAmount: $($response.data.supportAmount)" | Out-File -FilePath result.txt -Append -Encoding utf8
    "CurrentMembers: $($response.data.memberCount.current)" | Out-File -FilePath result.txt -Append -Encoding utf8
} catch {
    $_.Exception.Message | Out-File -FilePath result.txt -Encoding utf8
}
