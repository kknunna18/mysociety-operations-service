package com.mysociety.operations;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

class ControllerErrorTest {
    @Test
    void invalidComplaintReturnsRfc9457Problem() throws Exception {
        OperationsService service = org.mockito.Mockito.mock(OperationsService.class);
        MockMvc mvc = MockMvcBuilders.standaloneSetup(new ComplaintController(service)).setControllerAdvice(new ProblemHandling()).build();
        mvc.perform(post("/complaints").contentType(MediaType.APPLICATION_JSON).content("{\"category\":\"\",\"subject\":\"\",\"description\":\"\"}"))
                .andExpect(status().isBadRequest()).andExpect(content().contentTypeCompatibleWith("application/problem+json"))
                .andExpect(jsonPath("$.detail").value("Request validation failed"));
    }
}
