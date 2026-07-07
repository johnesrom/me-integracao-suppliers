package com.exemplo.mesuppliers.features.suppliers.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor(staticName = "of")
public class SupplierResponseDto {
    private String id;
    private String status;
    private String message;
}
