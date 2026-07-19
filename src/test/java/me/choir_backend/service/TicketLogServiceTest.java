package me.choir_backend.service;

import me.choir_backend.Boundary.TicketLogEntryResponse;
import me.choir_backend.model.Member;
import me.choir_backend.model.Session;
import me.choir_backend.model.TicketTransaction;
import me.choir_backend.model.TicketTransactionType;
import me.choir_backend.repository.TicketTransactionRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TicketLogServiceTest {

    @Mock
    TicketTransactionRepository ticketTransactionRepository;

    @InjectMocks
    TicketLogService ticketLogService;

    final Member member = new Member("Anna", "key", 1, 1);

    @Test
    void recordPersistsTransactionWithAllFields() {
        Session session = new Session();

        ticketLogService.record(member, 0, -1, TicketTransactionType.CHECK_IN, session);

        ArgumentCaptor<TicketTransaction> captor = ArgumentCaptor.forClass(TicketTransaction.class);
        verify(ticketTransactionRepository).save(captor.capture());
        TicketTransaction saved = captor.getValue();
        assertEquals(member, saved.getMember());
        assertEquals(0, saved.getRegularDelta());
        assertEquals(-1, saved.getCommitDelta());
        assertEquals(TicketTransactionType.CHECK_IN, saved.getType());
        assertEquals(session, saved.getSession());
        assertNotNull(saved.getTimestamp());
    }

    @Test
    void recordSkipsZeroDeltaEntries() {
        ticketLogService.record(member, 0, 0, TicketTransactionType.ADMIN_ADJUSTMENT, null);

        verify(ticketTransactionRepository, never()).save(any());
    }

    @Test
    void recordAcceptsTicketDelta() {
        ticketLogService.record(member, new TicketDelta(1, 0), TicketTransactionType.FREE_SESSION_REFUND, null);

        verify(ticketTransactionRepository).save(any(TicketTransaction.class));
    }

    @Test
    void initialBalanceIsRecordedAsOneRowPerTicketType() {
        Member newMember = new Member("Ben", "key2", 2, 3);

        ticketLogService.recordInitialBalance(newMember);

        ArgumentCaptor<TicketTransaction> captor = ArgumentCaptor.forClass(TicketTransaction.class);
        verify(ticketTransactionRepository, org.mockito.Mockito.times(2)).save(captor.capture());
        TicketTransaction regularRow = captor.getAllValues().get(0);
        TicketTransaction commitRow = captor.getAllValues().get(1);
        assertEquals(2, regularRow.getRegularDelta());
        assertEquals(0, regularRow.getCommitDelta());
        assertEquals(0, commitRow.getRegularDelta());
        assertEquals(3, commitRow.getCommitDelta());
        assertEquals(TicketTransactionType.INITIAL_BALANCE, regularRow.getType());
        assertEquals(TicketTransactionType.INITIAL_BALANCE, commitRow.getType());
    }

    @Test
    void initialBalanceSkipsZeroTicketTypes() {
        Member newMember = new Member("Cara", "key3", 1, 0);

        ticketLogService.recordInitialBalance(newMember);

        ArgumentCaptor<TicketTransaction> captor = ArgumentCaptor.forClass(TicketTransaction.class);
        verify(ticketTransactionRepository).save(captor.capture());
        assertEquals(1, captor.getValue().getRegularDelta());
        assertEquals(0, captor.getValue().getCommitDelta());
    }

    @Test
    void fullLogMapsAllEntries() {
        when(ticketTransactionRepository.findAllByMemberOrderByTimestampDescIdDesc(member))
                .thenReturn(List.of(
                        new TicketTransaction(member, 0, -1, TicketTransactionType.CHECK_IN, new Session()),
                        new TicketTransaction(member, 1, 1, TicketTransactionType.INITIAL_BALANCE, null)));

        List<TicketLogEntryResponse> log = ticketLogService.getFullLog(member);

        assertEquals(2, log.size());
        assertEquals(TicketTransactionType.CHECK_IN, log.get(0).type());
        assertEquals(TicketTransactionType.INITIAL_BALANCE, log.get(1).type());
    }
}
