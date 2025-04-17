package com.conference.controller;

import com.conference.model.Conference;
import com.conference.model.Paper;
import com.conference.model.enums.PaperState;
import com.conference.service.ConferenceService;
import com.conference.service.PaperService;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import org.springframework.ui.Model;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

@Slf4j
@Controller
@RequestMapping("/pc-chair")
@RequiredArgsConstructor
public class PCChairController {

    private final ConferenceService conferenceService;
    private final PaperService paperService;

    @GetMapping("/conferences/create")
    public String createConference() {
        return "pc-chair/create-conference";
    }

    @PostMapping("/conferences/create")
    public String handleCreateConference(@RequestParam String name,
                                       @RequestParam String description,
                                       @RequestParam String startDate,
                                       @RequestParam String endDate) {
        try {
            SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");
            
            Conference conference = new Conference();
            conference.setName(name);
            conference.setDescription(description);
            conference.setStartDate(dateFormat.parse(startDate));
            conference.setEndDate(dateFormat.parse(endDate));
            
            conferenceService.createConference(conference);
            return "redirect:/dashboard";
        } catch (Exception e) {
            return "redirect:/error?message=" + e.getMessage();
        }
    }

    @GetMapping("/decisions")
    public String viewPapersForDecision(Model model) {
        try {
            List<Paper> papers = paperService.getPapersForDecision();
            model.addAttribute("papers", papers);
            return "pc-chair/make-decisions";
        } catch (Exception e) {
            log.error("Error loading papers for decision: {}", e.getMessage());
            model.addAttribute("error", "Error loading papers: " + e.getMessage());
            return "pc-chair/make-decisions";
        }
    }

    @PostMapping("/papers/{id}/decide")
    public String makePaperDecision(@PathVariable Long id,
                                  @RequestParam String decision,
                                  RedirectAttributes redirectAttributes) {
        try {
            PaperState state = PaperState.valueOf(decision.toUpperCase());
            paperService.makePaperDecision(id, state);
            redirectAttributes.addFlashAttribute("success", 
                "Decision (" + decision + ") made successfully for paper #" + id);
        } catch (Exception e) {
            log.error("Error making decision for paper {}: {}", id, e.getMessage());
            redirectAttributes.addFlashAttribute("error", 
                "Error making decision: " + e.getMessage());
        }
        return "redirect:/pc-chair/decisions";
    }
}