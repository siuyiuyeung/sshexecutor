package igsl.group.automation.repository;

import igsl.group.automation.entity.ExecutionResult;
import igsl.group.automation.entity.SshConfig;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ExecutionResultRepository extends JpaRepository<ExecutionResult, Long>, JpaSpecificationExecutor<ExecutionResult> {
    List<ExecutionResult> findBySshConfigOrderByExecutedAtDesc(SshConfig sshConfig);
    List<ExecutionResult> findTop10ByOrderByExecutedAtDesc();
}