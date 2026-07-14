package mx.gob.hospital.sgo.oficio;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.context.ActiveProfiles;

import java.time.Instant;
import java.time.LocalDate;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@ActiveProfiles("test")
class OficioRepositoryTest {

    @Autowired
    OficioRepository oficioRepository;

    @Test
    void guardarYRecuperarOficio() {
        Oficio oficio = crearOficio("OF-2026-001");

        Oficio guardado = oficioRepository.save(oficio);

        assertThat(guardado.getId()).isNotNull();

        Optional<Oficio> recuperado = oficioRepository.findById(guardado.getId());
        assertThat(recuperado).isPresent();
        assertThat(recuperado.get().getFolio()).isEqualTo("OF-2026-001");
        assertThat(recuperado.get().getDireccion()).isEqualTo(Direccion.ENTRADA);
        assertThat(recuperado.get().getEstado()).isEqualTo(EstadoOficio.RECIBIDO);
        assertThat(recuperado.get().getRemitente()).isEqualTo("Secretaría de Salud");
        assertThat(recuperado.get().getCreadoEn()).isNotNull();
    }

    @Test
    void folioDuplicadoLanzaExcepcion() {
        Oficio primero = crearOficio("DUP-001");
        oficioRepository.saveAndFlush(primero);

        Oficio duplicado = crearOficio("DUP-001");

        assertThatThrownBy(() -> oficioRepository.saveAndFlush(duplicado))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void flywayEjecutaMigracionesYHibernateValida() {
        // Si el contexto de Spring levantó sin errores, Flyway corrió V1 y V2
        // y Hibernate validó el esquema contra las entidades (ddl-auto=validate).
        long total = oficioRepository.count();
        assertThat(total).isGreaterThanOrEqualTo(0);
    }

    private Oficio crearOficio(String folio) {
        Oficio oficio = new Oficio();
        oficio.setFolio(folio);
        oficio.setDireccion(Direccion.ENTRADA);
        oficio.setFecha(LocalDate.of(2026, 7, 13));
        oficio.setRemitente("Secretaría de Salud");
        oficio.setDestinatario("Director General");
        oficio.setAsunto("Solicitud de informe mensual");
        oficio.setEstado(EstadoOficio.RECIBIDO);
        oficio.setCreadoPor("admin");
        oficio.setCreadoEn(Instant.now());
        return oficio;
    }
}
