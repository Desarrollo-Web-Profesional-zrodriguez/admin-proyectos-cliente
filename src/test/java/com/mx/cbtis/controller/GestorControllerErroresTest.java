package com.mx.cbtis.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.ui.ExtendedModelMap;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.servlet.ModelAndView;

import com.mx.cbtis.modelo.Proyectos;
import com.mx.cbtis.modelo.Tareas;
import com.mx.cbtis.modelo.Usuario;

@ExtendWith(MockitoExtension.class)
class GestorControllerErroresTest {

    @Mock
    RestTemplate template;

    @InjectMocks
    GestorController controller;

    private Usuario crearUsuario(String nombre, String correo, String pass) {
        Usuario u = new Usuario();
        u.setNombre(nombre);
        u.setCorreo_electronico(correo);
        u.setContrasena(pass);
        return u;
    }

    private Proyectos crearProyecto(Integer id, String nombre) {
        Proyectos p = new Proyectos();
        p.setId_proyecto(id);
        p.setNombre(nombre);
        return p;
    }

    // ---------- Pruebas de escenario exitoso ----------

    @Test
    @DisplayName("AUT-E01 consultarProyectos: el backend devuelve 2 proyectos validos")
    void consultarProyectos_devuelveLista() {
        Proyectos[] datos = { crearProyecto(1, "A"), crearProyecto(2, "B") };
        when(template.postForObject(eq("http://localhost:8081/buscarProyectos"), any(), eq(Proyectos[].class)))
                .thenReturn(datos);

        ResponseEntity<Object> respuesta = controller.consultarProyectos(new ExtendedModelMap());

        assertEquals(HttpStatus.OK, respuesta.getStatusCode());
        assertEquals(2, ((List<?>) respuesta.getBody()).size());
    }

    @Test
    @DisplayName("AUT-E02 verificarLogin: el backend devuelve el usuario con credenciales validas")
    void verificarLogin_credencialesValidas() {
        Usuario enviado = crearUsuario(null, "usuario@correo.com", "1234");
        Usuario encontrado = crearUsuario("Usuario Prueba", "usuario@correo.com", "1234");
        when(template.postForEntity("http://localhost:8081/getUsuarioLogin", enviado, Usuario.class))
                .thenReturn(ResponseEntity.ok(encontrado));

        ResponseEntity<Object> respuesta = controller.verificarUsuario(enviado);

        assertEquals(HttpStatus.OK, respuesta.getStatusCode());
        assertEquals("Usuario Prueba", ((Usuario) respuesta.getBody()).getNombre());
    }

    @Test
    @DisplayName("AUT-E03 saveUsuario: datos validos llaman a /addUsuario y redirigen a /login")
    void saveUsuario_datosValidos_redirigeLogin() {
        Usuario nuevo = crearUsuario("Usuario Prueba", "usuario@correo.com", "1234");
        when(template.postForEntity("http://localhost:8081/addUsuario", nuevo, Integer.class))
                .thenReturn(ResponseEntity.ok(1));

        ModelAndView resultado = controller.setSaveUsuario(nuevo);

        assertEquals("redirect:/login", resultado.getViewName());
        verify(template).postForEntity("http://localhost:8081/addUsuario", nuevo, Integer.class);
    }

    @Test
    @DisplayName("AUT-E04 verificarTarea: tarea con nombre, lista y proyecto devuelve 200 con valor 1")
    void verificarTarea_datosValidos() {
        Tareas tarea = new Tareas();
        tarea.setNombre("Tarea prueba");
        tarea.setLista("pendientes");
        tarea.setId_proyecto(1);
        when(template.postForEntity("http://localhost:8081/addTareas", tarea, Integer.class))
                .thenReturn(ResponseEntity.ok(1));

        ResponseEntity<Object> respuesta = controller.verificarTarea(tarea);

        assertEquals(HttpStatus.OK, respuesta.getStatusCode());
        assertEquals(1, respuesta.getBody());
    }

