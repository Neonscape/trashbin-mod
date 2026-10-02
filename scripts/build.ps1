param(
    [ValidateSet('all','1.18.2','1.19.2','1.20.1')][string]$Minecraft = 'all',
    [string]$Task = 'release',
    [switch]$GameTests,
    [ValidateSet('none','cei','sophisticated','both')][string]$Compatibility = 'none'
)
$ErrorActionPreference = 'Stop'
$projectRoot = Split-Path $PSScriptRoot -Parent
$candidates = @($env:JAVA_HOME, 'C:\Program Files\Zulu\zulu-17')
$candidates += @(Get-ChildItem 'C:\Program Files\Eclipse Adoptium','C:\Program Files\Java' -Directory -ErrorAction SilentlyContinue | Select-Object -ExpandProperty FullName)
$jdk17 = $candidates | Where-Object {
    $_ -and (Test-Path -LiteralPath (Join-Path $_ 'bin\javac.exe')) -and
    (Test-Path -LiteralPath (Join-Path $_ 'release')) -and
    ((Get-Content -LiteralPath (Join-Path $_ 'release') -Raw) -match 'JAVA_VERSION="17\.')
} | Select-Object -First 1
if (-not $jdk17) { throw 'Install a Java 17 JDK or set JAVA_HOME to one.' }
$originalJavaHome = $env:JAVA_HOME
$originalGradleHome = $env:GRADLE_USER_HOME
$originalJavaOptions = $env:JAVA_TOOL_OPTIONS
try {
    $env:JAVA_HOME = $jdk17
    $env:GRADLE_USER_HOME = Join-Path $projectRoot '.tools\gradle-home'
    $socketDir = Join-Path $projectRoot '.tools\jtmp'
    New-Item -ItemType Directory -Force -Path $socketDir | Out-Null
    $env:JAVA_TOOL_OPTIONS = "$originalJavaOptions `"-Djdk.net.unixdomain.tmpdir=$socketDir`"".Trim()
    $gradleArgs = @($Task, '--console=plain')
    if ($Minecraft -ne 'all') { $gradleArgs += "-Pmc=$Minecraft" }
    if ($GameTests) { $gradleArgs += '-PgameTests=true' }
    if ($Compatibility -ne 'none') {
        if (-not $GameTests) { throw 'Compatibility fixtures are available only with -GameTests.' }
        $gradleArgs += "-Pcompat=$Compatibility"
    }
    Push-Location $projectRoot
    try { & .\gradlew.bat @gradleArgs; $buildExitCode = $LASTEXITCODE }
    finally { Pop-Location }
    if ($buildExitCode -ne 0) { throw "Gradle failed with exit code $buildExitCode" }
} finally {
    $env:JAVA_HOME = $originalJavaHome
    $env:GRADLE_USER_HOME = $originalGradleHome
    $env:JAVA_TOOL_OPTIONS = $originalJavaOptions
}
