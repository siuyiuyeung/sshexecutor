package igsl.group.automation.controller;

import igsl.group.automation.dto.AutomationResultDTO;
import igsl.group.automation.entity.AutomationResult;
import igsl.group.automation.repository.AutomationResultRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;

@RestController
@RequestMapping("/api/history")
@RequiredArgsConstructor
public class HistoryController {

    private final AutomationResultRepository resultRepository;

    @GetMapping
    public Page<AutomationResultDTO> getHistory(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) Long configId,
            @RequestParam(required = false) AutomationResult.Status status,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endDate) {

        PageRequest pageRequest = PageRequest.of(page, size, Sort.by("startTime").descending());
        Page<AutomationResult> results;

        if (configId != null) {
            results = resultRepository.findByConfigId(configId, pageRequest);
        } else if (status != null) {
            results = resultRepository.findByStatus(status, pageRequest);
        } else if (startDate != null && endDate != null) {
            results = resultRepository.findByStartTimeBetween(startDate, endDate, pageRequest);
        } else {
            results = resultRepository.findAll(pageRequest);
        }

        return results.map(AutomationResultDTO::fromEntity);
    }

    @GetMapping("/{id}")
    public ResponseEntity<AutomationResultDTO> getResult(@PathVariable Long id) {
        return resultRepository.findById(id)
                .map(result -> ResponseEntity.ok(AutomationResultDTO.fromEntity(result)))
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/{id}/screenshot/{index}")
    public ResponseEntity<byte[]> getScreenshot(@PathVariable Long id, @PathVariable int index) {
        return resultRepository.findById(id)
                .map(result -> {
                    if (result.getScreenshotPaths() != null &&
                            index >= 0 &&
                            index < result.getScreenshotPaths().size()) {
                        try {
                            Path path = Paths.get(result.getScreenshotPaths().get(index));
                            byte[] image = Files.readAllBytes(path);
                            return ResponseEntity.ok()
                                    .header("Content-Type", "image/png")
                                    .body(image);
                        } catch (IOException e) {
                            return ResponseEntity.notFound().<byte[]>build();
                        }
                    }
                    return ResponseEntity.notFound().<byte[]>build();
                })
                .orElse(ResponseEntity.notFound().build());
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteResult(@PathVariable Long id) {
        if (resultRepository.existsById(id)) {
            resultRepository.deleteById(id);
            return ResponseEntity.ok().build();
        }
        return ResponseEntity.notFound().build();
    }
}