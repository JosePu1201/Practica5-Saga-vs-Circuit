package com.Payment_Service.demo.controller;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import static org.junit.jupiter.api.Assertions.assertEquals;

class PaymentDemoDependencyControllerTest {

    private final PaymentDemoDependencyController controller = new PaymentDemoDependencyController();

    @Test
    @DisplayName("demoDependency - Retorna HTTP 200 para success y 503 para fail")
    void testModes() throws InterruptedException {
        ResponseEntity responseSuccess = controller.demoDependency("success");
        assertEquals(HttpStatus.OK, responseSuccess.getStatusCode());

        ResponseEntity responseFail = controller.demoDependency("fail");
        assertEquals(HttpStatus.SERVICE_UNAVAILABLE, responseFail.getStatusCode());
    }
}
