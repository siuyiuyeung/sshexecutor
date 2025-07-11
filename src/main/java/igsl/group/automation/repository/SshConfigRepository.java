package igsl.group.automation.repository;

import igsl.group.automation.entity.SshConfig;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface SshConfigRepository extends JpaRepository<SshConfig, Long> {
    Optional<SshConfig> findByName(String name);
    boolean existsByName(String name);
}