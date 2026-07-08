$ErrorActionPreference = "Stop"

Write-Host "=== 1. Запуск сборки мода ===" -ForegroundColor Cyan
$env:JAVA_HOME="C:\Users\05071\AppData\Roaming\PrismLauncher\java\java-runtime-delta"

$buildResult = Start-Process -FilePath "cmd.exe" -ArgumentList "/c gradlew.bat assemble" -NoNewWindow -Wait -PassThru

if ($buildResult.ExitCode -ne 0) {
    Write-Error "Сборка завершилась с ошибкой $($buildResult.ExitCode). Отправка изменений отменена."
}

Write-Host "=== 2. Индексация файлов в Git ===" -ForegroundColor Cyan
git add -A

# Проверка наличия изменений
$gitStatus = git status --porcelain
if ([string]::IsNullOrEmpty($gitStatus)) {
    Write-Host "Нет новых изменений для коммита." -ForegroundColor Yellow
} else {
    Write-Host "=== 3. Создание коммита ===" -ForegroundColor Cyan
    $dateStr = Get-Date -Format "yyyy-MM-dd HH:mm:ss"
    git commit -m "Auto-update: $dateStr"
}

Write-Host "=== 4. Отправка изменений на GitHub ===" -ForegroundColor Cyan
git push origin
Write-Host "=== Обновление успешно выполнено! ===" -ForegroundColor Green
