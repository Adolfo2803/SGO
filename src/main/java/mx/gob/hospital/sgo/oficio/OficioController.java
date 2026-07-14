package mx.gob.hospital.sgo.oficio;

import jakarta.validation.Valid;
import mx.gob.hospital.sgo.area.Area;
import mx.gob.hospital.sgo.area.AreaRepository;
import mx.gob.hospital.sgo.common.ArchivoInvalidoException;
import mx.gob.hospital.sgo.common.FolioDuplicadoException;
import mx.gob.hospital.sgo.storage.AlmacenArchivos;
import mx.gob.hospital.sgo.turnado.Turnado;
import mx.gob.hospital.sgo.turnado.TurnadoService;
import org.springframework.core.io.Resource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Controller
public class OficioController {

    private final OficioService oficioService;
    private final AlmacenArchivos almacenArchivos;
    private final AreaRepository areaRepository;
    private final TurnadoService turnadoService;

    public OficioController(OficioService oficioService,
                            AlmacenArchivos almacenArchivos,
                            AreaRepository areaRepository,
                            TurnadoService turnadoService) {
        this.oficioService = oficioService;
        this.almacenArchivos = almacenArchivos;
        this.areaRepository = areaRepository;
        this.turnadoService = turnadoService;
    }

    // --- Oficio de entrada ---

    @GetMapping("/oficios/nuevo")
    public String mostrarFormulario(Model model) {
        if (!model.containsAttribute("oficioForm")) {
            model.addAttribute("oficioForm", new OficioForm());
        }
        return "oficio/formulario";
    }

    @PostMapping("/oficios")
    public String registrar(@Valid @ModelAttribute("oficioForm") OficioForm form,
                            BindingResult result,
                            @RequestParam(value = "archivo", required = false) MultipartFile archivo,
                            RedirectAttributes redirect,
                            Model model) {

        if (result.hasErrors()) {
            return "oficio/formulario";
        }

        try {
            Oficio oficio = oficioService.registrarEntrada(
                    form.getFolio(),
                    form.getFecha(),
                    form.getRemitente(),
                    form.getDestinatario(),
                    form.getAsunto(),
                    archivo);

            redirect.addFlashAttribute("mensaje",
                    "Oficio «" + oficio.getFolio() + "» registrado exitosamente.");
            return "redirect:/oficios/nuevo";

        } catch (FolioDuplicadoException | ArchivoInvalidoException e) {
            model.addAttribute("error", e.getMessage());
            return "oficio/formulario";
        }
    }

    // --- Oficio de salida ---

    @GetMapping("/oficios/salida/nuevo")
    public String mostrarFormularioSalida(Model model) {
        if (!model.containsAttribute("salidaForm")) {
            model.addAttribute("salidaForm", new OficioSalidaForm());
        }
        return "oficio/formulario-salida";
    }

    @PostMapping("/oficios/salida")
    public String registrarSalida(@Valid @ModelAttribute("salidaForm") OficioSalidaForm form,
                                  BindingResult result,
                                  @RequestParam(value = "archivo", required = false) MultipartFile archivo,
                                  RedirectAttributes redirect,
                                  Model model) {

        if (result.hasErrors()) {
            return "oficio/formulario-salida";
        }

        try {
            Oficio oficio = oficioService.registrarSalida(
                    form.getFecha(),
                    form.getDestinatario(),
                    form.getAsunto(),
                    archivo);

            redirect.addFlashAttribute("mensaje",
                    "Oficio de salida registrado con folio:");
            redirect.addFlashAttribute("folioGenerado", oficio.getFolio());
            return "redirect:/oficios/salida/nuevo";

        } catch (ArchivoInvalidoException e) {
            model.addAttribute("error", e.getMessage());
            return "oficio/formulario-salida";
        }
    }

    // --- Listado y detalle ---

    @GetMapping("/oficios")
    public String listar(@ModelAttribute("filtro") FiltroOficioForm filtro,
                         @PageableDefault(size = 20) Pageable pageable,
                         Model model) {
        Page<Oficio> pagina = oficioService.buscar(filtro, pageable);
        model.addAttribute("pagina", pagina);
        model.addAttribute("direcciones", Direccion.values());
        model.addAttribute("estados", EstadoOficio.values());
        model.addAttribute("areas", areaRepository.findAll());
        return "oficio/listado";
    }

    @GetMapping("/oficios/{id}")
    public String detalle(@PathVariable Long id, Model model) {
        Oficio oficio = oficioService.buscarPorId(id);
        if (oficio == null) {
            return "redirect:/oficios";
        }

        List<Turnado> turnados = turnadoService.listarPorOficio(id);

        Set<Long> areasYaTurnadas = turnados.stream()
                .map(t -> t.getArea().getId())
                .collect(Collectors.toSet());
        List<Area> areasDisponibles = areaRepository.findByActivaTrue().stream()
                .filter(a -> !areasYaTurnadas.contains(a.getId()))
                .toList();

        model.addAttribute("oficio", oficio);
        model.addAttribute("turnados", turnados);
        model.addAttribute("areasDisponibles", areasDisponibles);
        return "oficio/detalle";
    }

    @PostMapping("/oficios/{id}/turnar")
    public String turnar(@PathVariable Long id,
                         @RequestParam("areaIds") List<Long> areaIds,
                         RedirectAttributes redirect) {
        try {
            turnadoService.turnar(id, areaIds);
            redirect.addFlashAttribute("mensaje",
                    "Se turnó a " + areaIds.size() + " área(s). El envío está en proceso.");
        } catch (IllegalArgumentException e) {
            redirect.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/oficios/" + id;
    }

    @GetMapping("/oficios/{id}/archivo")
    public ResponseEntity<Resource> descargarArchivo(@PathVariable Long id) {
        Oficio oficio = oficioService.buscarPorId(id);
        if (oficio == null || oficio.getRutaArchivo() == null) {
            return ResponseEntity.notFound().build();
        }

        Resource recurso;
        try {
            recurso = almacenArchivos.cargar(oficio.getRutaArchivo());
        } catch (Exception e) {
            return ResponseEntity.notFound().build();
        }

        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_PDF)
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + oficio.getFolio() + ".pdf\"")
                .body(recurso);
    }
}
