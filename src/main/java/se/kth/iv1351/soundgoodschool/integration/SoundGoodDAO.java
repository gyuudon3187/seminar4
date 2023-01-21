package se.kth.iv1351.soundgoodschool.integration;

import se.kth.iv1351.soundgoodschool.model.InstrumentForRent;
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
    private static final String RETURN_DATE_COLUMN_NAME = "actual_return_date";

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
    private static final String IS_NOT_NULL = " IS NOT NULL ";

    private Connection connection;
    private PreparedStatement readAllRentableInstrumentsByInstrumentTypeStmt;
    private PreparedStatement createInstrumentRentalStmt;
    private PreparedStatement readNumberOfRentedInstrumentsByStudentSSNStmt;
    private PreparedStatement readAvailableInstrumentForRentIdsByInstrumentTypeStmt;
    private PreparedStatement readStudentIdByStudentSSNStmt;
    private PreparedStatement readCurrentlyRentedInstrumentIdByInstrTypeBrandAndStudentIdStmt;
    private PreparedStatement updateRentalReturnDateStmt;

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

    /**
     * Reads all rentable instruments based on the specified instrument type.
     *
     * @param unprocessedInstrumentType the instrument type before the first letter is converted to upper case and the
     *                                  rest to lower case
     * @return all rentable instruments of the specified instrument type
     * @throws SoundGoodDBException thrown if the instruments cannot be retrieved
     */
    public List<InstrumentForRent> readAllRentableInstrumentsByInstrumentType(String unprocessedInstrumentType)
            throws SoundGoodDBException {
        String failureMsg = "Failed to list rentable instruments";
        ResultSet rs = null;

        List<InstrumentForRent> instruments = null;
        String instrumentType = firstLetterToUpperCaseAndRestToLowerCase(unprocessedInstrumentType);
        try {
            dummySelectForUpdate(RENTAL_PERIOD_TABLE_NAME);
            dummySelectForUpdate(INSTR_FOR_RENT_TABLE_NAME);
            readAllRentableInstrumentsByInstrumentTypeStmt.setString(1, instrumentType);
            rs = readAllRentableInstrumentsByInstrumentTypeStmt.executeQuery();
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

    /**
     * Updates an instrument for rent as rented by a specified student until a specified end date.
     *
     * @param rentalPeriod a DTO containing the student's id, the rental instrument's id and the end date
     * @throws SoundGoodDBException thrown if:
     *                              1) the instrument is unavailable
     *                              2) the student's rental quota is exceeded
     *                              3) the rental period is longer than one year
     */
    public boolean createInstrumentRental(RentalPeriodDTO rentalPeriod) throws SoundGoodDBException {
        String failureMsg = "Failed to rent instrument.";

        boolean rentalSuccessful = false;

        try {
            createInstrumentRentalStmt.setLong(1, rentalPeriod.getStudentId());
            createInstrumentRentalStmt.setLong(2, rentalPeriod.getInstrumentForRentId());
            createInstrumentRentalStmt.setTimestamp(3, rentalPeriod.getEndDate());
            int result = createInstrumentRentalStmt.executeUpdate();

            if(result != 1) {
                handleException(failureMsg, null);
            } else {
                rentalSuccessful = true;
            }
        } catch(Exception e) {
            handleException(failureMsg, e);
        }

        return rentalSuccessful;
    }

    /**
     * Reads the number of instruments that has been rented by the specified student.
     *
     * @param studentSsn the student's social security number
     * @return the number of instruments that has been rented by the student
     * @throws SoundGoodDBException thrown if the number of instruments cannot be retrieved
     */
    public Integer readNumberOfRentedInstrumentsByStudentSSN(String studentSsn) throws SoundGoodDBException {
        String failureMsg = "Failed to retrieve number of rented instruments";
        ResultSet rs = null;

        Integer numberOfRentedInstruments = 0;

        try {
            dummySelectForUpdate(RENTAL_PERIOD_TABLE_NAME);
            dummySelectForUpdate(INSTR_FOR_RENT_TABLE_NAME);
            readNumberOfRentedInstrumentsByStudentSSNStmt.setString(1, studentSsn);
            rs = readNumberOfRentedInstrumentsByStudentSSNStmt.executeQuery();
            if(rs.next()) numberOfRentedInstruments = rs.getInt(RENTED_INSTR_COUNT_NAME);
        } catch(Exception e) {
            e.printStackTrace();
            handleException(failureMsg, e);
        } finally {
            closeResultSet(failureMsg, rs);
        }

        return numberOfRentedInstruments;
    }

    /**
     * Reads the ID:s of all instruments available for rent of a specified instrument type.
     *
     * @param unprocessedInstrumentType the instrument type before the first letter is converted to upper case and the
     *                                  rest to lower case
     * @param unprocessedBrand the instrument brand before the first letter is converted to upper case and the
     *                         rest to lower case
     * @return the ID:s of all instruments available for rent of a specified instrument type
     * @throws SoundGoodDBException thrown if the ID:s cannot be retrieved
     */
    public ArrayList<Long> readAvailableInstrumentForRentIdsByInstrumentType(String unprocessedInstrumentType,
                                                                             String unprocessedBrand) throws SoundGoodDBException {
        String failureMsg = "Failed to retrieve ids for available rental instruments";
        ResultSet rs = null;

        ArrayList<Long> availableInstrumentForRentIds = new ArrayList<>();
        String instrumentType = firstLetterToUpperCaseAndRestToLowerCase(unprocessedInstrumentType);
        String brand = firstLetterToUpperCaseAndRestToLowerCase(unprocessedBrand);

        try {
            dummySelectForUpdate(RENTAL_PERIOD_TABLE_NAME);
            dummySelectForUpdate(INSTR_FOR_RENT_TABLE_NAME);
            readAvailableInstrumentForRentIdsByInstrumentTypeStmt.setString(1, instrumentType);
            readAvailableInstrumentForRentIdsByInstrumentTypeStmt.setString(2, brand);
            rs = readAvailableInstrumentForRentIdsByInstrumentTypeStmt.executeQuery();
            while(rs.next()) availableInstrumentForRentIds.add(rs.getLong(INSTR_FOR_RENT_PK_COLUMN_NAME));
        } catch (Exception e) {
            handleException(failureMsg, e);
        } finally {
            closeResultSet(failureMsg, rs);
        }

        return availableInstrumentForRentIds;
    }

    /**
     * Reads the ID of a student based on the student's social security number.
     *
     * @param studentSsn the student's social security number
     * @return the student's ID
     * @throws SoundGoodDBException thrown if student ID cannot be retrieved
     */
    public Long readStudentIdByStudentSSN(String studentSsn) throws SoundGoodDBException {
        String failureMsg = "Failed to retrieve student id";
        ResultSet rs = null;

        Long studentId = null;

        try {
            readStudentIdByStudentSSNStmt.setString(1, studentSsn);
            rs = readStudentIdByStudentSSNStmt.executeQuery();
            if(rs.next()) studentId = rs.getLong(PERSON_PK_COLUMN_NAME);
        } catch (Exception e) {
            handleException(failureMsg, e);
        } finally {
            closeResultSet(failureMsg, rs);
        }

        return studentId;
    }

    /**
     * Reads the ID of an instrument currently rented by the student based on the specified instrument type,
     * instrument brand and the student's ID.
     *
     * @param unprocessedInstrumentType the instrument type before the first letter is converted to upper case and the
     *                                  rest to lower case
     * @param unprocessedBrand the instrument brand before the first letter is converted to upper case and the
     *                         rest to lower case
     * @param studentId the student's ID
     * @return the ID of the currently rented instrument
     * @throws SoundGoodDBException thrown if the instrument ID cannot be retrieved
     */
    public Long readCurrentlyRentedInstrumentIdByInstrTypeBrandAndStudentId(
            String unprocessedInstrumentType,
            String unprocessedBrand,
            Long studentId) throws SoundGoodDBException {
        String failureMsg = "Failed to retrieve instrument id";
        ResultSet rs = null;

        String instrumentType = firstLetterToUpperCaseAndRestToLowerCase(unprocessedInstrumentType);
        String brand = firstLetterToUpperCaseAndRestToLowerCase(unprocessedBrand);

        Long currentlyRentedInstrumentId = null;

        try {
            dummySelectForUpdate(RENTAL_PERIOD_TABLE_NAME);
            dummySelectForUpdate(INSTR_FOR_RENT_TABLE_NAME);
            readCurrentlyRentedInstrumentIdByInstrTypeBrandAndStudentIdStmt.setLong(1, studentId);
            readCurrentlyRentedInstrumentIdByInstrTypeBrandAndStudentIdStmt.setString(2, brand);
            readCurrentlyRentedInstrumentIdByInstrTypeBrandAndStudentIdStmt.setString(3, instrumentType);
            rs = readCurrentlyRentedInstrumentIdByInstrTypeBrandAndStudentIdStmt.executeQuery();
            if(rs.next()) currentlyRentedInstrumentId = rs.getLong(RENTAL_PERIOD_PK2_COLUMN_NAME);
        } catch (Exception e) {
            handleException(failureMsg, e);
        } finally {
            closeResultSet(failureMsg, rs);
        }

        return currentlyRentedInstrumentId;
    }

    /**
     * Updates the date on which the rented instrument was returned.
     *
     * @param studentId the id of the student that rented the instrument
     * @param instrumentId the id of the instrument rented by the student
     * @return whether the update was successful or not
     * @throws SoundGoodDBException thrown if update cannot be completed
     */
    public boolean updateRentalReturnDate(Long studentId, Long instrumentId) throws SoundGoodDBException {
        String failureMsg = "Failed to update return date of rental";
        int result = 0;

        boolean updateSuccessful = true;

        try {
            updateRentalReturnDateStmt.setLong(1, studentId);
            updateRentalReturnDateStmt.setLong(2, instrumentId);
            result = updateRentalReturnDateStmt.executeUpdate();
            if(result != 1) handleException(failureMsg, null);
        } catch (Exception e) {
            handleException(failureMsg, e);
        }

        return updateSuccessful;
    }

    /**
     * Commits the current transaction.
     *
     * @throws SoundGoodDBException thrown if unable to commit the current transaction
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
        prepareReadAllRentableInstrumentsByInstrumentTypeStmt();
        prepareCreateInstrumentRentalStmt();
        prepareReadNumberOfRentedInstrumentsByStudentSSNStmt();
        prepareReadAvailableInstrumentForRentIdsByInstrumentTypeStmt();
        prepareReadStudentIdByStudentSSNStmt();
        prepareReadCurrentlyRentedInstrumentIdByInstrTypeBrandAndStudentIdStmt();
        prepareCreateInstrumentRentalStmt();
        prepareUpdateRentalReturnDateStmt();
    }

    private void prepareReadAllRentableInstrumentsByInstrumentTypeStmt() throws SQLException {
        final String SUBQUERY_TABLE_NAME = "rent_instr";

        readAllRentableInstrumentsByInstrumentTypeStmt = connection.prepareStatement(
            select( "DISTINCT " +
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
            getAdditionalConditionsForFindingAvailableRentalInstruments()
        );
    }

    private void prepareCreateInstrumentRentalStmt() throws SQLException {
        String  VALUE1 = WILDCARD,
                VALUE2 = WILDCARD,
                VALUE3 = "CURRENT_TIMESTAMP(0)",
                VALUE4 = WILDCARD;

        createInstrumentRentalStmt = connection.prepareStatement(
            insertInto(
                RENTAL_PERIOD_TABLE_NAME,

                RENTAL_PERIOD_PK1_COLUMN_NAME,
                RENTAL_PERIOD_PK2_COLUMN_NAME,
                START_DATE_COLUMN_NAME,
                END_DATE_COLUMN_NAME,

                VALUE1,
                VALUE2,
                VALUE3,
                VALUE4
            )
        );
    }

    private void prepareReadNumberOfRentedInstrumentsByStudentSSNStmt() throws SQLException {
        final String SUBQUERY_TABLE_NAME = "specified_student_id";

        readNumberOfRentedInstrumentsByStudentSSNStmt = connection.prepareStatement(
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
                ) +
                where(RETURN_DATE_COLUMN_NAME, IS_NULL)
        );
    }

    private void prepareReadAvailableInstrumentForRentIdsByInstrumentTypeStmt() throws SQLException {
        final String SUBQUERY_TABLE_NAME = "selected_instrument";

        readAvailableInstrumentForRentIdsByInstrumentTypeStmt = connection.prepareStatement(
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
            where(BRAND_COLUMN_NAME, EQUALS, WILDCARD) +
            getAdditionalConditionsForFindingAvailableRentalInstruments() +
            and(specifyTableForColumn(
                    INSTR_FOR_RENT_TABLE_NAME,
                    INSTR_FOR_RENT_PK_COLUMN_NAME
                ) + IS_NOT_NULL
            )

        );
    }

    private void prepareReadStudentIdByStudentSSNStmt() throws SQLException {
        readStudentIdByStudentSSNStmt = connection.prepareStatement(
            select(PERSON_PK_COLUMN_NAME) +
            from(PERSON_TABLE_NAME) +
            where(SSN_COLUMN_NAME, EQUALS, WILDCARD) +
            forUpdate()
        );
    }

    private void prepareReadCurrentlyRentedInstrumentIdByInstrTypeBrandAndStudentIdStmt() throws SQLException {
        readCurrentlyRentedInstrumentIdByInstrTypeBrandAndStudentIdStmt = connection.prepareStatement(
            select(RENTAL_PERIOD_PK2_COLUMN_NAME) +
            from(RENTAL_PERIOD_TABLE_NAME) +
            join(INNER, ON_EQUAL,
                INSTR_FOR_RENT_TABLE_NAME,

                RENTAL_PERIOD_PK2_COLUMN_NAME,
                specifyTableForColumn(
                    INSTR_FOR_RENT_TABLE_NAME,
                    INSTR_FOR_RENT_PK_COLUMN_NAME
                )
            ) +
            join(INNER, ON_EQUAL,
                INSTR_TABLE_NAME,

                INSTR_ID_COLUMN_NAME,
                specifyTableForColumn(
                    INSTR_TABLE_NAME,
                    INSTR_PK_COLUMN_NAME
                )
            ) +
            where(RENTAL_PERIOD_PK1_COLUMN_NAME, EQUALS, WILDCARD) +
            and(BRAND_COLUMN_NAME + EQUALS + WILDCARD) +
            and(INSTR_TYPE_COLUMN_NAME + EQUALS + WILDCARD) +
            and(RETURN_DATE_COLUMN_NAME + IS_NULL) +
            limit(1)
        );
    }

    private void prepareUpdateRentalReturnDateStmt() throws SQLException {
        final String VALUE = "CURRENT_TIMESTAMP(0)";

        updateRentalReturnDateStmt = connection.prepareStatement(
            update(RENTAL_PERIOD_TABLE_NAME, RETURN_DATE_COLUMN_NAME, VALUE) +
            where(RENTAL_PERIOD_PK1_COLUMN_NAME, EQUALS, WILDCARD) +
            and(RENTAL_PERIOD_PK2_COLUMN_NAME + EQUALS + WILDCARD) +
            and(RETURN_DATE_COLUMN_NAME + IS_NULL)
        );
    }

    private String getAdditionalConditionsForFindingAvailableRentalInstruments() {
        return and("( " +RENTAL_PERIOD_PK2_COLUMN_NAME + IS_NULL) +
                or(RENTAL_PERIOD_PK2_COLUMN_NAME) +
                notIn(
                    subquery(
                        select(RENTAL_PERIOD_PK2_COLUMN_NAME),
                        from(RENTAL_PERIOD_TABLE_NAME),
                        where(RETURN_DATE_COLUMN_NAME, IS_NULL) +
                            groupBy(RENTAL_PERIOD_PK2_COLUMN_NAME) +
                            havingCountLargerThan(0) + ")"
                    )
                );
    }

    private String and(String stmt) {
        return " AND " + stmt;
    }

    private String as(String tableName) {
        return " AS " + tableName;
    }

    private void dummySelectForUpdate(String table) throws SQLException {
        connection.createStatement().executeQuery("SELECT * FROM " + table + " FOR UPDATE");
    }

    private String groupBy(String column) {
        return " GROUP BY " + column;
    }

    private String havingCountLargerThan(int count) {
        return " HAVING COUNT(*) > " + count;
    }

    private String or(String stmt) {
        return " OR " + stmt;
    }

    private String forUpdate() {
        return " FOR UPDATE";
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

    private String limit(int limitCount) {
        return " LIMIT " + limitCount;
    }

    private String join(String joinOperator, String joinCondition, String table, String column1, String column2) {
        return " " + joinOperator + " " + table + " ON " + column1 + " " + joinCondition + " " + column2;
    }

    private String notIn(String list) {
        return " NOT IN " + list;
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

    private String update(String table, String column, String value) {
        return " UPDATE " + table + " SET " + column + EQUALS + value;
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
