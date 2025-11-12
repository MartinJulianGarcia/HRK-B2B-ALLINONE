package com.hrk.tienda_b2b.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
public class EmailService {

    private final JavaMailSender mailSender;

    @Value("${spring.mail.from:}")
    private String fromEmail;

    public EmailService(JavaMailSender mailSender) {
        this.mailSender = mailSender;
    }

    public void enviarPasswordTemporal(String emailDestinatario, String nombreUsuario, String passwordTemporal) {
        // Verificar si el email está configurado
        if (fromEmail == null || fromEmail.isEmpty() || fromEmail.contains("tu-email")) {
            System.err.println("🔴 [EMAIL SERVICE] Email no configurado en application.properties");
            System.err.println("🔴 [EMAIL SERVICE] Por favor configura spring.mail.username, spring.mail.password y spring.mail.from");
            throw new IllegalStateException(
                "El servicio de email no está configurado. " +
                "Por favor configura las propiedades de email en application.properties: " +
                "spring.mail.username, spring.mail.password y spring.mail.from"
            );
        }

        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(fromEmail);
            message.setTo(emailDestinatario);
            message.setSubject("Recuperación de Contraseña - HRK B2B");
            
            String cuerpo = String.format(
                "Hola %s,\n\n" +
                "Has solicitado restablecer tu contraseña.\n\n" +
                "Tu nueva contraseña temporal es: %s\n\n" +
                "Por seguridad, deberás cambiarla cuando inicies sesión.\n\n" +
                "Si no solicitaste este correo, puedes ignorarlo.\n\n" +
                "Saludos,\n" +
                "Equipo HRK B2B",
                nombreUsuario, passwordTemporal
            );
            
            message.setText(cuerpo);
            
            mailSender.send(message);
            System.out.println("✅ [EMAIL SERVICE] Email de recuperación enviado a: " + emailDestinatario);
        } catch (Exception e) {
            System.err.println("🔴 [EMAIL SERVICE] Error al enviar email: " + e.getMessage());
            e.printStackTrace();
            throw new RuntimeException("Error al enviar el email de recuperación: " + e.getMessage(), e);
        }
    }
}

