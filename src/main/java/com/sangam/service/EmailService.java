package com.sangam.service;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

@Service
public class EmailService {

    private final JavaMailSender mailSender;

    @Value("${app.admin.email}")
    private String adminEmail;

    @Value("${app.mail.from}")
    private String fromEmail;

    @Value("${app.mail.from-name}")
    private String fromName;

    public EmailService(JavaMailSender mailSender) {
        this.mailSender = mailSender;
    }

    // ─────────────────────────────────────────────────────────────────────────
    // PRIVATE HELPERS
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Wraps any email body content inside the shared
     * Hanuman Sangam branded HTML template.
     */
    private String wrapInTemplate(String bodyContent) {
        return "<!DOCTYPE html>" +
            "<html lang='en'>" +
            "<head>" +
            "  <meta charset='UTF-8'/>" +
            "  <meta name='viewport' content='width=device-width,initial-scale=1'/>" +
            "  <title>Hanuman Sangam</title>" +
            "</head>" +
            "<body style='margin:0;padding:0;background:#fdf6ee;font-family:Georgia,serif;'>" +
            "<table width='100%' cellpadding='0' cellspacing='0'" +
            "  style='background:#fdf6ee;padding:40px 16px;'>" +
            "<tr><td align='center'>" +
            "<table width='600' cellpadding='0' cellspacing='0'" +
            "  style='max-width:600px;width:100%;border-radius:16px;" +
            "  box-shadow:0 4px 24px rgba(0,0,0,0.10);overflow:hidden;'>" +

            // ── HEADER ────────────────────────────────────────────────────
            "<tr><td style='" +
            "  background:linear-gradient(135deg,#bf360c 0%,#e65c00 50%,#f9a825 100%);" +
            "  padding:36px 40px;text-align:center;'>" +
            "  <div style='font-size:52px;margin-bottom:10px;'>🙏</div>" +
            "  <h1 style='margin:0;color:#ffffff;font-size:28px;font-weight:700;" +
            "    letter-spacing:2px;font-family:Georgia,serif;" +
            "    text-shadow:0 1px 4px rgba(0,0,0,0.2);'>Hanuman Sangam</h1>" +
            "  <p style='margin:8px 0 0;color:rgba(255,255,255,0.90);font-size:13px;" +
            "    letter-spacing:3px;text-transform:uppercase;font-family:Arial,sans-serif;'>" +
            "    &#2404; जय श्री राम &#2404;</p>" +
            "</td></tr>" +

            // ── BODY ──────────────────────────────────────────────────────
            "<tr><td style='background:#ffffff;padding:40px;'>" +
            bodyContent +
            "</td></tr>" +

            // ── DIVIDER ───────────────────────────────────────────────────
            "<tr><td style='background:#fff8f0;padding:20px 40px;text-align:center;" +
            "  border-top:1px solid #f0e0cc;border-bottom:1px solid #f0e0cc;'>" +
            "  <p style='margin:0;color:#e65c00;font-size:22px;letter-spacing:6px;'>&#x1F549; &#x1F64F; &#x1F549;</p>" +
            "</td></tr>" +

            // ── FOOTER ────────────────────────────────────────────────────
            "<tr><td style='background:#1a0a00;padding:28px 40px;text-align:center;'>" +
            "  <p style='margin:0 0 6px;color:#f9a825;font-size:13px;font-weight:700;" +
            "    letter-spacing:2px;font-family:Arial,sans-serif;'>HANUMAN SANGAM</p>" +
            "  <p style='margin:0 0 12px;color:rgba(255,255,255,0.55);font-size:12px;" +
            "    line-height:1.8;font-family:Arial,sans-serif;'>" +
            "    This is an automated message — please do not reply to this email.<br/>" +
            "    For support: <a href='mailto:hanumansangamu@gmail.com'" +
            "      style='color:#f9a825;text-decoration:none;'>hanumansangamu@gmail.com</a>" +
            "  </p>" +
            "</td></tr>" +

            "</table>" +
            "</td></tr>" +
            "</table>" +
            "</body></html>";
    }

