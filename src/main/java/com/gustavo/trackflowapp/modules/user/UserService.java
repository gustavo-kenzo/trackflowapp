package com.gustavo.trackflowapp.modules.user;

import com.gustavo.trackflowapp.modules.user.dto.UserDataDTO;
import com.gustavo.trackflowapp.modules.user.dto.UserRegisterDTO;
import com.gustavo.trackflowapp.modules.user.dto.UserUpdateDTO;
import com.gustavo.trackflowapp.shared.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;


@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Transactional
    public UserDataDTO registerUser(UserRegisterDTO dto) {
        var password = passwordEncoder.encode(dto.password());
        var user = new User(dto.name(), dto.email(), password);
        userRepository.save(user);
        return new UserDataDTO(user.getId(), user.getName(), user.getEmail());
    }

    @Transactional
    public void delete(Long id) {
        var rowsAffected = userRepository.deleteByIdAndReturnCount(id);
        if (rowsAffected == 0)
            throw new ResourceNotFoundException("User id not found for hard delete");
    }

    @Transactional
    public UserDataDTO update(UserUpdateDTO dto, Long id) {
        var user = userRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("User not found for update"));
        if (dto.name() != null)
            user.updateName(dto.name());
        if (dto.password() != null) {
            var password = passwordEncoder.encode(dto.password());
            user.updatePassword(password);
        }
        return new UserDataDTO(user);
    }

    public UserDataDTO getMyProfile(Long id) {
        var user = userRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("User not found for list"));
        return new UserDataDTO(user);
    }
}
