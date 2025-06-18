package igsl.group.automation.dto;

import igsl.group.automation.entity.AutomationStep;
import igsl.group.automation.entity.ScheduleConfig;
import lombok.Data;

import java.util.List;

@Data
public class AutomationConfigDTO {
    private String name;
    private String description;
    private List<AutomationStep> steps;
    private ScheduleConfig schedule;
    private boolean active;
} 