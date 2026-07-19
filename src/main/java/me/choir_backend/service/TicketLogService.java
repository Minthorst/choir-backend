package me.choir_backend.service;

import me.choir_backend.Boundary.TicketLogEntryResponse;
import me.choir_backend.model.Member;
import me.choir_backend.model.Session;
import me.choir_backend.model.TicketTransaction;
import me.choir_backend.model.TicketTransactionType;
import me.choir_backend.repository.TicketTransactionRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class TicketLogService {

    private final TicketTransactionRepository ticketTransactionRepository;

    public TicketLogService(TicketTransactionRepository ticketTransactionRepository) {
        this.ticketTransactionRepository = ticketTransactionRepository;
    }

    public void record(Member member, int regularDelta, int commitDelta,
                       TicketTransactionType type, Session session) {
        if (regularDelta == 0 && commitDelta == 0) return;
        ticketTransactionRepository.save(new TicketTransaction(member, regularDelta, commitDelta, type, session));
    }

    public void record(Member member, TicketDelta delta, TicketTransactionType type, Session session) {
        record(member, delta.regularDelta(), delta.commitDelta(), type, session);
    }

    public void recordInitialBalance(Member member) {
        record(member, member.getRegularTickets(), 0, TicketTransactionType.INITIAL_BALANCE, null);
        record(member, 0, member.getCommitTickets(), TicketTransactionType.INITIAL_BALANCE, null);
    }

    public List<TicketLogEntryResponse> getFullLog(Member member) {
        return toResponses(ticketTransactionRepository.findAllByMemberOrderByTimestampDescIdDesc(member));
    }

    private List<TicketLogEntryResponse> toResponses(List<TicketTransaction> transactions) {
        return transactions.stream()
                .map(t -> new TicketLogEntryResponse(t.getTimestamp(), t.getRegularDelta(), t.getCommitDelta(), t.getType()))
                .toList();
    }
}
