param([switch]$BuildOnly)

$ErrorActionPreference = 'Stop'
$projectDirectory = $PSScriptRoot
$toolsDirectory = Join-Path $projectDirectory '.tools'

# Find a full JDK 17 or newer, including the one bundled with VS Code Java.
function Test-Jdk($directory) {
    if (!$directory -or !(Test-Path -LiteralPath (Join-Path $directory 'bin\javac.exe'))) {
        return $false
    }
    $releaseFile = Join-Path $directory 'release'
    if (!(Test-Path -LiteralPath $releaseFile)) {
        return $false
    }
    $version = Select-String -LiteralPath $releaseFile -Pattern '^JAVA_VERSION="(\d+)'
    return $version -and [int]$version.Matches[0].Groups[1].Value -ge 17
}

$jdkDirectory = $null
if (Test-Jdk $env:JAVA_HOME) {
    $jdkDirectory = $env:JAVA_HOME
}

if (!$jdkDirectory) {
    $compiler = Get-Command javac.exe -ErrorAction SilentlyContinue
    if ($compiler) {
        $candidate = Split-Path (Split-Path $compiler.Source -Parent) -Parent
        if (Test-Jdk $candidate) {
            $jdkDirectory = $candidate
        }
    }
}

if (!$jdkDirectory) {
    $jdkPatterns = @(
        "$env:USERPROFILE\.jdks\*",
        "$env:USERPROFILE\.vscode\extensions\redhat.java-*\jre\*",
        "$env:ProgramFiles\Java\*",
        "$env:ProgramFiles\Eclipse Adoptium\*",
        "$env:ProgramFiles\Microsoft\jdk-*",
        "$env:ProgramFiles\JetBrains\*\jbr"
    )
    foreach ($pattern in $jdkPatterns) {
        foreach ($candidate in (Get-ChildItem -Path $pattern -Directory -ErrorAction SilentlyContinue | Sort-Object Name -Descending)) {
            if (Test-Jdk $candidate.FullName) {
                $jdkDirectory = $candidate.FullName
                break
            }
        }
        if ($jdkDirectory) {
            break
        }
    }
}

if (!$jdkDirectory) {
    throw 'A JDK 17 or newer is required. Install a JDK or the VS Code Java extension, or set JAVA_HOME to your JDK folder.'
}

# Changes apply to this launcher process and its children only.
$env:JAVA_HOME = $jdkDirectory
$env:PATH = (Join-Path $jdkDirectory 'bin') + ';' + $env:PATH
Write-Host "Using Java from $jdkDirectory"

$maven = Get-Command mvn.cmd -ErrorAction SilentlyContinue
if ($maven) {
    $mavenCommand = $maven.Source
} else {
    $mavenVersion = '3.9.11'
    $mavenDirectory = Join-Path $toolsDirectory "apache-maven-$mavenVersion"
    $mavenCommand = Join-Path $mavenDirectory 'bin\mvn.cmd'
    if (!(Test-Path -LiteralPath $mavenCommand)) {
        New-Item -ItemType Directory -Path $toolsDirectory -Force | Out-Null
        $archive = Join-Path $toolsDirectory "apache-maven-$mavenVersion-bin.zip"
        $downloadUrl = "https://repo.maven.apache.org/maven2/org/apache/maven/apache-maven/$mavenVersion/apache-maven-$mavenVersion-bin.zip"
        Write-Host "Downloading portable Maven $mavenVersion (first launch only)..."
        [Net.ServicePointManager]::SecurityProtocol = [Net.SecurityProtocolType]::Tls12
        Invoke-WebRequest -UseBasicParsing -Uri $downloadUrl -OutFile $archive
        $checksum = (Invoke-WebRequest -UseBasicParsing -Uri "$downloadUrl.sha512").Content.Trim()
        $expectedHash = ($checksum -split '\s+')[0]
        $actualHash = (Get-FileHash -LiteralPath $archive -Algorithm SHA512).Hash
        if ($actualHash -ine $expectedHash) {
            throw 'The Maven download failed its SHA-512 check. Please run this launcher again.'
        }
        Expand-Archive -LiteralPath $archive -DestinationPath $toolsDirectory -Force
    }
}

# Keep downloaded dependencies inside the project and run where pokemon.db lives.
$repositoryArgument = '-Dmaven.repo.local=' + (Join-Path $toolsDirectory 'repository')
Push-Location -LiteralPath $projectDirectory
try {
    if ($BuildOnly) {
        & $mavenCommand --batch-mode --no-transfer-progress $repositoryArgument compile
    } else {
        & $mavenCommand --batch-mode --no-transfer-progress $repositoryArgument compile javafx:run
    }
    if ($LASTEXITCODE -ne 0) {
        throw "Maven exited with code $LASTEXITCODE. See the build output above."
    }
} finally {
    Pop-Location
}
