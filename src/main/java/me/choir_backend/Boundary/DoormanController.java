package me.choir_backend.Boundary;

import me.choir_backend.service.MemberService;
import me.choir_backend.service.SessionLifecycleService;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@CrossOrigin
@RestController
@RequestMapping("/doorman")
public class DoormanController {

    private final MemberService memberService;
    private final SessionLifecycleService sessionLifecycleService;

    public DoormanController(MemberService memberService, SessionLifecycleService sessionLifecycleService) {
        this.memberService = memberService;
        this.sessionLifecycleService = sessionLifecycleService;
    }

    @GetMapping("/members")
    public List<GetMemberNameResponse> getAllMemberNames() {
        return memberService.getAllMemberNames();
    }

    @PostMapping("/checkin/{id}")
    public DoormanCheckInResponse checkInMemberById(@PathVariable Long id) {
        return sessionLifecycleService.checkInMemberById(id);
    }
}