package com.project.watchmate.auth.application;

import java.time.LocalDateTime;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.project.watchmate.common.error.EmailDeliveryUnavailableException;
import com.project.watchmate.common.error.InvalidEmailVerificationTokenException;
import com.project.watchmate.auth.domain.EmailVerificationToken;
import com.project.watchmate.user.domain.Users;
import com.project.watchmate.auth.persistence.EmailVerificationTokenRepository;
import com.project.watchmate.user.persistence.UsersRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import software.amazon.awssdk.services.ses.SesClient;
import software.amazon.awssdk.services.ses.model.Body;
import software.amazon.awssdk.services.ses.model.Content;
import software.amazon.awssdk.services.ses.model.Destination;
import software.amazon.awssdk.services.ses.model.Message;
import software.amazon.awssdk.services.ses.model.SendEmailRequest;
import software.amazon.awssdk.services.ses.model.SesException;

@Service
@Slf4j
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

        EmailVerificationToken verificationToken = Objects.requireNonNull(EmailVerificationToken.builder()
        .token(token)
        .user(user)
        .expiresAt(LocalDateTime.now().plusMinutes(15))
        .build());

        tokenRepository.save(verificationToken);
        log.info("Created email verification token username={}", user.getUsername());
        return token;
    }

    @Transactional
    public void verifyEmailToken(String token){
        Optional<EmailVerificationToken> tokenOpt = tokenRepository.getByToken(token);
        if (tokenOpt.isEmpty()){
            log.warn("Email verification failed reason=token_not_found");
            throw new InvalidEmailVerificationTokenException("Invalid verification token");
        }
        EmailVerificationToken verificationToken = tokenOpt.get();

        if (verificationToken.getExpiresAt().isBefore(LocalDateTime.now())){
            tokenRepository.delete(verificationToken);
            log.warn("Email verification failed reason=token_expired username={}", verificationToken.getUser().getUsername());
            throw new InvalidEmailVerificationTokenException("Verification token expired");
        }

        Users user = verificationToken.getUser();
        user.setEmailVerified(true);
        usersRepository.save(user);

        tokenRepository.delete(verificationToken);
        log.info("Email verified username={}", user.getUsername());
    }

    public void sendVerificationEmail(String toEmail, String token){
        String subject = "Verify Your Email - WatchMate";

        String verificationLink = appDomain + "/verify-email?token=" + token;

        String bodyText = """
            <!DOCTYPE html>
            <html lang="en">
            <head>
                <meta charset="utf-8">
                <meta name="viewport" content="width=device-width, initial-scale=1.0">
                <meta name="color-scheme" content="dark">
                <meta name="supported-color-schemes" content="dark">
                <title>Verify your email</title>
            </head>
            <body style="margin:0; padding:0; background-color:#0c0d0d; -webkit-font-smoothing:antialiased; -moz-osx-font-smoothing:grayscale;">
                <!-- Preheader (hidden preview text) -->
                <div style="display:none; max-height:0; overflow:hidden; mso-hide:all;">
                    Confirm your email to start tracking what you watch. This link expires in 15 minutes.
                </div>

                <table role="presentation" width="100%" cellpadding="0" cellspacing="0" border="0" style="background-color:#0c0d0d;">
                    <tr>
                        <td align="center" style="padding:48px 16px;">
                            <table role="presentation" width="560" cellpadding="0" cellspacing="0" border="0" style="width:560px; max-width:100%;">

                                <!-- Brand -->
                                <tr>
                                    <td align="center" style="padding-bottom:28px;">
                                        <span style="font-family:-apple-system,'Segoe UI',Roboto,Helvetica,Arial,sans-serif; font-size:22px; font-weight:800; letter-spacing:-0.5px; color:#edefee;">Watch<span style="color:#45c793;">Mate</span></span>
                                    </td>
                                </tr>

                                <!-- Card -->
                                <tr>
                                    <td style="background-color:#16181a; border:1px solid #26292b; border-radius:14px; overflow:hidden;">
                                        <table role="presentation" width="100%" cellpadding="0" cellspacing="0" border="0">

                                            <!-- Jade accent bar -->
                                            <tr>
                                                <td style="height:4px; background-color:#2fae7e; font-size:0; line-height:0;">&nbsp;</td>
                                            </tr>

                                            <tr>
                                                <td style="padding:44px 44px 40px 44px;">

                                                    <h1 style="margin:0 0 14px 0; font-family:-apple-system,'Segoe UI',Roboto,Helvetica,Arial,sans-serif; font-size:26px; line-height:34px; font-weight:700; letter-spacing:-0.4px; color:#edefee;">
                                                        Verify your email address
                                                    </h1>

                                                    <p style="margin:0 0 28px 0; font-family:-apple-system,'Segoe UI',Roboto,Helvetica,Arial,sans-serif; font-size:15px; line-height:24px; color:#b4b9b7;">
                                                        Welcome to WatchMate! You're one step away from tracking, rating, and discovering everything you watch. Confirm your email address to activate your account.
                                                    </p>

                                                    <!-- CTA button -->
                                                    <table role="presentation" cellpadding="0" cellspacing="0" border="0" align="center" style="margin:0 auto 28px auto;">
                                                        <tr>
                                                            <td align="center" style="background-color:#45c793; border-radius:10px;">
                                                                <a href="{{VERIFICATION_LINK}}" target="_blank" style="display:inline-block; padding:14px 40px; font-family:-apple-system,'Segoe UI',Roboto,Helvetica,Arial,sans-serif; font-size:15px; font-weight:700; letter-spacing:0.2px; color:#06231a; text-decoration:none; border-radius:10px;">
                                                                    Verify Email
                                                                </a>
                                                            </td>
                                                        </tr>
                                                    </table>

                                                    <!-- Expiry note -->
                                                    <table role="presentation" width="100%" cellpadding="0" cellspacing="0" border="0" style="margin-bottom:28px;">
                                                        <tr>
                                                            <td align="center" style="background-color:#1d1f21; border:1px solid #26292b; border-radius:8px; padding:12px 16px; font-family:-apple-system,'Segoe UI',Roboto,Helvetica,Arial,sans-serif; font-size:13px; line-height:20px; color:#7f8583;">
                                                                &#9202;&nbsp; This link expires in <span style="color:#6fd1a8; font-weight:600;">15 minutes</span>
                                                            </td>
                                                        </tr>
                                                    </table>

                                                    <!-- Divider -->
                                                    <table role="presentation" width="100%" cellpadding="0" cellspacing="0" border="0">
                                                        <tr>
                                                            <td style="height:1px; background-color:#26292b; font-size:0; line-height:0;">&nbsp;</td>
                                                        </tr>
                                                    </table>

                                                    <!-- Fallback link -->
                                                    <p style="margin:24px 0 6px 0; font-family:-apple-system,'Segoe UI',Roboto,Helvetica,Arial,sans-serif; font-size:13px; line-height:20px; color:#7f8583;">
                                                        If the button doesn't work, copy and paste this link into your browser:
                                                    </p>
                                                    <p style="margin:0; word-break:break-all; font-family:-apple-system,'Segoe UI',Roboto,Helvetica,Arial,sans-serif; font-size:13px; line-height:20px;">
                                                        <a href="{{VERIFICATION_LINK}}" target="_blank" style="color:#2fae7e; text-decoration:underline;">{{VERIFICATION_LINK}}</a>
                                                    </p>

                                                </td>
                                            </tr>
                                        </table>
                                    </td>
                                </tr>

                                <!-- Footer -->
                                <tr>
                                    <td align="center" style="padding:28px 24px 0 24px;">
                                        <p style="margin:0 0 6px 0; font-family:-apple-system,'Segoe UI',Roboto,Helvetica,Arial,sans-serif; font-size:12px; line-height:18px; color:#7f8583;">
                                            You're receiving this email because an account was created on WatchMate with this address.
                                        </p>
                                        <p style="margin:0; font-family:-apple-system,'Segoe UI',Roboto,Helvetica,Arial,sans-serif; font-size:12px; line-height:18px; color:#7f8583;">
                                            If this wasn't you, you can safely ignore this email.
                                        </p>
                                        <p style="margin:16px 0 0 0; font-family:-apple-system,'Segoe UI',Roboto,Helvetica,Arial,sans-serif; font-size:12px; line-height:18px; color:#7f8583;">
                                            &#169; 2026 <span style="color:#b4b9b7; font-weight:600;">WatchMate</span>
                                        </p>
                                    </td>
                                </tr>

                            </table>
                        </td>
                    </tr>
                </table>
            </body>
            </html>
            """.replace("{{VERIFICATION_LINK}}", verificationLink);

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

            try {
            sesClient.sendEmail(emailRequest);
            log.info("Verification email sent");
            } catch (SesException ex) {
                log.error("Verification email send failed reason={} statusCode={}",
                    ex.awsErrorDetails() != null ? ex.awsErrorDetails().errorCode() : "unknown",
                    ex.statusCode(),
                    ex);
                throw new EmailDeliveryUnavailableException("Email delivery is temporarily unavailable", ex);
            } catch (RuntimeException ex) {
                log.error("Verification email send failed due to client/runtime error", ex);
                throw new EmailDeliveryUnavailableException("Email delivery is temporarily unavailable", ex);
            }
    }

    @Transactional
    public void resendVerificationEmail(String email){
        Users user = usersRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("User not Found"));
        if (user.isEmailVerified()){
            throw new IllegalStateException("Email already Verified");
        }

        tokenRepository.deleteByUserId(user.getId());

        String token = createToken(user);

        sendVerificationEmail(email, token);
        log.info("Verification email resent username={}", user.getUsername());
    }
}



