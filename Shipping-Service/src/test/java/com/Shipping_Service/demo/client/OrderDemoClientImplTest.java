package com.Shipping_Service.demo.client;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import static org.mockito.ArgumentMatchers.anyString;
import org.mockito.Mock;
import static org.mockito.Mockito.when;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.client.RestClient;

import static org.junit.jupiter.api.Assertions.assertEquals;

@ExtendWith(MockitoExtension.class)
class OrderDemoClientImplTest {

    @Mock
    private RestClient restClient;

    @Mock
    private RestClient.RequestHeadersUriSpec requestHeadersUriSpec;

    @Mock
    private RestClient.ResponseSpec responseSpec;

    private OrderDemoClientImpl client;

    @BeforeEach
    void setUp() {
        client = new OrderDemoClientImpl(restClient, "http://localhost:8081/api/circuit-breaker/demo-dependency");
    }

    @Test
    @DisplayName("callDemoDependency - Llamada REST exitosa")
    void testCallDemoDependency() {
        when(restClient.get()).thenReturn(requestHeadersUriSpec);
        when(requestHeadersUriSpec.uri(anyString(), anyString())).thenReturn(requestHeadersUriSpec);
        when(requestHeadersUriSpec.retrieve()).thenReturn(responseSpec);
        when(responseSpec.body(String.class)).thenReturn("{\"status\":\"ok\"}");

        String result = client.callDemoDependency("success");

        assertEquals("{\"status\":\"ok\"}", result);
    }
}
