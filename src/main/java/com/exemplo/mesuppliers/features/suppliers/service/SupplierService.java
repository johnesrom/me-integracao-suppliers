package com.exemplo.mesuppliers.features.suppliers.service;

import com.exemplo.mesuppliers.features.suppliers.dto.SupplierPayloadDto;
import com.exemplo.mesuppliers.features.suppliers.dto.SupplierResponseDto;

public interface SupplierService {
    SupplierResponseDto create(SupplierPayloadDto requestDto);
    SupplierResponseDto update(SupplierPayloadDto requestDto);
}
