package mx.gob.hospital.sgo.common;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@ControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(FolioDuplicadoException.class)
    public String handleFolioDuplicado(FolioDuplicadoException e, RedirectAttributes redirect) {
        redirect.addFlashAttribute("error", e.getMessage());
        return "redirect:/oficios/nuevo";
    }

    @ExceptionHandler(ArchivoInvalidoException.class)
    public String handleArchivoInvalido(ArchivoInvalidoException e, RedirectAttributes redirect) {
        redirect.addFlashAttribute("error", e.getMessage());
        return "redirect:/oficios/nuevo";
    }

    @ExceptionHandler(Exception.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public String handleGeneral(Exception e, Model model) {
        log.error("Error no controlado", e);
        model.addAttribute("status", 500);
        return "error";
    }
}