    /**
     * Core send helper — used by all public methods.
     */
    private void sendHtmlEmail(String to, String subject, String htmlBody) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            helper.setFrom(fromEmail, fromName);
            helper.setTo(to);
            helper.setSubject(subject);
            helper.setText(wrapInTemplate(htmlBody), true);
            mailSender.send(message);
        } catch (MessagingException | java.io.UnsupportedEncodingException e) {
            throw new RuntimeException("Failed to send email to " + to + ": " + e.getMessage(), e);
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // 1. OTP VERIFICATION
    // ─────────────────────────────────────────────────────────────────────────

    public void sendOtp(String toEmail, String otp) {
        String body =
            "<h2 style='margin:0 0 4px;color:#e65c00;font-size:22px;" +
            "  font-family:Georgia,serif;'>Email Verification</h2>" +
            "<p style='margin:0 0 24px;color:#aaa;font-size:12px;" +
            "  letter-spacing:1px;text-transform:uppercase;font-family:Arial,sans-serif;" +
            "  border-bottom:2px solid #f0e0cc;padding-bottom:18px;'>" +
            "  One-Time Password for Registration</p>" +

            "<p style='margin:0 0 16px;color:#333;font-size:15px;line-height:1.8;'>" +
            "  Dear Member,</p>" +
            "<p style='margin:0 0 28px;color:#555;font-size:15px;line-height:1.8;" +
            "  font-family:Arial,sans-serif;'>" +
            "  Thank you for joining <strong style='color:#e65c00;'>Hanuman Sangam</strong>! " +
            "  Please use the OTP below to verify your email address " +
            "  and complete your registration.</p>" +

            // OTP box
            "<div style='background:linear-gradient(135deg,#fff8f0,#fff3e0);" +
            "  border:2px dashed #e65c00;border-radius:14px;" +
            "  padding:32px 20px;text-align:center;margin:0 0 28px;'>" +
            "  <p style='margin:0 0 10px;color:#aaa;font-size:11px;" +
            "    letter-spacing:3px;text-transform:uppercase;" +
            "    font-family:Arial,sans-serif;'>Your One-Time Password</p>" +
            "  <div style='font-size:46px;font-weight:700;color:#e65c00;" +
            "    letter-spacing:16px;font-family:\"Courier New\",monospace;" +
            "    text-shadow:0 2px 8px rgba(230,92,0,0.15);margin:8px 0;'>" +
            otp +
            "  </div>" +
            "  <p style='margin:12px 0 0;color:#999;font-size:12px;" +
            "    font-family:Arial,sans-serif;'>" +
            "    Valid for <strong>5 minutes</strong> only</p>" +
            "</div>" +

            // Warning
            "<div style='background:#fff8e1;border-left:4px solid #f9a825;" +
            "  border-radius:0 10px 10px 0;padding:16px 18px;margin:0 0 24px;'>" +
            "  <p style='margin:0;color:#7a5c00;font-size:13px;" +
            "    line-height:1.7;font-family:Arial,sans-serif;'>" +
            "    <strong>Security Notice:</strong> Do not share this OTP with anyone. " +
            "    Hanuman Sangam will <strong>never</strong> ask for your OTP " +
            "    over phone or email.</p>" +
            "</div>" +

            "<p style='margin:0 0 20px;color:#888;font-size:13px;" +
            "  font-family:Arial,sans-serif;line-height:1.7;'>" +
            "  If you did not request this OTP, please ignore this email.</p>" +

            "<p style='margin:0 0 4px;color:#e65c00;font-size:17px;font-weight:600;'>" +
            "  Jai Bajrang Bali!</p>" +
            "<p style='margin:0;color:#aaa;font-size:13px;" +
            "  font-family:Arial,sans-serif;'>Hanuman Sangam Team</p>";

        sendHtmlEmail(toEmail,
            "Your OTP: " + otp + " — Hanuman Sangam Verification",
            body);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // 2. MEMBERSHIP APPROVED
    // ─────────────────────────────────────────────────────────────────────────

    public void sendApprovalNotification(String toEmail, String memberName) {
        String body =
            "<h2 style='margin:0 0 4px;color:#2e7d32;font-size:22px;" +
            "  font-family:Georgia,serif;'>Membership Approved!</h2>" +
            "<p style='margin:0 0 24px;color:#aaa;font-size:12px;" +
            "  letter-spacing:1px;text-transform:uppercase;font-family:Arial,sans-serif;" +
            "  border-bottom:2px solid #f0e0cc;padding-bottom:18px;'>" +
            "  Welcome to the Hanuman Sangam Family</p>" +

            "<p style='margin:0 0 16px;color:#333;font-size:15px;line-height:1.8;'>" +
            "  Dear <strong>" + memberName + "</strong>,</p>" +

            "<p style='margin:0 0 24px;color:#555;font-size:15px;" +
            "  line-height:1.8;font-family:Arial,sans-serif;'>" +
            "  We are delighted to inform you that your membership request for " +
            "  <strong style='color:#e65c00;'>Hanuman Sangam</strong> has been " +
            "  <strong style='color:#2e7d32;'>approved</strong> by our admin. " +
            "  You are now an official member of our sacred community!</p>" +

            // Green success banner
            "<div style='background:linear-gradient(135deg,#e8f5e9,#c8e6c9);" +
            "  border:1px solid #a5d6a7;border-radius:14px;" +
            "  padding:28px 20px;text-align:center;margin:0 0 28px;'>" +
            "  <h3 style='margin:0 0 8px;color:#1b5e20;font-size:20px;" +
            "    font-family:Georgia,serif;'>You are officially a Member!</h3>" +
            "  <p style='margin:0;color:#388e3c;font-size:14px;" +
            "    font-family:Arial,sans-serif;'>" +
            "    Your account is now active and ready to use.</p>" +
            "</div>" +

            // What's next
            "<div style='background:#fafafa;border:1px solid #f0e0cc;" +
            "  border-radius:12px;padding:24px;margin:0 0 24px;'>" +
            "  <h4 style='margin:0 0 16px;color:#e65c00;font-size:12px;" +
            "    letter-spacing:2px;text-transform:uppercase;" +
            "    font-family:Arial,sans-serif;'>What Can You Do Now?</h4>" +
            "  <table cellpadding='0' cellspacing='0' width='100%'>" +
            "  <tr><td style='padding:8px 0;font-size:16px;width:28px;'>&#x1F510;</td>" +
            "    <td style='padding:8px 0;color:#444;font-size:14px;" +
            "      font-family:Arial,sans-serif;line-height:1.6;'>" +
            "      Login using your <strong>mobile number</strong> and password</td></tr>" +
            "  <tr><td style='padding:8px 0;font-size:16px;'>&#x1F4CB;</td>" +
            "    <td style='padding:8px 0;color:#444;font-size:14px;" +
            "      font-family:Arial,sans-serif;line-height:1.6;'>" +
            "      Access your personal <strong>member dashboard</strong></td></tr>" +
            "  <tr><td style='padding:8px 0;font-size:16px;'>&#x1F514;</td>" +
            "    <td style='padding:8px 0;color:#444;font-size:14px;" +
            "      font-family:Arial,sans-serif;line-height:1.6;'>" +
            "      Receive <strong>community announcements</strong> and updates</td></tr>" +
            "  <tr><td style='padding:8px 0;font-size:16px;'>&#x1F64F;</td>" +
            "    <td style='padding:8px 0;color:#444;font-size:14px;" +
            "      font-family:Arial,sans-serif;line-height:1.6;'>" +
            "      Participate in <strong>Sangam events</strong> and activities</td></tr>" +
            "  </table>" +
            "</div>" +

            "<p style='margin:0 0 4px;color:#e65c00;font-size:17px;font-weight:600;'>" +
            "  Jai Bajrang Bali!</p>" +
            "<p style='margin:0;color:#aaa;font-size:13px;" +
            "  font-family:Arial,sans-serif;'>Hanuman Sangam Team</p>";

        sendHtmlEmail(toEmail,
            "Membership Approved — Welcome to Hanuman Sangam!",
            body);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // 3. MEMBERSHIP REJECTED
    // ─────────────────────────────────────────────────────────────────────────

    public void sendRejectionNotification(String toEmail, String memberName) {
        String body =
            "<h2 style='margin:0 0 4px;color:#c62828;font-size:22px;" +
            "  font-family:Georgia,serif;'>Membership Status Update</h2>" +
            "<p style='margin:0 0 24px;color:#aaa;font-size:12px;" +
            "  letter-spacing:1px;text-transform:uppercase;font-family:Arial,sans-serif;" +
            "  border-bottom:2px solid #f0e0cc;padding-bottom:18px;'>" +
            "  Regarding your Hanuman Sangam registration</p>" +

            "<p style='margin:0 0 16px;color:#333;font-size:15px;line-height:1.8;'>" +
            "  Dear <strong>" + memberName + "</strong>,</p>" +

            "<p style='margin:0 0 24px;color:#555;font-size:15px;" +
            "  line-height:1.8;font-family:Arial,sans-serif;'>" +
            "  Thank you for your interest in joining " +
            "  <strong style='color:#e65c00;'>Hanuman Sangam</strong>. " +
            "  After careful review, we regret to inform you that your " +
            "  membership request has <strong style='color:#c62828;'>not been approved</strong> " +
            "  at this time.</p>" +

            // Red status box
            "<div style='background:#ffebee;border:1px solid #ffcdd2;" +
            "  border-left:5px solid #c62828;border-radius:0 12px 12px 0;" +
            "  padding:22px;margin:0 0 28px;'>" +
            "  <p style='margin:0 0 8px;color:#b71c1c;font-size:15px;" +
            "    font-weight:700;font-family:Arial,sans-serif;'>Status: Not Approved</p>" +
            "  <p style='margin:0;color:#c62828;font-size:14px;" +
            "    line-height:1.7;font-family:Arial,sans-serif;'>" +
            "    Your registration did not meet our current membership criteria. " +
            "    Please contact the admin for further clarification.</p>" +
            "</div>" +

            // Contact
            "<div style='background:#fafafa;border:1px solid #f0e0cc;" +
            "  border-radius:12px;padding:24px;margin:0 0 24px;'>" +
            "  <h4 style='margin:0 0 14px;color:#e65c00;font-size:12px;" +
            "    letter-spacing:2px;text-transform:uppercase;" +
            "    font-family:Arial,sans-serif;'>Need Help or Clarification?</h4>" +
            "  <p style='margin:0 0 10px;color:#555;font-size:14px;" +
            "    line-height:1.7;font-family:Arial,sans-serif;'>" +
            "    If you believe this is an error or need more information, " +
            "    please contact us — we are happy to help.</p>" +
            "  <p style='margin:0;color:#444;font-size:14px;" +
            "    font-family:Arial,sans-serif;'>" +
            "    Email: <a href='mailto:hanumansangamu@gmail.com'" +
            "      style='color:#e65c00;font-weight:600;" +
            "      text-decoration:none;'>hanumansangamu@gmail.com</a></p>" +
            "</div>" +

            "<p style='margin:0 0 20px;color:#888;font-size:14px;" +
            "  line-height:1.7;font-family:Arial,sans-serif;'>" +
            "  We appreciate your interest and hope to serve you better in the future. " +
            "  You are always welcome to reapply.</p>" +

            "<p style='margin:0 0 4px;color:#e65c00;font-size:17px;font-weight:600;'>" +
            "  Jai Bajrang Bali!</p>" +
            "<p style='margin:0;color:#aaa;font-size:13px;" +
            "  font-family:Arial,sans-serif;'>Hanuman Sangam Team</p>";

        sendHtmlEmail(toEmail,
            "Membership Status Update — Hanuman Sangam",
            body);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // 4. ANNOUNCEMENT
    // ─────────────────────────────────────────────────────────────────────────

    public void sendAnnouncementEmail(String toEmail, String memberName,
                                      String title, String announcementBody) {
        String body =
            "<h2 style='margin:0 0 4px;color:#e65c00;font-size:22px;" +
            "  font-family:Georgia,serif;'>Announcement: " + title + "</h2>" +
            "<p style='margin:0 0 24px;color:#aaa;font-size:12px;" +
            "  letter-spacing:1px;text-transform:uppercase;font-family:Arial,sans-serif;" +
            "  border-bottom:2px solid #f0e0cc;padding-bottom:18px;'>" +
            "  Important announcement from Hanuman Sangam</p>" +

            "<p style='margin:0 0 24px;color:#333;font-size:15px;line-height:1.8;'>" +
            "  Dear <strong>" + memberName + "</strong>,</p>" +

            "<div style='background:linear-gradient(135deg,#fff8f0,#fff3e0);" +
            "  border:1px solid #ffccbc;border-radius:14px;" +
            "  padding:30px;margin:0 0 28px;'>" +
            "  <div style='color:#333;font-size:15px;line-height:1.9;" +
            "    font-family:Arial,sans-serif;white-space:pre-line;'>" +
            announcementBody +
            "  </div>" +
            "</div>" +

            "<p style='margin:0 0 4px;color:#e65c00;font-size:17px;font-weight:600;'>" +
            "  Jai Bajrang Bali!</p>" +
            "<p style='margin:0;color:#aaa;font-size:13px;" +
            "  font-family:Arial,sans-serif;'>Hanuman Sangam Team</p>";

        sendHtmlEmail(toEmail, title + " — Hanuman Sangam", body);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // 5. CONTACT MESSAGE → ADMIN
    // ─────────────────────────────────────────────────────────────────────────

    public void sendContactMessageToAdmin(String memberName, String memberEmail,
                                          String memberPhone, String message) {
        String safeEmail = (memberEmail == null || memberEmail.isBlank())
            ? "Not provided" : memberEmail;
        String safePhone = (memberPhone == null || memberPhone.isBlank())
            ? "Not provided" : memberPhone;

        String body =
            "<h2 style='margin:0 0 4px;color:#e65c00;font-size:22px;" +
            "  font-family:Georgia,serif;'>New Contact Message</h2>" +
            "<p style='margin:0 0 24px;color:#aaa;font-size:12px;" +
            "  letter-spacing:1px;text-transform:uppercase;font-family:Arial,sans-serif;" +
            "  border-bottom:2px solid #f0e0cc;padding-bottom:18px;'>" +
            "  Received via Hanuman Sangam Member Portal</p>" +

            // Member info
            "<div style='background:#fafafa;border:1px solid #f0e0cc;" +
            "  border-radius:12px;padding:22px;margin:0 0 24px;'>" +
            "  <h4 style='margin:0 0 14px;color:#e65c00;font-size:12px;" +
            "    letter-spacing:2px;text-transform:uppercase;" +
            "    font-family:Arial,sans-serif;'>Member Details</h4>" +
            "  <table cellpadding='0' cellspacing='0' width='100%'>" +
            "  <tr>" +
            "    <td style='padding:9px 0;color:#999;font-size:13px;" +
            "      font-family:Arial,sans-serif;width:90px;'>Name</td>" +
            "    <td style='padding:9px 0;color:#222;font-size:14px;" +
            "      font-weight:600;font-family:Arial,sans-serif;'>" + memberName + "</td>" +
            "  </tr>" +
            "  <tr style='border-top:1px solid #f0e0cc;'>" +
            "    <td style='padding:9px 0;color:#999;font-size:13px;" +
            "      font-family:Arial,sans-serif;'>Email</td>" +
            "    <td style='padding:9px 0;color:#222;font-size:14px;" +
            "      font-family:Arial,sans-serif;'>" + safeEmail + "</td>" +
            "  </tr>" +
            "  <tr style='border-top:1px solid #f0e0cc;'>" +
            "    <td style='padding:9px 0;color:#999;font-size:13px;" +
            "      font-family:Arial,sans-serif;'>Phone</td>" +
            "    <td style='padding:9px 0;color:#222;font-size:14px;" +
            "      font-family:Arial,sans-serif;'>" + safePhone + "</td>" +
            "  </tr>" +
            "  </table>" +
            "</div>" +

            // Message content
            "<div style='background:#fff8f0;border:1px solid #ffccbc;" +
            "  border-left:5px solid #e65c00;border-radius:0 12px 12px 0;" +
            "  padding:22px;margin:0 0 24px;'>" +
            "  <h4 style='margin:0 0 12px;color:#e65c00;font-size:12px;" +
            "    letter-spacing:2px;text-transform:uppercase;" +
            "    font-family:Arial,sans-serif;'>Message</h4>" +
            "  <p style='margin:0;color:#333;font-size:15px;" +
            "    line-height:1.9;font-family:Arial,sans-serif;" +
            "    white-space:pre-line;'>" + message + "</p>" +
            "</div>" +

            // Reply hint
            "<div style='background:#e8f5e9;border:1px solid #c8e6c9;" +
            "  border-radius:10px;padding:14px 18px;'>" +
            "  <p style='margin:0;color:#2e7d32;font-size:13px;" +
            "    font-family:Arial,sans-serif;'>" +
            "    To reply, email: " +
            "    <a href='mailto:" + safeEmail + "'" +
            "      style='color:#1b5e20;font-weight:600;text-decoration:none;'>" +
            safeEmail + "</a>" +
            "  </p>" +
            "</div>";

        try {
            MimeMessage mimeMessage = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(mimeMessage, true, "UTF-8");
            helper.setFrom(fromEmail, fromName);
            helper.setTo(adminEmail);
            helper.setReplyTo(memberEmail.isBlank() ? adminEmail : memberEmail);
            helper.setSubject("Contact Message from " + memberName + " — Hanuman Sangam");
            helper.setText(wrapInTemplate(body), true);
            mailSender.send(mimeMessage);
        } catch (MessagingException | java.io.UnsupportedEncodingException e) {
            throw new RuntimeException(
                "Failed to send contact email to admin: " + e.getMessage(), e);
        }
    }
}
