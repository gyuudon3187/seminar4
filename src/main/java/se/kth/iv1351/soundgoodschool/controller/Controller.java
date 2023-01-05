package se.kth.iv1351.soundgoodschool.controller;

import se.kth.iv1351.soundgoodschool.integration.SoundGoodDAO;
import se.kth.iv1351.soundgoodschool.integration.SoundGoodDBException;
import se.kth.iv1351.soundgoodschool.model.InstrumentForRentDTO;
import se.kth.iv1351.soundgoodschool.model.InstrumentForRentException;
import se.kth.iv1351.soundgoodschool.model.RentalPeriod;
import se.kth.iv1351.soundgoodschool.model.RentalPeriodException;

import java.util.ArrayList;
import java.util.List;

public class Controller {
    private final SoundGoodDAO sgDb;

    /**
     * Controller constructor.
     *
     * @throws SoundGoodDBException if instantiation of DAO fails.
     */
    public Controller() throws SoundGoodDBException {
        sgDb = new SoundGoodDAO();
    }

    /**
     * Retrieves all rentable instruments of a specific instrument type.
     * Valid instrument types are currently piano, flute, violin, guitar and drums.
     *
     * @param instrumentType the type (piano, flute etc) of the rental instrument
     * @return a list of rentable instruments
     * @throws InstrumentForRentException if the rentable instruments cannot be retrieved
     */
    public List<? extends InstrumentForRentDTO> listRentalInstruments(String instrumentType)
            throws InstrumentForRentException {

        String failureMsg = "Failed to list rentable instruments";

        try {
            List<? extends InstrumentForRentDTO> res = sgDb.findAllRentableInstruments(instrumentType);
            commitOngoingTransaction(failureMsg);
            return res;
        } catch (SoundGoodDBException sgdbe) {
            throw new InstrumentForRentException(failureMsg, sgdbe);
        }
    }

    /**
     * Sets an instrument as rented.
     *
     * @param instrumentType the type (piano, flute etc) of the rental instrument
     * @param brand the brand (yamaha, gibson etc) of the rental instrument
     * @param rentalEndDate the date until which the student wishes to rent the instrument
     * @param studentSsn the student's social security number
     * @throws RentalPeriodException 1) if the rental instrument quota is exceeded, or
     *                               2) if the rental instrument is unavailable,
     */
    public void rentInstrument(String instrumentType,
                               String brand,
                               String rentalEndDate,
                               String studentSsn)
            throws RentalPeriodException {

        String failureMsg = "Failed to list rentable instruments";

        try {
            Integer numberOfRentedInstruments = sgDb.findNumberOfRentedInstrumentsByStudentSSN(studentSsn);
            ArrayList<Long> availableInstrumentIds =
                    sgDb.findAvailableInstrumentForRentIdsByInstrumentType(instrumentType, brand);
            Long studentId = sgDb.findStudentIdByStudentSSN(studentSsn);
            sgDb.setInstrAsRented(new RentalPeriod(
                    studentId,
                    availableInstrumentIds,
                    rentalEndDate + " 18:00:00",
                    numberOfRentedInstruments)
            );
            commitOngoingTransaction(failureMsg);
        } catch (SoundGoodDBException | InstrumentForRentException sgdbe) {
            sgdbe.printStackTrace();
        }
    }

    private void commitOngoingTransaction(String failureMsg) throws InstrumentForRentException {
        try {
            sgDb.commit();
        } catch (SoundGoodDBException sgdbe) {
            throw new InstrumentForRentException(failureMsg, sgdbe);
        }
    }
}
