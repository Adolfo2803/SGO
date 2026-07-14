package mx.gob.hospital.sgo.turnado;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import mx.gob.hospital.sgo.oficio.Oficio;
import mx.gob.hospital.sgo.storage.AlmacenArchivos;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

@Service
public class ServicioCorreo {

    private final JavaMailSender mailSender;
    private final TemplateEngine templateEngine;
    private final AlmacenArchivos almacenArchivos;
    private final String remitente;

    public ServicioCorreo(JavaMailSender mailSender,
                          TemplateEngine templateEngine,
                          AlmacenArchivos almacenArchivos,
                          @Value("${sgo.correo.remitente}") String remitente) {
        this.mailSender = mailSender;
        this.templateEngine = templateEngine;
        this.almacenArchivos = almacenArchivos;
        this.remitente = remitente;
    }

    public void enviar(Turnado turnado) throws MessagingException {
        Oficio oficio = turnado.getOficio();

        Context ctx = new Context();
        ctx.setVariable("oficio", oficio);
        String html = templateEngine.process("correo/turnado", ctx);

        MimeMessage mensaje = mailSender.createMimeMessage();
        MimeMessageHelper helper = new MimeMessageHelper(mensaje, true, "UTF-8");
        helper.setFrom(remitente);
        helper.setTo(turnado.getArea().getCorreo());
        helper.setSubject("Oficio turnado: " + oficio.getFolio() + " - " + oficio.getAsunto());
        helper.setText(html, true);

        if (oficio.getRutaArchivo() != null) {
            Resource pdf = almacenArchivos.cargar(oficio.getRutaArchivo());
            helper.addAttachment(oficio.getFolio() + ".pdf", pdf);
        }

        mailSender.send(mensaje);
    }
}
