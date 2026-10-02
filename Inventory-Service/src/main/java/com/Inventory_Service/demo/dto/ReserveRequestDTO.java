package com.Inventory_Service.demo.dto;

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
public class ReserveRequestDTO {

    @NotNull(message = "El identificador del producto es obligatorio")
    @Positive(message = "El identificador del producto debe ser positivo")
    private Long productId;

    @NotNull(message = "La cantidad a reservar es obligatoria")
    @Min(value = 1, message = "La cantidad solicitada debe ser de al menos 1 unidad")
    private Integer quantity;
}