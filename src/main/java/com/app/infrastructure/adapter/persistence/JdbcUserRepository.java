package com.app.infrastructure.adapter.persistence;

import com.app.domain.model.User;
import com.app.domain.port.out.UserRepository;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class JdbcUserRepository implements UserRepository {

    private final DatabaseConfig config;

    public JdbcUserRepository(DatabaseConfig config) {
        this.config = config;
    }

    @Override
    public Optional<User> findById(String id) {
        String sql = "SELECT id, firstname, lastname, email FROM users WHERE id = ?";
        try (Connection conn = config.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, id);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapResultSetToUser(rs));
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return Optional.empty();
    }

    @Override
    public void saveAll(List<User> users) {
        String sql = "INSERT INTO users (id, firstname, lastname, email) VALUES (?, ?, ?, ?) " +
                "ON CONFLICT(id) DO UPDATE SET " +
                "firstname = excluded.firstname, " +
                "lastname = excluded.lastname, " +
                "email = excluded.email";

        try (Connection conn = config.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            for (User user : users) {
                pstmt.setString(1, user.id());
                pstmt.setString(2, user.firstName());
                pstmt.setString(3, user.lastName());
                pstmt.setString(4, user.email());
                pstmt.addBatch();
            }
            pstmt.executeBatch();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    @Override
    public List<User> findAll() {
        List<User> users = new ArrayList<>();
        String sql = "SELECT id, firstname, lastname, email FROM users";
        try (Connection conn = config.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql);
             ResultSet rs = pstmt.executeQuery()) {
            while (rs.next()) {
                users.add(mapResultSetToUser(rs));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return users;
    }

    private User mapResultSetToUser(ResultSet rs) throws SQLException {
        return new User(
                rs.getString("id"),
                rs.getString("firstname"),
                rs.getString("lastname"),
                rs.getString("email")
        );
    }
}