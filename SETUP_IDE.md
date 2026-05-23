# Setup IntelliJ IDE for Auction System

## Problem
IDE báo lỗi: "Please add JDK 8 or later to Project Structure | Platform Settings | SDKs"

## Solution

### JDK Path on your machine
```
/Library/Java/JavaVirtualMachines/jdk-25.jdk/Contents/Home
```

### Step 1: Open IDE Settings
1. **IntelliJ IDEA** → **Preferences** (macOS) or **Settings** (Windows/Linux)
2. Search for **"SDKs"** or navigate to **Platform Settings** → **SDKs**

### Step 2: Add JDK 25 to IDE
1. Click **+** button to add new SDK
2. Select **Add JDK**
3. Paste this path: `/Library/Java/JavaVirtualMachines/jdk-25.jdk/Contents/Home`
4. Click **Open** → IDE will detect and add JDK 25

### Step 3: Set Project SDK
1. **File** → **Project Structure** (or Cmd + ;)
2. Under **Project Settings** → **Project**
3. Set **SDK** to **JDK 25** from dropdown
4. Set **Language level** to **21** or higher
5. Click **Apply** → **OK**

### Step 4: Rebuild Project
1. **Build** → **Rebuild Project** (or Cmd + Shift + K)
2. Wait for compilation to finish

### Step 5: Run ClientLauncher
1. Right-click on `ClientLauncher.java`
2. Select **Run 'ClientLauncher.main()'**

## Or: Command Line Build & Run
```bash
cd /Users/hungdomanh/Desktop/Auction-system/auctionSystem

# Clean build (already done)
mvn clean install -DskipTests

# Run with Maven
mvn javafx:run -Djavafx.mainClass=com.auction.client.ClientLauncher
```

## Verify Setup
Check current project settings:
- IntelliJ IDEA Preferences → Project Structure → Project → SDK should show **JDK 25**
- Language level should be **21 or higher**
- pom.xml `<java.version>21</java.version>` is correct

## Done! ✅
Your project should now compile and run without JDK errors.