    @Test
    @DisplayName("AUT-E05 getProyecto: la vista proyecto carga la lista de usuarios (3)")
    void vistaProyecto_cargaUsuarios() {
        Usuario[] usuarios = { crearUsuario("A", "a@c.com", "1"), crearUsuario("B", "b@c.com", "2"),
                crearUsuario("C", "c@c.com", "3") };
        when(template.postForObject(eq("http://localhost:8081/buscarUsuarios"), any(), eq(Usuario[].class)))
                .thenReturn(usuarios);
        ExtendedModelMap modelo = new ExtendedModelMap();

        String vista = controller.getProyecto(modelo);

        assertEquals("proyecto", vista);
        assertEquals(3, ((List<?>) modelo.get("listaUsuarios")).size());
    }

    // ---------- Pruebas de error controlado ----------

    @Test
    @DisplayName("AUT-F01 consultarProyectos: backend con error 500 devuelve 503 con mensaje")
    void consultarProyectos_backendError() {
        when(template.postForObject(eq("http://localhost:8081/buscarProyectos"), any(), eq(Proyectos[].class)))
                .thenThrow(new HttpServerErrorException(HttpStatus.INTERNAL_SERVER_ERROR));

        ResponseEntity<Object> respuesta = controller.consultarProyectos(new ExtendedModelMap());

        assertEquals(HttpStatus.SERVICE_UNAVAILABLE, respuesta.getStatusCode());
        assertNotNull(respuesta.getBody());
    }

    @Test
    @DisplayName("AUT-F02 saveUsuario: sin correo ni contrasena no llama al backend y redirige a /register?error")
    void saveUsuario_datosIncompletos() {
        Usuario vacio = new Usuario();

        ModelAndView resultado = controller.setSaveUsuario(vacio);

        assertEquals("redirect:/register?error", resultado.getViewName());
        verifyNoInteractions(template);
    }

    @Test
    @DisplayName("AUT-F03 actualizarProyecto: proyecto inexistente (backend 404) devuelve 404 controlado")
    void actualizarProyecto_noExiste() {
        when(template.getForObject(eq("http://localhost:8081/buscarProyecto?id_proyecto={id_proyecto}"),
                eq(Proyectos.class), anyMap())).thenThrow(new HttpClientErrorException(HttpStatus.NOT_FOUND));

        ResponseEntity<Object> respuesta = controller.updateProyecto(crearProyecto(99999, null));

        assertEquals(HttpStatus.NOT_FOUND, respuesta.getStatusCode());
    }

    @Test
    @DisplayName("AUT-F04 verificarLogin: API caida (conexion rechazada) devuelve 503 con mensaje")
    void verificarLogin_apiCaida() {
        Usuario enviado = crearUsuario(null, "usuario@correo.com", "1234");
        when(template.postForEntity("http://localhost:8081/getUsuarioLogin", enviado, Usuario.class))
                .thenThrow(new ResourceAccessException("Connection refused"));

        ResponseEntity<Object> respuesta = controller.verificarUsuario(enviado);

        assertEquals(HttpStatus.SERVICE_UNAVAILABLE, respuesta.getStatusCode());
        assertNotNull(respuesta.getBody());
    }

    @Test
    @DisplayName("AUT-F05 verificarTarea: backend rechaza la fecha invalida (400) y se propaga 400")
    void verificarTarea_fechaInvalida() {
        Tareas tarea = new Tareas();
        tarea.setNombre("Tarea prueba");
        when(template.postForEntity("http://localhost:8081/addTareas", tarea, Integer.class))
                .thenThrow(new HttpClientErrorException(HttpStatus.BAD_REQUEST));

        ResponseEntity<Object> respuesta = controller.verificarTarea(tarea);

        assertEquals(HttpStatus.BAD_REQUEST, respuesta.getStatusCode());
    }

    @Test
    @DisplayName("AUT-F06 actualizarProyecto: el backend responde 200 con objeto vacio y se devuelve 404")
    void actualizarProyecto_objetoVacio() {
        when(template.getForObject(eq("http://localhost:8081/buscarProyecto?id_proyecto={id_proyecto}"),
                eq(Proyectos.class), anyMap())).thenReturn(new Proyectos());

        ResponseEntity<Object> respuesta = controller.updateProyecto(crearProyecto(99999, null));

        assertEquals(HttpStatus.NOT_FOUND, respuesta.getStatusCode());
    }
}
