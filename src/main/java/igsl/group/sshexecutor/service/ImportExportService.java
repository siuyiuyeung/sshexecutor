package igsl.group.sshexecutor.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import igsl.group.sshexecutor.dto.SshConfigDto;
import igsl.group.sshexecutor.entity.SshConfig;
import igsl.group.sshexecutor.repository.SshConfigRepository;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

@Service
@Slf4j
@RequiredArgsConstructor
public class ImportExportService {
    private final SshConfigRepository sshConfigRepository;
    private final ObjectMapper objectMapper;

    public byte[] exportConfigurations(List<Long> configIds) throws IOException {
        List<SshConfig> configs;

        if (configIds == null || configIds.isEmpty()) {
            configs = sshConfigRepository.findAll();
        } else {
            configs = sshConfigRepository.findAllById(configIds);
        }

        // Convert to DTOs (excluding sensitive data if needed)
        List<SshConfigDto> dtos = configs.stream()
                .map(this::toDto)
                .toList();

        // Create JSON
        String json = objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(dtos);

        // Create ZIP file with JSON
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try (ZipOutputStream zos = new ZipOutputStream(baos)) {
            ZipEntry entry = new ZipEntry("ssh-configs.json");
            zos.putNextEntry(entry);
            zos.write(json.getBytes(StandardCharsets.UTF_8));
            zos.closeEntry();
        }

        return baos.toByteArray();
    }

    @Transactional
    public ImportResult importConfigurations(MultipartFile file, boolean overwrite) {
        ImportResult result = new ImportResult();

        try {
            List<SshConfigDto> dtos = parseImportFile(file);

            for (SshConfigDto dto : dtos) {
                try {
                    processConfigImport(dto, overwrite, result);
                } catch (Exception e) {
                    log.error("Error importing config: " + dto.getName(), e);
                    result.addError("Failed to import '" + dto.getName() + "': " + e.getMessage());
                }
            }

        } catch (Exception e) {
            log.error("Error parsing import file", e);
            result.addError("Failed to parse import file: " + e.getMessage());
        }

        return result;
    }

    private List<SshConfigDto> parseImportFile(MultipartFile file) throws IOException {
        if (file.getOriginalFilename() != null && file.getOriginalFilename().endsWith(".zip")) {
            // Handle ZIP file
            try (ZipInputStream zis = new ZipInputStream(file.getInputStream())) {
                ZipEntry entry;
                while ((entry = zis.getNextEntry()) != null) {
                    if (entry.getName().equals("ssh-configs.json")) {
                        byte[] buffer = zis.readAllBytes();
                        String json = new String(buffer, StandardCharsets.UTF_8);
                        return objectMapper.readValue(json, new TypeReference<List<SshConfigDto>>() {});
                    }
                }
                throw new IOException("No ssh-configs.json found in ZIP file");
            }
        } else {
            // Handle JSON file directly
            String json = new String(file.getBytes(), StandardCharsets.UTF_8);
            return objectMapper.readValue(json, new TypeReference<List<SshConfigDto>>() {});
        }
    }

    private void processConfigImport(SshConfigDto dto, boolean overwrite, ImportResult result) {
        // Validate DTO
        if (dto.getName() == null || dto.getName().trim().isEmpty()) {
            result.addError("Configuration name is required");
            return;
        }

        // Check if config already exists
        boolean exists = sshConfigRepository.existsByName(dto.getName());

        if (exists && !overwrite) {
            result.addSkipped(dto.getName() + " (already exists)");
            return;
        }

        // Create or update config
        SshConfig config;
        if (exists) {
            config = sshConfigRepository.findByName(dto.getName()).orElseThrow();
            result.addUpdated(dto.getName());
        } else {
            config = new SshConfig();
            result.addCreated(dto.getName());
        }

        // Update fields
        config.setName(dto.getName());
        config.setHost(dto.getHost());
        config.setPort(dto.getPort() != null ? dto.getPort() : 22);
        config.setUsername(dto.getUsername());
        config.setAuthType(dto.getAuthType() != null ? dto.getAuthType() : SshConfig.AuthType.PASSWORD);
        config.setPassword(dto.getPassword());
        config.setPrivateKey(dto.getPrivateKey());
        config.setPassphrase(dto.getPassphrase());
        config.setCommand(dto.getCommand());
        config.setScript(dto.getScript());
        config.setDescription(dto.getDescription());

        sshConfigRepository.save(config);
    }

    private SshConfigDto toDto(SshConfig config) {
        SshConfigDto dto = new SshConfigDto();
        dto.setName(config.getName());
        dto.setHost(config.getHost());
        dto.setPort(config.getPort());
        dto.setUsername(config.getUsername());
        dto.setAuthType(config.getAuthType());
        dto.setPassword(config.getPassword());
        dto.setPrivateKey(config.getPrivateKey());
        dto.setPassphrase(config.getPassphrase());
        dto.setCommand(config.getCommand());
        dto.setScript(config.getScript());
        dto.setDescription(config.getDescription());
        return dto;
    }

    @Data
    public static class ImportResult {
        private List<String> created = new ArrayList<>();
        private List<String> updated = new ArrayList<>();
        private List<String> skipped = new ArrayList<>();
        private List<String> errors = new ArrayList<>();

        public void addCreated(String name) { created.add(name); }
        public void addUpdated(String name) { updated.add(name); }
        public void addSkipped(String name) { skipped.add(name); }
        public void addError(String error) { errors.add(error); }

        public int getTotalProcessed() {
            return created.size() + updated.size() + skipped.size();
        }

        public boolean hasErrors() {
            return !errors.isEmpty();
        }
    }
}
