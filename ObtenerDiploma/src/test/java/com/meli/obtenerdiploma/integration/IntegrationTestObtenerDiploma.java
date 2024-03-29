package com.meli.obtenerdiploma.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.meli.obtenerdiploma.model.StudentDTO;
import com.meli.obtenerdiploma.model.SubjectDTO;
import com.meli.obtenerdiploma.repository.StudentDAO;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultMatcher;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.result.MockMvcResultHandlers;

import java.util.List;

import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
public class IntegrationTestObtenerDiploma {
    @Autowired
    MockMvc mockMvc;

    static StudentDAO studentDAO=new StudentDAO();

    private static ObjectWriter writer;

    SubjectDTO kahoot;
    SubjectDTO musica;
    SubjectDTO poo;

    StudentDTO student;

    @BeforeAll
    public static void setUp(){
        writer = new ObjectMapper()
                .configure(SerializationFeature.WRAP_ROOT_VALUE, false)
                .writer().withDefaultPrettyPrinter();
    }

    @BeforeEach
    public void beforeEach(){
        kahoot=new SubjectDTO("Kahoot",1.0);
        musica=new SubjectDTO("Musica",2.0);
        poo=new SubjectDTO("POO",9.0);

        student=new StudentDTO(1L, "Manuel", "El alumno Manuel ha obtenido un promedio de 4,00. Puedes mejorar.", 4.0, List.of(kahoot, musica, poo));

        if(!studentDAO.exists(student)){
            studentDAO.save(student);
        }
    }

    @Test
    void testGivenValidUserIdGetDiplomaWithAverangeScore() throws Exception{
        this.mockMvc.perform(MockMvcRequestBuilders.get("/analyzeScores/{studentId}", 1))
                .andDo(MockMvcResultHandlers.print())
                .andExpect(content().contentType("application/json"))
                .andExpect(jsonPath("$.studentName").value("Manuel"))
                .andExpect(jsonPath("$.averageScore").value(4.0))
                .andExpect(jsonPath("$.subjects.length()").value(3))
                .andExpect(jsonPath("$.subjects[?(@.name == \"" + kahoot.getName() + "\"  && @.score == " + kahoot.getScore() + ")]").exists());
    }

    @Test
    void helloTest() throws Exception{
        // Arrange
        String userJson=writer.writeValueAsString(student);
        System.out.println(userJson);

        ResultMatcher expectedStatus= status().isOk();
        ResultMatcher expectedJson = content().json(userJson);
        ResultMatcher expectedContentType = content().contentType(MediaType.APPLICATION_JSON);

        MockHttpServletRequestBuilder request = MockMvcRequestBuilders.get("/analyzeScores/{studentId}", 1);

        // Act & Assert
        mockMvc.perform(request)
                .andDo(MockMvcResultHandlers.print())
                .andExpect(expectedContentType)
                .andExpect(expectedJson)
                .andExpect(expectedStatus);
    }

    @Test
    void testGivenAnInvalidStudentIdThrowExceptionMessage() throws Exception {

        this.mockMvc.perform(MockMvcRequestBuilders.get("/analyzeScores/{studentId}", 22))
                .andDo(MockMvcResultHandlers.print())
                .andExpect(status().isNotFound())
                .andExpect(content().contentType("application/json"))
                .andExpect(jsonPath("$.name").value("StudentNotFoundException"));
    }
}
