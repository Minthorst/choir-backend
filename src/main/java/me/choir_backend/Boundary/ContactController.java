package me.choir_backend.Boundary;

import jakarta.validation.Valid;
import me.choir_backend.service.ContactService;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/contact")
public class ContactController {

    private final ContactService contactService;

    public ContactController(ContactService contactService) {
        this.contactService = contactService;
    }

    @PostMapping
    public void sendFeedback(@Valid @RequestBody ContactRequest request, Authentication authentication) {
        contactService.sendFeedback(request, authentication);
    }
}
