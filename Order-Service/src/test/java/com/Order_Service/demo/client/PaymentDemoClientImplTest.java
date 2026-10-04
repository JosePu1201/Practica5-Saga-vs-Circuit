package com.Order_Service.demo.client;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import org.mockito.Mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.client.RestClient;

import static org.junit.jupiter.api.Assertions.assertEquals;

@ExtendWith(MockitoExtension.class)
class PaymentDemoClientImplTest {

    @Mock
    private RestClient restClient;

    @Mock
    private RestClient.RequestHeadersUriSpec requestHeadersUriSpec;

    @Mock
    private RestClient.ResponseSpec responseSpec;

    private PaymentDemoClientImpl client;

    @BeforeEach
    void setUp() {
        client = new PaymentDemoClientImpl(restClient, "http://localhost:8083/api/circuit-breaker/demo-dependency");
    }

    @Test
    @DisplayName("callDemoDependency - Ejecuta llamada GET exitosa sin query param previo")
    void testCallDemoDependencyNoQueryParams() {
        when(restClient.get()).thenReturn(requestHeadersUriSpec);
        when(requestHeadersUriSpec.uri(anyString(), anyString())).thenReturn(requestHeadersUriSpec);
        when(requestHeadersUriSpec.retrieve()).thenReturn(responseSpec);
        when(responseSpec.body(String.class)).thenReturn("{\"status\":\"ok\"}");

        String result = client.callDemoDependency("success");

        assertEquals("{\"status\":\"ok\"}", result);
        verify(requestHeadersUriSpec).uri("http://localhost:8083/api/circuit-breaker/demo-dependency?mode={mode}", "success");
    }

    @Test
    @DisplayName("callDemoDependency - Ejecuta llamada GET exitosa cuando targetUrl ya contiene query params")
    void testCallDemoDependencyWithQueryParams() {
        PaymentDemoClientImpl clientWithQuery = new PaymentDemoClientImpl(restClient, "http://localhost:8083/api/circuit-breaker/demo-dependency?foo=bar");

        when(restClient.get()).thenReturn(requestHeadersUriSpec);
        when(requestHeadersUriSpec.uri(anyString(), anyString())).thenReturn(requestHeadersUriSpec);
        when(requestHeadersUriSpec.retrieve()).thenReturn(responseSpec);
        when(responseSpec.body(String.class)).thenReturn("{\"status\":\"ok\"}");

        String result = clientWithQuery.callDemoDependency("fail");

        assertEquals("{\"status\":\"ok\"}", result);
        verify(requestHeadersUriSpec).uri("http://localhost:8083/api/circuit-breaker/demo-dependency?foo=bar&mode={mode}", "fail");
    }
}
