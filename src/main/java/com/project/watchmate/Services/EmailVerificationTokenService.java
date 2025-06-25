package com.project.watchmate.Services;

import java.time.LocalDateTime;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.project.watchmate.Models.EmailVerificationToken;
import com.project.watchmate.Models.Users;
import com.project.watchmate.Repositories.EmailVerificationTokenRepository;
import com.project.watchmate.Repositories.UsersRepository;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import software.amazon.awssdk.services.ses.SesClient;
import software.amazon.awssdk.services.ses.model.Body;
import software.amazon.awssdk.services.ses.model.Content;
import software.amazon.awssdk.services.ses.model.Destination;
import software.amazon.awssdk.services.ses.model.Message;
import software.amazon.awssdk.services.ses.model.SendEmailRequest;

@Service
@RequiredArgsConstructor
public class EmailVerificationTokenService {

    private final EmailVerificationTokenRepository tokenRepository;

    private final UsersRepository usersRepository;

    private final SesClient sesClient;

    @Value("${app.domain}")
    private String appDomain;

    @Value("${verified.sender}")
    private String verifiedSender;

    @Transactional
    public String createToken(Users user){
        String token = UUID.randomUUID().toString();

        EmailVerificationToken verificationToken = EmailVerificationToken.builder()
        .token(token)
        .user(user)
        .expiresAt(LocalDateTime.now().plusMinutes(15))
        .build();

        tokenRepository.save(verificationToken);
        return token;
    }

    @Transactional
    public boolean verifyToken(String token){
        EmailVerificationToken verificationToken = tokenRepository.getByToken(token);

        if (verificationToken == null){
            return false;
        }

        if (verificationToken.getExpiresAt().isBefore(LocalDateTime.now())){
            tokenRepository.delete(verificationToken);
            return false;
        }

        Users user = verificationToken.getUser();
        user.setEmailVerified(true);
        usersRepository.save(user);

        tokenRepository.delete(verificationToken);

        return true;
    }

    public void sendVerificationEmail(String toEmail, String token){
        String subject = "Verify Your Email - WatchMate";

        String verificationLink = appDomain + "/verify?token=" + token;

        String bodyText = String.format("""
            <html>
            <body>
                <p>Hello,</p>
                <p>Thank you for registering with <strong>WatchMate</strong>!</p>
                <p>Please verify your email by clicking the button below:</p>
                <p>
                    <a href="%s" style="
                        background-color:rgb(240, 84, 53);
                        color: white;
                        padding: 10px 20px;
                        text-decoration: none;
                        display: inline-block;
                        border-radius: 5px;
                        font-weight: bold;
                    ">
                        Click here to verify
                    </a>
                </p>
                <p>This link will expire in 15 minutes.</p>
                <p>– WatchMate Team</p>
            </body>
            </html>
            """, verificationLink);

            Destination destination = Destination.builder()
            .toAddresses(toEmail)
            .build();

            Message message = Message.builder()
            .subject(Content.builder().data(subject).build())
            .body(Body.builder().html(Content.builder().data(bodyText).build()).build())
            .build();

            SendEmailRequest emailRequest = SendEmailRequest.builder()
            .destination(destination)
            .message(message)
            .source(verifiedSender)
            .build();

            sesClient.sendEmail(emailRequest);
    }

    @Transactional
    public void resendVerificationEmail(String email){
        Users user = usersRepository.findByEmail(email);

        if (user == null){
            throw new IllegalArgumentException("User not Found");
        }
        if (user.isEmailVerified()){
            throw new IllegalStateException("Email already Verified");
        }

        tokenRepository.deleteByUserId(user.getId());

        String token = createToken(user);

        sendVerificationEmail(email, token);
    }
}
