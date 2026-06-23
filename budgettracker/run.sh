#!/bin/bash
echo "Building Budget Tracker..."
mvn package -q
if [ $? -eq 0 ]; then
    echo "Launching..."
    java -jar target/BudgetTracker.jar
else
    echo "Build failed. Check output above."
fi
