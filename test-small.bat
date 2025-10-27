@echo off
echo ========================================
echo Testing on Small Dataset
echo ========================================
java -jar run.jar datasets/small/in.txt datasets/small/dictionary.txt datasets/small/test_out.txt
echo.
echo Verifying correctness...
powershell -Command "Get-Content datasets\small\out.txt | Sort-Object | Out-File datasets\small\out_sorted.txt"
powershell -Command "Get-Content datasets\small\test_out.txt | Sort-Object | Out-File datasets\small\test_out_sorted.txt"
fc datasets\small\out_sorted.txt datasets\small\test_out_sorted.txt
if %ERRORLEVEL% EQU 0 (
    echo ========================================
    echo SUCCESS: Output matches expected!
    echo ========================================
) else (
    echo ========================================
    echo WARNING: Output differs from expected
    echo ========================================
)