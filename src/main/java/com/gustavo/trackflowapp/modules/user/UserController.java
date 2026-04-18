package com.gustavo.trackflowapp.modules.user;

import com.gustavo.trackflowapp.modules.user.dto.UserDataDTO;
import com.gustavo.trackflowapp.modules.user.dto.UserRegisterDTO;
import com.gustavo.trackflowapp.modules.user.dto.UserUpdateDTO;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.util.UriComponentsBuilder;

@RestController
@RequestMapping("users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    @PostMapping
    public ResponseEntity<UserDataDTO> registerUser(@RequestBody @Valid UserRegisterDTO dto, UriComponentsBuilder uriBuilder) {
        var userData = userService.registerUser(dto);
        var uri = uriBuilder.path("users/{id}").buildAndExpand(userData.id()).toUri();
        return ResponseEntity.created(uri).body(userData);
    }

    @DeleteMapping
    public ResponseEntity deleteMyProfile(@AuthenticationPrincipal(expression = "id") Long id) {
        userService.delete(id);
        return ResponseEntity.noContent().build();
    }

    @PutMapping
    public ResponseEntity<UserDataDTO> updateMyProfile(@RequestBody @Valid UserUpdateDTO dto, @AuthenticationPrincipal(expression = "id") Long id) {
        return ResponseEntity.ok(userService.update(dto, id));
    }

    @GetMapping
    public ResponseEntity getMyProfile(@AuthenticationPrincipal(expression = "id") Long id) {
        return ResponseEntity.ok(userService.getMyProfile(id));
    }
}
