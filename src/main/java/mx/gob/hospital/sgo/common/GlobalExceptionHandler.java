package mx.gob.hospital.sgo.common;

import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@ControllerAdvice
public class GlobalExceptionHandler {

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
}
