package me.choir_backend.repository;

import me.choir_backend.model.Member;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface MemberRepository extends JpaRepository<Member, Long> {
    Optional<Member> findBySecretKey(String secretKey);
}
