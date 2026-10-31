param(
    [ValidateSet('all','1.18.2','1.19.2','1.20.1','1.21.1')][string]$Minecraft = 'all',
    [string]$Task = 'release',
    [switch]$GameTests,
    [ValidateSet('none','cei','sophisticated','both')][string]$Compatibility = 'none'
)
$ErrorActionPreference = 'Stop'
$projectRoot = Split-Path $PSScriptRoot -Parent
$requiredJava = if ($Minecraft -eq 'all' -or $Minecraft -eq '1.21.1') { 21 } else { 17 }
$candidates = @($env:JAVA_HOME, "C:\Program Files\Zulu\zulu-$requiredJava")
$candidates += @($env:JAVA_HOME_17_X64, $env:JAVA_HOME_21_X64)
$candidates += @(Get-ChildItem 'C:\Program Files\Eclipse Adoptium','C:\Program Files\Java','C:\Program Files\Microsoft','C:\Program Files\Zulu' -Directory -ErrorAction SilentlyContinue | Select-Object -ExpandProperty FullName)
$installedJdks = @($candidates | Where-Object {
    $_ -and (Test-Path -LiteralPath (Join-Path $_ 'bin\javac.exe')) -and
    (Test-Path -LiteralPath (Join-Path $_ 'release'))
} | Select-Object -Unique)
$selectedJdk = $installedJdks | Where-Object {
    ((Get-Content -LiteralPath (Join-Path $_ 'release') -Raw) -match "JAVA_VERSION=`"$requiredJava\.")
} | Select-Object -First 1
if (-not $selectedJdk) { throw "Install a Java $requiredJava JDK or set JAVA_HOME to one." }
$originalJavaHome = $env:JAVA_HOME
$originalGradleHome = $env:GRADLE_USER_HOME
$originalJavaOptions = $env:JAVA_TOOL_OPTIONS
try {
    $env:JAVA_HOME = $selectedJdk
    $env:GRADLE_USER_HOME = Join-Path $projectRoot '.tools\gradle-home'
    $socketDir = Join-Path $projectRoot '.tools\jtmp'
    New-Item -ItemType Directory -Force -Path $socketDir | Out-Null
    $env:JAVA_TOOL_OPTIONS = "$originalJavaOptions `"-Djdk.net.unixdomain.tmpdir=$socketDir`"".Trim()
    # Java/Gradle do not automatically use Windows' configured HTTP proxy.
    # Reuse it for this process only; never persist proxy details in the repository.
    $repositoryUri = [Uri]'https://maven.neoforged.net/'
    $systemProxy = [System.Net.WebRequest]::GetSystemWebProxy().GetProxy($repositoryUri)
    if ($systemProxy -and $systemProxy.Host -ne $repositoryUri.Host -and $systemProxy.Scheme -eq 'http' -and
            $originalJavaOptions -notmatch '-D(https?|socks)\.proxyHost=') {
        $env:JAVA_TOOL_OPTIONS += " -Dhttps.proxyHost=$($systemProxy.Host) -Dhttps.proxyPort=$($systemProxy.Port) -Dhttp.proxyHost=$($systemProxy.Host) -Dhttp.proxyPort=$($systemProxy.Port)"
    }
    $gradleArgs = @($Task, '--console=plain', "-Porg.gradle.java.installations.paths=$($installedJdks -join ',')")
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
