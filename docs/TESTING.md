# Testing and recorded results

## Latest complete original-project regression

**84/128 test entry points passed; 44 failed.** This result predates GitHub publication and was confirmed before/after the earlier verified cleanup.

- 43 failures involve historical preservation guards rejecting the unchanged `src/game/Encounter.java` revision.
- `Phase3GameplayTest` also fails `Mini arena/boss/solid footprint clear` at its existing assertion.
- No production code, tests or preservation hashes were changed to improve those results.

This is a recorded result from the complete original development environment, not the expected result of running this public source-only checkout.

## Public test sources

All **181 current Java test/support source files** are preserved without edits. Together with the 102 production sources they form the existing 283-source regression compilation. `test.ps1` calls `tools/test_v2.py`; two existing metadata-check helpers are also included.

The complete suite additionally requires runtime assets, the 54 current PNG test fixtures, historical preservation ZIPs, approved-source manifests and some historical validation material. Those binaries/build records are intentionally excluded from this repository. Merely supplying the portable runtime folders does not restore every historical preservation fixture. A fresh clone's test execution can therefore fail for missing fixtures and **must not be described as reproducing 84/128**.

In a full development environment containing the original approved fixtures:

```powershell
.\test.ps1
```

The existing runner uses Python 3 and the Java compiler/runtime, compiles all game/test sources, and runs the standalone Java test entry points. No test assertions have been weakened or removed in this publication.

## Portable release validation

The current portable release was compiled for Java 17 bytecode and executed on **Oracle Java 26.0.1, Windows 64-bit**. The executable JAR, native menu, Start flow, initial Santa's Base gameplay, opening-scene advancement and Java Sound initialization passed. Its 209 traced image paths and 152 distinct registered audio resources decoded successfully from an independently copied distribution outside the development directory.

These startup/resource results are distinct from a complete regression pass or a full manual game playthrough. Java 17 runtime compatibility was not executed or certified. Detailed internal validation logs, cleanup manifests, screenshots and the academic Word report are kept outside this repository.
