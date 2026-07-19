package me.choir_backend.repository;

import me.choir_backend.model.Member;
import me.choir_backend.model.TicketTransaction;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface TicketTransactionRepository extends JpaRepository<TicketTransaction, Long> {

    List<TicketTransaction> findAllByMemberOrderByTimestampDescIdDesc(Member member);
}
