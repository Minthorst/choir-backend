package me.choir_backend.model;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MemberTest {

    private Member memberWith(int regular, int commit) {
        return new Member("Test Member", "red-abba-42", regular, commit);
    }

    @Test
    void hasEnoughTicketsWithPositiveBalance() {
        assertTrue(memberWith(2, 1).hasEnoughTicketsForCheckin());
    }

    @Test
    void hasEnoughTicketsJustAboveMinimum() {
        assertTrue(memberWith(-2, 0).hasEnoughTicketsForCheckin());
    }

    @Test
    void hasNotEnoughTicketsAtMinimum() {
        assertFalse(memberWith(-3, 0).hasEnoughTicketsForCheckin());
    }

    @Test
    void hasNotEnoughTicketsBelowMinimum() {
        assertFalse(memberWith(-2, -2).hasEnoughTicketsForCheckin());
    }

    @Test
    void ticketSumIsUsedAcrossBothTypes() {
        assertTrue(memberWith(-4, 2).hasEnoughTicketsForCheckin());
    }
}
