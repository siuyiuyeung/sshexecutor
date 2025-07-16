package igsl.group.automation.entity;

import com.fasterxml.jackson.annotation.JsonBackReference;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.EntityListeners;
import igsl.group.automation.listener.AutomationStepEntityListener;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

@Entity
@Data
@Table(name = "automation_steps")
@EqualsAndHashCode(exclude = {"config"})
@ToString(exclude = {"config"})
@EntityListeners(AutomationStepEntityListener.class)
public class AutomationStep {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "step_order")
    private int order;

    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    private StepType type;

    @Column(name = "selector")
    private String selector; // CSS selector or XPath

    @Column(name = "input_value")
    private String value; // Input value or URL

    @Column(name = "wait_seconds")
    private int waitSeconds = 0;

    @Column(name = "capture_screenshot")
    private boolean captureScreenshot = false;

    @Column(name = "capture_selector")
    private String captureSelector; // Specific area to capture

    @Column(name = "is_encrypted")
    private boolean encrypted = false; // Flag to indicate if value is encrypted

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "config_id")
    @JsonBackReference
    private AutomationConfig config;

    public enum StepType {
        NAVIGATE, CLICK, INPUT, PASSWORD_INPUT, WAIT, SCREENSHOT, SCROLL, SELECT
    }
}