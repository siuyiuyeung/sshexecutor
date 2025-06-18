package igsl.group.automation.repository;

import igsl.group.automation.entity.ScheduledTask;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ScheduledTaskRepository extends JpaRepository<ScheduledTask, Long> {
    List<ScheduledTask> findByActive(boolean active);
    List<ScheduledTask> findBySshConfigId(Long sshConfigId);
    boolean existsByNameAndIdNot(String name, Long id);
    boolean existsByName(String name);
}
