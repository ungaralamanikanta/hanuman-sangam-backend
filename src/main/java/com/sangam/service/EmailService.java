package com.sangam.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.RestTemplate;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * EmailService — Hanuman Sangam
 *
 * SMTP → Resend HTTP API (Render free tier compatible).
 * Port block problem ledu — pure HTTPS call.
 *
 * ── application.properties ──────────────────────────────────────
 *   resend.api.key=${RESEND_API_KEY}
 *   app.mail.from=onboarding@resend.dev
 *   app.mail.from-name=Hanuman Sangam
 *   app.admin.email=hanumansangamu@gmail.com
 *
 * ── Render Dashboard → Environment ──────────────────────────────
 *   RESEND_API_KEY = re_xxxxxxxxxxxxxxxx
 * ────────────────────────────────────────────────────────────────
 */
@Service
public class EmailService {

    private static final Logger log = LoggerFactory.getLogger(EmailService.class);

    private static final String RESEND_URL    = "https://api.resend.com/emails";
    private static final int    MAX_RETRIES   = 3;
    private static final long   RETRY_DELAY   = 1500L;

    @Value("${resend.api.key}")
    private String resendApiKey;

    @Value("${app.mail.from}")
    private String fromEmail;

    @Value("${app.mail.from-name}")
    private String fromName;

    @Value("${app.admin.email}")
    private String adminEmail;

    private final RestTemplate restTemplate = new RestTemplate();
    private final ObjectMapper objectMapper  = new ObjectMapper();

    // ─────────────────────────────────────────────────────────────
    // VALIDATION
    // ─────────────────────────────────────────────────────────────

    private void validateEmail(String email) {
        if (email == null || !email.matches("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+$")) {
            throw new IllegalArgumentException("Invalid email address: " + email);
        }
    }

    // ─────────────────────────────────────────────────────────────
    // CORE SEND — Resend HTTP API with retry
    // ─────────────────────────────────────────────────────────────

    private void send(String to, String subject, String html) {
        send(to, subject, html, null);
    }

    private void send(String to, String subject, String html, String replyTo) {
        validateEmail(to);

        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(resendApiKey);
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setAccept(List.of(MediaType.APPLICATION_JSON));

        String jsonBody;
        try {
            Map<String, Object> payload = new LinkedHashMap<>();
            payload.put("from",    fromName + " <" + fromEmail + ">");
            payload.put("to",      List.of(to));
            payload.put("subject", subject);
            payload.put("html",    wrapInTemplate(html));
            if (!isBlank(replyTo)) payload.put("reply_to", replyTo);
            jsonBody = objectMapper.writeValueAsString(payload);
        } catch (Exception e) {
            throw new RuntimeException("Failed to serialize email payload", e);
        }

        HttpEntity<String> request = new HttpEntity<>(jsonBody, headers);

        for (int attempt = 1; attempt <= MAX_RETRIES; attempt++) {
            try {
                ResponseEntity<String> response =
                        restTemplate.postForEntity(RESEND_URL, request, String.class);

                if (response.getStatusCode().is2xxSuccessful()) {
                    log.info("✅ Email sent | to={} | subject='{}' | attempt={}",
                            to, subject, attempt);
                    return;
                }

            } catch (HttpClientErrorException e) {
                // 4xx — bad API key, bad email, etc. — retry useless
                log.error("❌ Resend 4xx error ({}): {} — to={}",
                        e.getStatusCode(), e.getResponseBodyAsString(), to);
                throw new RuntimeException(
                        "Email send failed (check API key / from address): "
                        + e.getResponseBodyAsString(), e);

            } catch (HttpServerErrorException e) {
                // 5xx — Resend server issue — retry cheyyi
                log.warn("⚠️  Resend 5xx ({}) attempt {}/{} for {}",
                        e.getStatusCode(), attempt, MAX_RETRIES, to);

            } catch (Exception e) {
                log.warn("⚠️  Network error attempt {}/{} for {}: {}",
                        attempt, MAX_RETRIES, to, e.getMessage());
            }

            if (attempt < MAX_RETRIES) {
                try { Thread.sleep(RETRY_DELAY); }
                catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    throw new RuntimeException("Email send interrupted", ie);
                }
            }
        }

