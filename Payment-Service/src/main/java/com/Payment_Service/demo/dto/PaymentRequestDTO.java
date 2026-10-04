package com.Payment_Service.demo.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PaymentRequestDTO {

    @NotNull(message = "El identificador de la orden (orderId) es obligatorio")
    @Positive(message = "El identificador de la orden debe ser un número positivo")
    private Long orderId;

    @NotNull(message = "El importe (amount) es obligatorio")
    @Min(value = 1, message = "El importe del pago debe ser mayor a cero")
    private Integer amount;
}