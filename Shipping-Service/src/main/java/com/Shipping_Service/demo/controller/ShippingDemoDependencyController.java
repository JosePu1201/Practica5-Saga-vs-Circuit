package com.Shipping_Service.demo.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/circuit-breaker")
public class ShippingDemoDependencyController {

    @GetMapping("/demo-dependency")
    public ResponseEntity demoDependency(@RequestParam(defaultValue = "success") String mode)
            throws InterruptedException {
        switch (mode.toLowerCase()) {
            case "fail":
                return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                        .body("{\"error\":\"Simulated Shipping Service 503 Outage\"}");
            case "slow":
                // 3500 ms supera el read-timeout configurado de 2000 ms en Inventory Service
                Thread.sleep(3500);
                return ResponseEntity.ok("{\"status\":\"delayed_success\"}");
            case "success":
            default:
                return ResponseEntity
                        .ok("{\"status\":\"success\",\"message\":\"Shipping Service respondió correctamente\"}");
        }
    }
}