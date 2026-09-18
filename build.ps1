#requires -version 5.1
$ErrorActionPreference = "Stop"

# ---- Mod metadata ----
$modId = "rockdices.partyaggro"
$modName = "Party Aggro"
$modVersion = "1.0.3"
$gameVersion = "1.3.3"
$modDescription = "Adventure party aggro/retaliation controls for PvP. Server + client."
$author = "Rockdices"

# ---- Paths ----
$root = Split-Path -Parent $MyInvocation.MyCommand.Path
$gameDir = "D:\Steam\steamapps\common\Necesse"

$jdk = $null
foreach ($candidate in @($env:JAVA_HOME, "C:\Program Files\Java\jdk-21")) {
    if ($candidate -and (Test-Path (Join-Path $candidate "bin\javac.exe"))) {
        $jdk = $candidate
        break
    }
}
if (-not $jdk) {
    throw "Could not find a JDK with javac. Set JAVA_HOME or edit build.ps1."
}
$javac = Join-Path $jdk "bin\javac.exe"
$jar = Join-Path $jdk "bin\jar.exe"

if (-not (Test-Path (Join-Path $gameDir "Necesse.jar"))) {
    throw "Necesse.jar not found at $gameDir. Edit gameDir in build.ps1."
}

$buildDir = Join-Path $root "build"
$classesDir = Join-Path $buildDir "classes"
$jarDir = Join-Path $buildDir "jar"
$srcDir = Join-Path $root "src\main\java"
$resDir = Join-Path $root "src\main\resources"

Remove-Item -Recurse -Force $classesDir -ErrorAction SilentlyContinue
New-Item -ItemType Directory -Force -Path $classesDir, $jarDir | Out-Null

# ---- Compile ----
$sources = Get-ChildItem -Path $srcDir -Recurse -Filter *.java | ForEach-Object { $_.FullName }
if (-not $sources) { throw "No Java sources found under $srcDir" }

$classpath = "$gameDir\Necesse.jar;$gameDir\lib\*"
Write-Host "Compiling $($sources.Count) source file(s)..."
& $javac --release 8 -encoding UTF-8 -Xlint:-options -cp $classpath -d $classesDir $sources
if ($LASTEXITCODE -ne 0) { throw "javac failed with exit code $LASTEXITCODE" }

# ---- Resources ----
$resDst = Join-Path $classesDir "resources"
if (Test-Path $resDir) {
    New-Item -ItemType Directory -Force -Path $resDst | Out-Null
    Copy-Item -Path (Join-Path $resDir "*") -Destination $resDst -Recurse -Force
}

# ---- mod.info (UTF-8 without BOM) ----
$modInfo = @"
{
    id = $modId,
    name = $modName,
    version = $modVersion,
    gameVersion = $gameVersion,
    description = $modDescription,
    author = $author,
    clientside = false
}
"@
[System.IO.File]::WriteAllText((Join-Path $classesDir "mod.info"), $modInfo, (New-Object System.Text.UTF8Encoding($false)))

# ---- Jar ----
$jarName = (($modName -replace ' ', '') + "-$gameVersion-$modVersion.jar")
$jarPath = Join-Path $jarDir $jarName
Remove-Item -Force $jarPath -ErrorAction SilentlyContinue
& $jar cf $jarPath -C $classesDir .
if ($LASTEXITCODE -ne 0) { throw "jar failed with exit code $LASTEXITCODE" }

Write-Host "Built: $jarPath"
