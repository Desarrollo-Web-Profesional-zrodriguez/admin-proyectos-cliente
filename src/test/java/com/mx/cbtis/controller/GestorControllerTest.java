package com.mx.cbtis.controller;

import static org.hamcrest.Matchers.hasSize;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrlPattern;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

import java.util.Locale;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.servlet.View;
import org.springframework.web.servlet.ViewResolver;
import org.springframework.web.servlet.view.RedirectView;

import com.mx.cbtis.modelo.Comentarios;
import com.mx.cbtis.modelo.Proyectos;
import com.mx.cbtis.modelo.Tareas;
import com.mx.cbtis.modelo.Usuario;

import jakarta.servlet.ServletException;

/**
 * Pruebas unitarias de GestorController (cliente).
 * El backend se simula con Mockito (RestTemplate mock), por lo que NO se
 * necesita levantar el backend ni la base de datos para ejecutarlas.
 *
 * Ubicacion: Ejercicio5Cliente/src/test/java/com/mx/cbtis/controller/GestorControllerTest.java
 */
@ExtendWith(MockitoExtension.class)
class GestorControllerTest {

    private static final String API = "http://localhost:8081";
    private static final String JSON_USUARIO = "{\"nombre\":\"Ana\",\"correo_electronico\":\"ana22533@cbtis75.edu.mx\",\"contrasena\":\"0722\"}";
    private static final String JSON_PROYECTO = "{\"id_proyecto\":197}";
    private static final String JSON_TAREA = "{\"id_tarea\":199,\"id_proyecto\":197}";
    private static final String JSON_COMENTARIO = "{\"id_tarea\":199}";

