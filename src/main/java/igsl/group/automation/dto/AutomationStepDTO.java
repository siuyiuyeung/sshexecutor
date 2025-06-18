package igsl.group.automation.dto;

import igsl.group.automation.entity.AutomationStep;
import lombok.Data;

@Data
public class AutomationStepDTO {
    private Long id;
    private int order;
    private AutomationStep.StepType type;
    private String selector;
    private String value;
    private int waitSeconds;
    private boolean captureScreenshot;
    private String captureSelector;

    public static AutomationStepDTO fromEntity(AutomationStep step) {
        AutomationStepDTO dto = new AutomationStepDTO();
        dto.setId(step.getId());
        dto.setOrder(step.getOrder());
        dto.setType(step.getType());
        dto.setSelector(step.getSelector());
        dto.setValue(step.getValue());
        dto.setWaitSeconds(step.getWaitSeconds());
        dto.setCaptureScreenshot(step.isCaptureScreenshot());
        dto.setCaptureSelector(step.getCaptureSelector());
        return dto;
    }

    public AutomationStep toEntity() {
        AutomationStep step = new AutomationStep();
        step.setId(this.id);
        step.setOrder(this.order);
        step.setType(this.type);
        step.setSelector(this.selector);
        step.setValue(this.value);
        step.setWaitSeconds(this.waitSeconds);
        step.setCaptureScreenshot(this.captureScreenshot);
        step.setCaptureSelector(this.captureSelector);
        return step;
    }
}
