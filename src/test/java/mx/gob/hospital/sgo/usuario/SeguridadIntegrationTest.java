package mx.gob.hospital.sgo.usuario;

import mx.gob.hospital.sgo.oficio.Oficio;
import mx.gob.hospital.sgo.oficio.OficioRepository;
import mx.gob.hospital.sgo.oficio.OficioService;
import mx.gob.hospital.sgo.storage.AlmacenArchivos;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

import java.nio.file.Path;
import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
class SeguridadIntegrationTest {

    @TempDir
    static Path tempAlmacen;

    @DynamicPropertySource
    static void props(DynamicPropertyRegistry registry) {
        registry.add("sgo.almacen.raiz", () -> tempAlmacen.toString());
    }

    @Autowired OficioService oficioService;
    @Autowired OficioRepository oficioRepository;
    @Autowired AlmacenArchivos almacenArchivos;

    @Test
    @WithMockUser(username = "secretaria.gomez")
    void oficioRegistrado_guardaUsernameRealEnCreadoPor() {
        MockMultipartFile pdf = new MockMultipartFile(
                "archivo", "test.pdf", "application/pdf",
                "%PDF-1.4 contenido".getBytes());

        Oficio oficio = oficioService.registrarEntrada(
                "AUD-" + System.nanoTime(),
                LocalDate.of(2026, 7, 13),
                "Remitente", "Destinatario", "Asunto", pdf);

        Oficio guardado = oficioRepository.findById(oficio.getId()).orElseThrow();
        assertThat(guardado.getCreadoPor()).isEqualTo("secretaria.gomez");
    }

    @Test
    void bcrypt_verificaCorrectamente() {
        BCryptPasswordEncoder encoder = new BCryptPasswordEncoder(12);
        String hash = encoder.encode("mi_contraseña_segura");

        assertThat(encoder.matches("mi_contraseña_segura", hash)).isTrue();
        assertThat(encoder.matches("contraseña_incorrecta", hash)).isFalse();
    }

    @Test
    void bcrypt_dosHashesDeLaMismaContrasena_sonDistintos() {
        BCryptPasswordEncoder encoder = new BCryptPasswordEncoder(12);
        String hash1 = encoder.encode("misma_contraseña");
        String hash2 = encoder.encode("misma_contraseña");

        assertThat(hash1).isNotEqualTo(hash2);
        assertThat(encoder.matches("misma_contraseña", hash1)).isTrue();
        assertThat(encoder.matches("misma_contraseña", hash2)).isTrue();
    }
}
