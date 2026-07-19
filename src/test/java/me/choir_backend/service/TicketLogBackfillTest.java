package me.choir_backend.service;

import me.choir_backend.model.Member;
import me.choir_backend.repository.MemberRepository;
import me.choir_backend.repository.TicketTransactionRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TicketLogBackfillTest {

    @Mock
    MemberRepository memberRepository;
    @Mock
    TicketTransactionRepository ticketTransactionRepository;
    @Mock
    TicketLogService ticketLogService;

    @InjectMocks
    TicketLogBackfill ticketLogBackfill;

    @Test
    void snapshotsEveryMemberWhenLedgerIsEmpty() {
        Member anna = new Member("Anna", "key1", 3, 1);
        Member ben = new Member("Ben", "key2", 0, 2);
        when(ticketTransactionRepository.count()).thenReturn(0L);
        when(memberRepository.findAll()).thenReturn(List.of(anna, ben));

        ticketLogBackfill.run(null);

        verify(ticketLogService).recordInitialBalance(anna);
        verify(ticketLogService).recordInitialBalance(ben);
    }

    @Test
    void doesNothingOnceLedgerEntriesExist() {
        when(ticketTransactionRepository.count()).thenReturn(5L);

        ticketLogBackfill.run(null);

        verify(ticketLogService, never()).recordInitialBalance(any());
    }
}
