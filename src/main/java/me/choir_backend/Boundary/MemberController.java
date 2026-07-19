package me.choir_backend.Boundary;

import me.choir_backend.service.MemberService;
import me.choir_backend.service.ScheduleService;
import me.choir_backend.service.SessionLifecycleService;
import me.choir_backend.service.TicketLogService;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/member")
public class MemberController {

    private final MemberService memberService;
    private final SessionLifecycleService sessionLifecycleService;
    private final ScheduleService scheduleService;
    private final TicketLogService ticketLogService;

    public MemberController(MemberService memberService, SessionLifecycleService sessionLifecycleService,
                            ScheduleService scheduleService, TicketLogService ticketLogService) {
        this.memberService = memberService;
        this.sessionLifecycleService = sessionLifecycleService;
        this.scheduleService = scheduleService;
        this.ticketLogService = ticketLogService;
    }

    @GetMapping("/{secretKey}")
    public GetMemberInfoResponse getMemberInfo(@PathVariable String secretKey) {
        return memberService.getMemberInfo(secretKey);
    }

    @PostMapping("/checkin/{secretKey}")
    public void checkInMember(@PathVariable String secretKey) {
        sessionLifecycleService.checkInMember(secretKey);
    }

    @GetMapping("/{secretKey}/ticketlog")
    public List<TicketLogEntryResponse> getTicketLog(@PathVariable String secretKey) {
        return ticketLogService.getFullLog(memberService.getMandatoryMember(secretKey));
    }

    @GetMapping(value = "/schedule/ics", produces = "text/calendar")
    public String getScheduleIcs() {
        return scheduleService.fetchIcs();
    }
}
