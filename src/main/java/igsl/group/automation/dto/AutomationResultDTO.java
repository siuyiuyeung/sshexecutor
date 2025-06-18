package igsl.group.automation.dto;

import igsl.group.automation.entity.AutomationResult;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
public class AutomationResultDTO {
    private Long id;
    private Long configId;
    private String configName;
    private String configDescription;
    private AutomationResult.Status status;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private String logs;
    private List<String> screenshotPaths;
    private String errorMessage;
    private List<AutomationStepDTO> steps;

    public static AutomationResultDTO fromEntity(AutomationResult result) {
        AutomationResultDTO dto = new AutomationResultDTO();
        dto.setId(result.getId());
        dto.setConfigId(result.getConfig().getId());
        dto.setConfigName(result.getConfig().getName());
        dto.setConfigDescription(result.getConfig().getDescription());
        dto.setStatus(result.getStatus());
        dto.setStartTime(result.getStartTime());
        dto.setEndTime(result.getEndTime());
        dto.setLogs(result.getLogs());
        dto.setScreenshotPaths(result.getScreenshotPaths());
        dto.setErrorMessage(result.getErrorMessage());

        // Convert steps
        if (result.getConfig().getSteps() != null) {
            dto.setSteps(result.getConfig().getSteps().stream()
                    .map(AutomationStepDTO::fromEntity)
                    .toList());
        }

        return dto;
    }
}