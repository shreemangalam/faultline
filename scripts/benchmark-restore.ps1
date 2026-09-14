[CmdletBinding()]
param(
    [ValidateRange(3, 50000)]
    [int]$EventCount = 50000,

    [ValidateRange(1, 16)]
    [int]$ParallelWorkers = 8,

    [ValidateRange(1024, 65535)]
    [int]$Port = 18082,

    [string]$OutputPath,

    [switch]$KeepEnvironment
)

$ErrorActionPreference = 'Stop'
$projectName = 'faultline-restore-benchmark'
$composePath = Join-Path (Split-Path $PSScriptRoot -Parent) 'infra/benchmark.compose.yaml'
$baseUrl = "http://localhost:$Port"
$env:RESTORE_BENCHMARK_PORT = $Port
$httpClient = [System.Net.Http.HttpClient]::new()
$httpClient.Timeout = [TimeSpan]::FromMinutes(5)

function Invoke-JsonRequest {
    param(
        [System.Net.Http.HttpMethod]$Method,
        [string]$Uri,
        [object]$Body
    )

    $request = [System.Net.Http.HttpRequestMessage]::new($Method, $Uri)
    try {
        if ($null -ne $Body) {
            $json = $Body | ConvertTo-Json -Depth 8 -Compress
            $request.Content = [System.Net.Http.StringContent]::new(
                $json,
                [System.Text.Encoding]::UTF8,
                'application/json')
        }
        $response = $httpClient.SendAsync($request).GetAwaiter().GetResult()
        try {
            $content = $response.Content.ReadAsStringAsync().GetAwaiter().GetResult()
            if (-not $response.IsSuccessStatusCode) {
                throw "HTTP $([int]$response.StatusCode) from $Uri`: $content"
            }
            if ([string]::IsNullOrWhiteSpace($content)) { return $null }
            return $content | ConvertFrom-Json
        }
        finally {
            $response.Dispose()
        }
    }
    finally {
        $request.Dispose()
    }
}

function Invoke-RecoveryWave {
    param(
        [int]$WaveNumber,
        [int]$WorkerCount
    )

    $tasks = @()
    $requests = @()
    for ($worker = 1; $worker -le $WorkerCount; $worker++) {
        $body = @{
            workerId = "benchmark-$WaveNumber-$worker"
            maxEvents = 10
            leaseSeconds = 60
        } | ConvertTo-Json -Compress
        $request = [System.Net.Http.HttpRequestMessage]::new(
            [System.Net.Http.HttpMethod]::Post,
            "$baseUrl/api/v1/worker/run-once")
        $request.Content = [System.Net.Http.StringContent]::new(
            $body,
            [System.Text.Encoding]::UTF8,
            'application/json')
        $requests += $request
        $tasks += $httpClient.SendAsync($request)
    }

    try {
        [System.Threading.Tasks.Task]::WaitAll([System.Threading.Tasks.Task[]]$tasks)
        $completed = 0
        foreach ($task in $tasks) {
            $response = $task.GetAwaiter().GetResult()
            try {
                $content = $response.Content.ReadAsStringAsync().GetAwaiter().GetResult()
                if (-not $response.IsSuccessStatusCode) {
                    throw "Worker request failed with HTTP $([int]$response.StatusCode): $content"
                }
                $executed = @($content | ConvertFrom-Json)
                foreach ($event in $executed) {
                    if ($event.classification -ne 'SUCCESS' -or $event.state -ne 'COMPLETED') {
                        throw "Unexpected recovery result: $content"
                    }
                }
                $completed += $executed.Count
            }
            finally {
                $response.Dispose()
            }
        }
        return $completed
    }
    finally {
        foreach ($request in $requests) { $request.Dispose() }
    }
}

function Assert-Equal {
    param(
        [object]$Actual,
        [object]$Expected,
        [string]$Label
    )

    if ($Actual -ne $Expected) {
        throw "$Label expected '$Expected' but was '$Actual'."
    }
}

