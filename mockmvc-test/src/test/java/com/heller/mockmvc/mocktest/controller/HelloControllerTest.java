package com.heller.mockmvc.mocktest.controller;

import com.heller.mockmvc.mocktest.MockMvcTest;
import org.junit.Test;
import org.springframework.http.MediaType;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

public class HelloControllerTest extends MockMvcTest {
    
    @Test
    public void testSayHello() throws Exception {
        mockMvc.perform(get("/hello/sayHello")
            .param("name", "heller")
            .accept(MediaType.ALL))
            .andExpect(status().isOk())
            .andExpect(content().contentType("application/json;charset=UTF-8"))
            .andExpect(content().json("{\"name\":\"heller\",\"word\":\"Hello.\"}"));
    }
    
}
