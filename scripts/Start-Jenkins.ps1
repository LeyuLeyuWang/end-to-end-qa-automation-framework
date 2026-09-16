param([int]$Port = 8081)
$ErrorActionPreference = 'Stop'
$projectRoot = Split-Path -Parent $PSScriptRoot
$jenkinsRoot = Join-Path $projectRoot '.tools/jenkins'
$war = Join-Path $jenkinsRoot 'jenkins.war'
if (-not (Test-Path -LiteralPath $war)) {
    throw 'Jenkins WAR is missing. Follow docs/JENKINS.md to install the local instance first.'
}
$listener = Get-NetTCPConnection -LocalPort $Port -State Listen -ErrorAction SilentlyContinue
if ($listener) { throw "Port $Port is already in use. Check the existing service before starting Jenkins." }
$java = (Get-Command java -ErrorAction Stop).Source
$jenkinsHome = Join-Path $jenkinsRoot 'home'
$arguments = @('-Xmx1024m', '-Dfile.encoding=UTF-8',
    ('-DJENKINS_HOME="' + $jenkinsHome + '"'), '-jar', ('"' + $war + '"'),
    '--httpListenAddress=127.0.0.1', "--httpPort=$Port")
$process = Start-Process -FilePath $java -ArgumentList $arguments -WindowStyle Hidden -PassThru `
    -RedirectStandardOutput (Join-Path $jenkinsRoot 'stdout.log') `
    -RedirectStandardError (Join-Path $jenkinsRoot 'stderr.log')
$process.Id | Set-Content -LiteralPath (Join-Path $jenkinsRoot 'jenkins.pid')
Write-Output "Jenkins is starting at http://127.0.0.1:$Port/ (PID $($process.Id))."
