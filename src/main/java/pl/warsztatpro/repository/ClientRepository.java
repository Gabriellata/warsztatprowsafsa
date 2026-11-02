package pl.warsztatpro.repository;

import pl.warsztatpro.database.DatabaseConnection;
import pl.warsztatpro.model.Client;
import pl.warsztatpro.model.ClientType;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class ClientRepository {

    public Client insert(Client client) throws SQLException {
        String sql = """
                INSERT INTO clients (type, first_name, last_name, company_name, phone, email, postal_code, street, city)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)
                """;
        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            bindClient(statement, client);
            statement.executeUpdate();
            try (ResultSet keys = statement.getGeneratedKeys()) {
                if (keys.next()) {
                    client.setId(keys.getLong(1));
                }
            }
            return client;
        }
    }

    public List<Client> findAll() throws SQLException {
        String sql = "SELECT * FROM clients ORDER BY id";
        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql);
             ResultSet resultSet = statement.executeQuery()) {
            return mapAll(resultSet);
        }
    }

    public List<Client> search(String query) throws SQLException {
        String sql = """
                SELECT * FROM clients
                WHERE LOWER(IFNULL(first_name, '')) LIKE ?
                   OR LOWER(IFNULL(last_name, '')) LIKE ?
                   OR LOWER(IFNULL(company_name, '')) LIKE ?
                ORDER BY id
                """;
        String pattern = "%" + query.toLowerCase() + "%";
        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, pattern);
            statement.setString(2, pattern);
            statement.setString(3, pattern);
            try (ResultSet resultSet = statement.executeQuery()) {
                return mapAll(resultSet);
            }
        }
    }

    public Optional<Client> findById(long id) throws SQLException {
        String sql = "SELECT * FROM clients WHERE id = ?";
        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, id);
            try (ResultSet resultSet = statement.executeQuery()) {
                if (resultSet.next()) {
                    return Optional.of(mapClient(resultSet));
                }
                return Optional.empty();
            }
        }
    }

    public boolean update(Client client) throws SQLException {
        String sql = """
                UPDATE clients
                SET type = ?, first_name = ?, last_name = ?, company_name = ?,
                    phone = ?, email = ?, postal_code = ?, street = ?, city = ?
                WHERE id = ?
                """;
        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            bindClient(statement, client);
            statement.setLong(10, client.getId());
            return statement.executeUpdate() > 0;
        }
    }

    public boolean deleteById(long id) throws SQLException {
        String sql = "DELETE FROM clients WHERE id = ?";
        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, id);
            return statement.executeUpdate() > 0;
        }
    }

    private static void bindClient(PreparedStatement statement, Client client) throws SQLException {
        statement.setString(1, client.getType().name());
        statement.setString(2, emptyToNull(client.getFirstName()));
        statement.setString(3, emptyToNull(client.getLastName()));
        statement.setString(4, emptyToNull(client.getCompanyName()));
        statement.setString(5, client.getPhone());
        statement.setString(6, client.getEmail());
        statement.setString(7, client.getPostalCode());
        statement.setString(8, client.getStreet());
        statement.setString(9, client.getCity());
    }

    private static List<Client> mapAll(ResultSet resultSet) throws SQLException {
        List<Client> clients = new ArrayList<>();
        while (resultSet.next()) {
            clients.add(mapClient(resultSet));
        }
        return clients;
    }

    private static Client mapClient(ResultSet resultSet) throws SQLException {
        Client client = new Client();
        client.setId(resultSet.getLong("id"));
        client.setType(ClientType.valueOf(resultSet.getString("type")));
        client.setFirstName(resultSet.getString("first_name"));
        client.setLastName(resultSet.getString("last_name"));
        client.setCompanyName(resultSet.getString("company_name"));
        client.setPhone(resultSet.getString("phone"));
        client.setEmail(resultSet.getString("email"));
        client.setPostalCode(resultSet.getString("postal_code"));
        client.setStreet(resultSet.getString("street"));
        client.setCity(resultSet.getString("city"));
        return client;
    }

    private static String emptyToNull(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value;
    }
}