        throw new RuntimeException(
                "Failed to send email to " + to + " after " + MAX_RETRIES + " attempts.");
    }

    // ─────────────────────────────────────────────────────────────
    // HTML WRAPPER TEMPLATE
    // ─────────────────────────────────────────────────────────────

    private String wrapInTemplate(String bodyContent) {
        return "<!DOCTYPE html><html lang='en'>" +
            "<head><meta charset='UTF-8'/>" +
            "<meta name='viewport' content='width=device-width,initial-scale=1'/></head>" +
            "<body style='margin:0;padding:0;background:#fdf6ee;font-family:Georgia,serif;'>" +
            "<table width='100%' cellpadding='0' cellspacing='0'" +
            " style='background:#fdf6ee;padding:36px 16px;'>" +
            "<tr><td align='center'>" +
            "<table width='600' cellpadding='0' cellspacing='0'" +
            " style='max-width:600px;width:100%;border-radius:16px;" +
            " box-shadow:0 4px 24px rgba(0,0,0,0.09);overflow:hidden;'>" +
            // ── Header
            "<tr><td style='background:linear-gradient(135deg,#bf360c,#e65c00,#f9a825);" +
            " padding:34px 40px;text-align:center;'>" +
            "<div style='font-size:50px;margin-bottom:10px;'>&#x1F64F;</div>" +
            "<h1 style='margin:0;color:#fff;font-size:27px;font-weight:700;" +
            " letter-spacing:2px;font-family:Georgia,serif;'>Hanuman Sangam</h1>" +
            "<p style='margin:7px 0 0;color:rgba(255,255,255,0.88);font-size:12px;" +
            " letter-spacing:3px;text-transform:uppercase;" +
            " font-family:Arial,sans-serif;'>&#2404; Jay Shri Ram &#2404;</p>" +
            "</td></tr>" +
            // ── Body
            "<tr><td style='background:#ffffff;padding:38px 40px;'>" +
            bodyContent + "</td></tr>" +
            // ── Divider
            "<tr><td style='background:#fff8f0;padding:16px;text-align:center;" +
            " border-top:1px solid #f0e0cc;border-bottom:1px solid #f0e0cc;'>" +
            "<span style='color:#e65c00;font-size:18px;letter-spacing:8px;'>~ ~ ~</span>" +
            "</td></tr>" +
            // ── Footer
            "<tr><td style='background:#1a0a00;padding:26px 40px;text-align:center;'>" +
            "<p style='margin:0 0 5px;color:#f9a825;font-size:12px;font-weight:700;" +
            " letter-spacing:2px;font-family:Arial,sans-serif;'>HANUMAN SANGAM</p>" +
            "<p style='margin:0 0 4px;color:rgba(255,255,255,0.5);font-size:11px;" +
            " font-family:Arial,sans-serif;'>This is an automated message — please do not reply.</p>" +
            "<p style='margin:0;font-size:11px;font-family:Arial,sans-serif;'>" +
            "<a href='mailto:hanumansangamu@gmail.com'" +
            " style='color:#f9a825;text-decoration:none;'>hanumansangamu@gmail.com</a></p>" +
            "</td></tr>" +
            "</table></td></tr></table></body></html>";
    }

    // ─────────────────────────────────────────────────────────────
    // 1. OTP — Registration
    // ─────────────────────────────────────────────────────────────

    public void sendOtp(String toEmail, String otp) {
        log.info("Sending registration OTP to {}", toEmail);
        String html =
            "<h2 style='margin:0 0 4px;color:#e65c00;font-size:21px;" +
            " font-family:Georgia,serif;'>Email Verification</h2>" +
            "<p style='margin:0 0 26px;color:#bbb;font-size:11px;letter-spacing:1px;" +
            " text-transform:uppercase;font-family:Arial,sans-serif;" +
            " padding-bottom:18px;border-bottom:2px solid #f0e0cc;'>" +
            "One-Time Password for Hanuman Sangam Registration</p>" +
            "<p style='margin:0 0 14px;color:#333;font-size:15px;line-height:1.8;'>Dear Member,</p>" +
            "<p style='margin:0 0 26px;color:#555;font-size:15px;" +
            " line-height:1.8;font-family:Arial,sans-serif;'>" +
            "Thank you for joining <strong style='color:#e65c00;'>Hanuman Sangam</strong>! " +
            "Use the OTP below to verify your email and complete your registration.</p>" +
            otpBox(otp) +
            securityNotice("Hanuman Sangam will <strong>never</strong> ask for your OTP. " +
                "Do not share it with anyone.") +
            "<p style='margin:0 0 20px;color:#888;font-size:13px;" +
            " font-family:Arial,sans-serif;line-height:1.7;'>" +
            "If you did not request this OTP, please ignore this email safely.</p>" +
            footer();

        send(toEmail, "Hanuman Sangam — Email Verification Code", html);
    }

    // ─────────────────────────────────────────────────────────────
    // 2. OTP — Forgot Password
    // ─────────────────────────────────────────────────────────────

    public void sendForgotPasswordOtp(String toEmail, String memberName, String otp) {
        log.info("Sending forgot-password OTP to {}", toEmail);
        String html =
            "<h2 style='margin:0 0 4px;color:#e65c00;font-size:21px;" +
            " font-family:Georgia,serif;'>Password Reset Request</h2>" +
            "<p style='margin:0 0 26px;color:#bbb;font-size:11px;letter-spacing:1px;" +
            " text-transform:uppercase;font-family:Arial,sans-serif;" +
            " padding-bottom:18px;border-bottom:2px solid #f0e0cc;'>" +
            "One-Time Password for Password Reset</p>" +
            "<p style='margin:0 0 14px;color:#333;font-size:15px;line-height:1.8;'>" +
            "Dear <strong>" + escapeHtml(memberName) + "</strong>,</p>" +
            "<p style='margin:0 0 26px;color:#555;font-size:15px;" +
            " line-height:1.8;font-family:Arial,sans-serif;'>" +
            "We received a request to reset your " +
            "<strong style='color:#e65c00;'>Hanuman Sangam</strong> password. " +
            "Use the OTP below to proceed.</p>" +
            otpBox(otp) +
            securityNotice("If you did not request a password reset, please ignore this email. " +
                "Your password will <strong>not</strong> be changed.") +
            footer();

        send(toEmail, "Hanuman Sangam — Password Reset OTP", html);
    }

    // ─────────────────────────────────────────────────────────────
    // 3. Membership Approved
    // ─────────────────────────────────────────────────────────────

    public void sendApprovalNotification(String toEmail, String memberName) {
        log.info("Sending approval notification to {}", toEmail);
        String html =
            "<h2 style='margin:0 0 4px;color:#2e7d32;font-size:21px;" +
            " font-family:Georgia,serif;'>Membership Approved! &#x2705;</h2>" +
            "<p style='margin:0 0 26px;color:#bbb;font-size:11px;" +
            " text-transform:uppercase;font-family:Arial,sans-serif;" +
            " padding-bottom:18px;border-bottom:2px solid #f0e0cc;'>" +
            "Welcome to the Hanuman Sangam Family</p>" +
            "<p style='margin:0 0 14px;color:#333;font-size:15px;line-height:1.8;'>" +
            "Dear <strong>" + escapeHtml(memberName) + "</strong>,</p>" +
            "<p style='margin:0 0 24px;color:#555;font-size:15px;" +
            " line-height:1.8;font-family:Arial,sans-serif;'>" +
            "Your membership for <strong style='color:#e65c00;'>Hanuman Sangam</strong> has been " +
            "<strong style='color:#2e7d32;'>APPROVED</strong>! " +
            "You are now an official member of our sacred community!</p>" +
            "<div style='background:linear-gradient(135deg,#e8f5e9,#c8e6c9);" +
            " border:1px solid #a5d6a7;border-radius:14px;" +
            " padding:26px 20px;text-align:center;margin:0 0 26px;'>" +
            "<h3 style='margin:0 0 8px;color:#1b5e20;font-size:19px;" +
            " font-family:Georgia,serif;'>&#x1F64F; You are officially a Member!</h3>" +
            "<p style='margin:0;color:#388e3c;font-size:14px;font-family:Arial,sans-serif;'>" +
            "Your account is now active and ready to use.</p></div>" +
            footer();

        send(toEmail, "Membership Approved — Welcome to Hanuman Sangam!", html);
    }

    // ─────────────────────────────────────────────────────────────
    // 4. Membership Rejected
    // ─────────────────────────────────────────────────────────────

    public void sendRejectionNotification(String toEmail, String memberName) {
        log.info("Sending rejection notification to {}", toEmail);
        String html =
            "<h2 style='margin:0 0 4px;color:#c62828;font-size:21px;" +
            " font-family:Georgia,serif;'>Membership Status Update</h2>" +
            "<p style='margin:0 0 26px;color:#bbb;font-size:11px;" +
            " text-transform:uppercase;font-family:Arial,sans-serif;" +
            " padding-bottom:18px;border-bottom:2px solid #f0e0cc;'>" +
            "Regarding your Hanuman Sangam registration</p>" +
            "<p style='margin:0 0 14px;color:#333;font-size:15px;line-height:1.8;'>" +
            "Dear <strong>" + escapeHtml(memberName) + "</strong>,</p>" +
            "<p style='margin:0 0 24px;color:#555;font-size:15px;" +
            " line-height:1.8;font-family:Arial,sans-serif;'>" +
            "After careful review, your membership request has " +
            "<strong style='color:#c62828;'>not been approved</strong> at this time. " +
            "Please contact our admin at " +
            "<a href='mailto:hanumansangamu@gmail.com' style='color:#e65c00;'>" +
            "hanumansangamu@gmail.com</a> for clarification.</p>" +
            footer();

        send(toEmail, "Membership Status Update — Hanuman Sangam", html);
    }

    // ─────────────────────────────────────────────────────────────
    // 5. Announcement
    // ─────────────────────────────────────────────────────────────

    public void sendAnnouncementEmail(String toEmail, String memberName,
                                      String title, String announcementBody) {
        log.info("Sending announcement '{}' to {}", title, toEmail);
        String html =
            "<h2 style='margin:0 0 4px;color:#e65c00;font-size:21px;" +
            " font-family:Georgia,serif;'>&#x1F4E2; " + escapeHtml(title) + "</h2>" +
            "<p style='margin:0 0 26px;color:#bbb;font-size:11px;" +
            " text-transform:uppercase;font-family:Arial,sans-serif;" +
            " padding-bottom:18px;border-bottom:2px solid #f0e0cc;'>" +
            "Important update from Hanuman Sangam</p>" +
            "<p style='margin:0 0 24px;color:#333;font-size:15px;line-height:1.8;'>" +
            "Dear <strong>" + escapeHtml(memberName) + "</strong>,</p>" +
            "<div style='background:linear-gradient(135deg,#fff8f0,#fff3e0);" +
            " border:1px solid #ffccbc;border-radius:14px;padding:28px;margin:0 0 26px;'>" +
            "<div style='color:#333;font-size:15px;line-height:1.9;" +
            " font-family:Arial,sans-serif;white-space:pre-line;'>" +
            escapeHtml(announcementBody) + "</div></div>" +
            footer();

        send(toEmail, escapeHtml(title) + " — Hanuman Sangam", html);
    }

    // ─────────────────────────────────────────────────────────────
    // 6. Contact → Admin
    // ─────────────────────────────────────────────────────────────

    public void sendContactMessageToAdmin(String memberName, String memberEmail,
                                          String memberPhone, String message) {
        log.info("Sending contact message from {} to admin", memberName);
        String safeEmail = isBlank(memberEmail) ? "Not provided" : memberEmail;
        String safePhone = isBlank(memberPhone) ? "Not provided" : memberPhone;
        String replyTo   = isBlank(memberEmail) ? adminEmail   : memberEmail;

        String html =
            "<h2 style='margin:0 0 4px;color:#e65c00;font-size:21px;" +
            " font-family:Georgia,serif;'>&#x1F4E9; New Contact Message</h2>" +
            "<p style='margin:0 0 26px;color:#bbb;font-size:11px;" +
            " text-transform:uppercase;font-family:Arial,sans-serif;" +
            " padding-bottom:18px;border-bottom:2px solid #f0e0cc;'>" +
            "Received via Hanuman Sangam Member Portal</p>" +
            "<div style='background:#fafafa;border:1px solid #f0e0cc;" +
            " border-radius:12px;padding:20px;margin:0 0 22px;'>" +
            "<table cellpadding='0' cellspacing='8' width='100%'>" +
            infoRow("Name",  escapeHtml(memberName)) +
            infoRow("Email", escapeHtml(safeEmail)) +
            infoRow("Phone", escapeHtml(safePhone)) +
            "</table></div>" +
            "<div style='background:#fff8f0;border-left:5px solid #e65c00;" +
            " border-radius:0 12px 12px 0;padding:20px;margin:0 0 22px;'>" +
            "<p style='margin:0;color:#333;font-size:15px;" +
            " line-height:1.9;font-family:Arial,sans-serif;white-space:pre-line;'>" +
            escapeHtml(message) + "</p></div>" +
            footer();

        send(adminEmail,
             "Contact from " + escapeHtml(memberName) + " — Hanuman Sangam",
             html, replyTo);
    }

    // ─────────────────────────────────────────────────────────────
    // HTML HELPERS
    // ─────────────────────────────────────────────────────────────

    private String otpBox(String otp) {
        return "<div style='background:linear-gradient(135deg,#fff8f0,#fff3e0);" +
            " border:2px dashed #e65c00;border-radius:14px;" +
            " padding:30px 20px;text-align:center;margin:0 0 26px;'>" +
            "<p style='margin:0 0 10px;color:#bbb;font-size:11px;" +
            " letter-spacing:3px;text-transform:uppercase;" +
            " font-family:Arial,sans-serif;'>Your One-Time Password</p>" +
            "<div style='font-size:44px;font-weight:700;color:#e65c00;" +
            " letter-spacing:14px;font-family:\"Courier New\",monospace;margin:6px 0;'>" +
            escapeHtml(otp) + "</div>" +
            "<p style='margin:10px 0 0;color:#999;font-size:12px;font-family:Arial,sans-serif;'>" +
            "Valid for <strong>5 minutes</strong> only — do not share</p>" +
            "</div>";
    }

    private String securityNotice(String message) {
        return "<div style='background:#fff8e1;border-left:4px solid #f9a825;" +
            " border-radius:0 10px 10px 0;padding:14px 18px;margin:0 0 22px;'>" +
            "<p style='margin:0;color:#7a5c00;font-size:13px;" +
            " line-height:1.7;font-family:Arial,sans-serif;'>" +
            "<strong>Security Notice:</strong> " + message + "</p>" +
            "</div>";
    }

    private String footer() {
        return "<p style='margin:0 0 2px;color:#e65c00;font-size:16px;font-weight:600;'>" +
            "Jai Bajrang Bali! &#x1F64F;</p>" +
            "<p style='margin:0;color:#aaa;font-size:12px;" +
            " font-family:Arial,sans-serif;'>Hanuman Sangam Team</p>";
    }

    private String infoRow(String label, String value) {
        return "<tr>" +
               "<td style='color:#999;font-size:13px;font-family:Arial,sans-serif;" +
               " width:80px;padding:4px 0;'>" + label + "</td>" +
               "<td style='color:#222;font-size:14px;font-weight:600;" +
               " font-family:Arial,sans-serif;padding:4px 0;'>" + value + "</td>" +
               "</tr>";
    }

    private String escapeHtml(String text) {
        if (text == null) return "";
        return text
            .replace("&", "&amp;")
            .replace("<", "&lt;")
            .replace(">", "&gt;")
            .replace("\"", "&quot;");
    }

    private boolean isBlank(String s) {
        return s == null || s.isBlank();
    }
}
