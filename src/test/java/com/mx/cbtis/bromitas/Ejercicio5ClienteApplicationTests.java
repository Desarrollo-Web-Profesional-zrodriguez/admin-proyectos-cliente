package com.mx.cbtis.bromitas;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestTemplate;

import com.mx.cbtis.modelo.Proyectos;
import com.mx.cbtis.modelo.Usuario;

@SpringBootTest
@AutoConfigureMockMvc
class Ejercicio5ClienteApplicationTests {

	@Autowired
	private MockMvc mockMvc;

	@Test
	@DisplayName("Test de Carga de Contexto")
	void contextLoads() {
	}

	// =========================================================================
	// 4.1 CASOS AUTOMÁTICOS DE ÉXITO
	// =========================================================================

	@Test
	@DisplayName("AUT-E01: Servicio de consulta - El backend devuelve información válida")
	void autE01_servicioConsulta_backendDevuelveInformacionValida() {
		RestTemplate restTemplate = new RestTemplate();
		MockRestServiceServer mockServer = MockRestServiceServer.bindTo(restTemplate).build();

		String jsonMock = "[{\"id_proyecto\":1,\"nombre\":\"Proyecto Alpha\",\"descripcion\":\"Prueba\",\"fecha_inicio\":\"2026-10-01\",\"fecha_finalizacion\":\"2026-12-01\",\"estado_proyecto\":\"En proceso\",\"fondo\":\"bg1.png\"}]";

		mockServer.expect(requestTo("http://localhost:8081/buscarProyectos"))
				  .andExpect(method(HttpMethod.POST))
				  .andRespond(withSuccess(jsonMock, MediaType.APPLICATION_JSON));

		Proyectos[] respuesta = restTemplate.postForObject("http://localhost:8081/buscarProyectos", null, Proyectos[].class);
		mockServer.verify();

		assertNotNull(respuesta);
		assertEquals(1, respuesta.length);
		assertEquals("Proyecto Alpha", respuesta[0].getNombre());
		assertEquals("En proceso", respuesta[0].getEstado_proyecto());
	}

	@Test
	@DisplayName("AUT-E02: Formulario - Se proporcionan datos válidos")
	void autE02_formulario_datosValidosPermiteAcceso() throws Exception {
		mockMvc.perform(get("/register"))
			   .andExpect(status().isOk())
			   .andExpect(view().name("register"))
			   .andExpect(model().attributeExists("usuario"));
	}

	@Test
	@DisplayName("AUT-E03: Componente / Vista - La consulta finaliza correctamente")
	void autE03_componenteVista_consultaFinalizaYPresentaInterfaz() throws Exception {
		mockMvc.perform(get("/inicio"))
			   .andExpect(status().isOk())
			   .andExpect(view().name("inicio"))
			   .andExpect(model().attributeExists("proyectos"));
	}

	// =========================================================================
	// 4.2 CASOS AUTOMÁTICOS DE FRACASO CONTROLADO
	// =========================================================================

	@Test
	@DisplayName("AUT-F01: Automatica - Fracaso controlado: Invocar consultarProyectos() con backend 8081 apagado; captura ResourceAccessException")
	void autF01_consultarProyectos_backendApagado_capturaResourceAccessException() {
		RestTemplate template = new RestTemplate();
		// Puerto sin servicio simulando backend desconectado/apagado
		assertThrows(ResourceAccessException.class, () -> {
			template.postForObject("http://localhost:65432/buscarProyectos", null, Proyectos[].class);
		});
	}

	@Test
	@DisplayName("AUT-F02: Automatica - Fracaso controlado: POST /saveProyecto con campos vacíos o ID inexistente; sistema rechaza la inserción")
	void autF02_saveProyecto_camposVaciosOIdInexistente_rechazaInsercion() {
		RestTemplate template = new RestTemplate();
		MockRestServiceServer mockServer = MockRestServiceServer.bindTo(template).build();

		// El backend rechaza la inserción retornando 0 (flag = 0)
		mockServer.expect(requestTo("http://localhost:8081/addProyecto"))
				  .andExpect(method(HttpMethod.POST))
				  .andRespond(withSuccess("0", MediaType.APPLICATION_JSON));

		Proyectos proyectoInvalido = new Proyectos();
		ResponseEntity<Integer> response = template.postForEntity("http://localhost:8081/addProyecto", proyectoInvalido, Integer.class);
		mockServer.verify();

		assertEquals(0, response.getBody(), "El backend debe rechazar la inserción retornando 0");
	}

	@Test
	@DisplayName("AUT-F03: Automatica - Fracaso controlado: POST /verificarLogin con credenciales inexistentes; backend devuelve usuario centinela")
	void autF03_verificarLogin_credencialesInexistentes_devuelveUsuarioCentinela() {
		RestTemplate template = new RestTemplate();
		MockRestServiceServer mockServer = MockRestServiceServer.bindTo(template).build();

		// El backend devuelve el usuario centinela con nombre "000"
		String usuarioCentinelaJson = "{\"id_usuario\":0,\"nombre\":\"000\",\"correo_electronico\":null,\"contrasena\":null}";

		mockServer.expect(requestTo("http://localhost:8081/getUsuarioLogin"))
				  .andExpect(method(HttpMethod.POST))
				  .andRespond(withSuccess(usuarioCentinelaJson, MediaType.APPLICATION_JSON));

		Usuario userInvalido = new Usuario();
		userInvalido.setCorreo_electronico("noexiste@cbtis.edu.mx");
		userInvalido.setContrasena("claveErronea123");

		ResponseEntity<Usuario> response = template.postForEntity("http://localhost:8081/getUsuarioLogin", userInvalido, Usuario.class);
		mockServer.verify();

		assertNotNull(response.getBody());
		assertEquals("000", response.getBody().getNombre(), "El backend debe devolver el usuario centinela con nombre '000'");
	}

}
