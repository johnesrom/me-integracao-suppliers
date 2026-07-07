package com.exemplo.mesuppliers.features.authentication.service;

public interface AuthenticationService {

    String getValidBearerToken();

    String refreshBearerToken();
}
