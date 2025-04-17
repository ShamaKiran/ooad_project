package com.conference.controller;

import com.conference.model.Conference;
import com.conference.service.ConferenceService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.ExceptionHandler;
import java.util.List;

@Slf4j
@Controller
@RequestMapping("/conference-user")
@RequiredArgsConstructor
public class ConferenceUserController {

    private final ConferenceService conferenceService;

    @GetMapping("/conferences")
    public String listConferences(Model model) {
        try {
            log.debug("Fetching all conferences");
            List<Conference> conferences = conferenceService.getAllConferences();
            log.debug("Found {} conferences", conferences.size());
            model.addAttribute("conferences", conferences);
            return "conference-user/conferences";
        } catch (Exception e) {
            log.error("Error while fetching conferences", e);
            throw e;
        }
    }

    @GetMapping("/conferences/{id}")
    public String viewConference(@PathVariable Long id, Model model) {
        try {
            Conference conference = conferenceService.getConferenceById(id);
            model.addAttribute("conference", conference);
            return "conference-user/conference-detail";
        } catch (Exception e) {
            log.error("Error while fetching conference with id: {}", id, e);
            throw e;
        }
    }

    @ExceptionHandler(Exception.class)
    public String handleError(Exception e, Model model) {
        log.error("An error occurred", e);
        model.addAttribute("error", e.getMessage());
        return "error";
    }
} 