    @Mock
    private RestTemplate restTemplate;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders
                .standaloneSetup(new GestorController(restTemplate))
                .setViewResolvers(new StubViewResolver())
                .build();
    }

    /** Resuelve vistas sin necesitar Thymeleaf: solo importa el NOMBRE de la vista. */
    private static class StubViewResolver implements ViewResolver {
        @Override
        public View resolveViewName(String viewName, Locale locale) {
            if (viewName.startsWith("redirect:")) {
                return new RedirectView(viewName.substring("redirect:".length()), true);
            }
            return (model, request, response) -> response.setStatus(200);
        }
    }

    // ------------------------------------------------------------ Vistas

    @Test
    @DisplayName("UT-01 GET / devuelve la vista home")
    void getHome_devuelveVistaHome() throws Exception {
        mockMvc.perform(get("/"))
                .andExpect(status().isOk())
                .andExpect(view().name("home"));
    }

    @Test
    @DisplayName("UT-02 GET /login devuelve la vista login")
    void getLogin_devuelveVistaLogin() throws Exception {
        mockMvc.perform(get("/login"))
                .andExpect(status().isOk())
                .andExpect(view().name("login"));
    }

    @Test
    @DisplayName("UT-03 GET /tutorial devuelve la vista tutorial")
    void getTutorial_devuelveVistaTutorial() throws Exception {
        mockMvc.perform(get("/tutorial"))
                .andExpect(status().isOk())
                .andExpect(view().name("tutorial"));
    }

    @Test
    @DisplayName("UT-04 GET /manual devuelve la vista manual")
    void getManual_devuelveVistaManual() throws Exception {
        mockMvc.perform(get("/manual"))
                .andExpect(status().isOk())
                .andExpect(view().name("manual"));
    }

    @Test
    @DisplayName("UT-05 GET /register devuelve la vista register con un Usuario vacio en el modelo")
    void getRegister_agregaUsuarioAlModelo() throws Exception {
        mockMvc.perform(get("/register"))
                .andExpect(status().isOk())
                .andExpect(view().name("register"))
                .andExpect(model().attributeExists("usuario"));
    }

    @Test
    @DisplayName("UT-06 GET /inicio devuelve la vista inicio con un Proyecto vacio en el modelo")
    void getInicio_agregaProyectosAlModelo() throws Exception {
        mockMvc.perform(get("/inicio"))
                .andExpect(status().isOk())
                .andExpect(view().name("inicio"))
                .andExpect(model().attributeExists("proyectos"));
    }

    @Test
    @DisplayName("UT-07 GET /proyecto carga la vista proyecto con la lista de usuarios del backend")
    void getProyecto_cargaUsuariosDelBackend() throws Exception {
        when(restTemplate.postForObject(anyString(), any(), eq(Usuario[].class)))
                .thenReturn(new Usuario[] { new Usuario() });

        mockMvc.perform(get("/proyecto"))
                .andExpect(status().isOk())
                .andExpect(view().name("proyecto"))
                .andExpect(model().attributeExists("tarea", "comentarios", "listaUsuarios"));

        verify(restTemplate).postForObject(eq(API + "/buscarUsuarios"), any(), eq(Usuario[].class));
    }

    @Test
    @DisplayName("UT-08 GET /tarea devuelve la vista proyecto con una Tarea vacia en el modelo")
    void getTarea_agregaTareaAlModelo() throws Exception {
        mockMvc.perform(get("/tarea"))
                .andExpect(status().isOk())
                .andExpect(view().name("proyecto"))
                .andExpect(model().attributeExists("tareas"));
    }

    // ----------------------------------------------------------- Usuarios

    @Test
    @DisplayName("UT-09 POST /saveUsuario guarda en el backend y redirige a /login")
    void saveUsuario_redirigeALogin() throws Exception {
        when(restTemplate.postForEntity(anyString(), any(), eq(Integer.class)))
                .thenReturn(ResponseEntity.ok(1));

        mockMvc.perform(post("/saveUsuario")
                        .param("nombre", "Ana")
                        .param("correo_electronico", "ana22533@cbtis75.edu.mx")
                        .param("contrasena", "0722"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/login"));

        verify(restTemplate).postForEntity(eq(API + "/addUsuario"), any(Usuario.class), eq(Integer.class));
    }

    @Test
    @DisplayName("UT-10 POST /saveUsuario con backend caido redirige a /register?error")
    void saveUsuario_backendCaido_redirigeConError() throws Exception {
        when(restTemplate.postForEntity(anyString(), any(), eq(Integer.class)))
                .thenThrow(new RestClientException("Backend no disponible"));

        mockMvc.perform(post("/saveUsuario")
                        .param("nombre", "Ana")
                        .param("correo_electronico", "ana22533@cbtis75.edu.mx")
                        .param("contrasena", "0722"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/register?error"));
    }

    @Test
    @DisplayName("UT-17 POST /verificarLogin con credenciales validas devuelve 200 y el usuario")
    void verificarLogin_credencialesValidas() throws Exception {
        when(restTemplate.postForEntity(anyString(), any(), eq(Usuario.class)))
                .thenReturn(ResponseEntity.ok(new Usuario()));

        mockMvc.perform(post("/verificarLogin")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(JSON_USUARIO))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON));

        verify(restTemplate).postForEntity(eq(API + "/getUsuarioLogin"), any(Usuario.class), eq(Usuario.class));
    }

    @Test
    @DisplayName("UT-18 POST /verificarLogin con credenciales incorrectas (401) devuelve 401")
    void verificarLogin_credencialesIncorrectas_devuelve401() throws Exception {
        when(restTemplate.postForEntity(anyString(), any(), eq(Usuario.class)))
                .thenThrow(new HttpClientErrorException(HttpStatus.UNAUTHORIZED));

        mockMvc.perform(post("/verificarLogin")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(JSON_USUARIO))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("UT-19 POST /consultarUsuarios devuelve la lista de usuarios")
    void consultarUsuarios_devuelveLista() throws Exception {
        when(restTemplate.postForObject(anyString(), any(), eq(Usuario[].class)))
                .thenReturn(new Usuario[] { new Usuario(), new Usuario() });

        mockMvc.perform(post("/consultarUsuarios"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)));

        verify(restTemplate).postForObject(eq(API + "/buscarUsuarios"), any(), eq(Usuario[].class));
    }

    @Test
    @DisplayName("UT-20 POST /consultarUsuarios sin usuarios devuelve lista vacia")
    void consultarUsuarios_sinUsuarios_devuelveListaVacia() throws Exception {
        when(restTemplate.postForObject(anyString(), any(), eq(Usuario[].class)))
                .thenReturn(new Usuario[] {});

        mockMvc.perform(post("/consultarUsuarios"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(0)));
    }

    // ---------------------------------------------------------- Proyectos

    @Test
    @DisplayName("UT-11 POST /saveProyecto guarda en el backend y redirige a /inicio")
    void saveProyecto_redirigeAInicio() throws Exception {
        when(restTemplate.postForEntity(anyString(), any(), eq(Integer.class)))
                .thenReturn(ResponseEntity.ok(1));

        mockMvc.perform(post("/saveProyecto").param("nombre", "My API Rest"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/inicio"));

        verify(restTemplate).postForEntity(eq(API + "/addProyecto"), any(Proyectos.class), eq(Integer.class));
    }

    @Test
    @DisplayName("UT-12 POST /saveProyecto con backend caido lanza error")
    void saveProyecto_backendCaido_lanzaError() {
        when(restTemplate.postForEntity(anyString(), any(), eq(Integer.class)))
                .thenThrow(new RestClientException("Backend no disponible"));

        assertThrows(ServletException.class,
                () -> mockMvc.perform(post("/saveProyecto").param("nombre", "My API Rest")));
    }

    @Test
    @DisplayName("UT-13 POST /consultarProyectos devuelve la lista de proyectos")
    void consultarProyectos_devuelveLista() throws Exception {
        when(restTemplate.postForObject(anyString(), any(), eq(Proyectos[].class)))
                .thenReturn(new Proyectos[] { new Proyectos() });

        mockMvc.perform(post("/consultarProyectos"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)));

        verify(restTemplate).postForObject(eq(API + "/buscarProyectos"), any(), eq(Proyectos[].class));
    }

    @Test
    @DisplayName("UT-14 POST /consultarProyectos sin proyectos devuelve lista vacia")
    void consultarProyectos_sinProyectos_devuelveListaVacia() throws Exception {
        when(restTemplate.postForObject(anyString(), any(), eq(Proyectos[].class)))
                .thenReturn(new Proyectos[] {});

        mockMvc.perform(post("/consultarProyectos"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(0)));
    }

    @Test
    @DisplayName("UT-15 POST /eliminarProyecto llama DELETE al backend y redirige a /inicio")
    void eliminarProyecto_redirigeAInicio() throws Exception {
        when(restTemplate.exchange(anyString(), eq(HttpMethod.DELETE), any(HttpEntity.class), eq(Integer.class)))
                .thenReturn(ResponseEntity.ok(1));

        mockMvc.perform(post("/eliminarProyecto")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(JSON_PROYECTO))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/inicio"));

        verify(restTemplate).exchange(eq(API + "/deleteProyecto"), eq(HttpMethod.DELETE),
                any(HttpEntity.class), eq(Integer.class));
    }

    @Test
    @DisplayName("UT-16 POST /actualizarProyecto busca el proyecto por id y devuelve 200")
    void actualizarProyecto_devuelveProyecto() throws Exception {
        Proyectos existente = new Proyectos();
        existente.setId_proyecto(197);
        when(restTemplate.getForObject(anyString(), eq(Proyectos.class), anyMap()))
                .thenReturn(existente);

        mockMvc.perform(post("/actualizarProyecto")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(JSON_PROYECTO))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON));

        verify(restTemplate).getForObject(eq(API + "/buscarProyecto?id_proyecto={id_proyecto}"),
                eq(Proyectos.class), anyMap());
    }

    // ------------------------------------------------------------- Tareas

    @Test
    @DisplayName("UT-21 POST /obtenerTareas devuelve las tareas del proyecto")
    void obtenerTareas_devuelveLista() throws Exception {
        when(restTemplate.postForObject(anyString(), any(), eq(Tareas[].class)))
                .thenReturn(new Tareas[] { new Tareas() });

        mockMvc.perform(post("/obtenerTareas")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(JSON_TAREA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)));

        verify(restTemplate).postForObject(eq(API + "/getTarea"), any(Tareas.class), eq(Tareas[].class));
    }

    @Test
    @DisplayName("UT-22 POST /obtenerTareas con backend caido lanza error")
    void obtenerTareas_backendCaido_lanzaError() {
        when(restTemplate.postForObject(anyString(), any(), eq(Tareas[].class)))
                .thenThrow(new RestClientException("Backend no disponible"));

        assertThrows(ServletException.class,
                () -> mockMvc.perform(post("/obtenerTareas")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(JSON_TAREA)));
    }

    @Test
    @DisplayName("UT-24 POST /saveTarea guarda en el backend y redirige a /proyecto")
    void saveTarea_redirigeAProyecto() throws Exception {
        when(restTemplate.postForEntity(anyString(), any(), eq(Integer.class)))
                .thenReturn(ResponseEntity.ok(1));

        mockMvc.perform(post("/saveTarea")
                        .param("nombre", "Base de datos")
                        .param("id_proyecto", "197")
                        .param("fecha_vencimiento", "2026-12-31"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrlPattern("/proyecto*"));

        verify(restTemplate).postForEntity(eq(API + "/addTareas"), any(Tareas.class), eq(Integer.class));
    }

    @Test
    @DisplayName("UT-26 POST /actualizarTarea busca la tarea por id y devuelve 200")
    void actualizarTarea_devuelveTarea() throws Exception {
        when(restTemplate.getForObject(anyString(), eq(Tareas.class), anyMap()))
                .thenReturn(new Tareas());

        mockMvc.perform(post("/actualizarTarea")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(JSON_TAREA))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON));

        verify(restTemplate).getForObject(eq(API + "/buscarTarea?id_tarea={id_tarea}"),
                eq(Tareas.class), anyMap());
    }

    @Test
    @DisplayName("UT-27 POST /eliminarTarea llama DELETE al backend y redirige a /proyecto")
    void eliminarTarea_redirigeAProyecto() throws Exception {
        when(restTemplate.exchange(anyString(), eq(HttpMethod.DELETE), any(HttpEntity.class), eq(Integer.class)))
                .thenReturn(ResponseEntity.ok(1));

        mockMvc.perform(post("/eliminarTarea")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(JSON_TAREA))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/proyecto"));

        verify(restTemplate).exchange(eq(API + "/deleteTareas"), eq(HttpMethod.DELETE),
                any(HttpEntity.class), eq(Integer.class));
    }

    @Test
    @DisplayName("UT-28 POST /eliminarTarea con backend caido lanza error")
    void eliminarTarea_backendCaido_lanzaError() {
        when(restTemplate.exchange(anyString(), eq(HttpMethod.DELETE), any(HttpEntity.class), eq(Integer.class)))
                .thenThrow(new RestClientException("Backend no disponible"));

        assertThrows(ServletException.class,
                () -> mockMvc.perform(post("/eliminarTarea")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(JSON_TAREA)));
    }

    @Test
    @DisplayName("UT-29 POST /verificarTarea guarda la tarea y devuelve el resultado del backend")
    void verificarTarea_devuelveResultado() throws Exception {
        when(restTemplate.postForEntity(anyString(), any(), eq(Integer.class)))
                .thenReturn(ResponseEntity.ok(1));

        mockMvc.perform(post("/verificarTarea")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(JSON_TAREA))
                .andExpect(status().isOk())
                .andExpect(content().string("1"));

        verify(restTemplate).postForEntity(eq(API + "/addTareas"), any(Tareas.class), eq(Integer.class));
    }

    @Test
    @DisplayName("UT-30 POST /cambiosTareas guarda los cambios y devuelve el resultado del backend")
    void cambiosTareas_devuelveResultado() throws Exception {
        when(restTemplate.postForEntity(anyString(), any(), eq(Integer.class)))
                .thenReturn(ResponseEntity.ok(1));

        mockMvc.perform(post("/cambiosTareas")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(JSON_TAREA))
                .andExpect(status().isOk())
                .andExpect(content().string("1"));

        verify(restTemplate).postForEntity(eq(API + "/addTareas"), any(Tareas.class), eq(Integer.class));
    }

    @Test
    @DisplayName("UT-31 POST /consultarTareas devuelve la lista de tareas")
    void consultarTareas_devuelveLista() throws Exception {
        when(restTemplate.postForObject(anyString(), any(), eq(Tareas[].class)))
                .thenReturn(new Tareas[] { new Tareas() });

        mockMvc.perform(post("/consultarTareas"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)));

        verify(restTemplate).postForObject(eq(API + "/buscarTareas"), any(), eq(Tareas[].class));
    }

    @Test
    @DisplayName("UT-32 POST /consultarTareas sin tareas devuelve lista vacia")
    void consultarTareas_sinTareas_devuelveListaVacia() throws Exception {
        when(restTemplate.postForObject(anyString(), any(), eq(Tareas[].class)))
                .thenReturn(new Tareas[] {});

        mockMvc.perform(post("/consultarTareas"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(0)));
    }

    // -------------------------------------------------------- Comentarios

    @Test
    @DisplayName("UT-23 POST /obtenerComentarios devuelve los comentarios de la tarea")
    void obtenerComentarios_devuelveLista() throws Exception {
        when(restTemplate.postForObject(anyString(), any(), eq(Comentarios[].class)))
                .thenReturn(new Comentarios[] { new Comentarios(), new Comentarios() });

        mockMvc.perform(post("/obtenerComentarios")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(JSON_COMENTARIO))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)));

        verify(restTemplate).postForObject(eq(API + "/getComentario"), any(Comentarios.class),
                eq(Comentarios[].class));
    }

    @Test
    @DisplayName("UT-25 POST /saveComentario guarda el comentario y devuelve el resultado del backend")
    void saveComentario_devuelveResultado() throws Exception {
        when(restTemplate.postForEntity(anyString(), any(), eq(Integer.class)))
                .thenReturn(ResponseEntity.ok(1));

        mockMvc.perform(post("/saveComentario")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(JSON_COMENTARIO))
                .andExpect(status().isOk())
                .andExpect(content().string("1"));

        verify(restTemplate).postForEntity(eq(API + "/addComentario"), any(Comentarios.class), eq(Integer.class));
    }

    @Test
    @DisplayName("UT-39 POST /saveComentario con backend caido devuelve 503")
    void saveComentario_backendCaido_devuelve503() throws Exception {
        when(restTemplate.postForEntity(anyString(), any(), eq(Integer.class)))
                .thenThrow(new RestClientException("Backend no disponible"));

        mockMvc.perform(post("/saveComentario")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(JSON_COMENTARIO))
                .andExpect(status().isServiceUnavailable());
    }

    @Test
    @DisplayName("UT-33 POST /consultarComentarios devuelve la lista de comentarios")
    void consultarComentarios_devuelveLista() throws Exception {
        when(restTemplate.postForObject(anyString(), any(), eq(Comentarios[].class)))
                .thenReturn(new Comentarios[] { new Comentarios() });

        mockMvc.perform(post("/consultarComentarios"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)));

        verify(restTemplate).postForObject(eq(API + "/buscarComentarios"), any(), eq(Comentarios[].class));
    }
}
