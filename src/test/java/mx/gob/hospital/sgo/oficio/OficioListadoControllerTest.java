package mx.gob.hospital.sgo.oficio;

import mx.gob.hospital.sgo.area.AreaRepository;
import mx.gob.hospital.sgo.config.SecurityConfig;
import mx.gob.hospital.sgo.storage.AlmacenArchivos;
import mx.gob.hospital.sgo.turnado.TurnadoService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

import static org.hamcrest.Matchers.containsString;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

@WebMvcTest(OficioController.class)
@Import(SecurityConfig.class)
class OficioListadoControllerTest {

    @Autowired
    MockMvc mockMvc;

    @MockBean OficioService oficioService;
    @MockBean AlmacenArchivos almacenArchivos;
    @MockBean AreaRepository areaRepository;
    @MockBean TurnadoService turnadoService;

    @Test
    @WithMockUser
    void getListado_muestraTablaConResultados() throws Exception {
        Oficio oficio = crearOficioCompleto("OF-001");
        PageImpl<Oficio> pagina = new PageImpl<>(List.of(oficio), PageRequest.of(0, 20), 1);
        when(oficioService.buscar(any(FiltroOficioForm.class), any(Pageable.class))).thenReturn(pagina);
        when(areaRepository.findAll()).thenReturn(List.of());

        mockMvc.perform(get("/oficios"))
                .andExpect(status().isOk())
                .andExpect(view().name("oficio/listado"))
                .andExpect(model().attributeExists("pagina", "filtro"))
                .andExpect(content().string(containsString("OF-001")));
    }

    @Test
    @WithMockUser
    void paginacionPreservaFiltros() throws Exception {
        Oficio oficio = crearOficioCompleto("OF-001");
        PageImpl<Oficio> pagina = new PageImpl<>(List.of(oficio), PageRequest.of(0, 20), 40);
        when(oficioService.buscar(any(FiltroOficioForm.class), any(Pageable.class))).thenReturn(pagina);
        when(areaRepository.findAll()).thenReturn(List.of());

        mockMvc.perform(get("/oficios")
                        .param("texto", "salud")
                        .param("direccion", "ENTRADA"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("texto=salud")))
                .andExpect(content().string(containsString("direccion=ENTRADA")));
    }

    @Test
    @WithMockUser
    void descargarArchivo_devuelvePdf() throws Exception {
        Oficio oficio = crearOficioCompleto("OF-001");
        oficio.setRutaArchivo("2026/07/abc.pdf");
        when(oficioService.buscarPorId(1L)).thenReturn(oficio);
        when(almacenArchivos.cargar("2026/07/abc.pdf"))
                .thenReturn(new ByteArrayResource("%PDF-1.4 test".getBytes()));

        mockMvc.perform(get("/oficios/1/archivo"))
                .andExpect(status().isOk())
                .andExpect(content().contentType("application/pdf"));
    }

    @Test
    @WithMockUser
    void descargarArchivo_oficioInexistente_devuelve404() throws Exception {
        when(oficioService.buscarPorId(999L)).thenReturn(null);

        mockMvc.perform(get("/oficios/999/archivo"))
                .andExpect(status().isNotFound());
    }

    private Oficio crearOficioCompleto(String folio) {
        Oficio o = new Oficio();
        o.setId(1L);
        o.setFolio(folio);
        o.setDireccion(Direccion.ENTRADA);
        o.setEstado(EstadoOficio.RECIBIDO);
        o.setFecha(LocalDate.of(2026, 7, 13));
        o.setRemitente("Secretaría de Salud");
        o.setDestinatario("Director General");
        o.setAsunto("Solicitud de informe");
        o.setCreadoPor("test");
        o.setCreadoEn(Instant.now());
        return o;
    }
}
