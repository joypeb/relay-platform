package com.joypeb.gateway.api.rest;

import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

@SpringBootTest
class SessionControllerTest {

	@Autowired
	private WebApplicationContext webApplicationContext;

	private MockMvc mockMvc;

	@BeforeEach
	void setUp() {
		mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext).build();
	}

	@Test
	void createSessionPreflight_whenOriginIsLocalhostPort_returnsCorsHeaders() throws Exception {
		mockMvc.perform(options("/api/v1/sessions")
						.header(HttpHeaders.ORIGIN, "http://localhost:63341")
						.header(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, "POST")
						.header(HttpHeaders.ACCESS_CONTROL_REQUEST_HEADERS, "content-type"))
				.andExpect(status().isOk())
				.andExpect(header().string(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN, "http://localhost:63341"))
				.andExpect(header().string(HttpHeaders.ACCESS_CONTROL_ALLOW_CREDENTIALS, "true"));
	}

	@Test
	void createSession_whenUserIdIsValid_returnsCreatedSession() throws Exception {
		mockMvc.perform(post("/api/v1/sessions")
						.header(HttpHeaders.ORIGIN, "http://localhost:63341")
						.header(TraceIdResolver.TRACE_ID_HEADER, "trace-login")
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{"userId":"user-1"}
								"""))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.success").value(true))
				.andExpect(jsonPath("$.data.userId").value("user-1"))
				.andExpect(jsonPath("$.data.sessionId").isString())
				.andExpect(jsonPath("$.traceId").value("trace-login"))
				.andExpect(jsonPath("$.timestamp").isString())
				.andExpect(header().string(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN, "http://localhost:63341"))
				.andExpect(header().string(HttpHeaders.ACCESS_CONTROL_ALLOW_CREDENTIALS, "true"));
	}

	@Test
	void createSession_whenUserIdIsInvalid_returnsValidationProblem() throws Exception {
		mockMvc.perform(post("/api/v1/sessions")
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{"userId":"invalid user"}
								"""))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.code").value("REQUEST_VALIDATION_FAILED"))
				.andExpect(jsonPath("$.traceId").isString())
				.andExpect(jsonPath("$.errors[0]", containsString("userId")));
	}

	@Test
	void currentSession_whenSessionExists_returnsCurrentUser() throws Exception {
		MvcResult result = mockMvc.perform(post("/api/v1/sessions")
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{"userId":"user-2"}
								"""))
				.andExpect(status().isCreated())
				.andReturn();

		MockHttpSession session = (MockHttpSession) result.getRequest().getSession(false);

		mockMvc.perform(get("/api/v1/sessions/current")
						.session(session))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.data.userId").value("user-2"))
				.andExpect(jsonPath("$.data.sessionId").value(session.getId()));
	}

	@Test
	void currentSession_whenSessionIsMissing_returnsUnauthorizedProblem() throws Exception {
		mockMvc.perform(get("/api/v1/sessions/current"))
				.andExpect(status().isUnauthorized())
				.andExpect(jsonPath("$.code").value("UNAUTHENTICATED"))
				.andExpect(jsonPath("$.traceId").isString());
	}

	@Test
	void deleteCurrentSession_whenSessionExists_invalidatesSession() throws Exception {
		MvcResult result = mockMvc.perform(post("/api/v1/sessions")
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{"userId":"user-3"}
								"""))
				.andExpect(status().isCreated())
				.andReturn();

		MockHttpSession session = (MockHttpSession) result.getRequest().getSession(false);

		mockMvc.perform(delete("/api/v1/sessions/current")
						.session(session))
				.andExpect(status().isNoContent());

		mockMvc.perform(get("/api/v1/sessions/current")
						.session(session))
				.andExpect(status().isUnauthorized());
	}
}
