package igsl.group.sshexecutor.service;

import igsl.group.sshexecutor.entity.ExecutionResult;
import igsl.group.sshexecutor.entity.SshConfig;
import igsl.group.sshexecutor.repository.ExecutionResultRepository;
import igsl.group.sshexecutor.repository.SshConfigRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class SshConfigService {

    private final SshConfigRepository sshConfigRepository;
    private final ExecutionResultRepository executionResultRepository;
    private final SshService sshService;

    public List<SshConfig> getAllConfigs() {
        return sshConfigRepository.findAll();
    }

    public Optional<SshConfig> getConfigById(Long id) {
        return sshConfigRepository.findById(id);
    }

    public Optional<SshConfig> getConfigByName(String name) {
        return sshConfigRepository.findByName(name);
    }

    @Transactional
    public SshConfig saveConfig(SshConfig config) {
        return sshConfigRepository.save(config);
    }

    @Transactional
    public void deleteConfig(Long id) {
        sshConfigRepository.deleteById(id);
    }

    public boolean configExists(String name) {
        return sshConfigRepository.existsByName(name);
    }

    @Transactional
    public ExecutionResult executeConfig(Long configId) {
        Optional<SshConfig> configOpt = sshConfigRepository.findById(configId);
        if (configOpt.isEmpty()) {
            throw new RuntimeException("Configuration not found with id: " + configId);
        }

        SshConfig config = configOpt.get();
        ExecutionResult result = sshService.executeCommand(config);
        return executionResultRepository.save(result);
    }

    @Transactional
    public List<ExecutionResult> executeAllConfigs() {
        List<SshConfig> configs = sshConfigRepository.findAll();
        return configs.stream()
                .map(config -> {
                    ExecutionResult result = sshService.executeCommand(config);
                    return executionResultRepository.save(result);
                })
                .toList();
    }

    @Transactional
    public List<ExecutionResult> executeSelectedConfigs(List<Long> configIds) {
        return configIds.stream()
                .map(this::executeConfig)
                .toList();
    }

    public List<ExecutionResult> getExecutionHistory(Long configId) {
        Optional<SshConfig> configOpt = sshConfigRepository.findById(configId);
        if (configOpt.isEmpty()) {
            return List.of();
        }
        return executionResultRepository.findBySshConfigOrderByExecutedAtDesc(configOpt.get());
    }

    public List<ExecutionResult> getRecentExecutions() {
        return executionResultRepository.findTop10ByOrderByExecutedAtDesc();
    }

    public boolean testConnection(Long configId) {
        Optional<SshConfig> configOpt = sshConfigRepository.findById(configId);
        return configOpt.map(sshService::testConnection).orElse(false);
    }
}
