package rszabadi.swiss;

import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

@Tag(name = "Session")
@RestController
public class MeController {

    public record MeResponse(String username, boolean canEdit) {
    }

    @Operation(summary = "Who is logged in, and may they change data?")
    @GetMapping("/me")
    public MeResponse me(Authentication authentication) {
        boolean canEdit = authentication.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));
        return new MeResponse(authentication.getName(), canEdit);
    }
}