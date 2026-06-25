package com.transdep.eticket.util;

import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.junit.Assert.*;

/**
 * Tests for {@link MngRegistrationNo} using the exact examples provided.
 */
public class MngRegistrationNoTest {

    private static final Logger logger = LoggerFactory.getLogger(MngRegistrationNoTest.class);

    @Test
    public void testExample_УХ85121219() {
        logger.info("Test: УХ85121219 -> 1985-12-12, Male");
        MngRegistrationNo reg = new MngRegistrationNo("УХ85121219");

        assertEquals("УХ", reg.getRegionCode());
        assertEquals(1985, reg.getBirthYear());
        assertEquals(12, reg.getBirthMonth());
        assertEquals(12, reg.getBirthDay());
        assertEquals("1985-12-12", reg.getDateOfBirth().toString());
        assertEquals(19, reg.getSerialNumber());
        assertEquals(MngRegistrationNo.Gender.MALE, reg.getGender());
        assertTrue(reg.isMale());
        assertFalse(reg.isFemale());

        logger.info("  ✓ {}", reg);
    }

    @Test
    public void testExample_УП12242929() {
        logger.info("Test: УП12242929 -> 2012-04-29, Female");
        MngRegistrationNo reg = new MngRegistrationNo("УП12242929");

        assertEquals("УП", reg.getRegionCode());
        assertEquals(2012, reg.getBirthYear());
        assertEquals(4, reg.getBirthMonth());
        assertEquals(29, reg.getBirthDay());
        assertEquals("2012-04-29", reg.getDateOfBirth().toString());
        assertEquals(29, reg.getSerialNumber());
        assertEquals(MngRegistrationNo.Gender.FEMALE, reg.getGender());
        assertFalse(reg.isMale());
        assertTrue(reg.isFemale());

        logger.info("  ✓ {}", reg);
    }

    @Test
    public void testExample_УХ14260407() {
        logger.info("Test: УХ14260407 -> 2014-06-04, Female");
        MngRegistrationNo reg = new MngRegistrationNo("УХ14260407");

        assertEquals("УХ", reg.getRegionCode());
        assertEquals(2014, reg.getBirthYear());
        assertEquals(6, reg.getBirthMonth());
        assertEquals(4, reg.getBirthDay());
        assertEquals("2014-06-04", reg.getDateOfBirth().toString());
        assertEquals(7, reg.getSerialNumber());
        assertEquals(MngRegistrationNo.Gender.FEMALE, reg.getGender());
        assertFalse(reg.isMale());
        assertTrue(reg.isFemale());

        logger.info("  ✓ {}", reg);
    }

    @Test
    public void testExample_УХ54021703() {
        logger.info("Test: УХ54021703 -> 1954-02-17, Female");
        MngRegistrationNo reg = new MngRegistrationNo("УХ54021703");

        assertEquals("УХ", reg.getRegionCode());
        assertEquals(1954, reg.getBirthYear());
        assertEquals(2, reg.getBirthMonth());
        assertEquals(17, reg.getBirthDay());
        assertEquals("1954-02-17", reg.getDateOfBirth().toString());
        assertEquals(3, reg.getSerialNumber());
        assertEquals(MngRegistrationNo.Gender.FEMALE, reg.getGender());
        assertFalse(reg.isMale());
        assertTrue(reg.isFemale());

        logger.info("  ✓ {}", reg);
    }

    @Test
    public void testGenderEnumLabels() {
        logger.info("Test: Gender enum labels");
        assertEquals("Male", MngRegistrationNo.Gender.MALE.getEnglish());
        assertEquals("Эрэгтэй", MngRegistrationNo.Gender.MALE.getMongolian());
        assertEquals("Female", MngRegistrationNo.Gender.FEMALE.getEnglish());
        assertEquals("Эмэгтэй", MngRegistrationNo.Gender.FEMALE.getMongolian());
        logger.info("  ✓ Gender labels correct");
    }

    @Test(expected = IllegalArgumentException.class)
    public void testInvalidFormat_TooShort() {
        logger.info("Test: Invalid format - too short");
        new MngRegistrationNo("УХ85121");
    }

    @Test(expected = IllegalArgumentException.class)
    public void testInvalidFormat_NullInput() {
        logger.info("Test: Invalid - null input");
        new MngRegistrationNo(null);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testInvalidFormat_BlankInput() {
        logger.info("Test: Invalid - blank input");
        new MngRegistrationNo("   ");
    }
}
