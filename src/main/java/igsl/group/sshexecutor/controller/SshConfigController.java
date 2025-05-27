package igsl.group.sshexecutor.controller;

import igsl.group.sshexecutor.entity.ExecutionResult;
import igsl.group.sshexecutor.entity.SshConfig;
import igsl.group.sshexecutor.service.SshConfigService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;

@Controller
@RequiredArgsConstructor
public class SshConfigController {

    private final SshConfigService sshConfigService;

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
}
