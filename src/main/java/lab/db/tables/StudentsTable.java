package lab.db.tables;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

import lab.utils.Utils;
import lab.db.Table;
import lab.model.Student;

public final class StudentsTable implements Table<Student, Integer> {
	public static final String TABLE_NAME = "students";

	private final Connection connection;

	public StudentsTable(final Connection connection) {
		this.connection = Objects.requireNonNull(connection);
	}

	@Override
	public String getTableName() {
		return TABLE_NAME;
	}

	@Override
	public boolean createTable() {
		// 1. Create the statement from the open connection inside a try-with-resources
		try (final Statement statement = this.connection.createStatement()) {
			// 2. Execute the statement with the given query
			statement.executeUpdate("CREATE TABLE " + TABLE_NAME + " (" + "id INT NOT NULL PRIMARY KEY,"
					+ "firstName CHAR(40)," + "lastName CHAR(40)," + "birthday DATE" + ")");
			return true;
		} catch (final SQLException e) {
			// 3. Handle possible SQLExceptions
			return false;
		}
	}

//     @Override
//     public Optional<Student> findByPrimaryKey(final Integer id) {
//         try(final Statement statement = this.connection.createStatement()) {
//        	 final ResultSet resultSet = statement.executeQuery(
//        			 "SELECT *" +
//        			 "FROM" + TABLE_NAME +
//        	 		 "WHERE id = " + id
//        	 		 );
//        	 return this.readStudentsFromResultSet(resultSet).stream().findFirst();
//         } catch (final SQLException e) {
//        	 return Optional.empty();
//         }
//     }

	@Override
	public Optional<Student> findByPrimaryKey(final Integer id) {
		/*
		 * meglio concatenare le stringhe di query in una prepare statement per evitare
		 * SQLInjection attacks
		 */
		final String query = "SELECT *" + "FROM" + TABLE_NAME + "WHERE id = ?";
		try (final PreparedStatement statement = this.connection.prepareStatement(query)) {
			statement.setInt(1, id);
			final ResultSet resultSet = statement.executeQuery();
			return this.readStudentsFromResultSet(resultSet).stream().findFirst();
		} catch (final SQLException e) {
			return Optional.empty();
		}
	}

	/**
	 * Given a ResultSet read all the students in it and collects them in a List
	 * 
	 * @param resultSet a ResultSet from which the Student(s) will be extracted
	 * @return a List of all the students in the ResultSet
	 */
	private List<Student> readStudentsFromResultSet(final ResultSet resultSet) {
		final List<Student> l = new ArrayList<>();
		try {
			while (resultSet.next()) {
				final int id = resultSet.getInt("id");
				final String name = resultSet.getString("firstName");
				final String surname = resultSet.getString("lastName");
				final Date birthday = Utils.sqlDateToDate(resultSet.getDate("birthday"));
				final Student student = new Student(id, name, surname, Optional.of(birthday));
				l.add(student);
			}
		} catch (SQLException e) {
			e.printStackTrace();
		}
		return l;
	}

	@Override
	public List<Student> findAll() {
		final String query = "SELECT *" + "FROM " + TABLE_NAME;
		try (final PreparedStatement statement = this.connection.prepareStatement(query)) {
			final ResultSet resultSet = statement.executeQuery();
			return this.readStudentsFromResultSet(resultSet).stream().collect(Collectors.toList());
		} catch (final SQLException e) {
			return Collections.emptyList();
		}
	}

	public List<Student> findByBirthday(final Date date) {
		final String query = "SELECT *" + "FROM " + TABLE_NAME + "WHERE birthday = ?";
		try (final PreparedStatement statement = this.connection.prepareStatement(query)) {
			statement.setDate(1, Utils.dateToSqlDate(date));
			final ResultSet resultSet = statement.executeQuery();
			return this.readStudentsFromResultSet(resultSet).stream().collect(Collectors.toList());
		} catch (final SQLException e) {
			return Collections.emptyList();
		}
	}

	@Override
	public boolean dropTable() {
		final String query = "DROP" + TABLE_NAME;
		try (final PreparedStatement statement = this.connection.prepareStatement(query)) {
			statement.executeUpdate(query);
			return true;
		} catch (final SQLException e) {
			return false;
		}
	}

	@Override
	public boolean save(final Student student) {
		final String query = "INSERT INTO" + TABLE_NAME + "(id, firstName, lastName, birthday)" + "VALUES ("
				+ student.getId() + ", " + student.getFirstName() + ", "
				+ (student.getBirthday().isPresent() ? Utils.dateToSqlDate(student.getBirthday().get()) : "");
		try (final PreparedStatement statement = this.connection.prepareStatement(query)) {
			statement.executeUpdate(query);
			return true;
		} catch (final SQLException e) {
			return false;
		}
	}

	@Override
	public boolean delete(final Integer id) {
		final String query = "DELETE FROM" + TABLE_NAME + "WHERE id = ?";
		try (final PreparedStatement statement = this.connection.prepareStatement(query)) {
			statement.setInt(1, id);
			statement.executeUpdate(query);
			return true;
		} catch (final SQLException e) {
			return false;
		}
	}

	@Override
	public boolean update(final Student student) {
		if (this.findByPrimaryKey(student.getId()).isPresent()) {
			final String query = "UPDATE" + TABLE_NAME + "SET id = ?, firstName = ?, lastName = ?, birthday = ?";
			try (final PreparedStatement statement = this.connection.prepareStatement(query)) {
				statement.setInt(1, student.getId());
				statement.setString(2, student.getFirstName());
				statement.setString(3, student.getLastName());
				if (student.getBirthday().isPresent()) {
					statement.setDate(4, Utils.dateToSqlDate(student.getBirthday().get()));
				}
				statement.executeUpdate(query);
				return true;
			} catch (final SQLException e) {
				return false;
			}
		} else {
			return false;
		}
	}
}