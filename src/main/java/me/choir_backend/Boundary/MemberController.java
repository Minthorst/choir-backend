package me.choir_backend.Boundary;

import me.choir_backend.service.MemberService;
import me.choir_backend.service.SessionLifecycleService;
import org.springframework.web.bind.annotation.*;

@CrossOrigin
@RestController
@RequestMapping("/member")
public class MemberController {

    private final MemberService memberService;
    private final SessionLifecycleService sessionLifecycleService;

    public MemberController(MemberService memberService, SessionLifecycleService sessionLifecycleService) {
        this.memberService = memberService;
        this.sessionLifecycleService = sessionLifecycleService;
    }

    @GetMapping("/{secretKey}")
    public GetMemberInfoResponse getMemberInfo(@PathVariable String secretKey) {
        return memberService.getMemberInfo(secretKey);
    }

    @PostMapping("/checkin/{secretKey}")
    public void checkInMember(@PathVariable String secretKey) {
        sessionLifecycleService.checkInMember(secretKey);
    }
}
