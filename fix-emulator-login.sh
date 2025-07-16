#!/bin/bash

echo "Fixing Android Emulator Login Issues"
echo "===================================="

# 1. Clean and rebuild the app
echo "1. Cleaning and rebuilding the app..."
./gradlew clean
./gradlew assembleDebug

# 2. Verify backend is accessible
echo -e "\n2. Testing backend connectivity..."
curl -s -o /dev/null -w "Backend status: %{http_code}\n" http://localhost:3000/health

# 3. Install fresh build on emulator
echo -e "\n3. Installing fresh build on emulator..."
./gradlew installDebug

echo -e "\n✅ Done! Please try logging in again with:"
echo "   Email: admin@plp.gov.kh"
echo "   Password: password123"
echo ""
echo "If still having issues, try:"
echo "1. Restart the Android emulator"
echo "2. Check emulator's network settings (Settings > Network & Internet)"
echo "3. Verify the emulator can access internet"