package igsl.group.automation.listener;

import igsl.group.automation.entity.AutomationStep;
import igsl.group.automation.jasypt.JasyptUtils;
import jakarta.persistence.PostLoad;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class AutomationStepEntityListener {

    @PrePersist
    @PreUpdate
    public void encrypt(AutomationStep step) {
        if (step.getType() == AutomationStep.StepType.PASSWORD_INPUT && step.getValue() != null && !step.getValue().isEmpty()) {
            if (!step.isEncrypted()) {
                try {
                    String encryptedValue = JasyptUtils.encrypt(step.getValue());
                    step.setValue(encryptedValue);
                    step.setEncrypted(true);
                    log.debug("Encrypted password value for step: {}", step.getId());
                } catch (Exception e) {
                    log.error("Failed to encrypt password value for step: {}", step.getId(), e);
                }
            }
        }
    }

    @PostLoad
    public void decrypt(AutomationStep step) {
        if (step.getType() == AutomationStep.StepType.PASSWORD_INPUT && step.getValue() != null && !step.getValue().isEmpty() && step.isEncrypted()) {
            try {
                String decryptedValue = JasyptUtils.decrypt(step.getValue());
                step.setValue(decryptedValue);
                step.setEncrypted(false);
                // Don't change the encrypted flag here - it should remain true in the entity
                log.debug("Decrypted password value for step: {}", step.getId());
            } catch (Exception e) {
                log.error("Failed to decrypt password value for step: {}", step.getId(), e);
                step.setValue(""); // Set to empty string if decryption fails
            }
        }
    }
}