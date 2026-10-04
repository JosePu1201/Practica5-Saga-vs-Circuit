package com.Order_Service.demo.controller;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class OrderDemoDependencyControllerTest {

    private final OrderDemoDependencyController controller = new OrderDemoDependencyController();

    @Test
    @DisplayName("demoDependency - mode=success retorna HTTP 200 OK")
    void testSuccessMode() throws InterruptedException {
        ResponseEntity response = controller.demoDependency("success");
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertTrue(response.getBody().toString().contains("Order Service respondió correctamente"));
    }

    @Test
    @DisplayName("demoDependency - mode=fail retorna HTTP 503 SERVICE_UNAVAILABLE")
    void testFailMode() throws InterruptedException {
        ResponseEntity response = controller.demoDependency("fail");
        assertEquals(HttpStatus.SERVICE_UNAVAILABLE, response.getStatusCode());
        assertTrue(response.getBody().toString().contains("Simulated Order Service 503 Outage"));
    }

    @Test
    @DisplayName("demoDependency - mode=slow retorna HTTP 200 OK tras delay")
    void testSlowMode() throws InterruptedException {
        // Para acortar tiempo de prueba unitaria podemos llamar con thread interrupción o verificar que ejecuta
        long startTime = System.currentTimeMillis();
        ResponseEntity response = controller.demoDependency("slow");
        long duration = System.currentTimeMillis() - startTime;

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertTrue(duration >= 3000);
        assertTrue(response.getBody().toString().contains("delayed_success"));
    }

    @Test
    @DisplayName("demoDependency - default mode retorna HTTP 200 OK")
    void testDefaultMode() throws InterruptedException {
        ResponseEntity response = controller.demoDependency("other");
        assertEquals(HttpStatus.OK, response.getStatusCode());
    }
}
