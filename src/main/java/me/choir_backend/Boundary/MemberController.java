package me.choir_backend.Boundary;

import me.choir_backend.service.MemberService;
import me.choir_backend.service.ScheduleService;
import me.choir_backend.service.SessionLifecycleService;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/member")
public class MemberController {

    private final MemberService memberService;
    private final SessionLifecycleService sessionLifecycleService;
    private final ScheduleService scheduleService;

    public MemberController(MemberService memberService, SessionLifecycleService sessionLifecycleService,
                            ScheduleService scheduleService) {
        this.memberService = memberService;
        this.sessionLifecycleService = sessionLifecycleService;
        this.scheduleService = scheduleService;
    }

    @GetMapping("/{secretKey}")
    public GetMemberInfoResponse getMemberInfo(@PathVariable String secretKey) {
        return memberService.getMemberInfo(secretKey);
    }

    @PostMapping("/checkin/{secretKey}")
    public void checkInMember(@PathVariable String secretKey) {
        sessionLifecycleService.checkInMember(secretKey);
    }

    @GetMapping(value = "/schedule/ics", produces = "text/calendar")
    public String getScheduleIcs() {
        return scheduleService.fetchIcs();
    }
}
