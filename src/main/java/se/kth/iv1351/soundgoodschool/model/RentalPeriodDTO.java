package se.kth.iv1351.soundgoodschool.model;

import java.sql.Timestamp;
import java.time.LocalDateTime;

public interface RentalPeriodDTO {

    Long getStudentId();
    Long getInstrumentForRentId();
    Timestamp getStartDate();
    Timestamp getEndDate();
}
