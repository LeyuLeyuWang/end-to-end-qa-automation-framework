package api;

import api.models.Booking;
import api.models.BookingDates;
import java.util.UUID;

final class BookingData {
    private BookingData() { }
    static Booking fresh() {
        return new Booking("QA-" + UUID.randomUUID(), "Portfolio", 123, true,
                new BookingDates("2027-02-10", "2027-02-12"), "Breakfast");
    }
}

