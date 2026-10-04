package com.Inventory_Service.demo.dto;

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
public class ReleaseReserveRequestDTO {

    @NotNull(message = "El identificador de la reserva (reserveId) es obligatorio")
    @Positive(message = "El identificador de la reserva debe ser positivo")
    private Long reserveId;
}