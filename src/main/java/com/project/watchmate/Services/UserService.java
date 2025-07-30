package com.project.watchmate.Services;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

import com.project.watchmate.Dto.LoginRequestDTO;
import com.project.watchmate.Dto.RegisterRequestDTO;
import com.project.watchmate.Exception.EmailException;
import com.project.watchmate.Exception.UsernameException;
import com.project.watchmate.Models.Users;
import com.project.watchmate.Repositories.UsersRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UsersRepository userRepo;

    private final AuthenticationManager authManager;

    private final JwtService jwtService;

    private final EmailVerificationTokenService emailService;

    private BCryptPasswordEncoder encoder = new BCryptPasswordEncoder(12);

    public void register(RegisterRequestDTO registerRequest){
        if (userRepo.existsByUsername(registerRequest.getUsername())) {
            throw new UsernameException("Username is already taken.");
        }
        if (userRepo.existsByEmail(registerRequest.getEmail())) {
        throw new EmailException("Email is already in use.");
        }

        try { Users user = Users.builder()
        .username(registerRequest.getUsername())
        .password(encoder.encode(registerRequest.getPassword()))
        .email(registerRequest.getEmail())
        .emailVerified(false)
        .build();
        userRepo.save(user);

        emailService.sendVerificationEmail(user.getEmail(), emailService.createToken(user));
        } catch (DataIntegrityViolationException ex){
            throw new RuntimeException("Account could not be created. Please try again.", ex);
        }
    }

    public String verify(LoginRequestDTO loginRequest) {
        Authentication auth = authManager.authenticate(new UsernamePasswordAuthenticationToken(loginRequest.getUsername(), loginRequest.getPassword()));

        if (auth.isAuthenticated()){
            return jwtService.generateToken(loginRequest.getUsername());
        }
        return "fail";
    }
}
