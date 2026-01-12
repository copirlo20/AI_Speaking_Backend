package com.aispeaking.controller;

import com.aispeaking.model.Question;
import com.aispeaking.model.SpeakingTest;
import com.aispeaking.service.SpeakingTestService;
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
import java.util.HashSet;
import java.util.List;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
class SpeakingTestControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private SpeakingTestService testService;

    private SpeakingTest mockTest1;
    private SpeakingTest mockTest2;
    private List<SpeakingTest> mockTests;

    @BeforeEach
    void setUp() {
        // Test 1: Beginner Level
        mockTest1 = new SpeakingTest();
        mockTest1.setId(1L);
        mockTest1.setTitle("Beginner Speaking Test");
        mockTest1.setDescription("A comprehensive test for beginner level English speakers");
        mockTest1.setLevel(SpeakingTest.TestLevel.A1);
        mockTest1.setDuration(30);
        mockTest1.setTotalQuestions(10);
        mockTest1.setActive(true);
        mockTest1.setQuestions(new HashSet<>());

        // Test 2: Intermediate Level
        mockTest2 = new SpeakingTest();
        mockTest2.setId(2L);
        mockTest2.setTitle("Intermediate Speaking Test");
        mockTest2.setDescription("A comprehensive test for intermediate level English speakers");
        mockTest2.setLevel(SpeakingTest.TestLevel.B1);
        mockTest2.setDuration(45);
        mockTest2.setTotalQuestions(15);
        mockTest2.setActive(true);
        mockTest2.setQuestions(new HashSet<>());

        mockTests = Arrays.asList(mockTest1, mockTest2);
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void testCreateTest_Success() throws Exception {
        // Arrange
        SpeakingTest newTest = new SpeakingTest();
        newTest.setTitle("New Test");
        newTest.setDescription("New test description");
        newTest.setLevel(SpeakingTest.TestLevel.A2);
        newTest.setDuration(30);
        newTest.setTotalQuestions(10);

        when(testService.createTest(any(SpeakingTest.class))).thenReturn(mockTest1);

        // Act & Assert
        mockMvc.perform(post("/api/tests")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(newTest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1L))
                .andExpect(jsonPath("$.title").value("Beginner Speaking Test"))
                .andExpect(jsonPath("$.level").value("A1"));

        verify(testService, times(1)).createTest(any(SpeakingTest.class));
    }

    @Test
    @WithMockUser
    void testGetAllTests_Success() throws Exception {
        // Arrange
        when(testService.getAllTests()).thenReturn(mockTests);

        // Act & Assert
        mockMvc.perform(get("/api/tests"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].id").value(1L))
                .andExpect(jsonPath("$[0].level").value("A1"))
                .andExpect(jsonPath("$[1].id").value(2L))
                .andExpect(jsonPath("$[1].level").value("B1"));

        verify(testService, times(1)).getAllTests();
    }

    @Test
    @WithMockUser
    void testGetActiveTests_Success() throws Exception {
        // Arrange
        when(testService.getActiveTests()).thenReturn(mockTests);

        // Act & Assert
        mockMvc.perform(get("/api/tests/active"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2));

        verify(testService, times(1)).getActiveTests();
    }

    @Test
    @WithMockUser
    void testGetTestById_Found() throws Exception {
        // Arrange
        when(testService.getTestById(1L)).thenReturn(Optional.of(mockTest1));

        // Act & Assert
        mockMvc.perform(get("/api/tests/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1L))
                .andExpect(jsonPath("$.title").value("Beginner Speaking Test"))
                .andExpect(jsonPath("$.duration").value(30));

        verify(testService, times(1)).getTestById(1L);
    }

    @Test
    @WithMockUser
    void testGetTestById_NotFound() throws Exception {
        // Arrange
        when(testService.getTestById(999L)).thenReturn(Optional.empty());

        // Act & Assert
        mockMvc.perform(get("/api/tests/999"))
                .andExpect(status().isNotFound());

        verify(testService, times(1)).getTestById(999L);
    }

    @Test
    @WithMockUser
    void testGetTestsByLevel_Success() throws Exception {
        // Arrange
        when(testService.getTestsByLevel(SpeakingTest.TestLevel.A1))
                .thenReturn(Arrays.asList(mockTest1));

        // Act & Assert
        mockMvc.perform(get("/api/tests/level/A1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].level").value("A1"));

        verify(testService, times(1)).getTestsByLevel(SpeakingTest.TestLevel.A1);
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void testUpdateTest_Success() throws Exception {
        // Arrange
        SpeakingTest updatedTest = new SpeakingTest();
        updatedTest.setTitle("Updated Test");
        updatedTest.setDescription("Updated description");
        updatedTest.setDuration(60);

        when(testService.updateTest(anyLong(), any(SpeakingTest.class))).thenReturn(mockTest1);

        // Act & Assert
        mockMvc.perform(put("/api/tests/1")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(updatedTest)))
                .andExpect(status().isOk());

        verify(testService, times(1)).updateTest(anyLong(), any(SpeakingTest.class));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void testDeleteTest_Success() throws Exception {
        // Arrange
        doNothing().when(testService).deleteTest(1L);

        // Act & Assert
        mockMvc.perform(delete("/api/tests/1"))
                .andExpect(status().isOk())
                .andExpect(content().string("Test deleted successfully"));

        verify(testService, times(1)).deleteTest(1L);
    }

    @Test
    @WithMockUser
    void testStartTest_Success() throws Exception {
        // Arrange
        doNothing().when(testService).incrementAttemptCount(1L);

        // Act & Assert
        mockMvc.perform(post("/api/tests/1/start"))
                .andExpect(status().isOk())
                .andExpect(content().string("Test started successfully"));

        verify(testService, times(1)).incrementAttemptCount(1L);
    }

    @Test
    @WithMockUser
    void testStartTest_NotFound() throws Exception {
        // Arrange
        doThrow(new RuntimeException("Test not found"))
                .when(testService).incrementAttemptCount(999L);

        // Act & Assert
        mockMvc.perform(post("/api/tests/999/start"))
                .andExpect(status().isBadRequest());

        verify(testService, times(1)).incrementAttemptCount(999L);
    }
}
