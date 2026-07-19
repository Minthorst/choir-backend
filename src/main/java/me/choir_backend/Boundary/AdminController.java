    package me.choir_backend.Boundary;

    import jakarta.validation.Valid;
    import me.choir_backend.service.MemberService;
    import me.choir_backend.service.SessionLifecycleService;
    import me.choir_backend.service.SessionService;
    import me.choir_backend.service.TicketLogService;
    import org.springframework.web.bind.annotation.*;

    import java.util.List;

    @RestController
    @RequestMapping("/admin")
    public class AdminController {

        private final SessionService sessionService;
        private final MemberService memberService;
        private final SessionLifecycleService sessionLifecycleService;
        private final TicketLogService ticketLogService;

        public AdminController(SessionService sessionService, MemberService memberService, SessionLifecycleService sessionLifecycleService, TicketLogService ticketLogService) {
            this.sessionService = sessionService;
            this.memberService = memberService;
            this.sessionLifecycleService = sessionLifecycleService;
            this.ticketLogService = ticketLogService;
        }

        @PostMapping("/finalizeSession")
        public EndSessionResponse finalizeSession(@Valid @RequestBody EndSessionRequest endSessionRequest){
            return sessionLifecycleService.finalizeSession(endSessionRequest);
        }

        @PostMapping("/member")
        public CreateMemberResponse createMember(@Valid @RequestBody CreateMemberRequest createMemberRequest) {
            return memberService.createMember(createMemberRequest);
        }

        @GetMapping("/members")
        public List<GetAdminMemberInfoResponse> getAllMembers(){
            return memberService.getAllMembersWithSecret();
        }

        @PostMapping("/tickets")
        public void addTickets(@Valid @RequestBody AddTicketsRequest addTicketsRequest){
            memberService.addTickets(addTicketsRequest);
        }

        @PostMapping("/members/archive")
        public void setMemberArchived(@Valid @RequestBody ArchiveMemberRequest request){
            memberService.setArchived(request.memberId(), request.archived());
        }

        @GetMapping("/members/{id}/ticketlog")
        public List<TicketLogEntryResponse> getMemberTicketLog(@PathVariable Long id){
            return ticketLogService.getFullLog(memberService.getMandatoryMemberById(id));
        }

        @GetMapping("/sessions")
        public List<GetSessionResponse> getAllSessions(){
            return sessionService.getAllSessions();
        }

        @GetMapping("/sessions/members/{id}")
        public List<GetAdminMemberInfoResponse> getAttendingMembersOfSession(@PathVariable Long id){
            return memberService.getAllMembersOfSession(id);
        }




    }
