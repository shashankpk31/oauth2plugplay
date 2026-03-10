# 🔨 Build and Installation Guide

## Prerequisites

Before building the OTP Auth Starter, ensure you have:

- ✅ **Java 17** or higher
- ✅ **Maven 3.6+**
- ✅ **Git** (optional)

### Check Java Version

```bash
java -version
# Should show: java version "17" or higher
```

### Check Maven Version

```bash
mvn -version
# Should show: Apache Maven 3.6.0 or higher
```

---

## Build the Starter

### Step 1: Navigate to Project Directory

```bash
cd otp-auth-starter
```

### Step 2: Clean and Build

```bash
mvn clean install
```

**Output:**
```
[INFO] Building jar: .../otp-auth-spring-boot-starter-1.0.0.jar
[INFO] Installing .../otp-auth-spring-boot-starter-1.0.0.jar to ~/.m2/repository/...
[INFO] BUILD SUCCESS
```

This will:
1. Compile all Java source files
2. Run tests (if any)
3. Package the JAR file
4. Install to your local Maven repository (`~/.m2/repository`)

### Step 3: Verify Installation

Check that the JAR is installed:

**Windows:**
```bash
dir %USERPROFILE%\.m2\repository\com\shashankpk\otp-auth-spring-boot-starter\1.0.0\
```

**Linux/Mac:**
```bash
ls ~/.m2/repository/com/shashankpk/otp-auth-spring-boot-starter/1.0.0/
```

You should see:
```
otp-auth-spring-boot-starter-1.0.0.jar
otp-auth-spring-boot-starter-1.0.0.pom
```

---

## Use in Your Project

### Option 1: Maven Dependency (Recommended)

Add to your project's `pom.xml`:

```xml
<dependency>
    <groupId>com.shashankpk</groupId>
    <artifactId>otp-auth-spring-boot-starter</artifactId>
    <version>1.0.0</version>
</dependency>
```

Then run:
```bash
mvn clean install
```

### Option 2: Add JAR Directly

Copy the JAR file to your project:

```bash
cp ~/.m2/repository/com/shashankpk/otp-auth-spring-boot-starter/1.0.0/otp-auth-spring-boot-starter-1.0.0.jar \
   your-project/libs/
```

Add to `pom.xml`:
```xml
<dependency>
    <groupId>com.shashankpk</groupId>
    <artifactId>otp-auth-spring-boot-starter</artifactId>
    <version>1.0.0</version>
    <scope>system</scope>
    <systemPath>${project.basedir}/libs/otp-auth-spring-boot-starter-1.0.0.jar</systemPath>
</dependency>
```

---

## Development Setup

If you want to modify the starter itself:

### 1. Import into IDE

**IntelliJ IDEA:**
1. File → Open → Select `otp-auth-starter/pom.xml`
2. Wait for Maven to download dependencies

**Eclipse:**
1. File → Import → Maven → Existing Maven Projects
2. Select `otp-auth-starter` directory

**VS Code:**
1. Open folder `otp-auth-starter`
2. Install "Extension Pack for Java"

### 2. Make Changes

Edit files in `src/main/java/com/shashankpk/otpauth/`

### 3. Rebuild

```bash
mvn clean install
```

### 4. Test Changes

Use the test project to verify changes:
```bash
cd ../test-app
mvn clean spring-boot:run
```

---

## Troubleshooting

### Issue: "mvn: command not found"

**Solution:** Install Maven or add to PATH

**Windows:**
```bash
# Download Maven from https://maven.apache.org/download.cgi
# Extract to C:\Maven
# Add C:\Maven\bin to PATH
```

**Mac:**
```bash
brew install maven
```

**Linux:**
```bash
sudo apt-get install maven
```

### Issue: "Java version not supported"

**Solution:** Install Java 17+

```bash
# Windows: Download from https://adoptium.net/
# Mac: brew install openjdk@17
# Linux: sudo apt-get install openjdk-17-jdk
```

### Issue: "BUILD FAILURE - compilation error"

**Solution:** Check Java and Maven versions, then:

```bash
mvn clean install -U
```

The `-U` flag forces update of dependencies.

### Issue: "Tests failing"

**Solution:** Skip tests temporarily:

```bash
mvn clean install -DskipTests
```

### Issue: "Cannot resolve dependencies"

**Solution:** Clear Maven cache and rebuild:

```bash
rm -rf ~/.m2/repository/com/shashankpk/otp-auth-spring-boot-starter
mvn clean install
```

---

## Publishing to Maven Central (Future)

If you want to publish this starter to Maven Central:

### 1. Setup GPG Key

```bash
gpg --gen-key
gpg --list-keys
```

### 2. Configure settings.xml

Add to `~/.m2/settings.xml`:

```xml
<servers>
    <server>
        <id>ossrh</id>
        <username>your-jira-id</username>
        <password>your-jira-pwd</password>
    </server>
</servers>
```

### 3. Add to pom.xml

```xml
<distributionManagement>
    <snapshotRepository>
        <id>ossrh</id>
        <url>https://oss.sonatype.org/content/repositories/snapshots</url>
    </snapshotRepository>
    <repository>
        <id>ossrh</id>
        <url>https://oss.sonatype.org/service/local/staging/deploy/maven2/</url>
    </repository>
</distributionManagement>
```

### 4. Deploy

```bash
mvn clean deploy
```

---

## Version Management

### Update Version

Edit `pom.xml`:

```xml
<version>1.1.0</version>
```

Then rebuild:
```bash
mvn clean install
```

### Create Release

```bash
git tag v1.0.0
git push origin v1.0.0
```

---

## Quick Reference

```bash
# Build and install
mvn clean install

# Build without tests
mvn clean install -DskipTests

# Run tests only
mvn test

# Clean build files
mvn clean

# Check for updates
mvn versions:display-dependency-updates

# Generate documentation
mvn javadoc:javadoc
```

---

**Your starter is now ready to use in any Spring Boot project!** 🎉
