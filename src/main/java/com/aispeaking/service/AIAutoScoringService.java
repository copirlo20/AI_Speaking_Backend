package com.aispeaking.service;

import com.aispeaking.dto.AIScoreResponse;
import com.aispeaking.model.Question;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * Advanced AI Scoring Service with detailed evaluation algorithms
 */
@Service
@RequiredArgsConstructor
public class AIAutoScoringService {

    @Value("${ai.service.url:http://localhost:5000}")
    private String aiServiceUrl;

    private final WebClient.Builder webClientBuilder;

    // Common filler words
    private static final Set<String> FILLER_WORDS = Set.of(
        "um", "uh", "er", "ah", "like", "you know", "sort of", "kind of",
        "basically", "actually", "literally", "well", "so", "I mean"
    );

    // Common grammar patterns to detect errors
    private static final Pattern SUBJECT_VERB_AGREEMENT = Pattern.compile(
        "\\b(he|she|it)\\s+(are|were|have|do)\\b", Pattern.CASE_INSENSITIVE
    );
    
    /**
     * Main method to automatically score speaking response
     */
    public AIScoreResponse autoScoreResponse(String transcription, Question question, Integer durationSeconds) {
        AIScoreResponse response = new AIScoreResponse();
        
        try {
            // Try to get AI-based evaluation first
            response = callAIService(transcription, question, durationSeconds);
        } catch (Exception e) {
            // Fallback to rule-based evaluation if AI service is unavailable
            response = ruleBasedEvaluation(transcription, question, durationSeconds);
        }
        
        // Calculate overall score
        response.setOverallScore(calculateOverallScore(response));
        
        // Generate comprehensive feedback
        response.setFeedback(generateDetailedFeedback(response));
        response.setSuggestions(generateSuggestions(response));
        
        return response;
    }

    /**
     * Call external AI service for evaluation
     */
    private AIScoreResponse callAIService(String transcription, Question question, Integer durationSeconds) {
        try {
            WebClient webClient = webClientBuilder.baseUrl(aiServiceUrl).build();

            Map<String, Object> request = new HashMap<>();
            request.put("transcription", transcription);
            request.put("question", question.getQuestionText());
            request.put("sample_answer", question.getSampleAnswer());
            request.put("duration", durationSeconds);
            request.put("difficulty", question.getDifficulty().name());

            Map<String, Object> apiResponse = webClient.post()
                .uri("/api/evaluate")
                .bodyValue(request)
                .retrieve()
                .bodyToMono(Map.class)
                .block();

            if (apiResponse != null) {
                AIScoreResponse response = new AIScoreResponse();
                response.setPronunciationScore(getDoubleValue(apiResponse, "pronunciation"));
                response.setFluencyScore(getDoubleValue(apiResponse, "fluency"));
                response.setGrammarScore(getDoubleValue(apiResponse, "grammar"));
                response.setVocabularyScore(getDoubleValue(apiResponse, "vocabulary"));
                response.setContentScore(getDoubleValue(apiResponse, "content"));
                return response;
            }
        } catch (Exception e) {
            // Fall through to rule-based evaluation
        }
        
        throw new RuntimeException("AI service unavailable");
    }

