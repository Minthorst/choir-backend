    package me.choir_backend.Boundary;

    import me.choir_backend.service.MemberService;
    import me.choir_backend.service.SessionLifecycleService;
    import me.choir_backend.service.SessionService;
    import org.springframework.web.bind.annotation.*;

    import java.util.List;

    @RestController
    @RequestMapping("/admin")
    public class AdminController {

        private final SessionService sessionService;
        private final MemberService memberService;
        private final SessionLifecycleService sessionLifecycleService;

        public AdminController(SessionService sessionService, MemberService memberService, SessionLifecycleService sessionLifecycleService) {
            this.sessionService = sessionService;
            this.memberService = memberService;
            this.sessionLifecycleService = sessionLifecycleService;
        }

        @PostMapping("/finalizeSession")
        public EndSessionResponse finalizeSession(@RequestBody EndSessionRequest endSessionRequest){
            return sessionLifecycleService.finalizeSession(endSessionRequest);
        }

        @PostMapping("/member")
        public CreateMemberResponse createMember(@RequestBody CreateMemberRequest createMemberRequest) {
            return memberService.createMember(createMemberRequest);
        }

        @GetMapping("/members")
        public List<GetAdminMemberInfoResponse> getAllMembers(){
            return memberService.getAllMembersWithSecret();
        }

        @PostMapping("/tickets")
        public void addTickets(@RequestBody AddTicketsRequest addTicketsRequest){
            memberService.addTickets(addTicketsRequest);
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
