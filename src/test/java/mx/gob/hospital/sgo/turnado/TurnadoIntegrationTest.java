package mx.gob.hospital.sgo.turnado;

import com.icegreen.greenmail.configuration.GreenMailConfiguration;
import com.icegreen.greenmail.junit5.GreenMailExtension;
import com.icegreen.greenmail.util.ServerSetupTest;
import jakarta.mail.internet.MimeMessage;
import jakarta.mail.internet.MimeMultipart;
import mx.gob.hospital.sgo.area.Area;
import mx.gob.hospital.sgo.area.AreaRepository;
import mx.gob.hospital.sgo.oficio.Direccion;
import mx.gob.hospital.sgo.oficio.EstadoOficio;
import mx.gob.hospital.sgo.oficio.Oficio;
import mx.gob.hospital.sgo.oficio.OficioRepository;
import mx.gob.hospital.sgo.storage.AlmacenArchivos;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

import java.nio.file.Path;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.doThrow;

@SpringBootTest
@ActiveProfiles("test")
class TurnadoIntegrationTest {

    @RegisterExtension
    static GreenMailExtension greenMail = new GreenMailExtension(ServerSetupTest.SMTP)
            .withConfiguration(GreenMailConfiguration.aConfig().withDisabledAuthentication());

    @TempDir
    static Path tempAlmacen;

    @DynamicPropertySource
    static void props(DynamicPropertyRegistry registry) {
        registry.add("sgo.almacen.raiz", () -> tempAlmacen.toString());
        registry.add("spring.mail.port", () -> 3025);
    }

    @Autowired OficioRepository oficioRepository;
    @Autowired AreaRepository areaRepository;
    @Autowired TurnadoRepository turnadoRepository;
    @Autowired TurnadoService turnadoService;
    @Autowired AlmacenArchivos almacenArchivos;
    @Autowired EnvioCorreoJob envioCorreoJob;
    @SpyBean  ServicioCorreo servicioCorreo;

    private Oficio oficio;
    private Area areaRH;
    private Area areaEnsenanza;

    @BeforeEach
    void setUp() {
        turnadoRepository.deleteAll();
        oficioRepository.deleteAll();

        MockMultipartFile pdf = new MockMultipartFile(
                "archivo", "oficio.pdf", "application/pdf",
                "%PDF-1.4 contenido de prueba para test de integración".getBytes());
        String ruta = almacenArchivos.guardar(pdf);

        oficio = new Oficio();
        oficio.setFolio("INT-" + System.nanoTime());
        oficio.setDireccion(Direccion.ENTRADA);
        oficio.setEstado(EstadoOficio.RECIBIDO);
        oficio.setFecha(LocalDate.of(2026, 7, 13));
        oficio.setRemitente("Secretaría de Salud");
        oficio.setDestinatario("Director General");
        oficio.setAsunto("Prueba de integración");
        oficio.setCreadoPor("test");
        oficio.setCreadoEn(Instant.now());
        oficio.setRutaArchivo(ruta);
        oficio = oficioRepository.save(oficio);

        areaRH = areaRepository.findAll().stream()
                .filter(a -> a.getNombre().equals("Recursos Humanos")).findFirst().orElseThrow();
        areaEnsenanza = areaRepository.findAll().stream()
                .filter(a -> a.getNombre().equals("Enseñanza")).findFirst().orElseThrow();
    }

    @Test
    void flujoCompleto_turnarYEnviarCorreoA2Areas() throws Exception {
        turnadoService.turnar(oficio.getId(), List.of(areaRH.getId(), areaEnsenanza.getId()));

        envioCorreoJob.procesar();

        MimeMessage[] correos = greenMail.getReceivedMessages();
        assertThat(correos).hasSize(2);

        for (MimeMessage correo : correos) {
            assertThat(correo.getSubject()).contains(oficio.getFolio());
            assertThat(correo.getContent()).isInstanceOf(MimeMultipart.class);
            MimeMultipart multipart = (MimeMultipart) correo.getContent();
            assertThat(multipart.getCount()).isGreaterThanOrEqualTo(2);
        }

        Oficio actualizado = oficioRepository.findById(oficio.getId()).orElseThrow();
        assertThat(actualizado.getEstado()).isEqualTo(EstadoOficio.TURNADO);

        List<Turnado> turnados = turnadoRepository.findByOficioIdOrderByIdAsc(oficio.getId());
        assertThat(turnados).hasSize(2);
        assertThat(turnados).allMatch(t -> t.getEstado() == EstadoTurnado.ENVIADO);
    }

    @Test
    void falloParalelo_unAreaFallaOtraExitosa() throws Exception {
        turnadoService.turnar(oficio.getId(), List.of(areaRH.getId(), areaEnsenanza.getId()));

        doThrow(new RuntimeException("SMTP simulado"))
                .when(servicioCorreo).enviar(argThat(t ->
                        t.getArea().getId().equals(areaEnsenanza.getId())));

        envioCorreoJob.procesar();

        List<Turnado> turnados = turnadoRepository.findByOficioIdOrderByIdAsc(oficio.getId());
        Turnado turnadoRH = turnados.stream()
                .filter(t -> t.getArea().getId().equals(areaRH.getId())).findFirst().orElseThrow();
        Turnado turnadoEnsenanza = turnados.stream()
                .filter(t -> t.getArea().getId().equals(areaEnsenanza.getId())).findFirst().orElseThrow();

        assertThat(turnadoRH.getEstado()).isEqualTo(EstadoTurnado.ENVIADO);
        assertThat(turnadoEnsenanza.getEstado()).isEqualTo(EstadoTurnado.FALLIDO);
        assertThat(turnadoEnsenanza.getIntentos()).isEqualTo(1);
        assertThat(turnadoEnsenanza.getUltimoError()).contains("SMTP simulado");

        Oficio actualizado = oficioRepository.findById(oficio.getId()).orElseThrow();
        assertThat(actualizado.getEstado()).isEqualTo(EstadoOficio.TURNADO);
    }
}
