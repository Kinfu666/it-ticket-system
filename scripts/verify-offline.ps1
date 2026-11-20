param([string]$BrowserPath = 'C:\Program Files\Google\Chrome\Application\chrome.exe')
$ErrorActionPreference = 'Stop'
$taskRoot = Split-Path $PSScriptRoot -Parent
Push-Location $taskRoot
$taskServer = $null
try {
    if (-not (Test-Path 'target/it-helpdesk-system-1.0.0.jar')) { throw 'Run mvnw.cmd clean verify first.' }
    New-Item -ItemType Directory -Force '.tools/acceptance' | Out-Null
    $taskDatabase = '.tools/acceptance/run-' + [guid]::NewGuid().ToString('N') + '.db'
    $env:BROWSER_PATH = $BrowserPath
    $env:HELPDESK_TEST_URL = 'http://127.0.0.1:18080'
    $taskPreviousPreference = $ErrorActionPreference
    try {
        $ErrorActionPreference = 'Continue'
        $taskJavaSettings = (& java -XshowSettings:properties -version 2>&1 | Out-String)
        if ($LASTEXITCODE -ne 0) { throw 'Cannot inspect Java settings.' }
    } finally { $ErrorActionPreference = $taskPreviousPreference }
    $taskJavaHome = [regex]::Match($taskJavaSettings, '(?m)^\s*java.home\s*=\s*(.+?)\s*$').Groups[1].Value.Trim()
    if (-not $taskJavaHome) { throw 'Cannot resolve the actual JDK home.' }
    $taskJavaExecutable = Join-Path $taskJavaHome 'bin/java.exe'
    & javac -d .tools/probe scripts/NetworkIsolationProbe.java
    if ($LASTEXITCODE -ne 0) { throw 'Network probe compilation failed.' }
    & java '-Djava.security.manager' '-Djava.security.policy==scripts/offline.policy' -cp .tools/probe NetworkIsolationProbe
    if ($LASTEXITCODE -ne 0) { throw 'Network isolation probe failed.' }
    $taskArguments = @('-Djava.security.manager', '-Djava.security.policy==scripts/offline.policy', '-jar',
        'target/it-helpdesk-system-1.0.0.jar', '--spring.profiles.active=demo', "--helpdesk.database=$taskDatabase", '--server.port=18080')
    function Start-TestServer([string]$LogName) {
        $listener = [System.Net.Sockets.TcpListener]::new([System.Net.IPAddress]::Loopback, 18080)
        try { $listener.Start() } finally { $listener.Stop() }
        $process = Start-Process -FilePath $taskJavaExecutable -ArgumentList $taskArguments -WorkingDirectory $taskRoot `
            -WindowStyle Hidden -PassThru -RedirectStandardOutput ".tools/acceptance/$LogName.log" -RedirectStandardError ".tools/acceptance/$LogName-error.log"
        for ($attempt=0; $attempt -lt 120; $attempt++) {
            if ($process.HasExited) { throw "Test server exited; see $LogName.log" }
            try {
                $response = Invoke-WebRequest 'http://127.0.0.1:18080/login' -UseBasicParsing -TimeoutSec 1
                $started = Select-String -LiteralPath ".tools/acceptance/$LogName.log" -Pattern 'Started HelpdeskApplication' -Quiet
                if ($response.StatusCode -eq 200 -and $started -and -not $process.HasExited) {
                    Write-Host "Server PID=$($process.Id), log=$LogName.log"
                    return $process
                }
            } catch { Start-Sleep -Milliseconds 250 }
        }
        Stop-Process -Id $process.Id
        throw 'Test server startup timed out.'
    }
    $taskServer = Start-TestServer 'offline-server'
    & node scripts/browser-acceptance.cjs
    if ($LASTEXITCODE -ne 0) { throw 'Browser acceptance failed.' }
    Stop-Process -Id $taskServer.Id
    $taskServer.WaitForExit()
    $taskServer = Start-TestServer 'restarted-server'
    & node scripts/browser-restart-check.cjs
    if ($LASTEXITCODE -ne 0) { throw 'Restart verification failed.' }
    Write-Output "PASS: isolated jar, browser workflow and restart; database=$taskDatabase"
} finally {
    if ($null -ne $taskServer -and -not $taskServer.HasExited) { Stop-Process -Id $taskServer.Id }
    Pop-Location
}
