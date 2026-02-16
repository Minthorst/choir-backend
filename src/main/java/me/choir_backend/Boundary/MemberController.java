package me.choir_backend.Boundary;

import me.choir_backend.service.MemberService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/member")
public class MemberController {

    @Autowired
    MemberService memberService;

    @GetMapping("/{secretKey}")
    public GetMemberInfoResponse getMemberInfo(@PathVariable String secretKey) {
        return memberService.getMemberInfo(secretKey);
    }

    @PostMapping("/admin")
    public CreateMemberResponse createMember(@RequestBody CreateMemberRequest createMemberRequest) {
        return memberService.createMember(createMemberRequest);
    }

}
