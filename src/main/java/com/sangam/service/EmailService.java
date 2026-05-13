package com.sangam.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.List;
import java.util.Map;

/**
 * EmailService — sends transactional emails via Brevo REST API (HTTPS/443).
 *
 * Why not SMTP?
 *   Render.com blocks all outbound SMTP (ports 465 & 587).
 *   HTTPS calls on port 443 are never blocked, so this always works.
 *
 * Setup required in Brevo:
 *   1. Settings → Senders, domains & IPs → Add & verify sender email
 *   2. Settings → SMTP & API → API keys tab → Generate key → set as BREVO_API_KEY env var
 */
@Service
public class EmailService {

    @Value("${brevo.api.key}")
    private String apiKey;

    @Value("${app.admin.email}")
    private String adminEmail;

    @Value("${app.mail.from}")
    private String fromEmail;

    @Value("${app.mail.from-name}")
    private String fromName;

    private static final String BREVO_SEND_URL = "https://api.brevo.com/v3/smtp/email";

    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .build();

    private final ObjectMapper objectMapper = new ObjectMapper();

    // ─────────────────────────────────────────────────────────────────────────
    // PRIVATE HELPERS
    // ─────────────────────────────────────────────────────────────────────────

    private String wrapInTemplate(String bodyContent) {
        return "<!DOCTYPE html><html lang='en'>" +
            "<head><meta charset='UTF-8'/>" +
            "<meta name='viewport' content='width=device-width,initial-scale=1'/>" +
            "</head>" +
            "<body style='margin:0;padding:0;background:#fdf6ee;" +
            "font-family:Georgia,serif;'>" +

            "<table width='100%' cellpadding='0' cellspacing='0'" +
            " style='background:#fdf6ee;padding:36px 16px;'>" +
            "<tr><td align='center'>" +
            "<table width='600' cellpadding='0' cellspacing='0'" +
            " style='max-width:600px;width:100%;border-radius:16px;" +
            " box-shadow:0 4px 24px rgba(0,0,0,0.09);overflow:hidden;'>" +

            "<tr><td style='background:linear-gradient(135deg,#bf360c,#e65c00,#f9a825);" +
            " padding:34px 40px;text-align:center;'>" +
            "<div style='font-size:50px;margin-bottom:10px;'>&#x1F64F;</div>" +
            "<h1 style='margin:0;color:#fff;font-size:27px;font-weight:700;" +
            " letter-spacing:2px;font-family:Georgia,serif;'>" +
            "Hanuman Sangam</h1>" +
            "<p style='margin:7px 0 0;color:rgba(255,255,255,0.88);font-size:12px;" +
            " letter-spacing:3px;text-transform:uppercase;" +
            " font-family:Arial,sans-serif;'>&#2404; Jay Shri Ram &#2404;</p>" +
            "</td></tr>" +

            "<tr><td style='background:#ffffff;padding:38px 40px;'>" +
            bodyContent +
            "</td></tr>" +

            "<tr><td style='background:#fff8f0;padding:16px;text-align:center;" +
            " border-top:1px solid #f0e0cc;border-bottom:1px solid #f0e0cc;'>" +
            "<span style='color:#e65c00;font-size:18px;letter-spacing:8px;'>" +
            "~ ~ ~</span>" +
            "</td></tr>" +

            "<tr><td style='background:#1a0a00;padding:26px 40px;text-align:center;'>" +
            "<p style='margin:0 0 5px;color:#f9a825;font-size:12px;font-weight:700;" +
            " letter-spacing:2px;font-family:Arial,sans-serif;'>HANUMAN SANGAM</p>" +
            "<p style='margin:0 0 4px;color:rgba(255,255,255,0.5);font-size:11px;" +
            " font-family:Arial,sans-serif;'>" +
            "This is an automated message — please do not reply.</p>" +
            "<p style='margin:0;font-size:11px;font-family:Arial,sans-serif;'>" +
            "<a href='mailto:hanumansangamu@gmail.com'" +
            " style='color:#f9a825;text-decoration:none;'>" +
            "hanumansangamu@gmail.com</a></p>" +
            "</td></tr>" +

            "</table></td></tr></table>" +
            "</body></html>";
    }

