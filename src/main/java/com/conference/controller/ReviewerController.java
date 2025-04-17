package com.conference.controller;

import com.conference.model.Paper;
import com.conference.model.enums.PaperState;  // Add this import
import com.conference.service.PaperService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import java.util.List;

@Slf4j
@Controller
@RequestMapping("/reviewer")
@RequiredArgsConstructor
public class ReviewerController {
    private final PaperService paperService;

    @GetMapping("/papers")
    public String viewPapers(Model model, Authentication authentication) {
        try {
            String username = authentication.getName();
            log.info("Reviewer {} accessing papers", username);
            
            // Get all papers that need review
            List<Paper> allPapers = paperService.getAllSubmittedPapers();
            
            // Add reviewer-specific information
            model.addAttribute("papers", allPapers);
            model.addAttribute("currentReviewer", username);
            
            log.info("Found {} papers for reviewer {}", allPapers.size(), username);
            
            return "reviewer/papers";
        } catch (Exception e) {
            log.error("Error in viewPapers for {}: {}", authentication.getName(), e.getMessage());
            model.addAttribute("error", "Error loading papers: " + e.getMessage());
            return "reviewer/papers";
        }
    }

    @GetMapping("/papers/{id}/review")
    public String reviewPaper(@PathVariable Long id, Model model, Authentication authentication) {
        try {
            Paper paper = paperService.getPaperById(id);
            
            // Check if paper exists and is available for review
            if (paper == null) {
                throw new RuntimeException("Paper not found");
            }
            
            // Check if the reviewer has already reviewed this paper
            if (paper.getReviews() != null && 
                paper.getReviews().stream()
                    .anyMatch(r -> r.getReviewer().getUsername().equals(authentication.getName()))) {
                throw new RuntimeException("You have already reviewed this paper");
            }
            
            model.addAttribute("paper", paper);
            return "reviewer/review-paper";
        } catch (Exception e) {
            log.error("Error accessing paper for review: {}", e.getMessage());
            model.addAttribute("error", e.getMessage());
            return "redirect:/reviewer/papers";
        }
    }

    @PostMapping("/papers/{id}/submit-review")
    public String submitReview(
            @PathVariable Long id,
            @RequestParam String comments,
            @RequestParam int score,
            Authentication authentication,
            RedirectAttributes redirectAttributes) {
        try {
            if (authentication == null) {
                throw new RuntimeException("User not authenticated");
            }
            
            String username = authentication.getName();
            log.info("Submitting review for paper {} by reviewer {}", id, username);
            
            paperService.submitReview(id, username, comments, score);
            
            redirectAttributes.addFlashAttribute("success", "Review submitted successfully!");
            return "redirect:/reviewer/papers";
            
        } catch (Exception e) {
            log.error("Error submitting review: {}", e.getMessage());
            redirectAttributes.addFlashAttribute("error", "Failed to submit review: " + e.getMessage());
            return "redirect:/reviewer/papers/" + id + "/review";
        }
    }

    @GetMapping("/reviews")
    public String viewMyReviews(Model model, Authentication authentication) {
        try {
            String username = authentication.getName();
            log.info("Reviewer {} accessing their reviews", username);
            
            // Get papers reviewed by this reviewer using the correct method
            List<Paper> reviewedPapers = paperService.getPapersByReviewer(username);
            
            model.addAttribute("papers", reviewedPapers);
            log.info("Found {} papers reviewed by {}", reviewedPapers.size(), username);
            
            return "reviewer/my-reviews";
        } catch (Exception e) {
            log.error("Error in viewMyReviews for {}: {}", authentication.getName(), e.getMessage());
            model.addAttribute("error", "Error loading reviews: " + e.getMessage());
            return "reviewer/my-reviews";
        }
    }
}