@echo off
echo Building Budget Tracker...
mvn package -q
if %errorlevel% neq 0 (
    echo Build failed. Check output above.
    pause
    exit /b 1
)
echo Launching...
java -jar target\BudgetTracker.jar
pause
