package com.Inventory_Service.demo.client;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import static org.mockito.ArgumentMatchers.anyString;
import org.mockito.Mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.client.RestClient;

import static org.junit.jupiter.api.Assertions.assertEquals;

@ExtendWith(MockitoExtension.class)
class ShippingDemoClientImplTest {

    @Mock
    private RestClient restClient;

    @Mock
    private RestClient.RequestHeadersUriSpec requestHeadersUriSpec;

    @Mock
    private RestClient.ResponseSpec responseSpec;

    private ShippingDemoClientImpl client;

    @BeforeEach
    void setUp() {
        client = new ShippingDemoClientImpl(restClient, "http://localhost:8080/api/circuit-breaker/demo-dependency");
    }

    @Test
    @DisplayName("callDemoDependency - Sin query params en targetUrl")
    void testCallDemoDependencyNoQueryParams() {
        when(restClient.get()).thenReturn(requestHeadersUriSpec);
        when(requestHeadersUriSpec.uri(anyString(), anyString())).thenReturn(requestHeadersUriSpec);
        when(requestHeadersUriSpec.retrieve()).thenReturn(responseSpec);
        when(responseSpec.body(String.class)).thenReturn("{\"status\":\"ok\"}");

        String result = client.callDemoDependency("success");

        assertEquals("{\"status\":\"ok\"}", result);
        verify(requestHeadersUriSpec).uri("http://localhost:8080/api/circuit-breaker/demo-dependency?mode={mode}", "success");
    }

    @Test
    @DisplayName("callDemoDependency - Con query params previos en targetUrl")
    void testCallDemoDependencyWithQueryParams() {
        ShippingDemoClientImpl clientWithQuery = new ShippingDemoClientImpl(restClient, "http://localhost:8080/api/circuit-breaker/demo-dependency?foo=bar");

        when(restClient.get()).thenReturn(requestHeadersUriSpec);
        when(requestHeadersUriSpec.uri(anyString(), anyString())).thenReturn(requestHeadersUriSpec);
        when(requestHeadersUriSpec.retrieve()).thenReturn(responseSpec);
        when(responseSpec.body(String.class)).thenReturn("{\"status\":\"ok\"}");

        String result = clientWithQuery.callDemoDependency("fail");

        assertEquals("{\"status\":\"ok\"}", result);
        verify(requestHeadersUriSpec).uri("http://localhost:8080/api/circuit-breaker/demo-dependency?foo=bar&mode={mode}", "fail");
    }
}
