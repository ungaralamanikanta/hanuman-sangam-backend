package com.sangam.service;

import com.resend.Resend;
import com.resend.core.exception.ResendException;
import com.resend.services.emails.model.CreateEmailOptions;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class EmailService {

    @Value("${resend.api.key}")
    private String apiKey;

    @Value("${app.admin.email:hanumansangamu@gmail.com}")
    private String adminEmail;

    // ─── Shared HTML Helpers ────────────────────────────────────────────────

    private String wrapInTemplate(String bodyContent) {
        return "<!DOCTYPE html>" +
            "<html lang='en'><head><meta charset='UTF-8'>" +
            "<meta name='viewport' content='width=device-width,initial-scale=1'>" +
            "<title>Hanuman Sangam</title></head>" +
            "<body style='margin:0;padding:0;background:#fdf6ee;font-family:Georgia,serif;'>" +

            // Outer wrapper
            "<table width='100%' cellpadding='0' cellspacing='0' style='background:#fdf6ee;padding:40px 0;'>" +
            "<tr><td align='center'>" +
            "<table width='600' cellpadding='0' cellspacing='0' style='max-width:600px;width:100%;'>" +

            // ── Header ──
            "<tr><td style='" +
            "background:linear-gradient(135deg,#e65c00 0%,#f9a825 100%);" +
            "border-radius:16px 16px 0 0;padding:36px 40px;text-align:center;'>" +
            "<div style='font-size:48px;margin-bottom:8px;'>🙏</div>" +
            "<h1 style='margin:0;color:#fff;font-size:26px;font-weight:700;" +
            "letter-spacing:1px;font-family:Georgia,serif;'>Hanuman Sangam</h1>" +
            "<p style='margin:6px 0 0;color:rgba(255,255,255,0.88);font-size:13px;" +
            "letter-spacing:2px;text-transform:uppercase;'>जय श्री राम</p>" +
            "</td></tr>" +

            // ── Body ──
            "<tr><td style='background:#ffffff;padding:40px;border-left:1px solid #f0e0cc;" +
            "border-right:1px solid #f0e0cc;'>" +
            bodyContent +
            "</td></tr>" +

            // ── Footer ──
            "<tr><td style='background:#1a0a00;border-radius:0 0 16px 16px;padding:24px 40px;" +
            "text-align:center;'>" +
            "<p style='margin:0 0 6px;color:#f9a825;font-size:13px;font-weight:600;" +
            "letter-spacing:1px;'>HANUMAN SANGAM</p>" +
            "<p style='margin:0;color:rgba(255,255,255,0.5);font-size:11px;line-height:1.8;'>" +
            "This is an automated message. Please do not reply to this email.<br>" +
            "For support, contact us at hanumansangamu@gmail.com" +
            "</p>" +
            "<p style='margin:12px 0 0;color:rgba(255,165,0,0.6);font-size:20px;'>🕉️</p>" +
            "</td></tr>" +

            "</table></td></tr></table>" +
            "</body></html>";
    }

    private void sendEmail(String to, String subject, String htmlBody) {
        Resend resend = new Resend(apiKey);
        CreateEmailOptions params = CreateEmailOptions.builder()
                .from("Hanuman Sangam <onboarding@resend.dev>")
                .to(to)
                .replyTo(adminEmail)
                .subject(subject)
                .html(wrapInTemplate(htmlBody))
                .build();
        try {
            resend.emails().send(params);
        } catch (ResendException e) {
            throw new RuntimeException("Email sending failed: " + e.getMessage(), e);
        }
    }

    // ─── 1. OTP Verification Email ──────────────────────────────────────────

    public void sendOtp(String toEmail, String otp) {
        String body =
            "<h2 style='margin:0 0 6px;color:#e65c00;font-size:20px;'>OTP Verification</h2>" +
            "<p style='margin:0 0 24px;color:#888;font-size:13px;border-bottom:1px solid #f0e0cc;padding-bottom:20px;'>" +
            "Complete your registration with Hanuman Sangam</p>" +

            "<p style='margin:0 0 16px;color:#444;font-size:15px;line-height:1.7;'>" +
            "Dear Member,</p>" +

            "<p style='margin:0 0 24px;color:#444;font-size:15px;line-height:1.7;'>" +
            "Thank you for joining <strong>Hanuman Sangam</strong>! " +
            "Please use the OTP below to verify your email address and complete your registration.</p>" +

            // OTP Box
            "<div style='background:linear-gradient(135deg,#fff3e0,#ffe0b2);border:2px dashed #e65c00;" +
            "border-radius:12px;padding:30px;text-align:center;margin:24px 0;'>" +
            "<p style='margin:0 0 8px;color:#888;font-size:12px;letter-spacing:2px;" +
            "text-transform:uppercase;'>Your One-Time Password</p>" +
            "<div style='font-size:42px;font-weight:700;color:#e65c00;letter-spacing:14px;" +
            "font-family:\"Courier New\",monospace;margin:8px 0;'>" + otp + "</div>" +
            "<p style='margin:8px 0 0;color:#999;font-size:12px;'>⏱️ Valid for <strong>5 minutes</strong> only</p>" +
            "</div>" +

            // Warning
            "<div style='background:#fff8e1;border-left:4px solid #f9a825;padding:14px 18px;" +
            "border-radius:0 8px 8px 0;margin:20px 0;'>" +
            "<p style='margin:0;color:#7a5c00;font-size:13px;line-height:1.6;'>" +
            "⚠️ <strong>Do not share this OTP</strong> with anyone. " +
            "Hanuman Sangam will never ask for your OTP over phone or email.</p>" +
            "</div>" +

            "<p style='margin:24px 0 0;color:#444;font-size:15px;line-height:1.7;'>" +
            "If you did not request this OTP, please ignore this email.</p>" +

            "<p style='margin:20px 0 0;color:#e65c00;font-size:16px;font-weight:600;'>🙏 जय बजरंग बली!</p>" +
            "<p style='margin:4px 0 0;color:#888;font-size:13px;'>Hanuman Sangam Team</p>";

        sendEmail(toEmail, "🙏 Your OTP - Hanuman Sangam Verification", body);
    }

    // ─── 2. Approval Notification ────────────────────────────────────────────

    public void sendApprovalNotification(String toEmail, String memberName) {
        String body =
            "<h2 style='margin:0 0 6px;color:#2e7d32;font-size:20px;'>Registration Approved! ✅</h2>" +
            "<p style='margin:0 0 24px;color:#888;font-size:13px;border-bottom:1px solid #f0e0cc;padding-bottom:20px;'>" +
            "Welcome to the Hanuman Sangam family</p>" +

            "<p style='margin:0 0 16px;color:#444;font-size:15px;line-height:1.7;'>" +
            "Dear <strong>" + memberName + "</strong>,</p>" +

            "<p style='margin:0 0 20px;color:#444;font-size:15px;line-height:1.7;'>" +
            "We are delighted to inform you that your membership request for " +
            "<strong>Hanuman Sangam</strong> has been <strong style='color:#2e7d32;'>approved</strong> " +
            "by the admin. Welcome to our sacred community! 🎉</p>" +

            // Green success box
            "<div style='background:linear-gradient(135deg,#e8f5e9,#c8e6c9);border:1px solid #a5d6a7;" +
            "border-radius:12px;padding:24px;text-align:center;margin:24px 0;'>" +
            "<div style='font-size:48px;margin-bottom:12px;'>✅</div>" +
            "<h3 style='margin:0 0 8px;color:#1b5e20;font-size:18px;'>You're officially a member!</h3>" +
            "<p style='margin:0;color:#2e7d32;font-size:14px;'>Your account is now active and ready to use.</p>" +
            "</div>" +

            // What's next
            "<div style='background:#fafafa;border:1px solid #f0e0cc;border-radius:10px;padding:20px;margin:20px 0;'>" +
            "<h4 style='margin:0 0 14px;color:#e65c00;font-size:14px;letter-spacing:1px;text-transform:uppercase;'>What's Next?</h4>" +
            "<table cellpadding='0' cellspacing='0' width='100%'>" +
            "<tr><td style='padding:6px 0;color:#444;font-size:14px;'>🔐</td>" +
            "<td style='padding:6px 8px;color:#444;font-size:14px;'>Login using your mobile number and password</td></tr>" +
            "<tr><td style='padding:6px 0;color:#444;font-size:14px;'>📋</td>" +
            "<td style='padding:6px 8px;color:#444;font-size:14px;'>Access your member dashboard</td></tr>" +
            "<tr><td style='padding:6px 0;color:#444;font-size:14px;'>🔔</td>" +
            "<td style='padding:6px 8px;color:#444;font-size:14px;'>Stay updated with community announcements</td></tr>" +
            "<tr><td style='padding:6px 0;color:#444;font-size:14px;'>🙏</td>" +
            "<td style='padding:6px 8px;color:#444;font-size:14px;'>Participate in sangam events and activities</td></tr>" +
            "</table></div>" +

            "<p style='margin:20px 0 4px;color:#e65c00;font-size:16px;font-weight:600;'>🙏 जय बजरंग बली!</p>" +
            "<p style='margin:0;color:#888;font-size:13px;'>Hanuman Sangam Team</p>";

        sendEmail(toEmail, "✅ Membership Approved - Welcome to Hanuman Sangam!", body);
    }

    // ─── 3. Rejection Notification ───────────────────────────────────────────

    public void sendRejectionNotification(String toEmail, String memberName) {
        String body =
            "<h2 style='margin:0 0 6px;color:#c62828;font-size:20px;'>Registration Update</h2>" +
            "<p style='margin:0 0 24px;color:#888;font-size:13px;border-bottom:1px solid #f0e0cc;padding-bottom:20px;'>" +
            "Regarding your Hanuman Sangam membership request</p>" +

            "<p style='margin:0 0 16px;color:#444;font-size:15px;line-height:1.7;'>" +
            "Dear <strong>" + memberName + "</strong>,</p>" +

            "<p style='margin:0 0 20px;color:#444;font-size:15px;line-height:1.7;'>" +
            "Thank you for your interest in joining <strong>Hanuman Sangam</strong>. " +
            "After careful review, we regret to inform you that your membership request " +
            "has not been approved at this time.</p>" +

            // Red info box
            "<div style='background:#ffebee;border:1px solid #ffcdd2;border-left:4px solid #c62828;" +
            "border-radius:8px;padding:20px;margin:24px 0;'>" +
            "<p style='margin:0 0 8px;color:#b71c1c;font-size:14px;font-weight:600;'>❌ Status: Not Approved</p>" +
            "<p style='margin:0;color:#c62828;font-size:14px;line-height:1.6;'>" +
            "Your registration did not meet our current membership requirements. " +
            "Please contact the admin for further clarification.</p>" +
            "</div>" +

            // What to do next
            "<div style='background:#fafafa;border:1px solid #f0e0cc;border-radius:10px;padding:20px;margin:20px 0;'>" +
            "<h4 style='margin:0 0 12px;color:#e65c00;font-size:14px;letter-spacing:1px;text-transform:uppercase;'>Need Help?</h4>" +
            "<p style='margin:0 0 10px;color:#444;font-size:14px;line-height:1.6;'>" +
            "If you believe this is a mistake or need more information, please reach out to us:</p>" +
            "<p style='margin:0;color:#444;font-size:14px;'>📧 Email: <a href='mailto:hanumansangamu@gmail.com' " +
            "style='color:#e65c00;'>hanumansangamu@gmail.com</a></p>" +
            "</div>" +

            "<p style='margin:20px 0 4px;color:#444;font-size:14px;line-height:1.7;'>" +
            "We appreciate your interest and hope to welcome you in the future.</p>" +

            "<p style='margin:20px 0 4px;color:#e65c00;font-size:16px;font-weight:600;'>🙏 जय बजरंग बली!</p>" +
            "<p style='margin:0;color:#888;font-size:13px;'>Hanuman Sangam Team</p>";

        sendEmail(toEmail, "📋 Membership Status Update - Hanuman Sangam", body);
    }

    // ─── 4. Announcement Email ───────────────────────────────────────────────

    public void sendAnnouncementEmail(String toEmail, String memberName,
                                      String title, String announcementBody) {
        String body =
            "<h2 style='margin:0 0 6px;color:#e65c00;font-size:20px;'>📢 " + title + "</h2>" +
            "<p style='margin:0 0 24px;color:#888;font-size:13px;border-bottom:1px solid #f0e0cc;padding-bottom:20px;'>" +
            "Important announcement from Hanuman Sangam</p>" +

            "<p style='margin:0 0 16px;color:#444;font-size:15px;line-height:1.7;'>" +
            "Dear <strong>" + memberName + "</strong>,</p>" +

            // Announcement content box
            "<div style='background:linear-gradient(135deg,#fff3e0,#fce4ec);border:1px solid #ffccbc;" +
            "border-radius:12px;padding:28px;margin:20px 0;'>" +
            "<div style='font-size:32px;margin-bottom:12px;text-align:center;'>📢</div>" +
            "<div style='color:#444;font-size:15px;line-height:1.8;white-space:pre-line;'>" +
            announcementBody +
            "</div></div>" +

            "<p style='margin:20px 0 4px;color:#e65c00;font-size:16px;font-weight:600;'>🙏 जय बजरंग बली!</p>" +
            "<p style='margin:0;color:#888;font-size:13px;'>Hanuman Sangam Team</p>";

        sendEmail(toEmail, "📢 " + title + " - Hanuman Sangam", body);
    }

    // ─── 5. Contact Message to Admin ─────────────────────────────────────────

    public void sendContactMessageToAdmin(String memberName, String memberEmail,
                                          String memberPhone, String message) {
        String emailDisplay = (memberEmail == null || memberEmail.isBlank()) ? "Not provided" : memberEmail;
        String phoneDisplay = (memberPhone == null || memberPhone.isBlank()) ? "Not provided" : memberPhone;

        String body =
            "<h2 style='margin:0 0 6px;color:#e65c00;font-size:20px;'>📩 New Contact Message</h2>" +
            "<p style='margin:0 0 24px;color:#888;font-size:13px;border-bottom:1px solid #f0e0cc;padding-bottom:20px;'>" +
            "A member has sent a message through the Hanuman Sangam portal</p>" +

            // Member info
            "<div style='background:#fafafa;border:1px solid #f0e0cc;border-radius:10px;padding:20px;margin:0 0 20px;'>" +
            "<h4 style='margin:0 0 14px;color:#e65c00;font-size:13px;letter-spacing:1px;text-transform:uppercase;'>Member Details</h4>" +
            "<table cellpadding='0' cellspacing='0' width='100%'>" +
            "<tr><td style='padding:8px 0;color:#888;font-size:13px;width:80px;'>👤 Name</td>" +
            "<td style='padding:8px 0;color:#333;font-size:14px;font-weight:600;'>" + memberName + "</td></tr>" +
            "<tr><td style='padding:8px 0;color:#888;font-size:13px;'>📧 Email</td>" +
            "<td style='padding:8px 0;color:#333;font-size:14px;'>" + emailDisplay + "</td></tr>" +
            "<tr><td style='padding:8px 0;color:#888;font-size:13px;'>📱 Phone</td>" +
            "<td style='padding:8px 0;color:#333;font-size:14px;'>" + phoneDisplay + "</td></tr>" +
            "</table></div>" +

            // Message content
            "<div style='background:#fff8f0;border:1px solid #ffccbc;border-left:4px solid #e65c00;" +
            "border-radius:0 10px 10px 0;padding:20px;margin:0 0 20px;'>" +
            "<h4 style='margin:0 0 12px;color:#e65c00;font-size:13px;letter-spacing:1px;text-transform:uppercase;'>💬 Message</h4>" +
            "<p style='margin:0;color:#333;font-size:15px;line-height:1.8;white-space:pre-line;'>" + message + "</p>" +
            "</div>" +

            // Reply hint
            "<div style='background:#e8f5e9;border:1px solid #c8e6c9;border-radius:8px;padding:14px 18px;'>" +
            "<p style='margin:0;color:#2e7d32;font-size:13px;'>✅ To reply, send an email to: " +
            "<a href='mailto:" + memberEmail + "' style='color:#1b5e20;font-weight:600;'>" + emailDisplay + "</a></p>" +
            "</div>";

        // Send to admin email directly
        Resend resend = new Resend(apiKey);
        CreateEmailOptions params = CreateEmailOptions.builder()
                .from("Hanuman Sangam Portal <onboarding@resend.dev>")
                .to(adminEmail)
                .replyTo(memberEmail.isBlank() ? adminEmail : memberEmail)
                .subject("📩 Contact Message from " + memberName + " - Hanuman Sangam")
                .html(wrapInTemplate(body))
                .build();
        try {
            resend.emails().send(params);
        } catch (ResendException e) {
            throw new RuntimeException("Failed to send contact email to admin: " + e.getMessage(), e);
        }
    }
}
