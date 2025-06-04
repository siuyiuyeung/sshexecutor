package igsl.group.sshexecutor.controller;

import igsl.group.sshexecutor.entity.ScheduledTask;
import igsl.group.sshexecutor.service.ScheduleService;
import igsl.group.sshexecutor.service.SshConfigService;
import jakarta.validation.Valid;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/schedules")
public class ScheduleController {

    private final ScheduleService scheduleService;
    private final SshConfigService sshConfigService;

    public ScheduleController(ScheduleService scheduleService, SshConfigService sshConfigService) {
        this.scheduleService = scheduleService;
        this.sshConfigService = sshConfigService;
    }

    @GetMapping
    public String listSchedules(Model model) {
        model.addAttribute("schedules", scheduleService.getAllScheduledTasks());
        return "schedules/list";
    }

    @GetMapping("/new")
    public String newSchedule(Model model) {
        model.addAttribute("schedule", new ScheduledTask());
        model.addAttribute("configs", sshConfigService.getAllConfigs());
        return "schedules/form";
    }

    @GetMapping("/{id}/edit")
    public String editSchedule(@PathVariable Long id, Model model) {
        model.addAttribute("schedule", scheduleService.getScheduledTask(id));
        model.addAttribute("configs", sshConfigService.getAllConfigs());
        return "schedules/form";
    }

    @PostMapping("/save")
    public String saveSchedule(@Valid @ModelAttribute("schedule") ScheduledTask schedule,
                               BindingResult result,
                               RedirectAttributes redirectAttributes,
                               Model model) {
        if (result.hasErrors()) {
            model.addAttribute("configs", sshConfigService.getAllConfigs());
            return "schedules/form";
        }

        try {
            if (schedule.getId() == null) {
                scheduleService.createScheduledTask(schedule);
                redirectAttributes.addFlashAttribute("successMessage", "Schedule created successfully!");
            } else {
                scheduleService.updateScheduledTask(schedule.getId(), schedule);
                redirectAttributes.addFlashAttribute("successMessage", "Schedule updated successfully!");
            }
            return "redirect:/schedules";
        } catch (Exception e) {
            model.addAttribute("configs", sshConfigService.getAllConfigs());
            model.addAttribute("errorMessage", "Error: " + e.getMessage());
            return "schedules/form";
        }
    }

    @PostMapping("/{id}/delete")
    public String deleteSchedule(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        try {
            scheduleService.deleteScheduledTask(id);
            redirectAttributes.addFlashAttribute("successMessage", "Schedule deleted successfully!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Error deleting schedule: " + e.getMessage());
        }
        return "redirect:/schedules";
    }

    @PostMapping("/{id}/toggle")
    @ResponseBody
    public void toggleSchedule(@PathVariable Long id) {
        scheduleService.toggleTaskStatus(id);
    }

    @PostMapping("/{id}/execute")
    @ResponseBody
    public void executeSchedule(@PathVariable Long id) {
        scheduleService.executeTaskManually(id);
    }

    @GetMapping("/validate-cron")
    @ResponseBody
    public boolean validateCron(@RequestParam String expression) {
        try {
            org.springframework.scheduling.support.CronExpression.parse(expression);
            return true;
        } catch (Exception e) {
            return false;
        }
    }
}