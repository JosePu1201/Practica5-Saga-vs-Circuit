package com.Order_Service.demo.dto;

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
public class OrderRequestDTO {

    @NotNull(message = "El identificador del cliente (customerId) es obligatorio")
    @Positive(message = "El identificador del cliente debe ser un número entero positivo")
    private Long customerId;

    @NotNull(message = "El monto total de la orden es obligatorio")
    @Min(value = 0, message = "El monto total no puede ser negativo")
    private Integer total;

    @NotNull(message = "El identificador del producto es obligatorio")
    @Positive(message = "El identificador del producto debe ser un número entero positivo")
    private Long productId;

    @NotNull(message = "La cantidad es obligatoria")
    @Positive(message = "La cantidad debe ser un número entero positivo")
    private Integer quantity;

    @NotNull(message = "La dirección es obligatoria")
    private String address;
}