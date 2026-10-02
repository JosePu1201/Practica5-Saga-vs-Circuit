package com.Shipping_Service.demo.dto;

import java.time.LocalDateTime;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ShipmentScheduleRequestDTO {

    @NotNull(message = "El identificador del pedido (orderId) es obligatorio")
    @Positive(message = "El identificador del pedido debe ser un número positivo")
    private Long orderId;

    @NotBlank(message = "La dirección de entrega es obligatoria")
    @Size(min = 5, max = 255, message = "La dirección debe tener entre 5 y 255 caracteres")
    private String address;

    private LocalDateTime scheduleDate;
}