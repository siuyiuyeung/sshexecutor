package igsl.group.automation.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Data
@Configuration
@ConfigurationProperties(prefix = "ssh.execution")
public class ExecutionConfig {
    /**
     * Maximum number of concurrent SSH connections
     */
    private int maxConcurrentConnections = 10;

    /**
     * Connection timeout in milliseconds
     */
    private int connectionTimeout = 30000;

    /**
     * Command execution timeout in milliseconds
     */
    private int executionTimeout = 30000;

    /**
     * Whether to use virtual threads for execution
     */
    private boolean useVirtualThreads = true;
}
