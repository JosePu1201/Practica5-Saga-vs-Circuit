package com.Order_Service.demo.dto;

import com.Order_Service.demo.enums.StatusOrder;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class OrderStatusUpdateDTO {

    @NotNull(message = "El nuevo estado del pedido es obligatorio")
    private StatusOrder status;
}