    /**
     * Rule-based evaluation when AI service is not available
     */
    private AIScoreResponse ruleBasedEvaluation(String transcription, Question question, Integer durationSeconds) {
        AIScoreResponse response = new AIScoreResponse();
        
        // Evaluate pronunciation (based on transcription quality)
        AIScoreResponse.PronunciationDetails pronDetails = evaluatePronunciation(transcription);
        response.setPronunciationDetails(pronDetails);
        response.setPronunciationScore(calculatePronunciationScore(pronDetails));
        
        // Evaluate fluency
        AIScoreResponse.FluencyDetails fluencyDetails = evaluateFluency(transcription, durationSeconds);
        response.setFluencyDetails(fluencyDetails);
        response.setFluencyScore(calculateFluencyScore(fluencyDetails));
        
        // Evaluate grammar
        AIScoreResponse.GrammarDetails grammarDetails = evaluateGrammar(transcription);
        response.setGrammarDetails(grammarDetails);
        response.setGrammarScore(calculateGrammarScore(grammarDetails));
        
        // Evaluate vocabulary
        AIScoreResponse.VocabularyDetails vocabDetails = evaluateVocabulary(transcription, question.getDifficulty().name());
        response.setVocabularyDetails(vocabDetails);
        response.setVocabularyScore(calculateVocabularyScore(vocabDetails));
        
        // Evaluate content
        AIScoreResponse.ContentDetails contentDetails = evaluateContent(transcription, question);
        response.setContentDetails(contentDetails);
        response.setContentScore(calculateContentScore(contentDetails));
        
        return response;
    }

    /**
     * Evaluate pronunciation based on text analysis
     */
    private AIScoreResponse.PronunciationDetails evaluatePronunciation(String transcription) {
        String[] words = transcription.toLowerCase().split("\\s+");
        int wordCount = words.length;
        
        // Detect potential mispronunciations (incomplete words, unusual patterns)
        List<String> problematicWords = new ArrayList<>();
        for (String word : words) {
            if (word.length() <= 2 || word.matches(".*[^a-z'-].*")) {
                problematicWords.add(word);
            }
        }
        
        double clarity = Math.max(0.0, 1.0 - (problematicWords.size() * 0.05));
        
        return new AIScoreResponse.PronunciationDetails(
            wordCount,
            problematicWords.size(),
            problematicWords.subList(0, Math.min(5, problematicWords.size())),
            clarity
        );
    }

    private Double calculatePronunciationScore(AIScoreResponse.PronunciationDetails details) {
        double score = 10.0;
        
        // Deduct for mispronunciations
        score -= details.getMispronunciationCount() * 0.5;
        
        // Factor in clarity
        score *= details.getClarity();
        
        return Math.max(0.0, Math.min(10.0, score));
    }

    /**
     * Evaluate fluency
     */
    private AIScoreResponse.FluencyDetails evaluateFluency(String transcription, Integer durationSeconds) {
        String lowerTranscription = transcription.toLowerCase();
        String[] words = lowerTranscription.split("\\s+");
        int wordCount = words.length;
        
        // Calculate words per minute
        double wordsPerMinute = durationSeconds > 0 ? 
            (wordCount * 60.0) / durationSeconds : 0;
        
        // Count filler words - check each word individually
        List<String> fillerWordsFound = new ArrayList<>();
        int fillerCount = 0;
        for (String word : words) {
            // Remove punctuation for matching
            String cleanWord = word.replaceAll("[^a-z\\s]", "").trim();
            if (FILLER_WORDS.contains(cleanWord)) {
                fillerWordsFound.add(cleanWord);
                fillerCount++;
            }
        }
        
        // Also check for multi-word filler phrases
        for (String filler : FILLER_WORDS) {
            if (filler.contains(" ") && lowerTranscription.contains(filler)) {
                fillerWordsFound.add(filler);
                fillerCount++;
            }
        }
        
        // Estimate pause count (multiple spaces, punctuation patterns)
        int pauseCount = (int) transcription.chars().filter(ch -> ch == '.' || ch == ',' || ch == ';').count();
        
        return new AIScoreResponse.FluencyDetails(
            wordsPerMinute,
            pauseCount,
            fillerCount,
            fillerWordsFound.stream().distinct().collect(Collectors.toList())
        );
    }

