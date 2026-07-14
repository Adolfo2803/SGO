package mx.gob.hospital.sgo.oficio;

import mx.gob.hospital.sgo.area.Area;
import mx.gob.hospital.sgo.area.AreaRepository;
import mx.gob.hospital.sgo.turnado.EstadoTurnado;
import mx.gob.hospital.sgo.turnado.Turnado;
import mx.gob.hospital.sgo.turnado.TurnadoRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.test.context.ActiveProfiles;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@ActiveProfiles("test")
class OficioSpecificationsTest {

    @Autowired
    OficioRepository oficioRepository;

    @Autowired
    AreaRepository areaRepository;

    @Autowired
    TurnadoRepository turnadoRepository;

    private Oficio oficioJulio;
    private Oficio oficioAgosto;
    private Area areaRH;

    @BeforeEach
    void setUp() {
        turnadoRepository.deleteAll();
        oficioRepository.deleteAll();

        oficioJulio = crearOficio("OF-JUL-001", Direccion.ENTRADA, EstadoOficio.RECIBIDO,
                LocalDate.of(2026, 7, 10), "Secretaría de Salud", "Informe mensual de julio");
        oficioAgosto = crearOficio("OF-AGO-001", Direccion.SALIDA, EstadoOficio.TURNADO,
                LocalDate.of(2026, 8, 5), "Hospital General", "Solicitud de presupuesto");

        areaRH = areaRepository.findAll().stream()
                .filter(a -> a.getNombre().equals("Recursos Humanos"))
                .findFirst().orElseThrow();

        Turnado turnado = new Turnado();
        turnado.setOficio(oficioJulio);
        turnado.setArea(areaRH);
        turnado.setEstado(EstadoTurnado.ENVIADO);
        turnadoRepository.save(turnado);
    }

    @Test
    void fechaDesde_filtraCorrectamente() {
        List<Oficio> resultado = oficioRepository.findAll(
                OficioSpecifications.fechaDesde(LocalDate.of(2026, 8, 1)));
        assertThat(resultado).extracting(Oficio::getFolio).containsExactly("OF-AGO-001");
    }

    @Test
    void fechaHasta_filtraCorrectamente() {
        List<Oficio> resultado = oficioRepository.findAll(
                OficioSpecifications.fechaHasta(LocalDate.of(2026, 7, 31)));
        assertThat(resultado).extracting(Oficio::getFolio).containsExactly("OF-JUL-001");
    }

    @Test
    void direccion_filtraCorrectamente() {
        List<Oficio> resultado = oficioRepository.findAll(
                OficioSpecifications.direccion(Direccion.SALIDA));
        assertThat(resultado).extracting(Oficio::getFolio).containsExactly("OF-AGO-001");
    }

    @Test
    void estado_filtraCorrectamente() {
        List<Oficio> resultado = oficioRepository.findAll(
                OficioSpecifications.estado(EstadoOficio.TURNADO));
        assertThat(resultado).extracting(Oficio::getFolio).containsExactly("OF-AGO-001");
    }

    @Test
    void textoLibre_buscaEnAsuntoRemitenteYFolio() {
        List<Oficio> porAsunto = oficioRepository.findAll(
                OficioSpecifications.textoLibre("presupuesto"));
        assertThat(porAsunto).extracting(Oficio::getFolio).containsExactly("OF-AGO-001");

        List<Oficio> porRemitente = oficioRepository.findAll(
                OficioSpecifications.textoLibre("secretaría"));
        assertThat(porRemitente).extracting(Oficio::getFolio).containsExactly("OF-JUL-001");

        List<Oficio> porFolio = oficioRepository.findAll(
                OficioSpecifications.textoLibre("AGO"));
        assertThat(porFolio).extracting(Oficio::getFolio).containsExactly("OF-AGO-001");
    }

    @Test
    void areaTurnada_filtraCorrectamente() {
        List<Oficio> resultado = oficioRepository.findAll(
                OficioSpecifications.areaTurnada(areaRH.getId()));
        assertThat(resultado).extracting(Oficio::getFolio).containsExactly("OF-JUL-001");
    }

    @Test
    void filtroNulo_noRestringe() {
        Specification<Oficio> spec = Specification.where(OficioSpecifications.fechaDesde(null))
                .and(OficioSpecifications.direccion(null))
                .and(OficioSpecifications.estado(null))
                .and(OficioSpecifications.textoLibre(null));

        List<Oficio> resultado = oficioRepository.findAll(spec);
        assertThat(resultado).hasSize(2);
    }

    @Test
    void combinacionDeFiltros() {
        Specification<Oficio> spec = Specification
                .where(OficioSpecifications.direccion(Direccion.ENTRADA))
                .and(OficioSpecifications.textoLibre("julio"));

        List<Oficio> resultado = oficioRepository.findAll(spec);
        assertThat(resultado).extracting(Oficio::getFolio).containsExactly("OF-JUL-001");
    }

    private Oficio crearOficio(String folio, Direccion direccion, EstadoOficio estado,
                               LocalDate fecha, String remitente, String asunto) {
        Oficio o = new Oficio();
        o.setFolio(folio);
        o.setDireccion(direccion);
        o.setEstado(estado);
        o.setFecha(fecha);
        o.setRemitente(remitente);
        o.setDestinatario("Director General");
        o.setAsunto(asunto);
        o.setCreadoPor("test");
        o.setCreadoEn(Instant.now());
        return oficioRepository.save(o);
    }
}
