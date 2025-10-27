@echo off
echo ========================================
echo Testing on Large Dataset (Optimized)
echo ========================================
java -Xms512m -Xmx1g -XX:+UseParallelGC -jar run.jar datasets/large/in.txt datasets/large/dictionary.txt datasets/large/test_out.txt
echo.
echo Verifying correctness (sorting before comparison)...
powershell -Command "Get-Content datasets\large\out.txt | Sort-Object | Out-File datasets\large\out_sorted.txt"
powershell -Command "Get-Content datasets\large\test_out.txt | Sort-Object | Out-File datasets\large\test_out_sorted.txt"
fc datasets\large\out_sorted.txt datasets\large\test_out_sorted.txt
if %ERRORLEVEL% EQU 0 (
    echo ========================================
    echo SUCCESS: Output matches expected!
    echo ========================================
) else (
    echo ========================================
    echo WARNING: Output differs from expected
    echo ========================================
)