    private Double calculateFluencyScore(AIScoreResponse.FluencyDetails details) {
        double score = 10.0;
        
        // Optimal speaking rate: 130-170 words per minute
        double wpm = details.getWordsPerMinute();
        if (wpm < 100 || wpm > 200) {
            score -= 2.0;
        } else if (wpm < 130 || wpm > 170) {
            score -= 1.0;
        }
        
        // Deduct for excessive filler words (> 5% of total words)
        int totalWords = (int) (wpm * 60 / 60); // approximate
        if (totalWords > 0 && details.getFillerWordCount() > totalWords * 0.05) {
            score -= 1.5;
        }
        
        // Deduct for too many or too few pauses
        if (details.getPauseCount() < 2) {
            score -= 0.5; // Too rushed
        } else if (details.getPauseCount() > totalWords / 5) {
            score -= 1.0; // Too choppy
        }
        
        return Math.max(0.0, Math.min(10.0, score));
    }

    /**
     * Evaluate grammar
     */
    private AIScoreResponse.GrammarDetails evaluateGrammar(String transcription) {
        String[] sentences = transcription.split("[.!?]+");
        int totalSentences = sentences.length;
        
        List<String> errorTypes = new ArrayList<>();
        int errorCount = 0;
        
        // Check subject-verb agreement
        Matcher matcher = SUBJECT_VERB_AGREEMENT.matcher(transcription);
        while (matcher.find()) {
            errorCount++;
            errorTypes.add("Subject-verb agreement");
        }
        
        // Check for incomplete sentences (very short or no verb)
        for (String sentence : sentences) {
            String trimmed = sentence.trim();
            if (trimmed.length() > 0 && trimmed.split("\\s+").length < 3) {
                errorCount++;
                errorTypes.add("Incomplete sentence");
            }
        }
        
        double accuracy = totalSentences > 0 ? 
            Math.max(0.0, 1.0 - ((double) errorCount / totalSentences)) : 1.0;
        
        return new AIScoreResponse.GrammarDetails(
            totalSentences,
            errorCount,
            errorTypes.stream().distinct().collect(Collectors.toList()),
            accuracy
        );
    }

    private Double calculateGrammarScore(AIScoreResponse.GrammarDetails details) {
        return details.getAccuracy() * 10.0;
    }

    /**
     * Evaluate vocabulary
     */
    private AIScoreResponse.VocabularyDetails evaluateVocabulary(String transcription, String difficulty) {
        String[] words = transcription.toLowerCase()
            .replaceAll("[^a-z\\s]", " ")
            .split("\\s+");
        
        Set<String> uniqueWords = new HashSet<>(Arrays.asList(words));
        int totalWords = words.length;
        int uniqueCount = uniqueWords.size();
        
        // Calculate lexical diversity (Type-Token Ratio)
        double lexicalDiversity = totalWords > 0 ? 
            (double) uniqueCount / totalWords : 0;
        
        // Identify advanced words (length > 7 characters)
        List<String> advancedWords = uniqueWords.stream()
            .filter(w -> w.length() > 7)
            .limit(10)
            .collect(Collectors.toList());
        
        // Determine vocabulary level
        String level;
        if (lexicalDiversity > 0.7 && advancedWords.size() > 5) {
            level = "ADVANCED";
        } else if (lexicalDiversity > 0.5 && advancedWords.size() > 3) {
            level = "INTERMEDIATE";
        } else {
            level = "BEGINNER";
        }
        
        return new AIScoreResponse.VocabularyDetails(
            uniqueCount,
            totalWords,
            lexicalDiversity,
            advancedWords,
            level
        );
    }

    private Double calculateVocabularyScore(AIScoreResponse.VocabularyDetails details) {
        double score = 5.0; // Base score
        
        // Add points for lexical diversity
        score += details.getLexicalDiversity() * 3.0;
        
        // Add points for advanced words
        score += Math.min(2.0, details.getAdvancedWords().size() * 0.2);
        
        return Math.max(0.0, Math.min(10.0, score));
    }

