package api.models;

/** JSON field names match the public API contract. */
public record Booking(String firstname, String lastname, int totalprice, boolean depositpaid,
                      BookingDates bookingdates, String additionalneeds) { }

