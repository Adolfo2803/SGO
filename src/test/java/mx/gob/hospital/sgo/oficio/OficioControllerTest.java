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
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.flash;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrlPattern;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

@WebMvcTest(OficioController.class)
@Import(SecurityConfig.class)
class OficioControllerTest {

    @Autowired
    MockMvc mockMvc;

    @MockBean OficioService oficioService;
    @MockBean AlmacenArchivos almacenArchivos;
    @MockBean AreaRepository areaRepository;
    @MockBean TurnadoService turnadoService;

    @Test
    void sinAutenticar_redirigirALogin() throws Exception {
        mockMvc.perform(get("/oficios/nuevo"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrlPattern("**/login"));
    }

    @Test
    @WithMockUser(username = "secretaria1")
    void autenticado_accedeAlFormulario() throws Exception {
        mockMvc.perform(get("/oficios/nuevo"))
                .andExpect(status().isOk())
                .andExpect(view().name("oficio/formulario"))
                .andExpect(model().attributeExists("oficioForm"));
    }

    @Test
    @WithMockUser(username = "secretaria1")
    void postValido_redirige() throws Exception {
        Oficio oficio = new Oficio();
        oficio.setId(1L);
        oficio.setFolio("OF-2026-001");

        when(oficioService.registrarEntrada(
                eq("OF-2026-001"), any(LocalDate.class),
                eq("Secretaría de Salud"), eq("Director General"),
                eq("Solicitud de informe"), any()))
                .thenReturn(oficio);

        MockMultipartFile archivo = new MockMultipartFile(
                "archivo", "oficio.pdf", "application/pdf",
                "%PDF-1.4 test".getBytes());

        mockMvc.perform(multipart("/oficios")
                        .file(archivo)
                        .param("folio", "OF-2026-001")
                        .param("fecha", "2026-07-13")
                        .param("remitente", "Secretaría de Salud")
                        .param("destinatario", "Director General")
                        .param("asunto", "Solicitud de informe")
                        .with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/oficios/nuevo"))
                .andExpect(flash().attributeExists("mensaje"));
    }

    @Test
    @WithMockUser(username = "secretaria1")
    void postSinCamposObligatorios_reRenderizaConErrores() throws Exception {
        MockMultipartFile archivo = new MockMultipartFile(
                "archivo", "oficio.pdf", "application/pdf",
                "%PDF-1.4 test".getBytes());

        mockMvc.perform(multipart("/oficios")
                        .file(archivo)
                        .param("folio", "")
                        .param("remitente", "")
                        .param("destinatario", "")
                        .param("asunto", "")
                        .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(view().name("oficio/formulario"))
                .andExpect(model().attributeHasFieldErrors("oficioForm",
                        "folio", "remitente", "destinatario", "asunto"));
    }

    @Test
    @WithMockUser(username = "secretaria1")
    void postSinCsrf_rechazadoCon403() throws Exception {
        mockMvc.perform(multipart("/oficios")
                        .param("folio", "OF-001")
                        .param("fecha", "2026-07-13")
                        .param("remitente", "Rem")
                        .param("destinatario", "Dest")
                        .param("asunto", "Asunto"))
                .andExpect(status().isForbidden());
    }
}
