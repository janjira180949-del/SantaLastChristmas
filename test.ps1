$ErrorActionPreference = 'Stop'
Push-Location -LiteralPath $PSScriptRoot
try {
    & python tools/test_v2.py
    if ($LASTEXITCODE -ne 0) { throw 'Regression suite failed; see build/v2/logs.' }
} finally { Pop-Location }
