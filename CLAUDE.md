# CLAUDE.md=This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Build and Development Commands

### Building the Application
```bash
mvn clean package
```

### Running the Application
```bash
# Run the packaged JAR
java -jar target/automation-scheduler-1.0.0.jar

# Or run directly with Maven
mvn spring-boot:run
```

### Development
```bash
# Compile only
mvn compile

# Run tests
mvn test

# Clean build artifacts
mvn clean
```

### Testing SSH Connections
Access the H2 database console at `http://localhost:8080/h2-console` with:
- JDBC URL: `jdbc:h2:file:./data/automation`
- Username: `sa`
- Password: (empty)

## Application Architecture

This is a **Spring Boot 3.5.0** application that combines **SSH command execution** and **Selenium web automation** with a unified scheduling system.

### Core Technologies
- **Java 24** with Spring Boot 3.5.0
- **H2 Database** (embedded, file-based at `./data/automation`)
- **JSch** for SSH connections
- **Selenium WebDriver** (Chrome) for UI automation
- **Jasypt** for credential encryption
- **Thymeleaf** for web templates
- **Spring Data JPA** for data persistence

### Main Application Domains

1. **SSH Automation** (`SshService`, `SshConfigService`)
   - Execute commands/scripts on remote servers
   - Manage SSH configurations with encrypted credentials
   - Support for password and private key authentication

2. **UI Automation** (`AutomationService`, `WebDriverService`) 
   - Selenium-based web automation with Chrome WebDriver
   - Screenshot capabilities (full page and element-specific)
   - Steps: navigate, click, input, wait, scroll, select, screenshot

3. **Unified Scheduling** (`UiAutomationSchedulerService`)
   - Schedule both SSH and UI automation tasks
   - Types: Once (specific time), Interval (minutes), Cron expressions

### Key Architectural Patterns

- **Layered Architecture**: Controllers → Services → Repositories → Entities
- **Entity Relationships**: 
  - `AutomationConfig` ↔ `AutomationStep` (1:N)
  - `AutomationConfig` ↔ `ScheduleConfig` (1:1)
  - `SshConfig` ↔ `ExecutionResult` (1:N)
- **Encryption**: Jasypt automatically encrypts SSH passwords and private key passphrases
- **Configuration Classes**: Separate configs for Selenium, Scheduling, Security, Web

### Security Implementation
- **Jasypt encryption** for sensitive SSH credentials using AES-256-CBC
- **Entity listeners** automatically encrypt/decrypt on database operations
- **No user authentication** - this is a single-user application
- SSH host key verification disabled for convenience

### Web Interface Structure
- **Controllers**: `SshConfigController`, `AutomationController`, `ScheduleController`, `ExecutionController`
- **Templates**: Thymeleaf templates in `src/main/resources/templates/`
- **Static assets**: Bootstrap, custom CSS/JS in `src/main/resources/static/`
- **Main endpoints**: `/ssh`, `/ui`, `/configs`, `/schedules`, `/executions`

### Configuration Files
- **`application.yml`**: Main configuration (database, logging, SSH settings)
- **Screenshot storage**: `./screenshots/` directory
- **Log files**: `./logs/` directory with rotation
- **Database**: `./data/automation.mv.db` (H2 file)

### Development Notes
- Application runs on port **8080** by default
- Screenshots saved to `./screenshots/` directory  
- Uses **virtual threads** for SSH connections (Java 21+ feature)
- Chrome WebDriver managed automatically by WebDriverManager
- Supports both headless and GUI browser modes
- SSH connection pooling with configurable limits and timeouts

### Testing and Debugging
- H2 console available at `/h2-console` for database inspection
- Detailed logging configured for automation packages
- Selenium DevTools logging disabled to reduce noise
- Screenshot capture on automation failures for debugging