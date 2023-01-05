package se.kth.iv1351.soundgoodschool.model;

public class InstrumentForRent implements InstrumentForRentDTO {
    private Long id;
    private Integer monthlyFee;
    private String brand;
    private String type;

    public InstrumentForRent(long id, int monthlyFee, String brand, String type) {
        this.id = id;
        this.monthlyFee = monthlyFee;
        this.brand = brand;
        this.type = type;
    }

    @Override
    public Long getId() {
        return id;
    }

    @Override
    public Integer getMonthlyFee() {
        return monthlyFee;
    }

    @Override
    public String getBrand() {
        return brand;
    }

    @Override
    public String getType() {
        return type;
    }

    @Override
    public String toString() {
        return  "Instrument: " + type +
                ", Monthly Fee: " + monthlyFee +
                ", Brand: " + brand;
    }
}
