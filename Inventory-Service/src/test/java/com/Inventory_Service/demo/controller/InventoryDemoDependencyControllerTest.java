package com.Inventory_Service.demo.controller;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class InventoryDemoDependencyControllerTest {

    private final InventoryDemoDependencyController controller = new InventoryDemoDependencyController();

    @Test
    @DisplayName("demoDependency - mode=success")
    void testSuccess() throws InterruptedException {
        ResponseEntity response = controller.demoDependency("success");
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertTrue(response.getBody().toString().contains("Inventory Service respondió correctamente"));
    }

    @Test
    @DisplayName("demoDependency - mode=fail")
    void testFail() throws InterruptedException {
        ResponseEntity response = controller.demoDependency("fail");
        assertEquals(HttpStatus.SERVICE_UNAVAILABLE, response.getStatusCode());
    }

    @Test
    @DisplayName("demoDependency - mode=slow")
    void testSlow() throws InterruptedException {
        long startTime = System.currentTimeMillis();
        ResponseEntity response = controller.demoDependency("slow");
        long duration = System.currentTimeMillis() - startTime;

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertTrue(duration >= 3000);
    }
}