    private void send(String to, String name, String subject, String html) {
        try {
            Map<String, Object> payload = Map.of(
                "sender",      Map.of("name", fromName, "email", fromEmail),
                "to",          List.of(Map.of(
                                   "email", to,
                                   "name",  name != null ? name : to)),
                "subject",     subject,
                "htmlContent", wrapInTemplate(html)
            );

            String json = objectMapper.writeValueAsString(payload);

            HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(BREVO_SEND_URL))
                .timeout(Duration.ofSeconds(15))
                .header("Accept",       "application/json")
                .header("Content-Type", "application/json")
                .header("api-key",      apiKey)
                .POST(HttpRequest.BodyPublishers.ofString(json))
                .build();

            HttpResponse<String> response =
                httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                throw new RuntimeException(
                    "Brevo API error " + response.statusCode() + ": " + response.body());
            }

        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            throw new RuntimeException("Email send failed: " + e.getMessage(), e);
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // 1. OTP VERIFICATION
    // ─────────────────────────────────────────────────────────────────────────

    public void sendOtp(String toEmail, String otp) {
        String html =
            "<h2 style='margin:0 0 4px;color:#e65c00;font-size:21px;" +
            " font-family:Georgia,serif;'>Email Verification</h2>" +
            "<p style='margin:0 0 26px;color:#bbb;font-size:11px;letter-spacing:1px;" +
            " text-transform:uppercase;font-family:Arial,sans-serif;" +
            " padding-bottom:18px;border-bottom:2px solid #f0e0cc;'>" +
            "One-Time Password for Hanuman Sangam Registration</p>" +

            "<p style='margin:0 0 14px;color:#333;font-size:15px;" +
            " line-height:1.8;'>Dear Member,</p>" +
            "<p style='margin:0 0 26px;color:#555;font-size:15px;" +
            " line-height:1.8;font-family:Arial,sans-serif;'>" +
            "Thank you for joining <strong style='color:#e65c00;'>Hanuman Sangam</strong>! " +
            "Use the OTP below to verify your email and complete your registration.</p>" +

            "<div style='background:linear-gradient(135deg,#fff8f0,#fff3e0);" +
            " border:2px dashed #e65c00;border-radius:14px;" +
            " padding:30px 20px;text-align:center;margin:0 0 26px;'>" +
            "<p style='margin:0 0 10px;color:#bbb;font-size:11px;" +
            " letter-spacing:3px;text-transform:uppercase;" +
            " font-family:Arial,sans-serif;'>Your One-Time Password</p>" +
            "<div style='font-size:44px;font-weight:700;color:#e65c00;" +
            " letter-spacing:14px;font-family:\"Courier New\",monospace;" +
            " margin:6px 0;'>" + otp + "</div>" +
            "<p style='margin:10px 0 0;color:#999;font-size:12px;" +
            " font-family:Arial,sans-serif;'>" +
            "Valid for <strong>5 minutes</strong> only — do not share</p>" +
            "</div>" +

            "<div style='background:#fff8e1;border-left:4px solid #f9a825;" +
            " border-radius:0 10px 10px 0;padding:14px 18px;margin:0 0 22px;'>" +
            "<p style='margin:0;color:#7a5c00;font-size:13px;" +
            " line-height:1.7;font-family:Arial,sans-serif;'>" +
            "<strong>Security Notice:</strong> Hanuman Sangam will " +
            "<strong>never</strong> ask for your OTP over phone or email. " +
            "Do not share it with anyone.</p>" +
            "</div>" +

            "<p style='margin:0 0 20px;color:#888;font-size:13px;" +
            " font-family:Arial,sans-serif;line-height:1.7;'>" +
            "If you did not request this OTP, please ignore this email safely.</p>" +

            "<p style='margin:0 0 2px;color:#e65c00;font-size:16px;" +
            " font-weight:600;'>Jai Bajrang Bali! &#x1F64F;</p>" +
            "<p style='margin:0;color:#aaa;font-size:12px;" +
            " font-family:Arial,sans-serif;'>Hanuman Sangam Team</p>";

        // ── FIX (Bug 4): OTP removed from subject line — no longer visible in
        // lock screens, notification banners, or email list previews ──
        send(toEmail, null,
            "Hanuman Sangam — Email Verification Code", html);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // 2. MEMBERSHIP APPROVED
    // ─────────────────────────────────────────────────────────────────────────

    public void sendApprovalNotification(String toEmail, String memberName) {
        String html =
            "<h2 style='margin:0 0 4px;color:#2e7d32;font-size:21px;" +
            " font-family:Georgia,serif;'>Membership Approved! &#x2705;</h2>" +
            "<p style='margin:0 0 26px;color:#bbb;font-size:11px;letter-spacing:1px;" +
            " text-transform:uppercase;font-family:Arial,sans-serif;" +
            " padding-bottom:18px;border-bottom:2px solid #f0e0cc;'>" +
            "Welcome to the Hanuman Sangam Family</p>" +

            "<p style='margin:0 0 14px;color:#333;font-size:15px;line-height:1.8;'>" +
            "Dear <strong>" + memberName + "</strong>,</p>" +
            "<p style='margin:0 0 24px;color:#555;font-size:15px;" +
            " line-height:1.8;font-family:Arial,sans-serif;'>" +
            "We are thrilled to inform you that your membership request for " +
            "<strong style='color:#e65c00;'>Hanuman Sangam</strong> has been " +
            "<strong style='color:#2e7d32;'>APPROVED</strong> by our admin. " +
            "You are now an official member of our sacred community!</p>" +

            "<div style='background:linear-gradient(135deg,#e8f5e9,#c8e6c9);" +
            " border:1px solid #a5d6a7;border-radius:14px;" +
            " padding:26px 20px;text-align:center;margin:0 0 26px;'>" +
            "<h3 style='margin:0 0 8px;color:#1b5e20;font-size:19px;" +
            " font-family:Georgia,serif;'>You are officially a Member!</h3>" +
            "<p style='margin:0;color:#388e3c;font-size:14px;" +
            " font-family:Arial,sans-serif;'>" +
            "Your account is now active and ready to use.</p>" +
            "</div>" +

            "<div style='background:#fafafa;border:1px solid #f0e0cc;" +
            " border-radius:12px;padding:22px;margin:0 0 22px;'>" +
            "<h4 style='margin:0 0 14px;color:#e65c00;font-size:11px;" +
            " letter-spacing:2px;text-transform:uppercase;" +
            " font-family:Arial,sans-serif;'>What Can You Do Now?</h4>" +
            "<table cellpadding='0' cellspacing='0' width='100%'>" +
            row("&#x1F510;", "Login with your <strong>mobile number</strong> and password") +
            row("&#x1F4CB;", "Access your personal <strong>member dashboard</strong>") +
            row("&#x1F514;", "Receive <strong>community announcements</strong> and updates") +
            row("&#x1F64F;", "Participate in <strong>Sangam events</strong> and activities") +
            "</table></div>" +

            "<p style='margin:0 0 2px;color:#e65c00;font-size:16px;" +
            " font-weight:600;'>Jai Bajrang Bali! &#x1F64F;</p>" +
            "<p style='margin:0;color:#aaa;font-size:12px;" +
            " font-family:Arial,sans-serif;'>Hanuman Sangam Team</p>";

        send(toEmail, memberName,
            "Membership Approved — Welcome to Hanuman Sangam!", html);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // 3. MEMBERSHIP REJECTED
    // ─────────────────────────────────────────────────────────────────────────

    public void sendRejectionNotification(String toEmail, String memberName) {
        String html =
            "<h2 style='margin:0 0 4px;color:#c62828;font-size:21px;" +
            " font-family:Georgia,serif;'>Membership Status Update</h2>" +
            "<p style='margin:0 0 26px;color:#bbb;font-size:11px;letter-spacing:1px;" +
            " text-transform:uppercase;font-family:Arial,sans-serif;" +
            " padding-bottom:18px;border-bottom:2px solid #f0e0cc;'>" +
            "Regarding your Hanuman Sangam registration</p>" +

            "<p style='margin:0 0 14px;color:#333;font-size:15px;line-height:1.8;'>" +
            "Dear <strong>" + memberName + "</strong>,</p>" +
            "<p style='margin:0 0 24px;color:#555;font-size:15px;" +
            " line-height:1.8;font-family:Arial,sans-serif;'>" +
            "Thank you for your interest in joining " +
            "<strong style='color:#e65c00;'>Hanuman Sangam</strong>. " +
            "After careful review, we regret to inform you that your membership " +
            "request has <strong style='color:#c62828;'>not been approved</strong> " +
            "at this time.</p>" +

            "<div style='background:#ffebee;border-left:5px solid #c62828;" +
            " border-radius:0 12px 12px 0;padding:20px;margin:0 0 26px;'>" +
            "<p style='margin:0 0 6px;color:#b71c1c;font-size:14px;" +
            " font-weight:700;font-family:Arial,sans-serif;'>" +
            "Status: Not Approved</p>" +
            "<p style='margin:0;color:#c62828;font-size:14px;" +
            " line-height:1.7;font-family:Arial,sans-serif;'>" +
            "Your registration did not meet our current membership criteria. " +
            "Please contact the admin for further clarification.</p>" +
            "</div>" +

            "<div style='background:#fafafa;border:1px solid #f0e0cc;" +
            " border-radius:12px;padding:20px;margin:0 0 22px;'>" +
            "<h4 style='margin:0 0 10px;color:#e65c00;font-size:11px;" +
            " letter-spacing:2px;text-transform:uppercase;" +
            " font-family:Arial,sans-serif;'>Need Help?</h4>" +
            "<p style='margin:0;color:#444;font-size:14px;" +
            " font-family:Arial,sans-serif;'>" +
            "Email us at: <a href='mailto:hanumansangamu@gmail.com'" +
            " style='color:#e65c00;font-weight:600;" +
            " text-decoration:none;'>hanumansangamu@gmail.com</a></p>" +
            "</div>" +

            "<p style='margin:0 0 18px;color:#888;font-size:13px;" +
            " font-family:Arial,sans-serif;line-height:1.7;'>" +
            "We appreciate your interest. You are always welcome to reapply.</p>" +

            "<p style='margin:0 0 2px;color:#e65c00;font-size:16px;" +
            " font-weight:600;'>Jai Bajrang Bali! &#x1F64F;</p>" +
            "<p style='margin:0;color:#aaa;font-size:12px;" +
            " font-family:Arial,sans-serif;'>Hanuman Sangam Team</p>";

        send(toEmail, memberName,
            "Membership Status Update — Hanuman Sangam", html);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // 4. ANNOUNCEMENT
    // ─────────────────────────────────────────────────────────────────────────

    public void sendAnnouncementEmail(String toEmail, String memberName,
                                      String title, String announcementBody) {
        String html =
            "<h2 style='margin:0 0 4px;color:#e65c00;font-size:21px;" +
            " font-family:Georgia,serif;'>&#x1F4E2; " + title + "</h2>" +
            "<p style='margin:0 0 26px;color:#bbb;font-size:11px;letter-spacing:1px;" +
            " text-transform:uppercase;font-family:Arial,sans-serif;" +
            " padding-bottom:18px;border-bottom:2px solid #f0e0cc;'>" +
            "Important update from Hanuman Sangam</p>" +

            "<p style='margin:0 0 24px;color:#333;font-size:15px;" +
            " line-height:1.8;'>Dear <strong>" + memberName + "</strong>,</p>" +

            "<div style='background:linear-gradient(135deg,#fff8f0,#fff3e0);" +
            " border:1px solid #ffccbc;border-radius:14px;" +
            " padding:28px;margin:0 0 26px;'>" +
            "<div style='color:#333;font-size:15px;line-height:1.9;" +
            " font-family:Arial,sans-serif;white-space:pre-line;'>" +
            announcementBody + "</div>" +
            "</div>" +

            "<p style='margin:0 0 2px;color:#e65c00;font-size:16px;" +
            " font-weight:600;'>Jai Bajrang Bali! &#x1F64F;</p>" +
            "<p style='margin:0;color:#aaa;font-size:12px;" +
            " font-family:Arial,sans-serif;'>Hanuman Sangam Team</p>";

        send(toEmail, memberName, title + " — Hanuman Sangam", html);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // 5. CONTACT MESSAGE → ADMIN
    // ─────────────────────────────────────────────────────────────────────────

    public void sendContactMessageToAdmin(String memberName, String memberEmail,
                                          String memberPhone, String message) {
        String safeEmail = isBlank(memberEmail) ? "Not provided" : memberEmail;
        String safePhone = isBlank(memberPhone) ? "Not provided" : memberPhone;
        String replyTo   = isBlank(memberEmail) ? adminEmail  : memberEmail;

        String html =
            "<h2 style='margin:0 0 4px;color:#e65c00;font-size:21px;" +
            " font-family:Georgia,serif;'>&#x1F4E9; New Contact Message</h2>" +
            "<p style='margin:0 0 26px;color:#bbb;font-size:11px;letter-spacing:1px;" +
            " text-transform:uppercase;font-family:Arial,sans-serif;" +
            " padding-bottom:18px;border-bottom:2px solid #f0e0cc;'>" +
            "Received via Hanuman Sangam Member Portal</p>" +

            "<div style='background:#fafafa;border:1px solid #f0e0cc;" +
            " border-radius:12px;padding:20px;margin:0 0 22px;'>" +
            "<h4 style='margin:0 0 14px;color:#e65c00;font-size:11px;" +
            " letter-spacing:2px;text-transform:uppercase;" +
            " font-family:Arial,sans-serif;'>Member Details</h4>" +
            "<table cellpadding='0' cellspacing='8' width='100%'>" +
            infoRow("Name",  memberName) +
            infoRow("Email", safeEmail) +
            infoRow("Phone", safePhone) +
            "</table></div>" +

            "<div style='background:#fff8f0;border-left:5px solid #e65c00;" +
            " border-radius:0 12px 12px 0;padding:20px;margin:0 0 22px;'>" +
            "<h4 style='margin:0 0 12px;color:#e65c00;font-size:11px;" +
            " letter-spacing:2px;text-transform:uppercase;" +
            " font-family:Arial,sans-serif;'>Message</h4>" +
            "<p style='margin:0;color:#333;font-size:15px;" +
            " line-height:1.9;font-family:Arial,sans-serif;" +
            " white-space:pre-line;'>" + message + "</p>" +
            "</div>" +

            "<div style='background:#e8f5e9;border:1px solid #c8e6c9;" +
            " border-radius:10px;padding:14px 18px;'>" +
            "<p style='margin:0;color:#2e7d32;font-size:13px;" +
            " font-family:Arial,sans-serif;'>" +
            "To reply, email: <a href='mailto:" + replyTo + "'" +
            " style='color:#1b5e20;font-weight:600;text-decoration:none;'>" +
            safeEmail + "</a></p>" +
            "</div>";

        try {
            Map<String, Object> payload = Map.of(
                "sender",      Map.of("name", fromName, "email", fromEmail),
                "to",          List.of(Map.of("email", adminEmail, "name", "Admin")),
                "replyTo",     Map.of("email", replyTo),
                "subject",     "Contact from " + memberName + " — Hanuman Sangam",
                "htmlContent", wrapInTemplate(html)
            );

            String json = objectMapper.writeValueAsString(payload);

            HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(BREVO_SEND_URL))
                .timeout(Duration.ofSeconds(15))
                .header("Accept",       "application/json")
                .header("Content-Type", "application/json")
                .header("api-key",      apiKey)
                .POST(HttpRequest.BodyPublishers.ofString(json))
                .build();

            HttpResponse<String> response =
                httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                throw new RuntimeException(
                    "Brevo API error " + response.statusCode() + ": " + response.body());
            }

        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            throw new RuntimeException("Failed to send contact email: " + e.getMessage(), e);
        }
    }

    private String row(String icon, String text) {
        return "<tr><td style='padding:7px 0;color:#444;font-size:14px;" +
               " font-family:Arial,sans-serif;line-height:1.6;'>" +
               icon + "&nbsp; " + text + "</td></tr>";
    }

    private String infoRow(String label, String value) {
        return "<tr>" +
               "<td style='color:#999;font-size:13px;font-family:Arial,sans-serif;" +
               " width:80px;padding:4px 0;'>" + label + "</td>" +
               "<td style='color:#222;font-size:14px;font-weight:600;" +
               " font-family:Arial,sans-serif;padding:4px 0;'>" + value + "</td>" +
               "</tr>";
    }

    private boolean isBlank(String s) {
        return s == null || s.isBlank();
    }
}
