package com.ruoyi.framework.oauth;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.same;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.Set;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.springframework.http.MediaType;
import org.springframework.http.converter.json.JacksonJsonHttpMessageConverter;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import com.fasterxml.jackson.databind.JsonNode;
import com.ruoyi.common.core.domain.entity.SysUser;
import com.ruoyi.common.core.domain.model.LoginUser;
import com.ruoyi.framework.web.service.RefreshSession;
import com.ruoyi.framework.web.service.TokenService;

class QuantaOAuthControllerTest
{
    private static final String ID = "a".repeat(43);
    private static final String BROWSER = "b".repeat(43);
    private static final String CSRF = "c".repeat(43);
    private static final String ORIGIN = "https://www.quantacenter.com";

    private QuantaOAuthFlow flow;
    private LoginUser login;
    private MockMvc mvc;

    @BeforeEach
    void setUp()
    {
        flow = mock(QuantaOAuthFlow.class);
        TokenService tokens = mock(TokenService.class);
        SysUser user = new SysUser();
        user.setUserId(8L);
        login = new LoginUser(8L, 1L, user, Set.of());
        RefreshSession refresh = new RefreshSession();
        refresh.setUserId(8L);
        when(tokens.getLoginUser(any(HttpServletRequest.class))).thenReturn(login);
        when(tokens.peekRefresh(any(HttpServletRequest.class))).thenReturn(refresh);
        // Explicitly exercise Spring Boot 4's Jackson 3 request binding, not a Jackson 2 test-only converter.
        mvc = MockMvcBuilders.standaloneSetup(new QuantaOAuthController(flow, tokens))
                .setMessageConverters(new JacksonJsonHttpMessageConverter())
                .build();
    }

    @ParameterizedTest
    @ValueSource(strings = { "approve", "deny" })
    void decisionBindsJsonAndForwardsUnchangedSecurityContext(String action) throws Exception
    {
        String response = "{\"code\":200}";
        when(flow.decide(eq(ID), eq(BROWSER), eq(CSRF), eq(ORIGIN), same(login), eq(8L),
                any(JsonNode.class), anyString())).thenReturn(QuantaOAuthResponse.json(200, response));

        mvc.perform(post("/oauth2/interaction/{id}/decision", ID)
                .cookie(new Cookie(QuantaOAuthController.BROWSER_COOKIE, BROWSER))
                .header("X-CSRF-Token", CSRF).header("Origin", ORIGIN)
                .contentType(MediaType.APPLICATION_JSON).content("{\"action\":\"" + action + "\"}"))
                .andExpect(status().isOk()).andExpect(content().string(response));

        ArgumentCaptor<JsonNode> body = ArgumentCaptor.forClass(JsonNode.class);
        verify(flow).decide(eq(ID), eq(BROWSER), eq(CSRF), eq(ORIGIN), same(login), eq(8L),
                body.capture(), anyString());
        assertTrue(body.getValue().isObject());
        assertEquals(1, body.getValue().size());
        assertEquals(action, body.getValue().get("action").textValue());
    }

    @Test
    void invalidActionTypesAndExtraFieldsAreNotCoercedOrDiscarded() throws Exception
    {
        when(flow.decide(eq(ID), eq(BROWSER), eq(CSRF), eq(ORIGIN), same(login), eq(8L),
                any(JsonNode.class), anyString())).thenReturn(QuantaOAuthResponse.json(400, "{\"code\":400}"));
        mvc.perform(post("/oauth2/interaction/{id}/decision", ID)
                .cookie(new Cookie(QuantaOAuthController.BROWSER_COOKIE, BROWSER))
                .header("X-CSRF-Token", CSRF).header("Origin", ORIGIN)
                .contentType(MediaType.APPLICATION_JSON).content("{\"action\":123,\"extra\":true}"))
                .andExpect(status().isBadRequest());

        ArgumentCaptor<JsonNode> body = ArgumentCaptor.forClass(JsonNode.class);
        verify(flow).decide(eq(ID), eq(BROWSER), eq(CSRF), eq(ORIGIN), same(login), eq(8L),
                body.capture(), anyString());
        assertTrue(body.getValue().get("action").isIntegralNumber());
        assertTrue(body.getValue().get("extra").booleanValue());
    }

    @ParameterizedTest
    @ValueSource(strings = { "[\"approve\"]", "null", "{\"action\":" })
    void invalidJsonBodiesAreRejectedBeforeTheFlow(String body) throws Exception
    {
        mvc.perform(post("/oauth2/interaction/{id}/decision", ID)
                .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isBadRequest());
        verifyNoInteractions(flow);
    }
}
