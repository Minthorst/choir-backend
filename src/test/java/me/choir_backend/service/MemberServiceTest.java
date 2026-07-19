package me.choir_backend.service;

import me.choir_backend.Boundary.AddTicketsRequest;
import me.choir_backend.Boundary.CreateMemberRequest;
import me.choir_backend.Boundary.CreateMemberResponse;
import me.choir_backend.Exception.DuplicateMemberNameException;
import me.choir_backend.Exception.ResourceNotFoundException;
import me.choir_backend.model.Member;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import me.choir_backend.repository.MemberRepository;

@ExtendWith(MockitoExtension.class)
class MemberServiceTest {

    @Mock
    MemberRepository memberRepository;
    @Mock
    MemberKeyGeneratorService memberKeyGeneratorService;
    @Mock
    AttendanceService attendanceService;
    @Mock
    SessionService sessionService;

    @InjectMocks
    MemberService memberService;

    @Test
    void createMemberRejectsDuplicateName() {
        when(memberRepository.existsByNameIgnoreCase("Anna")).thenReturn(true);

        assertThrows(DuplicateMemberNameException.class,
                () -> memberService.createMember(new CreateMemberRequest("Anna", 1, 1)));
        verify(memberRepository, never()).save(any());
    }

    @Test
    void createMemberTrimsNameAndReturnsGeneratedKey() {
        when(memberRepository.existsByNameIgnoreCase("Anna")).thenReturn(false);
        when(memberKeyGeneratorService.generateUnique()).thenReturn("blue-nirvana-17");

        CreateMemberResponse response = memberService.createMember(new CreateMemberRequest("  Anna  ", 2, 3));

        assertEquals("blue-nirvana-17", response.secretKey());
        ArgumentCaptor<Member> captor = ArgumentCaptor.forClass(Member.class);
        verify(memberRepository).save(captor.capture());
        Member saved = captor.getValue();
        assertEquals("Anna", saved.getName());
        assertEquals(2, saved.getRegularTickets());
        assertEquals(3, saved.getCommitTickets());
        assertEquals("blue-nirvana-17", saved.getSecretKey());
    }

    @Test
    void reduceTicketPrefersCommitTickets() {
        Member member = new Member("Anna", "key", 2, 2);

        memberService.reduceMembersTicket(member);

        assertEquals(1, member.getCommitTickets());
        assertEquals(2, member.getRegularTickets());
    }

    @Test
    void reduceTicketFallsBackToRegularTickets() {
        Member member = new Member("Anna", "key", 1, 0);

        memberService.reduceMembersTicket(member);

        assertEquals(0, member.getCommitTickets());
        assertEquals(0, member.getRegularTickets());
    }

    @Test
    void reduceTicketAllowsNegativeRegularBalance() {
        Member member = new Member("Anna", "key", 0, 0);

        memberService.reduceMembersTicket(member);

        assertEquals(-1, member.getRegularTickets());
        assertEquals(0, member.getCommitTickets());
    }

    @Test
    void addTicketsAddsBothTypes() {
        Member member = new Member("Anna", "key", 1, 1);
        when(memberRepository.findById(5L)).thenReturn(Optional.of(member));

        memberService.addTickets(new AddTicketsRequest(5L, 2, 3));

        assertEquals(4, member.getRegularTickets());
        assertEquals(3, member.getCommitTickets());
        verify(memberRepository).save(member);
    }

    @Test
    void addTicketsFailsForUnknownMember() {
        when(memberRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,
                () -> memberService.addTickets(new AddTicketsRequest(99L, 1, 1)));
    }

    @Test
    void setArchivedFlagsMemberAndSaves() {
        Member member = new Member("Anna", "key", 0, 0);
        when(memberRepository.findById(5L)).thenReturn(Optional.of(member));

        memberService.setArchived(5L, true);

        assertTrue(member.isArchived());
        verify(memberRepository).save(member);
    }

    @Test
    void getMandatoryMemberFailsForUnknownKey() {
        when(memberRepository.findBySecretKey("nope")).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> memberService.getMandatoryMember("nope"));
    }

    @Test
    void refundRegularTicketsIncrementsEachMember() {
        Member anna = new Member("Anna", "key1", 0, 1);
        Member ben = new Member("Ben", "key2", -1, 0);

        memberService.refundRegularTicketsAndSaveMembers(java.util.List.of(anna, ben));

        assertEquals(1, anna.getRegularTickets());
        assertEquals(0, ben.getRegularTickets());
        verify(memberRepository).saveAll(java.util.List.of(anna, ben));
    }
}
