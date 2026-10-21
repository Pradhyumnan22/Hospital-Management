$ErrorActionPreference = "Stop"
if (-not $env:JAVA_HOME) {
    $candidates = @(
        "C:\Program Files\Java\jdk-21.0.11",
        "C:\Program Files\Eclipse Adoptium\jdk-21.0.7.6-hotspot"
    )
    foreach ($candidate in $candidates) {
        if (Test-Path $candidate) {
            $env:JAVA_HOME = $candidate
            break
        }
    }
}
if (-not $env:JAVA_HOME) {
    throw "Set JAVA_HOME to a JDK 21 installation before starting the backend."
}
Set-Location "$PSScriptRoot\..\backend"
& .\mvnw.cmd spring-boot:run
