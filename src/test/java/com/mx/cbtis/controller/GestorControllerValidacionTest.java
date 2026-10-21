package com.mx.cbtis.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.verifyNoInteractions;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.servlet.ModelAndView;

import com.mx.cbtis.modelo.Usuario;

@ExtendWith(MockitoExtension.class)
class GestorControllerValidacionTest {

    @Mock
    RestTemplate template;

    @InjectMocks
    GestorController controller;

    @Test
    @DisplayName("UT-35 saveUsuario: correo valido y contrasena vacia redirige a /register?error sin llamar al backend")
    void saveUsuario_contrasenaVacia() {
        Usuario u = new Usuario();
        u.setCorreo_electronico("usuario@correo.com");
        u.setContrasena("   ");

        ModelAndView resultado = controller.setSaveUsuario(u);

        assertEquals("redirect:/register?error", resultado.getViewName());
        verifyNoInteractions(template);
    }

    @Test
    @DisplayName("UT-36 saveUsuario: correo vacio y contrasena valida redirige a /register?error sin llamar al backend")
    void saveUsuario_correoVacio() {
        Usuario u = new Usuario();
        u.setCorreo_electronico("");
        u.setContrasena("1234");

        ModelAndView resultado = controller.setSaveUsuario(u);

        assertEquals("redirect:/register?error", resultado.getViewName());
        verifyNoInteractions(template);
    }

    @Test
    @DisplayName("UT-37 saveUsuario: correo valido y contrasena nula redirige a /register?error sin llamar al backend")
    void saveUsuario_contrasenaNula() {
        Usuario u = new Usuario();
        u.setCorreo_electronico("usuario@correo.com");

        ModelAndView resultado = controller.setSaveUsuario(u);

        assertEquals("redirect:/register?error", resultado.getViewName());
        verifyNoInteractions(template);
    }
}
