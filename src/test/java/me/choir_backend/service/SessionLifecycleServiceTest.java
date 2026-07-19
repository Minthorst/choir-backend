package me.choir_backend.service;

import me.choir_backend.Boundary.DoormanCheckInResponse;
import me.choir_backend.Boundary.EndSessionRequest;
import me.choir_backend.Boundary.EndSessionResponse;
import me.choir_backend.Exception.InsufficientTicketsException;
import me.choir_backend.Exception.MemberAlreadyCheckedInException;
import me.choir_backend.Exception.WrongSessionTypeException;
import me.choir_backend.model.Member;
import me.choir_backend.model.Session;
import me.choir_backend.model.SessionType;
import me.choir_backend.model.TicketTransactionType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SessionLifecycleServiceTest {

    @Mock
    MemberService memberService;
    @Mock
    SessionService sessionService;
    @Mock
    AttendanceService attendanceService;
    @Mock
    TicketLogService ticketLogService;

    @InjectMocks
    SessionLifecycleService sessionLifecycleService;

    Session session;

    @BeforeEach
    void setUp() {
        session = new Session();
    }

    @Test
    void checkInReducesTicketAndSavesAttendance() {
        Member member = new Member("Anna", "key", 1, 0);
        when(memberService.getMandatoryMember("key")).thenReturn(member);
        when(sessionService.getActiveSessionForCheckIn()).thenReturn(session);
        when(attendanceService.isAlreadyAttending(member, session)).thenReturn(false);
        when(memberService.reduceMembersTicket(member)).thenReturn(new TicketDelta(0, -1));

        sessionLifecycleService.checkInMember("key");

        verify(sessionService).closeStaleSessions();
        verify(memberService).reduceMembersTicket(member);
        verify(memberService).saveMember(member);
        verify(ticketLogService).record(member, new TicketDelta(0, -1), TicketTransactionType.CHECK_IN, session);
        verify(attendanceService).saveAttendance(member, session);
    }

    @Test
    void checkInFailsWhenAlreadyCheckedIn() {
        Member member = new Member("Anna", "key", 1, 0);
        when(memberService.getMandatoryMember("key")).thenReturn(member);
        when(sessionService.getActiveSessionForCheckIn()).thenReturn(session);
        when(attendanceService.isAlreadyAttending(member, session)).thenReturn(true);

        assertThrows(MemberAlreadyCheckedInException.class,
                () -> sessionLifecycleService.checkInMember("key"));
        verify(memberService, never()).reduceMembersTicket(any());
        verify(attendanceService, never()).saveAttendance(any(), any());
        verify(ticketLogService, never()).record(any(), any(TicketDelta.class), any(), any());
    }

    @Test
    void checkInFailsWithoutEnoughTickets() {
        Member member = new Member("Anna", "key", -3, 0);
        when(memberService.getMandatoryMember("key")).thenReturn(member);
        when(sessionService.getActiveSessionForCheckIn()).thenReturn(session);
        when(attendanceService.isAlreadyAttending(member, session)).thenReturn(false);

        assertThrows(InsufficientTicketsException.class,
                () -> sessionLifecycleService.checkInMember("key"));
        verify(memberService, never()).reduceMembersTicket(any());
        verify(attendanceService, never()).saveAttendance(any(), any());
    }

    @Test
    void doormanCheckInReportsAlreadyCheckedInInsteadOfThrowing() {
        Member member = new Member("Anna", "key", 2, 1);
        when(memberService.getMandatoryMemberById(5L)).thenReturn(member);
        when(sessionService.getActiveSessionForCheckIn()).thenReturn(session);
        when(attendanceService.isAlreadyAttending(member, session)).thenReturn(true);

        DoormanCheckInResponse response = sessionLifecycleService.checkInMemberById(5L);

        assertTrue(response.checkedIn());
        assertEquals("Anna", response.name());
    }

    @Test
    void doormanCheckInHappyPath() {
        Member member = new Member("Anna", "key", 2, 1);
        when(memberService.getMandatoryMemberById(5L)).thenReturn(member);
        when(sessionService.getActiveSessionForCheckIn()).thenReturn(session);
        when(attendanceService.isAlreadyAttending(member, session)).thenReturn(false);
        when(memberService.reduceMembersTicket(member)).thenReturn(new TicketDelta(0, -1));

        DoormanCheckInResponse response = sessionLifecycleService.checkInMemberById(5L);

        assertFalse(response.checkedIn());
        verify(memberService).reduceMembersTicket(member);
        verify(ticketLogService).record(member, new TicketDelta(0, -1), TicketTransactionType.CHECK_IN, session);
        verify(attendanceService).saveAttendance(member, session);
    }

    @Test
    void finalizeFailsWhenSessionAlreadyFinalized() {
        session.setSessionType(SessionType.COMMIT);
        when(sessionService.findMandatorySession(1L)).thenReturn(session);

        assertThrows(WrongSessionTypeException.class,
                () -> sessionLifecycleService.finalizeSession(new EndSessionRequest(1L, SessionType.REGULAR_ONLY)));
    }

    @Test
    void finalizeFailsForInvalidRequestedType() {
        when(sessionService.findMandatorySession(1L)).thenReturn(session);

        assertThrows(WrongSessionTypeException.class,
                () -> sessionLifecycleService.finalizeSession(new EndSessionRequest(1L, SessionType.AUTO_CLOSE)));
        assertThrows(WrongSessionTypeException.class,
                () -> sessionLifecycleService.finalizeSession(new EndSessionRequest(1L, SessionType.NONE)));
    }

    @Test
    void finalizeAsRegularOnlyClosesSessionWithoutCharges() {
        when(sessionService.findMandatorySession(1L)).thenReturn(session);
        when(attendanceService.findMembersBySession(session.getId()))
                .thenReturn(List.of(new Member("Anna", "key", 1, 0)));

        EndSessionResponse response =
                sessionLifecycleService.finalizeSession(new EndSessionRequest(1L, SessionType.REGULAR_ONLY));

        assertEquals(1, response.presentMembers());
        assertEquals(0, response.absentCommitMembers());
        assertFalse(session.getOpen());
        assertEquals(SessionType.REGULAR_ONLY, session.getSessionType());
        verify(sessionService).saveSession(session);
        verify(memberService, never()).reduceMembersTicket(any());
        verify(memberService, never()).refundRegularTicket(any());
        verify(ticketLogService, never()).record(any(), any(TicketDelta.class), any(), any());
    }

    @Test
    void finalizeAsCommitChargesAbsenteesWithCommitTickets() {
        Member present = new Member("Anna", "key1", 1, 0);
        Member absentee = new Member("Ben", "key2", 0, 2);
        when(sessionService.findMandatorySession(1L)).thenReturn(session);
        when(attendanceService.findMembersBySession(session.getId())).thenReturn(List.of(present));
        when(memberService.findAbsenteesWithCommitTickets(session)).thenReturn(List.of(absentee));
        when(memberService.reduceMembersTicket(absentee)).thenReturn(new TicketDelta(0, -1));

        EndSessionResponse response =
                sessionLifecycleService.finalizeSession(new EndSessionRequest(1L, SessionType.COMMIT));

        assertEquals(1, response.presentMembers());
        assertEquals(1, response.absentCommitMembers());
        assertEquals(SessionType.COMMIT, session.getSessionType());
        verify(attendanceService).saveNoShowAttendance(List.of(absentee), session);
        verify(memberService).reduceMembersTicket(absentee);
        verify(ticketLogService).record(absentee, new TicketDelta(0, -1), TicketTransactionType.NO_SHOW_CHARGE, session);
        verify(memberService).saveMembers(List.of(absentee));
    }

    @Test
    void finalizeAsFreeRefundsAttendees() {
        Member present = new Member("Anna", "key1", 0, 0);
        when(sessionService.findMandatorySession(1L)).thenReturn(session);
        when(attendanceService.findMembersBySession(session.getId())).thenReturn(List.of(present));
        when(memberService.refundRegularTicket(present)).thenReturn(new TicketDelta(1, 0));

        EndSessionResponse response =
                sessionLifecycleService.finalizeSession(new EndSessionRequest(1L, SessionType.FREE));

        assertEquals(1, response.presentMembers());
        assertEquals(SessionType.FREE, session.getSessionType());
        verify(memberService).refundRegularTicket(present);
        verify(ticketLogService).record(present, new TicketDelta(1, 0), TicketTransactionType.FREE_SESSION_REFUND, session);
        verify(memberService).saveMembers(List.of(present));
    }
}
