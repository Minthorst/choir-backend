package me.choir_backend.service;

import jakarta.transaction.Transactional;
import me.choir_backend.model.Member;
import me.choir_backend.repository.MemberRepository;
import me.choir_backend.repository.TicketTransactionRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * One-time migration: members created before the ticket ledger existed have no
 * history, so on the first start with an empty ledger their current balance is
 * snapshotted as INITIAL_BALANCE entries. Once any ledger entry exists this
 * never runs again.
 */
@Component
public class TicketLogBackfill implements ApplicationRunner {
    private static final Logger log = LoggerFactory.getLogger(TicketLogBackfill.class);

    private final MemberRepository memberRepository;
    private final TicketTransactionRepository ticketTransactionRepository;
    private final TicketLogService ticketLogService;

    public TicketLogBackfill(MemberRepository memberRepository,
                             TicketTransactionRepository ticketTransactionRepository,
                             TicketLogService ticketLogService) {
        this.memberRepository = memberRepository;
        this.ticketTransactionRepository = ticketTransactionRepository;
        this.ticketLogService = ticketLogService;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        if (ticketTransactionRepository.count() > 0) return;
        List<Member> members = memberRepository.findAll();
        members.forEach(ticketLogService::recordInitialBalance);
        if (!members.isEmpty()) {
            log.info("Backfilled initial ticket-log balances for {} existing members", members.size());
        }
    }
}
