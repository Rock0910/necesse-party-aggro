#requires -version 5.1
$ErrorActionPreference = "Stop"

$root = Split-Path -Parent $MyInvocation.MyCommand.Path
$srcDir = Join-Path $root "src\main\java"
$testsDir = Join-Path $root "tests"
$outDir = Join-Path $root "build\test"

$jdk = $null
foreach ($candidate in @($env:JAVA_HOME, "C:\Program Files\Java\jdk-21")) {
    if ($candidate -and (Test-Path (Join-Path $candidate "bin\javac.exe"))) {
        $jdk = $candidate
        break
    }
}
if (-not $jdk) { throw "Could not find a JDK with javac." }
$javac = Join-Path $jdk "bin\javac.exe"
$java = Join-Path $jdk "bin\java.exe"

Remove-Item -Recurse -Force $outDir -ErrorAction SilentlyContinue
New-Item -ItemType Directory -Force -Path $outDir | Out-Null

# Pure data classes + tests, no game dependencies.
$dataFiles = Get-ChildItem -Path (Join-Path $srcDir "partyaggro\data") -Filter *.java | ForEach-Object { $_.FullName }
$testFiles = Get-ChildItem -Path $testsDir -Filter *.java | ForEach-Object { $_.FullName }

& $javac -encoding UTF-8 -d $outDir $dataFiles $testFiles
if ($LASTEXITCODE -ne 0) { throw "test javac failed" }

& $java -cp $outDir LogicTest
if ($LASTEXITCODE -ne 0) { throw "tests failed" }
