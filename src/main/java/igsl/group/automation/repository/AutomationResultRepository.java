package igsl.group.automation.repository;

import igsl.group.automation.entity.AutomationResult;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;

@Repository
public interface AutomationResultRepository extends JpaRepository<AutomationResult, Long> {
    Page<AutomationResult> findByConfigId(Long configId, Pageable pageable);
    Page<AutomationResult> findByStartTimeBetween(LocalDateTime start, LocalDateTime end, Pageable pageable);
    Page<AutomationResult> findByStatus(AutomationResult.Status status, Pageable pageable);
    long countByConfigId(Long configId);
    void deleteByConfigId(Long configId);
}