package igsl.group.sshexecutor.controller;

import igsl.group.sshexecutor.entity.ExecutionResult;
import igsl.group.sshexecutor.entity.SshConfig;
import igsl.group.sshexecutor.service.ImportExportService;
import igsl.group.sshexecutor.service.SshConfigService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.io.IOException;
import java.util.List;

@Controller
@RequiredArgsConstructor
public class SshConfigController {

    private final SshConfigService sshConfigService;
    private final ImportExportService importExportService;

    @GetMapping("/")
    public String index() {
        return "redirect:/dashboard";
    }

    @GetMapping("/dashboard")
    public String dashboard(Model model) {
        model.addAttribute("configs", sshConfigService.getAllConfigs());
        model.addAttribute("recentExecutions", sshConfigService.getRecentExecutions());
        return "dashboard";
    }

    @GetMapping("/configs")
    public String listConfigs(Model model) {
        model.addAttribute("configs", sshConfigService.getAllConfigs());
        return "configs/list";
    }

    @GetMapping("/configs/new")
    public String newConfig(Model model) {
        model.addAttribute("config", new SshConfig());
        return "configs/form";
    }

    @GetMapping("/configs/{id}/edit")
    public String editConfig(@PathVariable Long id, Model model) {
        sshConfigService.getConfigById(id).ifPresent(config ->
                model.addAttribute("config", config)
        );
        return "configs/form";
    }

    @PostMapping("/configs/save")
    public String saveConfig(@Valid @ModelAttribute("config") SshConfig config,
                             BindingResult result,
                             RedirectAttributes redirectAttributes) {
        if (result.hasErrors()) {
            return "configs/form";
        }

        try {
            sshConfigService.saveConfig(config);
            redirectAttributes.addFlashAttribute("successMessage", "Configuration saved successfully!");
            return "redirect:/configs";
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Error saving configuration: " + e.getMessage());
            return "redirect:/configs";
        }
    }

    @PostMapping("/configs/{id}/delete")
    public String deleteConfig(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        try {
            sshConfigService.deleteConfig(id);
            redirectAttributes.addFlashAttribute("successMessage", "Configuration deleted successfully!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Error deleting configuration: " + e.getMessage());
        }
        return "redirect:/configs";
    }

    @PostMapping("/configs/{id}/execute")
    @ResponseBody
    public ExecutionResult executeConfig(@PathVariable Long id) {
        return sshConfigService.executeConfig(id);
    }

    @PostMapping("/configs/execute-all")
    @ResponseBody
    public List<ExecutionResult> executeAllConfigs() {
        return sshConfigService.executeAllConfigs();
    }

    @PostMapping("/configs/execute-selected")
    @ResponseBody
    public List<ExecutionResult> executeSelectedConfigs(@RequestBody List<Long> configIds) {
        return sshConfigService.executeSelectedConfigs(configIds);
    }

    @GetMapping("/configs/{id}/test")
    @ResponseBody
    public boolean testConnection(@PathVariable Long id) {
        return sshConfigService.testConnection(id);
    }

    @GetMapping("/configs/{id}/history")
    public String viewHistory(@PathVariable Long id, Model model) {
        sshConfigService.getConfigById(id).ifPresent(config -> {
            model.addAttribute("config", config);
            model.addAttribute("history", sshConfigService.getExecutionHistory(id));
        });
        return "configs/history";
    }

    @GetMapping("/results/{id}")
    @ResponseBody
    public ExecutionResult getExecutionResult(@PathVariable Long id) {
        return sshConfigService.getExecutionResult(id);
    }

    @GetMapping("/configs/export")
    public ResponseEntity<byte[]> exportConfigs(@RequestParam(required = false) List<Long> ids) {
        try {
            byte[] zipData = importExportService.exportConfigurations(ids);

            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"ssh-configs-export.zip\"")
                    .contentType(MediaType.APPLICATION_OCTET_STREAM)
                    .body(zipData);
        } catch (IOException e) {
            return ResponseEntity.internalServerError().build();
        }
    }

    @GetMapping("/configs/import")
    public String showImportForm() {
        return "configs/import";
    }

    @PostMapping("/configs/import")
    public String importConfigs(@RequestParam("file") MultipartFile file,
                                @RequestParam(defaultValue = "false") boolean overwrite,
                                RedirectAttributes redirectAttributes) {
        if (file.isEmpty()) {
            redirectAttributes.addFlashAttribute("errorMessage", "Please select a file to import");
            return "redirect:/configs/import";
        }

        ImportExportService.ImportResult result = importExportService.importConfigurations(file, overwrite);

        if (result.hasErrors()) {
            redirectAttributes.addFlashAttribute("errorMessage",
                    "Import completed with errors: " + String.join(", ", result.getErrors()));
        } else {
            StringBuilder message = new StringBuilder("Import successful: ");
            if (!result.getCreated().isEmpty()) {
                message.append(result.getCreated().size()).append(" created, ");
            }
            if (!result.getUpdated().isEmpty()) {
                message.append(result.getUpdated().size()).append(" updated, ");
            }
            if (!result.getSkipped().isEmpty()) {
                message.append(result.getSkipped().size()).append(" skipped");
            }
            redirectAttributes.addFlashAttribute("successMessage", message.toString());
        }

        redirectAttributes.addFlashAttribute("importResult", result);
        return "redirect:/configs/import";
    }
}
