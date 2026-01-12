package com.aispeaking.controller;

import com.aispeaking.model.Question;
import com.aispeaking.service.QuestionService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
class QuestionControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private QuestionService questionService;

    private Question mockQuestion1;
    private Question mockQuestion2;
    private List<Question> mockQuestions;

    @BeforeEach
    void setUp() {
        // Question 1: Read Aloud
        mockQuestion1 = new Question();
        mockQuestion1.setId(1L);
        mockQuestion1.setQuestionText("Read the following passage aloud: 'The quick brown fox jumps over the lazy dog.'");
        mockQuestion1.setSampleAnswer("The quick brown fox jumps over the lazy dog.");
        mockQuestion1.setType(Question.QuestionType.INTRODUCTION);
        mockQuestion1.setDifficulty(Question.DifficultyLevel.BEGINNER);
        mockQuestion1.setTopic("Pronunciation");
        mockQuestion1.setTimeLimit(60);
        mockQuestion1.setPreparationTime(30);
        mockQuestion1.setActive(true);

        // Question 2: Describe Image
        mockQuestion2 = new Question();
        mockQuestion2.setId(2L);
        mockQuestion2.setQuestionText("Describe what you see in this image.");
        mockQuestion2.setSampleAnswer("In this image, I can see a beautiful landscape with mountains in the background and a lake in the foreground.");
        mockQuestion2.setType(Question.QuestionType.PICTURE_DESCRIPTION);
        mockQuestion2.setDifficulty(Question.DifficultyLevel.INTERMEDIATE);
        mockQuestion2.setTopic("Description");
        mockQuestion2.setTimeLimit(90);
        mockQuestion2.setPreparationTime(45);
        mockQuestion2.setImageUrl("https://example.com/image.jpg");
        mockQuestion2.setActive(true);

        mockQuestions = Arrays.asList(mockQuestion1, mockQuestion2);
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void testCreateQuestion_Success() throws Exception {
        // Arrange
        Question newQuestion = new Question();
        newQuestion.setQuestionText("What is your favorite hobby?");
        newQuestion.setType(Question.QuestionType.OPINION);
        newQuestion.setDifficulty(Question.DifficultyLevel.BEGINNER);
        newQuestion.setTopic("Personal");
        newQuestion.setTimeLimit(60);

        when(questionService.createQuestion(any(Question.class))).thenReturn(mockQuestion1);

        // Act & Assert
        mockMvc.perform(post("/api/questions")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(newQuestion)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1L))
                .andExpect(jsonPath("$.type").value("INTRODUCTION"));

        verify(questionService, times(1)).createQuestion(any(Question.class));
    }

    @Test
    @WithMockUser
    void testGetAllQuestions_Success() throws Exception {
        // Arrange
        when(questionService.getAllQuestions()).thenReturn(mockQuestions);

        // Act & Assert
        mockMvc.perform(get("/api/questions"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].id").value(1L))
                .andExpect(jsonPath("$[0].type").value("INTRODUCTION"))
                .andExpect(jsonPath("$[1].id").value(2L))
                .andExpect(jsonPath("$[1].type").value("PICTURE_DESCRIPTION"));

        verify(questionService, times(1)).getAllQuestions();
    }

    @Test
    @WithMockUser
    void testGetActiveQuestions_Success() throws Exception {
        // Arrange
        when(questionService.getActiveQuestions()).thenReturn(mockQuestions);

        // Act & Assert
        mockMvc.perform(get("/api/questions/active"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2));

        verify(questionService, times(1)).getActiveQuestions();
    }

    @Test
    @WithMockUser
    void testGetQuestionById_Found() throws Exception {
        // Arrange
        when(questionService.getQuestionById(1L)).thenReturn(Optional.of(mockQuestion1));

        // Act & Assert
        mockMvc.perform(get("/api/questions/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1L))
                .andExpect(jsonPath("$.questionText").value(mockQuestion1.getQuestionText()));

        verify(questionService, times(1)).getQuestionById(1L);
    }

    @Test
    @WithMockUser
    void testGetQuestionById_NotFound() throws Exception {
        // Arrange
        when(questionService.getQuestionById(999L)).thenReturn(Optional.empty());

        // Act & Assert
        mockMvc.perform(get("/api/questions/999"))
                .andExpect(status().isNotFound());

        verify(questionService, times(1)).getQuestionById(999L);
    }

    @Test
    @WithMockUser
    void testGetQuestionsByType_Success() throws Exception {
        // Arrange
        when(questionService.getQuestionsByType(Question.QuestionType.INTRODUCTION))
                .thenReturn(Arrays.asList(mockQuestion1));

        // Act & Assert
        mockMvc.perform(get("/api/questions/type/INTRODUCTION"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].type").value("INTRODUCTION"));

        verify(questionService, times(1)).getQuestionsByType(Question.QuestionType.INTRODUCTION);
    }

    @Test
    @WithMockUser
    void testGetQuestionsByDifficulty_Success() throws Exception {
        // Arrange
        when(questionService.getQuestionsByDifficulty(Question.DifficultyLevel.BEGINNER))
                .thenReturn(Arrays.asList(mockQuestion1));

        // Act & Assert
        mockMvc.perform(get("/api/questions/difficulty/BEGINNER"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].difficulty").value("BEGINNER"));

        verify(questionService, times(1)).getQuestionsByDifficulty(Question.DifficultyLevel.BEGINNER);
    }

    @Test
    @WithMockUser
    void testGetQuestionsByTopic_Success() throws Exception {
        // Arrange
        when(questionService.getQuestionsByTopic("Pronunciation"))
                .thenReturn(Arrays.asList(mockQuestion1));

        // Act & Assert
        mockMvc.perform(get("/api/questions/topic/Pronunciation"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].topic").value("Pronunciation"));

        verify(questionService, times(1)).getQuestionsByTopic("Pronunciation");
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void testUpdateQuestion_Success() throws Exception {
        // Arrange
        Question updatedQuestion = new Question();
        updatedQuestion.setQuestionText("Updated question text");
        updatedQuestion.setDifficulty(Question.DifficultyLevel.ADVANCED);

        when(questionService.updateQuestion(anyLong(), any(Question.class))).thenReturn(mockQuestion1);

        // Act & Assert
        mockMvc.perform(put("/api/questions/1")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(updatedQuestion)))
                .andExpect(status().isOk());

        verify(questionService, times(1)).updateQuestion(anyLong(), any(Question.class));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void testDeleteQuestion_Success() throws Exception {
        // Arrange
        doNothing().when(questionService).deleteQuestion(1L);

        // Act & Assert
        mockMvc.perform(delete("/api/questions/1"))
                .andExpect(status().isOk());

        verify(questionService, times(1)).deleteQuestion(1L);
    }
}
