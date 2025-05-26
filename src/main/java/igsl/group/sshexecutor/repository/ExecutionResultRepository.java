package igsl.group.sshexecutor.repository;

import igsl.group.sshexecutor.entity.ExecutionResult;
import igsl.group.sshexecutor.entity.SshConfig;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ExecutionResultRepository extends JpaRepository<ExecutionResult, Long> {
    List<ExecutionResult> findBySshConfigOrderByExecutedAtDesc(SshConfig sshConfig);
    List<ExecutionResult> findTop10ByOrderByExecutedAtDesc();
}
