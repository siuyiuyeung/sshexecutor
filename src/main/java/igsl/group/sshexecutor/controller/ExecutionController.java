package igsl.group.sshexecutor.controller;

import igsl.group.sshexecutor.entity.ExecutionResult;
import igsl.group.sshexecutor.service.ExecutionService;
import igsl.group.sshexecutor.service.SshConfigService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Controller
@RequestMapping("/executions")
@RequiredArgsConstructor
public class ExecutionController {
    private final ExecutionService executionService;
    private final SshConfigService sshConfigService;

    @GetMapping
    public String listExecutions(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) Long configId,
            @RequestParam(required = false) Boolean status,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime dateFrom,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime dateTo,
            Model model) {

        PageRequest pageRequest = PageRequest.of(page, size, Sort.by("executedAt").descending());
        Page<ExecutionResult> resultPage = executionService.findExecutions(configId, status, dateFrom, dateTo, pageRequest);

        model.addAttribute("results", resultPage.getContent());
        model.addAttribute("currentPage", resultPage.getNumber());
        model.addAttribute("totalPages", resultPage.getTotalPages());
        model.addAttribute("totalElements", resultPage.getTotalElements());
        model.addAttribute("pageSize", size);
        model.addAttribute("configs", sshConfigService.getAllConfigs());

        // Preserve filter parameters
        model.addAttribute("configId", configId);
        model.addAttribute("status", status != null ? status.toString() : "");
        model.addAttribute("dateFrom", dateFrom);
        model.addAttribute("dateTo", dateTo);

        return "executions/list";
    }

    @PostMapping("/{id}/delete")
    @ResponseBody
    public ResponseEntity<Void> deleteExecution(@PathVariable Long id) {
        executionService.deleteExecution(id);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/delete-batch")
    @ResponseBody
    public ResponseEntity<Void> deleteExecutions(@RequestBody List<Long> ids) {
        executionService.deleteExecutions(ids);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/export")
    public ResponseEntity<String> exportExecutions(
            @RequestParam(required = false) Long configId,
            @RequestParam(required = false) Boolean status,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime dateFrom,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime dateTo) {

        String csv = executionService.exportToCsv(configId, status, dateFrom, dateTo);

        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
        String filename = timestamp + ".csv";

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                .contentType(MediaType.TEXT_PLAIN)
                .body(csv);
    }
}
