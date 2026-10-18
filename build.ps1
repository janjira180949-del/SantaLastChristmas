param([switch]$Run)
$ErrorActionPreference = 'Stop'
$projectRoot = $PSScriptRoot
$outputPath = Join-Path $projectRoot 'out'
if (Test-Path -LiteralPath $outputPath) {
    $resolvedOutput = (Resolve-Path -LiteralPath $outputPath).Path
    if ($resolvedOutput -ne [IO.Path]::GetFullPath($outputPath)) { throw 'Unexpected build output path' }
    Remove-Item -LiteralPath $resolvedOutput -Recurse -Force
}
New-Item -ItemType Directory -Path $outputPath | Out-Null
$sourceFiles = @(Get-ChildItem -LiteralPath (Join-Path $projectRoot 'src') -Recurse -Filter '*.java' | ForEach-Object { $_.FullName })
& javac --release 17 -encoding UTF-8 -Xlint:all -d $outputPath $sourceFiles
if ($LASTEXITCODE -ne 0) { throw 'Java compilation failed' }
Write-Host "Compiled $($sourceFiles.Count) Java sources."
if ($Run) {
    Push-Location -LiteralPath $projectRoot
    try { & java -cp $outputPath game.Game } finally { Pop-Location }
}