    /**
     * Evaluate content relevance and completeness
     */
    private AIScoreResponse.ContentDetails evaluateContent(String transcription, Question question) {
        String questionText = question.getQuestionText().toLowerCase();
        String sampleAnswer = question.getSampleAnswer() != null ? 
            question.getSampleAnswer().toLowerCase() : "";
        String userResponse = transcription.toLowerCase();
        
        // Extract key topics from question and sample answer
        Set<String> keyTopics = extractKeyTopics(questionText + " " + sampleAnswer);
        
        // Check how many key topics are covered
        int keyPointsCovered = 0;
        List<String> missingPoints = new ArrayList<>();
        
        for (String topic : keyTopics) {
            if (userResponse.contains(topic)) {
                keyPointsCovered++;
            } else {
                missingPoints.add(topic);
            }
        }
        
        int totalKeyPoints = keyTopics.size();
        
        // Calculate relevance (keyword matching)
        double relevanceScore = totalKeyPoints > 0 ? 
            (double) keyPointsCovered / totalKeyPoints : 0.5;
        
        // Calculate completeness based on length and key points
        int expectedWordCount = sampleAnswer.split("\\s+").length;
        int actualWordCount = userResponse.split("\\s+").length;
        double lengthRatio = expectedWordCount > 0 ? 
            Math.min(1.0, (double) actualWordCount / expectedWordCount) : 0.5;
        
        double completenessScore = (relevanceScore + lengthRatio) / 2.0;
        
        return new AIScoreResponse.ContentDetails(
            relevanceScore,
            completenessScore,
            keyPointsCovered,
            totalKeyPoints,
            missingPoints.subList(0, Math.min(5, missingPoints.size()))
        );
    }

    private Double calculateContentScore(AIScoreResponse.ContentDetails details) {
        return (details.getRelevanceScore() * 5.0 + details.getCompletenessScore() * 5.0);
    }

    /**
     * Extract key topics from text
     */
    private Set<String> extractKeyTopics(String text) {
        // Remove common words and extract meaningful terms
        String[] words = text.toLowerCase()
            .replaceAll("[^a-z\\s]", " ")
            .split("\\s+");
        
        Set<String> commonWords = Set.of(
            "the", "a", "an", "and", "or", "but", "in", "on", "at", "to", "for",
            "of", "with", "is", "are", "was", "were", "be", "been", "being",
            "have", "has", "had", "do", "does", "did", "will", "would", "should",
            "could", "may", "might", "can", "what", "when", "where", "who", "how", "why"
        );
        
        return Arrays.stream(words)
            .filter(w -> w.length() > 3)
            .filter(w -> !commonWords.contains(w))
            .collect(Collectors.toSet());
    }

    /**
     * Calculate overall score from component scores
     */
    private Double calculateOverallScore(AIScoreResponse response) {
        double total = 0.0;
        int count = 0;
        
        if (response.getPronunciationScore() != null) {
            total += response.getPronunciationScore();
            count++;
        }
        if (response.getFluencyScore() != null) {
            total += response.getFluencyScore();
            count++;
        }
        if (response.getGrammarScore() != null) {
            total += response.getGrammarScore();
            count++;
        }
        if (response.getVocabularyScore() != null) {
            total += response.getVocabularyScore();
            count++;
        }
        if (response.getContentScore() != null) {
            total += response.getContentScore();
            count++;
        }
        
        return count > 0 ? total / count : 0.0;
    }

