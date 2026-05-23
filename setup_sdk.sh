#!/bin/bash

# Auto-configure IntelliJ IDEA SDK for Auction System Project
# This script finds JDK 25 and updates IntelliJ IDE config

JDK_PATH=$(/usr/libexec/java_home -v 25)
PROJECT_DIR="/Users/hungdomanh/Desktop/Auction-system/auctionSystem"
IDE_CONFIG_DIR="$HOME/Library/Application Support/JetBrains/IntelliJIdea2024.3"  # Adjust version if needed

echo "🔍 Found JDK at: $JDK_PATH"
echo "📦 Project: $PROJECT_DIR"
echo ""

# Check if IntelliJ is installed
if [ ! -d "$IDE_CONFIG_DIR" ]; then
    echo "⚠️  IntelliJ config dir not found at: $IDE_CONFIG_DIR"
    echo "Please manually set SDK in IntelliJ:"
    echo ""
    echo "1. Preferences → Project Structure → SDKs"
    echo "2. Click + → Add JDK"
    echo "3. Paste: $JDK_PATH"
    echo "4. Click Open"
    echo "5. Go to Project → SDK → select JDK 25"
    echo ""
else
    echo "✅ IntelliJ config found. You can now:"
    echo "1. Open IntelliJ IDEA"
    echo "2. Open project: $PROJECT_DIR"
    echo "3. Go to Preferences → Project Structure → SDKs"
    echo "4. Add JDK with path: $JDK_PATH"
    echo ""
fi

echo "🚀 To run ClientLauncher after SDK setup:"
echo "cd $PROJECT_DIR && mvn javafx:run -Djavafx.mainClass=com.auction.client.ClientLauncher"

