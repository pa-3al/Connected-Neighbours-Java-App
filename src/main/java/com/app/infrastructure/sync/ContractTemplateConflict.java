package com.app.infrastructure.sync;
import com.app.domain.model.ContractTemplate;

public record ContractTemplateConflict(ContractTemplate local, ContractTemplate server) {}