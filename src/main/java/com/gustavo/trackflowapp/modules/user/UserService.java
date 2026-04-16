package com.gustavo.trackflowapp.modules.user;

import com.gustavo.trackflowapp.modules.user.dto.UserDataDTO;
import com.gustavo.trackflowapp.modules.user.dto.UserRegisterDTO;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
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

    public Page<UserDataDTO> findAll(Pageable pageable) {
        return userRepository.findAll(pageable).map(UserDataDTO::new);
    }

    public UserDataDTO findById(Long id) {
        return userRepository
                .findById(id)
                .map(UserDataDTO::new)
                .orElseThrow(() -> new RuntimeException("User not found"));
    }

    @Transactional
    public void deleteById(Long id) {
        var rowsAffected = userRepository.deleteByIdAndReturnCount(id);
        if (rowsAffected == 0)
            throw new RuntimeException("User id not found");
    }
}
