package igsl.group.automation.service;

import igsl.group.automation.entity.ExecutionResult;
import igsl.group.automation.repository.ExecutionResultRepository;
import jakarta.persistence.criteria.Predicate;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ExecutionService {
    private final ExecutionResultRepository executionResultRepository;
    public Page<ExecutionResult> findExecutions(Long configId, Boolean status,
                                                LocalDateTime dateFrom, LocalDateTime dateTo,
                                                Pageable pageable) {
        Specification<ExecutionResult> spec = createSpecification(configId, status, dateFrom, dateTo);
        return executionResultRepository.findAll(spec, pageable);
    }

    private Specification<ExecutionResult> createSpecification(Long configId, Boolean status,
                                                               LocalDateTime dateFrom, LocalDateTime dateTo) {
        return (root, query, criteriaBuilder) -> {
            List<Predicate> predicates = new ArrayList<>();

            if (configId != null) {
                predicates.add(criteriaBuilder.equal(root.get("sshConfig").get("id"), configId));
            }

            if (status != null) {
                predicates.add(criteriaBuilder.equal(root.get("success"), status));
            }

            if (dateFrom != null) {
                predicates.add(criteriaBuilder.greaterThanOrEqualTo(root.get("executedAt"), dateFrom));
            }

            if (dateTo != null) {
                predicates.add(criteriaBuilder.lessThanOrEqualTo(root.get("executedAt"), dateTo));
            }

            return criteriaBuilder.and(predicates.toArray(new Predicate[0]));
        };
    }

    @Transactional
    public void deleteExecution(Long id) {
        executionResultRepository.deleteById(id);
    }

    @Transactional
    public void deleteExecutions(List<Long> ids) {
        executionResultRepository.deleteAllById(ids);
    }

    public String exportToCsv(Long configId, Boolean status, LocalDateTime dateFrom, LocalDateTime dateTo) {
        Specification<ExecutionResult> spec = createSpecification(configId, status, dateFrom, dateTo);
        List<ExecutionResult> results = executionResultRepository.findAll(spec);

        StringBuilder csv = new StringBuilder();
        csv.append("ID,Configuration,Host,Command,Status,Exit Code,Execution Time (ms),Executed At,Output,Error\n");

        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

        for (ExecutionResult result : results) {
            csv.append(result.getId()).append(",");
            csv.append(escapeCSV(result.getSshConfig().getName())).append(",");
            csv.append(escapeCSV(result.getSshConfig().getHost())).append(",");
            csv.append(escapeCSV(result.getCommand())).append(",");
            csv.append(result.getSuccess() ? "Success" : "Failed").append(",");
            csv.append(result.getExitCode()).append(",");
            csv.append(result.getExecutionTime()).append(",");
            csv.append(result.getExecutedAt().format(formatter)).append(",");
            csv.append(escapeCSV(result.getOutput())).append(",");
            csv.append(escapeCSV(result.getError())).append("\n");
        }

        return csv.toString();
    }

    private String escapeCSV(String value) {
        if (value == null) return "";
        if (value.contains(",") || value.contains("\"") || value.contains("\n")) {
            return "\"" + value.replace("\"", "\"\"") + "\"";
        }
        return value;
    }
}
