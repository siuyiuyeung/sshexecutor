package igsl.group.sshexecutor.config;

import igsl.group.sshexecutor.service.SshConfigService;
import jakarta.annotation.PreDestroy;
import org.springframework.context.annotation.Configuration;

@Configuration
public class SshConfigServiceConfig {

    private final SshConfigService sshConfigService;

    public SshConfigServiceConfig(SshConfigService sshConfigService) {
        this.sshConfigService = sshConfigService;
    }

    @PreDestroy
    public void cleanup() {
        // Shutdown the virtual thread executor when the application stops
        sshConfigService.shutdown();
    }
}
