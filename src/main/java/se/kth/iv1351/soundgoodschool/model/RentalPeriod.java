package se.kth.iv1351.soundgoodschool.model;


import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;

import static java.time.temporal.ChronoUnit.DAYS;

public class RentalPeriod implements RentalPeriodDTO {
    private Long studentId;
    private Long instrumentForRentId;
    private Timestamp start_date;
    private Timestamp end_date;

    public RentalPeriod(Long studentId,
                        ArrayList<Long> availableInstrumentForRentIds,
                        String endDate, Integer nrOfRentedInstr)
            throws RentalPeriodException {

        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
        validateRental(nrOfRentedInstr, availableInstrumentForRentIds, endDate, formatter);
        Long instrumentForRentId = availableInstrumentForRentIds.get(0);

        this.studentId = studentId;
        this.instrumentForRentId = instrumentForRentId;
        this.start_date = Timestamp.valueOf(
                formatter.format(LocalDateTime.now())
        );
        this.end_date = Timestamp.valueOf(endDate);
    }

    /**
     * Retrieves the student's surrogate ID within the database, which is a primary key of table rental_period.
     *
     * @return student's surrogate ID within the database
     */
    @Override
    public Long getStudentId() {
        return studentId;
    }

    /**
     * Retrieves the rental instrument's surrogate ID within the database, which is a primary key of table rental_period.
     *
     * @return
     */
    @Override
    public Long getInstrumentForRentId() {
        return instrumentForRentId;
    }

    /**
     * Retrieves the start date (& time) of the rental.
     *
     * @return
     */
    @Override
    public Timestamp getStartDate() {
        return start_date;
    }

    /**
     * Retrieves the end date (& time) of the rental.
     *
     * @return
     */
    @Override
    public Timestamp getEndDate() {
        return end_date;
    }

    private void validateRental(Integer nrOfRentedInstr,
                                ArrayList<Long> availableInstrumentForRentIds,
                                String endDate,
                                DateTimeFormatter formatter)
            throws RentalPeriodException {

        validateRentalInstrQuota(nrOfRentedInstr);
        validateRentalInstrumentAvailability(availableInstrumentForRentIds);
        validateRentalPeriod(endDate, formatter);
    }

    private void validateRentalInstrQuota(Integer nrOfRentedInstr) throws RentalPeriodException {
        if(nrOfRentedInstr >= 2) throw new RentalPeriodException("Student has exceeded his/her quota");
    }

    private void validateRentalInstrumentAvailability(ArrayList<Long> availableInstrumentForRentIds) throws RentalPeriodException {
        if(availableInstrumentForRentIds.isEmpty() || availableInstrumentForRentIds.get(0) == 0) {
            throw new RentalPeriodException("The instrument that the student wants to rent is unavailable");
        }
    }

    private void validateRentalPeriod(String endDateStr, DateTimeFormatter formatter) throws RentalPeriodException {
        LocalDateTime startDate = LocalDateTime.now();
        LocalDateTime endDate = LocalDateTime.parse(endDateStr, formatter);
        Long rentalDuration = DAYS.between(startDate, endDate);

        if(rentalDuration > 365) throw new RentalPeriodException("The rental period exceeds the allowed duration");
    }
}
