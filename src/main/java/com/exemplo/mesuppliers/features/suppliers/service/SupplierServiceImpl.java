package com.exemplo.mesuppliers.features.suppliers.service;

import com.exemplo.mesuppliers.features.suppliers.dto.SupplierPayloadDto;
import com.exemplo.mesuppliers.features.suppliers.dto.SupplierResponseDto;
import com.exemplo.mesuppliers.integration.me.MeRequestClient;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class SupplierServiceImpl implements SupplierService {

    private static final String MODULE = "suppliers";
    private final MeRequestClient meRequestClient;

    public SupplierServiceImpl(MeRequestClient meRequestClient) { this.meRequestClient = meRequestClient; }

    @Override
    public SupplierResponseDto create(SupplierPayloadDto requestDto) {
        meRequestClient.send(MODULE, HttpMethod.POST, "/v1/suppliers", requestDto, requestDto.getClientSupplierId());
        return SupplierResponseDto.of(requestDto.getClientSupplierId(), "SENT", "Cadastro enviado ao ME");
    }

    @Override
    public SupplierResponseDto update(SupplierPayloadDto requestDto) {
        String supplierId = requestDto.getMeId();
        if (!StringUtils.hasText(supplierId)) throw new IllegalArgumentException("ID_ME nao informado para update de supplier.");
        meRequestClient.send(MODULE, HttpMethod.PUT, "/v1/suppliers/{supplierId}", requestDto, requestDto.getClientSupplierId(), supplierId);
        return SupplierResponseDto.of(supplierId, "SENT", "Atualizacao enviada ao ME");
    }
}
