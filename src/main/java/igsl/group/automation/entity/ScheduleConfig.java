package igsl.group.automation.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Data;

@Entity
@Data
@Table(name = "schedule_configs")
public class ScheduleConfig {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Enumerated(EnumType.STRING)
    private ScheduleType type;
    
    @Column(name = "cron_expression")
    private String cronExpression;
    
    @Column(name = "interval_minutes")
    private Integer intervalMinutes;
    
    @Column(name = "run_once_at")
    private String runOnceAt; // ISO datetime string
    
    public enum ScheduleType {
        ONCE, INTERVAL, CRON
    }
} 