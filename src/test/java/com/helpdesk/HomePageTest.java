package com.helpdesk;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
class HomePageTest extends SqliteTestSupport {
    private final MockMvc mvc;

    @Autowired
    HomePageTest(MockMvc mvc) {
        this.mvc = mvc;
    }

    @Test
    void rendersHome() throws Exception {
        mvc.perform(get("/login"))
                .andExpect(status().isOk())
                .andExpect(view().name("auth/login"))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("本機資訊報修服務")));
    }

    @Test
    void embeddedServerResponds(
            @Autowired org.springframework.boot.test.web.client.TestRestTemplate http) {
        org.assertj.core.api.Assertions.assertThat(http.getForObject("/login", String.class))
                .contains("本機資訊報修服務");
    }
}