    /**
     * Generate detailed feedback
     */
    private String generateDetailedFeedback(AIScoreResponse response) {
        StringBuilder feedback = new StringBuilder();
        
        feedback.append("Overall Performance: ").append(String.format("%.1f/10", response.getOverallScore())).append("\n\n");
        
        // Pronunciation feedback
        if (response.getPronunciationDetails() != null) {
            feedback.append("🗣️ Pronunciation (").append(String.format("%.1f/10", response.getPronunciationScore())).append("): ");
            if (response.getPronunciationScore() >= 8.0) {
                feedback.append("Excellent clarity!");
            } else if (response.getPronunciationScore() >= 6.0) {
                feedback.append("Good pronunciation with minor issues.");
            } else {
                feedback.append("Needs improvement. Focus on clarity.");
            }
            feedback.append("\n");
        }
        
        // Fluency feedback
        if (response.getFluencyDetails() != null) {
            feedback.append("⚡ Fluency (").append(String.format("%.1f/10", response.getFluencyScore())).append("): ");
            double wpm = response.getFluencyDetails().getWordsPerMinute();
            if (wpm >= 130 && wpm <= 170) {
                feedback.append("Great speaking pace!");
            } else if (wpm < 100) {
                feedback.append("Try to speak a bit faster.");
            } else if (wpm > 200) {
                feedback.append("Slow down for better clarity.");
            }
            feedback.append("\n");
        }
        
        // Grammar feedback
        if (response.getGrammarDetails() != null) {
            feedback.append("📝 Grammar (").append(String.format("%.1f/10", response.getGrammarScore())).append("): ");
            if (response.getGrammarScore() >= 8.0) {
                feedback.append("Strong grammatical accuracy!");
            } else {
                feedback.append("Review grammar rules, especially ").append(
                    String.join(", ", response.getGrammarDetails().getErrorTypes())
                ).append(".");
            }
            feedback.append("\n");
        }
        
        // Vocabulary feedback
        if (response.getVocabularyDetails() != null) {
            feedback.append("📚 Vocabulary (").append(String.format("%.1f/10", response.getVocabularyScore())).append("): ");
            feedback.append("Your vocabulary level is ").append(response.getVocabularyDetails().getVocabularyLevel()).append(". ");
            if (response.getVocabularyScore() < 7.0) {
                feedback.append("Try using more varied and sophisticated words.");
            } else {
                feedback.append("Good word variety!");
            }
            feedback.append("\n");
        }
        
        // Content feedback
        if (response.getContentDetails() != null) {
            feedback.append("💡 Content (").append(String.format("%.1f/10", response.getContentScore())).append("): ");
            int covered = response.getContentDetails().getKeyPointsCovered();
            int total = response.getContentDetails().getTotalKeyPoints();
            feedback.append("You covered ").append(covered).append(" out of ").append(total).append(" key points. ");
            if (response.getContentScore() >= 8.0) {
                feedback.append("Comprehensive response!");
            } else {
                feedback.append("Consider adding more details about the topic.");
            }
        }
        
        return feedback.toString();
    }

    /**
     * Generate actionable suggestions
     */
    private List<String> generateSuggestions(AIScoreResponse response) {
        List<String> suggestions = new ArrayList<>();
        
        if (response.getPronunciationScore() != null && response.getPronunciationScore() < 7.0) {
            suggestions.add("Practice pronunciation with native speaker recordings");
        }
        
        if (response.getFluencyDetails() != null && response.getFluencyDetails().getFillerWordCount() > 5) {
            suggestions.add("Reduce filler words like 'um', 'uh', 'like' - pause instead");
        }
        
        if (response.getGrammarScore() != null && response.getGrammarScore() < 7.0) {
            suggestions.add("Review basic grammar rules and practice sentence construction");
        }
        
        if (response.getVocabularyDetails() != null && 
            response.getVocabularyDetails().getLexicalDiversity() < 0.5) {
            suggestions.add("Expand your vocabulary - try using synonyms and varied expressions");
        }
        
        if (response.getContentDetails() != null && response.getContentScore() < 7.0) {
            suggestions.add("Provide more specific examples and details when answering questions");
        }
        
        if (response.getFluencyDetails() != null) {
            double wpm = response.getFluencyDetails().getWordsPerMinute();
            if (wpm < 100) {
                suggestions.add("Practice speaking more fluently - aim for 130-170 words per minute");
            } else if (wpm > 200) {
                suggestions.add("Slow down your speaking pace for better clarity and comprehension");
            }
        }
        
        return suggestions;
    }

    private Double getDoubleValue(Map<String, Object> map, String key) {
        Object value = map.get(key);
        if (value instanceof Number) {
            return ((Number) value).doubleValue();
        }
        return 0.0;
    }
}
