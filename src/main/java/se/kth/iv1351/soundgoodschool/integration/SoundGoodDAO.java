package se.kth.iv1351.soundgoodschool.integration;

import se.kth.iv1351.soundgoodschool.model.InstrumentForRent;
import se.kth.iv1351.soundgoodschool.model.RentalPeriod;
import se.kth.iv1351.soundgoodschool.model.RentalPeriodDTO;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class SoundGoodDAO {
    private static final String INSTR_FOR_RENT_TABLE_NAME = "instrument_for_rent";
    private static final String INSTR_FOR_RENT_PK_COLUMN_NAME = "id";
    private static final String MONTHLY_FEE_COLUMN_NAME = "instrument_monthly_fee";
    private static final String BRAND_COLUMN_NAME = "brand";
    private static final String INSTR_ID_COLUMN_NAME = "instrument_id";


    private static final String INSTR_TABLE_NAME = "instrument";
    private static final String INSTR_PK_COLUMN_NAME = "id";
    private static final String INSTR_TYPE_COLUMN_NAME = "instrument_type";
    private static final String INSTRUCTOR_COLUMN_NAME = "instructor_id";

    private static final String RENTAL_PERIOD_TABLE_NAME = "rental_period";
    private static final String RENTAL_PERIOD_PK1_COLUMN_NAME = "student_id";
    private static final String RENTAL_PERIOD_PK2_COLUMN_NAME = "instrument_for_rent_id";
    private static final String START_DATE_COLUMN_NAME = "start_date";
    private static final String END_DATE_COLUMN_NAME = "end_date";

    private static final String PERSON_TABLE_NAME = "person";
    private static final String PERSON_PK_COLUMN_NAME = "id";
    private static final String SSN_COLUMN_NAME = "ssn";

    private static final String SKILL_LVL_TABLE_NAME = "skill_level";
    private static final String SKILL_LVL_PK_COLUMN_NAME = "instrument_id";
    private static final String CUR_SKILL_LVL_COLUMN_NAME = "current_skill_level";

    private static final String RENTED_INSTR_COUNT_NAME = "number_of_rented_instruments";

    private static final String ON_EQUAL = " = ";
    private static final String LEFT = " LEFT JOIN ";
    private static final String INNER = " INNER JOIN ";
    private static final String FULL = " FULL JOIN ";

    private static final String WILDCARD = " ? ";
    private static final String EQUALS = " = ";
    private static final String IS_NULL = " IS NULL ";

    private Connection connection;
    private PreparedStatement findAllRentableInstrumentsStmt;
    private PreparedStatement setInstrAsRentedStmt;
    private PreparedStatement findNumberOfRentedInstrumentsByStudentSSNStmt;
    private PreparedStatement findAvailableInstrumentForRentIdsByInstrumentTypeStmt;
    private PreparedStatement findStudentIdByStudentSSNStmt;

    /**
     * Constructs a new DAO object connected to the SoundGood Music database.
     */
    public SoundGoodDAO() throws SoundGoodDBException {
        try {
            connectToDB();
            prepareStatements();
        } catch (ClassNotFoundException | SQLException e) {
            throw new SoundGoodDBException("Could not connect to datasource.", e);
        }
    }

    public List<InstrumentForRent> findAllRentableInstruments(String unprocessedInstrumentType) throws SoundGoodDBException {
        String failureMsg = "Failed to list rentable instruments";
        ResultSet rs = null;

        List<InstrumentForRent> instruments = null;
        String instrumentType = firstLetterToUpperCaseAndRestToLowerCase(unprocessedInstrumentType);

        try {
            findAllRentableInstrumentsStmt.setString(1, instrumentType);
            rs = findAllRentableInstrumentsStmt.executeQuery();
            instruments = new ArrayList<InstrumentForRent>();
            while(rs.next()) {
                instruments.add(new InstrumentForRent(
                        rs.getInt(INSTR_FOR_RENT_PK_COLUMN_NAME),
                        rs.getInt(MONTHLY_FEE_COLUMN_NAME),
                        rs.getString(BRAND_COLUMN_NAME),
                        rs.getString(INSTR_TYPE_COLUMN_NAME)
                ));
            }
            rs.close();
        } catch (Exception e) {
            handleException(failureMsg, e);
        } finally {
            closeResultSet(failureMsg, rs);
        }

        return instruments;
    }

    public void setInstrAsRented(RentalPeriodDTO rentalPeriod) throws SoundGoodDBException {
        String failureMsg = "Failed to rent instrument.";

        try {
            setInstrAsRentedStmt.setLong(1, rentalPeriod.getStudentId());
            setInstrAsRentedStmt.setLong(2, rentalPeriod.getInstrumentForRentId());
            setInstrAsRentedStmt.setTimestamp(3, rentalPeriod.getEndDate());
            System.out.println("1");
            int result = setInstrAsRentedStmt.executeUpdate();
            System.out.println("2");
            if(result != 1) handleException(failureMsg, null);
        } catch(Exception e) {
            handleException(failureMsg, e);
        }
    }

    public Integer findNumberOfRentedInstrumentsByStudentSSN(String ssn) throws SoundGoodDBException {
        String failureMsg = "Failed to retrieve number of rented instruments";
        ResultSet rs = null;

        Integer numberOfRentedInstruments = 0;

        try {
            findNumberOfRentedInstrumentsByStudentSSNStmt.setString(1, ssn);
            rs = findNumberOfRentedInstrumentsByStudentSSNStmt.executeQuery();
            if(rs.next()) numberOfRentedInstruments = rs.getInt(RENTED_INSTR_COUNT_NAME);
        } catch(Exception e) {
            handleException(failureMsg, e);
        } finally {
            closeResultSet(failureMsg, rs);
        }

        return numberOfRentedInstruments;
    }

    public ArrayList<Long> findAvailableInstrumentForRentIdsByInstrumentType(String unprocessedInstrumentType,
                                                                             String unprocessedBrand) throws SoundGoodDBException {
        String failureMsg = "Failed to retrieve ids for available rental instruments";
        ResultSet rs = null;

        ArrayList<Long> availableInstrumentForRentIds = new ArrayList<>();
        String instrumentType = firstLetterToUpperCaseAndRestToLowerCase(unprocessedInstrumentType);
        String brand = firstLetterToUpperCaseAndRestToLowerCase(unprocessedBrand);

        try {
            findAvailableInstrumentForRentIdsByInstrumentTypeStmt.setString(1, instrumentType);
            findAvailableInstrumentForRentIdsByInstrumentTypeStmt.setString(2, brand);
            rs = findAvailableInstrumentForRentIdsByInstrumentTypeStmt.executeQuery();
            while(rs.next()) availableInstrumentForRentIds.add(rs.getLong(INSTR_FOR_RENT_PK_COLUMN_NAME));
        } catch (Exception e) {
            handleException(failureMsg, e);
        } finally {
            closeResultSet(failureMsg, rs);
        }

        return availableInstrumentForRentIds;
    }

    public Long findStudentIdByStudentSSN(String studentSsn) throws SoundGoodDBException {
        String failureMsg = "Failed to retrieve student id";
        ResultSet rs = null;

        Long studentId = null;

        try {
            findStudentIdByStudentSSNStmt.setString(1, studentSsn);
            rs = findStudentIdByStudentSSNStmt.executeQuery();
            if(rs.next()) studentId = rs.getLong(PERSON_PK_COLUMN_NAME);
        } catch (Exception e) {
            handleException(failureMsg, e);
        } finally {
            closeResultSet(failureMsg, rs);
        }

        return studentId;
    }

    /**
     * Commits the current transaction.
     *
     * @throws SoundGoodDBException If unable to commit the current transaction.
     */
    public void commit() throws SoundGoodDBException {
        try {
            connection.commit();
        } catch (SQLException e) {
            handleException("Failed to commit", e);
        }
    }

    private void connectToDB() throws ClassNotFoundException, SQLException {
        connection = DriverManager.getConnection(
                "jdbc:postgresql://localhost:5432/soundgood",
                "postgres",
                "password"
        );
        connection.setAutoCommit(false);
    }

    private void prepareStatements() throws SQLException {
        prepareFindAllRentableInstrumentsStmt();
        prepareSetInstrAsRentedStmt();
        prepareFindNumberOfRentedInstrumentsByStudentSSNStmt();
        prepareFindAvailableInstrumentForRentIdsByInstrumentType();
        prepareFindStudentIdByStudentSSN();
    }

    private void prepareFindAllRentableInstrumentsStmt() throws SQLException {
        final String SUBQUERY_TABLE_NAME = "rent_instr";

        findAllRentableInstrumentsStmt = connection.prepareStatement(
            select(
                INSTR_FOR_RENT_PK_COLUMN_NAME,
                MONTHLY_FEE_COLUMN_NAME,
                BRAND_COLUMN_NAME,
                INSTR_TYPE_COLUMN_NAME
            ) +
            from(
                subquery(
                    select(
                        specifyTableForColumn(
                            INSTR_FOR_RENT_TABLE_NAME,
                            INSTR_FOR_RENT_PK_COLUMN_NAME
                        ),
                        MONTHLY_FEE_COLUMN_NAME,
                        BRAND_COLUMN_NAME,
                        INSTR_TYPE_COLUMN_NAME
                    ),
                    from(INSTR_FOR_RENT_TABLE_NAME),
                    join(LEFT, ON_EQUAL,
                        INSTR_TABLE_NAME,

                        INSTR_ID_COLUMN_NAME,
                        specifyTableForColumn(
                            INSTR_TABLE_NAME,
                            INSTR_PK_COLUMN_NAME
                        )
                    )
                ) + as(SUBQUERY_TABLE_NAME)
            ) +
            join(LEFT, ON_EQUAL,
                RENTAL_PERIOD_TABLE_NAME,

                RENTAL_PERIOD_PK2_COLUMN_NAME,
                specifyTableForColumn(
                        SUBQUERY_TABLE_NAME,
                        INSTR_PK_COLUMN_NAME
                )
            ) +
            where(INSTR_TYPE_COLUMN_NAME, EQUALS, WILDCARD) +
            and(RENTAL_PERIOD_PK2_COLUMN_NAME + IS_NULL) + ";"
        );
    }

    private void prepareSetInstrAsRentedStmt() throws SQLException {
        String  value1 = WILDCARD,
                value2 = WILDCARD,
                value3 = "CURRENT_TIMESTAMP(0)",
                value4 = WILDCARD;

        setInstrAsRentedStmt = connection.prepareStatement(
            insertInto(
                RENTAL_PERIOD_TABLE_NAME,

                RENTAL_PERIOD_PK1_COLUMN_NAME,
                RENTAL_PERIOD_PK2_COLUMN_NAME,
                START_DATE_COLUMN_NAME,
                END_DATE_COLUMN_NAME,

                value1,
                value2,
                value3,
                value4
            )
        );
    }

    private void prepareFindNumberOfRentedInstrumentsByStudentSSNStmt() throws SQLException {
        final String SUBQUERY_TABLE_NAME = "specified_student_id";

        findNumberOfRentedInstrumentsByStudentSSNStmt = connection.prepareStatement(
            selectCountAs(RENTED_INSTR_COUNT_NAME) +
                from(RENTAL_PERIOD_TABLE_NAME) +
                join(INNER, ON_EQUAL,
                    subquery(
                        select(PERSON_PK_COLUMN_NAME),
                        from(PERSON_TABLE_NAME),
                        where(SSN_COLUMN_NAME, EQUALS, WILDCARD)
                    ) + as(SUBQUERY_TABLE_NAME),

                    PERSON_PK_COLUMN_NAME,
                    specifyTableForColumn(
                            RENTAL_PERIOD_TABLE_NAME,
                            RENTAL_PERIOD_PK1_COLUMN_NAME
                    )
                ) + ";"
        );
    }

    private void prepareFindAvailableInstrumentForRentIdsByInstrumentType() throws SQLException {
        final String SUBQUERY_TABLE_NAME = "selected_instrument";

        findAvailableInstrumentForRentIdsByInstrumentTypeStmt = connection.prepareStatement(
                select(
                    specifyTableForColumn(
                        INSTR_FOR_RENT_TABLE_NAME,
                        INSTR_FOR_RENT_PK_COLUMN_NAME
                    )
                ) +
                from(
                    subquery(
                        select(
                            specifyTableForColumn(
                                INSTR_TABLE_NAME,
                                INSTR_PK_COLUMN_NAME
                            )
                        ),
                        from(SKILL_LVL_TABLE_NAME),
                        join(FULL, ON_EQUAL,
                            INSTR_TABLE_NAME,

                            specifyTableForColumn(
                                INSTR_TABLE_NAME,
                                INSTR_PK_COLUMN_NAME
                            ),
                            specifyTableForColumn(
                                SKILL_LVL_TABLE_NAME,
                                SKILL_LVL_PK_COLUMN_NAME
                            )
                        ) +
                        where(CUR_SKILL_LVL_COLUMN_NAME, IS_NULL) +
                        and(INSTRUCTOR_COLUMN_NAME + IS_NULL) +
                        and(INSTR_TYPE_COLUMN_NAME + EQUALS + WILDCARD)
                    ) + as(SUBQUERY_TABLE_NAME)
                ) +
                join(LEFT, ON_EQUAL,
                    INSTR_FOR_RENT_TABLE_NAME,

                    specifyTableForColumn(
                            INSTR_FOR_RENT_TABLE_NAME,
                            INSTR_ID_COLUMN_NAME
                    ),
                    specifyTableForColumn(
                            SUBQUERY_TABLE_NAME,
                            "id"
                    )
                ) +
                join(FULL, ON_EQUAL,
                    RENTAL_PERIOD_TABLE_NAME,

                    specifyTableForColumn(
                        INSTR_FOR_RENT_TABLE_NAME,
                        INSTR_FOR_RENT_PK_COLUMN_NAME
                    ),
                    RENTAL_PERIOD_PK2_COLUMN_NAME
                ) +
                where(RENTAL_PERIOD_PK2_COLUMN_NAME, IS_NULL) +
                and(BRAND_COLUMN_NAME + EQUALS + WILDCARD) + ";"
        );
    }

    private void prepareFindStudentIdByStudentSSN() throws SQLException {
        findStudentIdByStudentSSNStmt = connection.prepareStatement(
                select(
                    PERSON_PK_COLUMN_NAME
                ) +
                from(PERSON_TABLE_NAME) +
                where(SSN_COLUMN_NAME, EQUALS, WILDCARD)
        );
    }

    private String and(String stmt) {
        return " AND " + stmt;
    }

    private String as(String tableName) {
        return " AS " + tableName;
    }

    private String from(String table) {
        return " FROM " + table;
    }

    private String insertInto(String table, String column1, String column2, String column3, String column4,
                              String value1, String value2, String value3, String value4) {
        return " INSERT INTO " + table + " (" +
                    column1 + ", " +
                    column2 + ", " +
                    column3 + ", " +
                    column4 + ") " +

                " VALUES (" +
                    value1 + ", " +
                    value2 + ", " +
                    value3 + ", " +
                    value4 + ");";
    }

    private String join(String joinOperator, String joinCondition, String table, String column1, String column2) {
        return " " + joinOperator + " " + table + " ON " + column1 + " " + joinCondition + " " + column2;
    }

    private String select(String column) {
        return " SELECT " + column;
    }

    private String select(String column1, String column2, String column3, String column4) {
        return " SELECT " + column1 + ", " + column2 + ", " + column3 + ", " + column4;
    }

    private String selectCountAs(String countName) {
        return " SELECT COUNT(*) AS " + countName;
    }

    private String specifyTableForColumn(String table, String column) {
        return table + "." + column;
    }

    private String subquery(String stmt1, String stmt2, String stmt3) {
        return " (" + stmt1 + " " + stmt2 + " " + stmt3 + ") ";
    }

    private String where(String column1, String condition) {
        return " WHERE " + column1 + " " + condition;
    }

    private String where(String column1, String condition, String column2) {
        return " WHERE " + column1 + " " + condition + " " + column2;
    }

    private void closeResultSet(String failureMsg, ResultSet result) throws SoundGoodDBException {
        try {
            result.close();
        } catch(Exception e) {
            throw new SoundGoodDBException(failureMsg + " Could not close result set.", e);
        }
    }

    private void handleException(String failureMsg, Exception cause) throws SoundGoodDBException {
        String completeFailureMsg = failureMsg;
        try {
            connection.rollback();
        } catch (SQLException rollbackExc) {
            completeFailureMsg = completeFailureMsg +
                    ". Also failed to rollback transaction because of: " + rollbackExc.getMessage();
        }

        if (cause != null) {
            throw new SoundGoodDBException(failureMsg, cause);
        } else {
            throw new SoundGoodDBException(failureMsg);
        }
    }

    private String firstLetterToUpperCaseAndRestToLowerCase(String str) {
        return str.substring(0, 1).toUpperCase() + str.substring(1).toLowerCase();
    }
}
