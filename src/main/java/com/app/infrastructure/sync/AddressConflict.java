package com.app.infrastructure.sync;

import com.app.domain.model.Address;

public record AddressConflict(Address local, Address server) {}