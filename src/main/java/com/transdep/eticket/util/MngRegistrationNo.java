package com.transdep.eticket.util;

import java.time.LocalDate;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Parser for Mongolian Registration Numbers (MNG Registration No).
 * <p>
 * Format: {@code XXYYMMDDNN}
 * <ul>
 *   <li>{@code XX} - 2-letter region/city code (e.g., УХ, УП)</li>
 *   <li>{@code YY} - 2-digit year</li>
 *   <li>{@code MM} - 2-digit month ({@code +20} if born in 2000s)</li>
 *   <li>{@code DD} - 2-digit day</li>
 *   <li>{@code NN} - 2-digit serial number (tens digit determines gender: odd → male, even → female)</li>
 * </ul>
 * </p>
 */
public class MngRegistrationNo {

    private static final Pattern PATTERN = Pattern.compile("^([A-ZА-ЯЁ]{2})(\\d{2})(\\d{2})(\\d{2})(\\d{2})$");

    private final String rawNumber;
    private final String regionCode;
    private final int birthYear;
    private final int birthMonth;
    private final int birthDay;
    private final LocalDate dateOfBirth;
    private final int serialNumber;
    private final Gender gender;

    public enum Gender {
        MALE("Male", "Эрэгтэй"),
        FEMALE("Female", "Эмэгтэй");

        private final String english;
        private final String mongolian;

        Gender(String english, String mongolian) {
            this.english = english;
            this.mongolian = mongolian;
        }

        public String getEnglish() { return english; }
        public String getMongolian() { return mongolian; }
    }

    /**
     * Parses a Mongolian Registration Number.
     *
     * @param registrationNo the raw registration number string (e.g., "УХ85121219")
     * @throws IllegalArgumentException if the number is invalid
     */
    public MngRegistrationNo(String registrationNo) {
        if (registrationNo == null || registrationNo.isEmpty()) {
            throw new IllegalArgumentException("Registration number must not be null or blank");
        }

        this.rawNumber = registrationNo.trim().toUpperCase();

        Matcher matcher = PATTERN.matcher(this.rawNumber);
        if (!matcher.matches()) {
            throw new IllegalArgumentException(
                "Invalid registration number format: '" + registrationNo
                    + "'. Expected pattern: XXYYMMDDNN (e.g., УХ85121219)"
            );
        }

        this.regionCode = matcher.group(1);
        int yy = Integer.parseInt(matcher.group(2));
        int mm = Integer.parseInt(matcher.group(3));
        int dd = Integer.parseInt(matcher.group(4));
        int nn = Integer.parseInt(matcher.group(5));

        // Parse date of birth
        int century;
        int actualMonth;
        if (mm > 12) {
            // Born in 2000s - month has 20 added to it
            actualMonth = mm - 20;
            century = 2000;
        } else {
            // Born in 1900s
            actualMonth = mm;
            century = 1900;
        }

        if (actualMonth < 1 || actualMonth > 12) {
            throw new IllegalArgumentException(
                "Invalid month in registration number: " + mm + " (resolves to " + actualMonth + ")"
            );
        }

        this.birthYear = century + yy;
        this.birthMonth = actualMonth;
        this.birthDay = dd;

        // Validate the date is real
        try {
            this.dateOfBirth = LocalDate.of(birthYear, birthMonth, birthDay);
        } catch (Exception e) {
            throw new IllegalArgumentException(
                "Invalid date in registration number: " + yy + "/" + mm + "/" + dd
                    + " (resolves to " + birthYear + "/" + actualMonth + "/" + birthDay + ")", e
            );
        }

        this.serialNumber = nn;

        // Gender: tens digit of the serial number — odd → male, even → female
        int tensDigit = nn / 10;
        this.gender = (tensDigit % 2 == 1) ? Gender.MALE : Gender.FEMALE;
    }

    // --- Getters ---

    /** Returns the raw registration number as provided. */
    public String getRawNumber() { return rawNumber; }

    /** Returns the 2-letter region/city code (e.g., УХ, УП). */
    public String getRegionCode() { return regionCode; }

    /** Returns the 4-digit birth year. */
    public int getBirthYear() { return birthYear; }

    /** Returns the birth month (1-12). */
    public int getBirthMonth() { return birthMonth; }

    /** Returns the birth day of month. */
    public int getBirthDay() { return birthDay; }

    /** Returns the date of birth as a {@link LocalDate}. */
    public LocalDate getDateOfBirth() { return dateOfBirth; }

    /** Returns the 2-digit serial number. */
    public int getSerialNumber() { return serialNumber; }

    /** Returns the gender derived from the serial number. */
    public Gender getGender() { return gender; }

    /**
     * Convenience check.
     * @return true if the person is male
     */
    public boolean isMale() { return gender == Gender.MALE; }

    /**
     * Convenience check.
     * @return true if the person is female
     */
    public boolean isFemale() { return gender == Gender.FEMALE; }

    @Override
    public String toString() {
        return String.format(
            "MngRegistrationNo{number='%s', region='%s', dob=%s, gender=%s}",
            rawNumber, regionCode, dateOfBirth, gender.getEnglish()
        );
    }
}
