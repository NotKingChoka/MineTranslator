$ErrorActionPreference = "Stop"

Write-Host "=== 1. Starting mod build ===" -ForegroundColor Cyan
$env:JAVA_HOME="C:\Users\05071\AppData\Roaming\PrismLauncher\java\java-runtime-delta"

$buildResult = Start-Process -FilePath "cmd.exe" -ArgumentList "/c gradlew.bat assemble" -NoNewWindow -Wait -PassThru

if ($buildResult.ExitCode -ne 0) {
    Write-Error "Build failed with exit code $($buildResult.ExitCode). Upload cancelled."
}

Write-Host "=== 2. Staging files in Git ===" -ForegroundColor Cyan
git add -A

# Check if there are changes
$gitStatus = git status --porcelain
if ([string]::IsNullOrEmpty($gitStatus)) {
    Write-Host "No new changes to commit." -ForegroundColor Yellow
} else {
    Write-Host "=== 3. Committing changes ===" -ForegroundColor Cyan
    $dateStr = Get-Date -Format "yyyy-MM-dd HH:mm:ss"
    git commit -m "Auto-update: $dateStr"
}

Write-Host "=== 4. Pushing changes to GitHub ===" -ForegroundColor Cyan
git push origin
Write-Host "=== Sync complete! ===" -ForegroundColor Green