function Wait-ForRestore {
    for ($attempt = 1; $attempt -le 120; $attempt++) {
        try {
            $health = Invoke-JsonRequest -Method ([System.Net.Http.HttpMethod]::Get) -Uri "$baseUrl/actuator/health"
            if ($health.status -eq 'UP') { return }
        }
        catch {
            if ($attempt -eq 120) { throw }
        }
        Start-Sleep -Seconds 1
    }
    throw 'Restore did not become healthy within 120 seconds.'
}

$result = $null
try {
    & docker compose --project-name $projectName -f $composePath up --build --detach
    if ($LASTEXITCODE -ne 0) { throw 'Docker Compose could not start the benchmark environment.' }
    Wait-ForRestore

    $loadTimer = [System.Diagnostics.Stopwatch]::StartNew()
    $scenario = Invoke-JsonRequest -Method ([System.Net.Http.HttpMethod]::Post) `
        -Uri "$baseUrl/api/v1/scenarios/thundering-herd" `
        -Body @{ eventCount = $EventCount }
    $loadTimer.Stop()
    Assert-Equal $scenario.eventCount $EventCount 'Loaded event count'

    $outageBatch = @(Invoke-JsonRequest -Method ([System.Net.Http.HttpMethod]::Post) `
        -Uri "$baseUrl/api/v1/worker/run-once" `
        -Body @{ workerId = 'benchmark-outage'; maxEvents = 10; leaseSeconds = 60 })
    $expectedOutageBatch = [Math]::Min(10, $EventCount)
    Assert-Equal $outageBatch.Count $expectedOutageBatch 'Outage batch size'
    foreach ($event in $outageBatch) {
        Assert-Equal $event.classification 'TRANSIENT_FAILURE' 'Outage classification'
    }

    $blockedBatch = @(Invoke-JsonRequest -Method ([System.Net.Http.HttpMethod]::Post) `
        -Uri "$baseUrl/api/v1/worker/run-once" `
        -Body @{ workerId = 'benchmark-circuit-check'; maxEvents = 10; leaseSeconds = 60 })
    Assert-Equal $blockedBatch.Count 0 'Circuit-blocked batch size'

    $encodedTarget = [Uri]::EscapeDataString($scenario.targetSystem)
    $endToEndRecoveryTimer = [System.Diagnostics.Stopwatch]::StartNew()
    Invoke-JsonRequest -Method ([System.Net.Http.HttpMethod]::Put) `
        -Uri "$baseUrl/api/v1/simulator/targets/$encodedTarget" `
        -Body @{ behavior = 'SUCCESS'; delayMillis = 0; resetHistory = $false } | Out-Null

    $circuit = @(Invoke-JsonRequest -Method ([System.Net.Http.HttpMethod]::Get) `
        -Uri "$baseUrl/api/v1/circuit-breakers") | Where-Object targetSystem -eq $scenario.targetSystem
    Assert-Equal $circuit.Count 1 'Scenario circuit count'
    Assert-Equal $circuit[0].state 'OPEN' 'Circuit state after outage'
    $openUntil = [DateTimeOffset]$circuit[0].openUntil
    $cooldownMillis = [Math]::Max(0, [Math]::Ceiling(($openUntil - [DateTimeOffset]::UtcNow).TotalMilliseconds) + 100)
    if ($cooldownMillis -gt 0) { Start-Sleep -Milliseconds $cooldownMillis }

    $drainTimer = [System.Diagnostics.Stopwatch]::StartNew()
    $completed = 0
    $wave = 0
    $idleWaves = 0
    while ($completed -lt $EventCount) {
        $wave++
        $waveCompleted = Invoke-RecoveryWave -WaveNumber $wave -WorkerCount $ParallelWorkers
        $completed += $waveCompleted
        if ($waveCompleted -eq 0) {
            $idleWaves++
            if ($idleWaves -ge 100) { throw 'Recovery made no progress for 100 consecutive waves.' }
            Start-Sleep -Milliseconds 100
        }
        else {
            $idleWaves = 0
        }
        if ($wave % 25 -eq 0 -or $completed -eq $EventCount) {
            Write-Host "Recovered $completed / $EventCount events."
        }
    }
    $drainTimer.Stop()
    $endToEndRecoveryTimer.Stop()

    $statistics = Invoke-JsonRequest -Method ([System.Net.Http.HttpMethod]::Get) -Uri "$baseUrl/api/v1/statistics"
    $simulator = Invoke-JsonRequest -Method ([System.Net.Http.HttpMethod]::Get) `
        -Uri "$baseUrl/api/v1/simulator/targets/$encodedTarget"
    $finalCircuit = @(Invoke-JsonRequest -Method ([System.Net.Http.HttpMethod]::Get) `
        -Uri "$baseUrl/api/v1/circuit-breakers") | Where-Object targetSystem -eq $scenario.targetSystem

    Assert-Equal $statistics.totalEvents $EventCount 'Final event count'
    Assert-Equal $statistics.eventCounts.COMPLETED $EventCount 'Completed event count'
    Assert-Equal $statistics.pendingEventCount 0 'Pending event count'
    Assert-Equal $statistics.terminalFailureCount 0 'Terminal failure count'
    Assert-Equal $statistics.attemptCounts.TRANSIENT_FAILURE $expectedOutageBatch 'Transient failure attempts'
    Assert-Equal $statistics.attemptCounts.SUCCEEDED $EventCount 'Successful attempts'
    Assert-Equal $statistics.totalAttempts ($EventCount + $expectedOutageBatch) 'Total attempts'
    Assert-Equal $simulator.requestCount ($EventCount + $expectedOutageBatch) 'Simulator request count'
    Assert-Equal $simulator.acceptedCount $EventCount 'Accepted side-effect count'
    Assert-Equal $finalCircuit[0].state 'CLOSED' 'Final circuit state'
    if ($null -eq $statistics.p95SuccessfulRecoveryLatencyMillis) {
        throw 'p95 successful recovery latency was not calculated.'
    }

    $drainRate = [Math]::Round($EventCount / $drainTimer.Elapsed.TotalSeconds, 2)
    $result = [ordered]@{
        verifiedAt = [DateTimeOffset]::UtcNow.ToString('o')
        eventCount = $EventCount
        parallelWorkers = $ParallelWorkers
        loadDurationMillis = $loadTimer.ElapsedMilliseconds
        endToEndRecoveryDurationMillis = $endToEndRecoveryTimer.ElapsedMilliseconds
        drainDurationMillis = $drainTimer.ElapsedMilliseconds
        drainRatePerSecond = $drainRate
        p95ReceiptToSuccessMillis = $statistics.p95SuccessfulRecoveryLatencyMillis
        outageAttempts = $expectedOutageBatch
        totalAttempts = $statistics.totalAttempts
        acceptedDeliveries = $simulator.acceptedCount
        pendingEvents = $statistics.pendingEventCount
        terminalFailures = $statistics.terminalFailureCount
        finalCircuitState = $finalCircuit[0].state
    }
}
finally {
    $httpClient.Dispose()
    if (-not $KeepEnvironment) {
        & docker compose --project-name $projectName -f $composePath down --volumes --remove-orphans
    }
    Remove-Item Env:RESTORE_BENCHMARK_PORT -ErrorAction SilentlyContinue
}

$jsonResult = $result | ConvertTo-Json -Depth 5
if (-not [string]::IsNullOrWhiteSpace($OutputPath)) {
    $resolvedOutput = $ExecutionContext.SessionState.Path.GetUnresolvedProviderPathFromPSPath($OutputPath)
    $outputDirectory = Split-Path $resolvedOutput -Parent
    if (-not [string]::IsNullOrWhiteSpace($outputDirectory)) {
        [System.IO.Directory]::CreateDirectory($outputDirectory) | Out-Null
    }
    [System.IO.File]::WriteAllText($resolvedOutput, $jsonResult + [Environment]::NewLine)
}
$jsonResult
