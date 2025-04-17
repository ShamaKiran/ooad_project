package com.conference.service.impl;

import com.conference.model.*;
import com.conference.model.enums.PaperState;
import com.conference.repository.ConferenceRepository;
import com.conference.repository.PaperRepository;
import com.conference.repository.UserRepository;
import com.conference.service.PaperService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.Date;
import java.util.List;
import java.util.HashSet;

// Add this import at the top
import java.util.Collection;

@Slf4j
@Service
@RequiredArgsConstructor
public class PaperServiceImpl implements PaperService {
    private final PaperRepository paperRepository;
    private final UserRepository userRepository;
    private final ConferenceRepository conferenceRepository;

    @Override
    @Transactional
    public Paper submitPaper(Paper paper, String username, Long conferenceId) {
        User author = userRepository.findByUsername(username)
            .orElseThrow(() -> new RuntimeException("User not found"));
            
        Conference conference = conferenceRepository.findById(conferenceId)
            .orElseThrow(() -> new RuntimeException("Conference not found"));
            
        paper.setConference(conference);
        paper.setState(PaperState.SUBMITTED);
        paper.setSubmissionDate(new Date());
        paper.setVersion(1);
        
        Author authorEntity = new Author();
        authorEntity.setUser(author);
        authorEntity.setPaper(paper);
        
        if (paper.getAuthors() == null) {
            paper.setAuthors(new HashSet<>());
        }
        paper.getAuthors().add(authorEntity);
        
        Paper savedPaper = paperRepository.save(paper);
        log.info("Paper submitted successfully: {}", savedPaper.getId());
        return savedPaper;
    }

    @Override
    public Paper getPaperById(Long id) {
        return paperRepository.findById(id)
            .orElseThrow(() -> new RuntimeException("Paper not found"));
    }

    @Override
    public List<Paper> getPapersByAuthor(String username) {
        return paperRepository.findByAuthors_User_Username(username);
    }

    @Override
    public List<Paper> getAllSubmittedPapers() {
        // Get papers in both SUBMITTED and UNDER_REVIEW states
        List<Paper> papers = paperRepository.findByStateIn(List.of(PaperState.SUBMITTED, PaperState.UNDER_REVIEW));
        log.info("Found {} papers in SUBMITTED or UNDER_REVIEW state", papers.size());
        return papers;
    }

    @Override
    public List<Paper> getPapersByReviewer(String username) {
        return paperRepository.findByReviews_Reviewer_Username(username);
    }

    @Override
    public List<Paper> getPapersAssignedToReviewer(String username) {
        try {
            // Get all papers in SUBMITTED state
            List<Paper> submittedPapers = paperRepository.findByState(PaperState.SUBMITTED);
            log.info("Found {} papers in SUBMITTED state", submittedPapers.size());
            
            // Filter out papers that this reviewer has already reviewed
            List<Paper> availablePapers = submittedPapers.stream()
                .filter(paper -> {
                    // If paper has no reviews, it's available
                    if (paper.getReviews() == null || paper.getReviews().isEmpty()) {
                        return true;
                    }
                    
                    // Check if current reviewer hasn't reviewed this paper
                    return paper.getReviews().stream()
                        .noneMatch(review -> 
                            review.getReviewer() != null && 
                            review.getReviewer().getUsername().equals(username));
                })
                .toList();
            
            log.info("Found {} papers available for reviewer {}", availablePapers.size(), username);
            return availablePapers;
            
        } catch (Exception e) {
            log.error("Error getting papers for reviewer {}: {}", username, e.getMessage());
            throw new RuntimeException("Failed to retrieve papers for review", e);
        }
    }

    @Override
    @Transactional
    public void submitReview(Long paperId, String reviewerUsername, String comments, int score) {
        Paper paper = getPaperById(paperId);
        User reviewer = userRepository.findByUsername(reviewerUsername)
            .orElseThrow(() -> new RuntimeException("Reviewer not found"));

        // Check if reviewer has already reviewed this paper
        if (paper.getReviews() != null && 
            paper.getReviews().stream()
                .anyMatch(r -> r.getReviewer().getUsername().equals(reviewerUsername))) {
            throw new RuntimeException("You have already reviewed this paper");
        }

        Review review = new Review();
        review.setPaper(paper);
        review.setReviewer(reviewer);
        review.setComments(comments);
        review.setScore(score);
        review.setSubmissionDate(new Date());

        if (paper.getReviews() == null) {
            paper.setReviews(new HashSet<>());
        }
        paper.getReviews().add(review);
        
        // Calculate average score
        double averageScore = paper.getReviews().stream()
            .mapToInt(Review::getScore)
            .average()
            .orElse(score);
        paper.setAverageScore(averageScore);
        
        // Update paper state
        if (paper.getState() == PaperState.SUBMITTED) {
            paper.setState(PaperState.UNDER_REVIEW);
        }
        
        paperRepository.save(paper);
        log.info("Review submitted for paper {}. Average score: {}", paperId, averageScore);
    }

    @Override
    public List<Paper> getPapersForDecision() {
        // Get papers that are under review or reviewed
        List<Paper> papers = paperRepository.findByStateIn(List.of(PaperState.UNDER_REVIEW));
        
        // Filter papers that have at least one review
        return papers.stream()
            .filter(paper -> paper.getReviews() != null && !paper.getReviews().isEmpty())
            .toList();
    }

    @Override
    @Transactional
    public void makePaperDecision(Long paperId, PaperState decision) {
        if (decision != PaperState.ACCEPTED && decision != PaperState.REJECTED) {
            throw new IllegalArgumentException("Decision must be either ACCEPTED or REJECTED");
        }

        Paper paper = getPaperById(paperId);
        
        // Ensure paper has reviews
        if (paper.getReviews() == null || paper.getReviews().isEmpty()) {
            throw new RuntimeException("Cannot make decision on paper without reviews");
        }
        
        paper.setState(decision);
        paperRepository.save(paper);
        log.info("Decision made for paper {}: {}", paperId, decision);
    }
}