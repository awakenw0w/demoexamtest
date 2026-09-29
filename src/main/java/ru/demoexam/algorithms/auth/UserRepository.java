package ru.demoexam.algorithms.auth;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

/** Отвечает за создание базы пользователей и проверку данных для входа. */
public class UserRepository {
    private static final String DATABASE_URL = "jdbc:sqlite:users.db";

    /** Подготавливает таблицу и добавляет демо-учётки при первом запуске. */
    public UserRepository() throws SQLException {
        createTable();
        addDemoUsersIfDatabaseIsEmpty();
    }

    /** Создаёт минимальную таблицу пользователей, если её ещё нет. */
    private void createTable() throws SQLException {
        String sql = "CREATE TABLE IF NOT EXISTS users ("
                + "id INTEGER PRIMARY KEY AUTOINCREMENT, "
                + "login TEXT NOT NULL UNIQUE, "
                + "password_hash TEXT NOT NULL, "
                + "role TEXT NOT NULL)";

        try (Connection connection = DriverManager.getConnection(DATABASE_URL);
             Statement statement = connection.createStatement()) {
            statement.execute(sql);
        }
    }

    /** Добавляет администратора и пользователя только в новую пустую базу. */
    private void addDemoUsersIfDatabaseIsEmpty() throws SQLException {
        String countSql = "SELECT COUNT(*) FROM users";
        boolean isEmpty;

        try (Connection connection = DriverManager.getConnection(DATABASE_URL);
             Statement statement = connection.createStatement();
             ResultSet result = statement.executeQuery(countSql)) {
            isEmpty = result.next() && result.getInt(1) == 0;
        }

        // Добавляем демо-пользователей только после закрытия запроса к таблице.
        if (isEmpty) {
            try (Connection connection = DriverManager.getConnection(DATABASE_URL)) {
                addUser(connection, "admin", "Admin123!", "admin");
                addUser(connection, "user", "User123!", "user");
            }
        }
    }

    /** Добавляет одну запись с хешированным паролем. */
    private void addUser(Connection connection, String login, String password, String role)
            throws SQLException {
        String sql = "INSERT INTO users (login, password_hash, role) VALUES (?, ?, ?)";

        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, login);
            statement.setString(2, PasswordHasher.hashPassword(password));
            statement.setString(3, role);
            statement.executeUpdate();
        }
    }

    /** Проверяет пару логин/пароль по записи в SQLite. */
    public boolean authenticate(String login, String password) throws SQLException {
        String sql = "SELECT password_hash FROM users WHERE login = ?";

        try (Connection connection = DriverManager.getConnection(DATABASE_URL);
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, login);

            try (ResultSet result = statement.executeQuery()) {
                return result.next()
                        && PasswordHasher.passwordMatches(password, result.getString("password_hash"));
            }
        }
    }
}
