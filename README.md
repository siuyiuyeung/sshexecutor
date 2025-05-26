# SSH Executor

A Spring Boot web application that allows you to manage SSH configurations and execute commands on remote servers through a user-friendly web interface.

## Features

- **SSH Connection Management**: Save and manage multiple SSH server configurations
- **Command Execution**: Execute commands on remote servers directly from the web interface
- **Script Execution**: Upload and execute shell scripts on remote servers
- **Execution History**: View recent command executions with their results
- **User-Friendly Dashboard**: Intuitive web interface for managing configurations and executions

## Prerequisites

- Java 24 or later
- Maven 3.6.0 or later
- Web browser (Chrome, Firefox, Safari, or Edge)

## Getting Started

### Installation

1. Clone the repository:
   ```bash
   git clone https://github.com/siuyiuyeung/sshexecutor.git
   cd sshexecutor
   ```

2. Build the application:
   ```bash
   mvn clean package
   ```

### Running the Application

1. Start the application:
   ```bash
   java -jar target/sshexecutor-0.0.1-SNAPSHOT.jar
   ```

2. Open your web browser and navigate to:
   ```
   http://localhost:8080
   ```

## Usage

### Adding a New SSH Configuration

1. Click on "Configurations" in the navigation bar
2. Click "Add New Configuration"
3. Fill in the SSH connection details:
   - Host
   - Port (default: 22)
   - Username
   - Password or SSH key
   - Optional: Default command to execute
4. Click "Save"

### Executing Commands

1. From the dashboard, select a saved configuration
2. Enter the command to execute (or use the default if configured)
3. Click "Execute"
4. View the command output and execution status

### Managing Scripts

1. Navigate to the configuration details
2. Upload a shell script or enter commands directly
3. Save the configuration
4. Execute the script from the dashboard or configuration details page

## Security Considerations

- **Important**: This application stores SSH credentials in an H2 database. In production, consider:
  - Using environment variables or a secure vault for sensitive information
  - Enabling HTTPS
  - Implementing user authentication
  - Using SSH keys instead of passwords when possible

## Configuration

The application uses an embedded H2 database by default. To configure a different database, update the `application.properties` file.

## Contributing

Pull requests are welcome. For major changes, please open an issue first to discuss what you would like to change.

## License

[MIT](https://choosealicense.com/licenses/mit/)