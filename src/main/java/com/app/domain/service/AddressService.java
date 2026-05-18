package com.app.domain.service;

import com.app.domain.model.Address;
import com.app.infrastructure.adapter.persistence.DatabaseConfig;
import com.app.infrastructure.sync.AddressSqliteGateway;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class AddressService {

    private final AddressSqliteGateway sqliteGateway;

    public AddressService(DatabaseConfig databaseConfig) {
        this.sqliteGateway = new AddressSqliteGateway(databaseConfig);
    }

    public List<Address> getAllAddresses() {
        try (Connection connection = sqliteGateway.openLocalConnection()) {
            Map<String, Address> addressMap = sqliteGateway.loadAddressesById(connection);
            return new ArrayList<>(addressMap.values());
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    public void updateAddress(Address address) {
        try (Connection connection = sqliteGateway.openLocalConnection()) {
            sqliteGateway.upsertAddress(connection, address);
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }
}