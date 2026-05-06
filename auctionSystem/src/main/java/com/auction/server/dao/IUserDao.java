package com.auction.server.dao;

import com.auction.shared.models.AuthUser;

import java.sql.SQLException;

public interface IUserDao {
    boolean existsByUsernameOrEmail(String username, String email) throws SQLException;

    void register(AuthUser user) throws SQLException;

    AuthUser findByUsername(String username) throws SQLException;
}