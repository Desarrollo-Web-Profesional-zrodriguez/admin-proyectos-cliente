package com.mx.cbtis.controller;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.servlet.ModelAndView;

import com.mx.cbtis.modelo.Comentarios;
import com.mx.cbtis.modelo.Proyectos;
import com.mx.cbtis.modelo.Tareas;
import com.mx.cbtis.modelo.Usuario;

@Controller
public class GestorController {

	private RestTemplate template;

	public GestorController() {
		this.template = new RestTemplate();
	}

	@Autowired
	public GestorController(RestTemplate template) {
		this.template = template;
	}

	@GetMapping("/")
	public String getHome() {
		return "home";
	}

	@GetMapping("/tutorial")
	public String getTutorial() {
		return "tutorial";
	}

	@GetMapping("/manual")
	public String getManual() {
		return "manual";
	}

	@GetMapping("/login")
	public String getLogin() {
		return "login";
	}

	@GetMapping("/register")
	public String getRegister(Model modelo) {
		Usuario usuario = new Usuario();
		modelo.addAttribute("usuario", usuario);
		return "register";
	}

	@PostMapping("/saveUsuario")
	public ModelAndView setSaveUsuario(@ModelAttribute Usuario usuario) {
		if (usuario == null || usuario.getCorreo_electronico() == null || usuario.getCorreo_electronico().trim().isEmpty()
				|| usuario.getContrasena() == null || usuario.getContrasena().trim().isEmpty()) {
			return new ModelAndView("redirect:/register?error");
		}
		try {
			String urlservicebd = "http://localhost:8081/addUsuario";
			template.postForEntity(urlservicebd, usuario, Integer.class);
			return new ModelAndView("redirect:/login");
		} catch (Exception e) {
			return new ModelAndView("redirect:/register?error");
		}
	}

	@GetMapping("/inicio")
	public String getInicio(Model modelo) {
		Proyectos proyectos = new Proyectos();
		modelo.addAttribute("proyectos", proyectos);
		return "inicio";
	}

	@PostMapping("/saveProyecto")
	public ModelAndView setSaveProyecto(@ModelAttribute Proyectos proyecto) {
		String urlservicebd = "http://localhost:8081/addProyecto";
		template.postForEntity(urlservicebd, proyecto, Integer.class);
		return new ModelAndView("redirect:/inicio");
	}

	@PostMapping("/consultarProyectos")
	public ResponseEntity<Object> consultarProyectos(Model modelo) {
		try {
			String urlservicebd = "http://localhost:8081/buscarProyectos";
			HttpHeaders headers = new HttpHeaders();
			headers.setContentType(MediaType.APPLICATION_JSON);
			Proyectos[] response = template.postForObject(urlservicebd, headers, Proyectos[].class);
			List<Proyectos> proyectos = (response != null) ? Arrays.asList(response) : Collections.emptyList();
			if (modelo != null) {
				modelo.addAttribute("listaProyectos", proyectos);
			}
			return new ResponseEntity<Object>(proyectos, HttpStatus.OK);
		} catch (HttpServerErrorException e) {
			return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body("Servicio no disponible");
		}
	}

	@PostMapping("/eliminarProyecto")
	public ModelAndView eliminarProyecto(@RequestBody Proyectos proyecto) {
		String urlservicebd = "http://localhost:8081/deleteProyecto";
		HttpEntity<Proyectos> request = new HttpEntity<Proyectos>(proyecto);
		template.exchange(urlservicebd, HttpMethod.DELETE, request, Integer.class);
		return new ModelAndView("redirect:/inicio");
	}

	@PostMapping("/actualizarProyecto")
	public ResponseEntity<Object> updateProyecto(@RequestBody Proyectos proyecto) {
		try {
			Map<String, Integer> params = new HashMap<String, Integer>();
			params.put("id_proyecto", proyecto.getId_proyecto());
			String urlservicebd = "http://localhost:8081/buscarProyecto?id_proyecto={id_proyecto}";
			Proyectos proyectos = template.getForObject(urlservicebd, Proyectos.class, params);
			if (proyectos == null || proyectos.getId_proyecto() == null) {
				return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
			}
			return new ResponseEntity<Object>(proyectos, HttpStatus.OK);
		} catch (HttpClientErrorException e) {
			return ResponseEntity.status(e.getStatusCode()).build();
		}
	}

	@PostMapping("/verificarLogin")
	public ResponseEntity<Object> verificarUsuario(@RequestBody Usuario paramUser) {
		try {
			String urlservicebd = "http://localhost:8081/getUsuarioLogin";
			ResponseEntity<Usuario> response = template.postForEntity(urlservicebd, paramUser, Usuario.class);
			return new ResponseEntity<>(response.getBody(), response.getStatusCode());
		} catch (ResourceAccessException e) {
			return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body("Error de conexion");
		} catch (HttpClientErrorException e) {
			return ResponseEntity.status(e.getStatusCode()).build();
		}
	}

	@PostMapping("/consultarUsuarios")
	public ResponseEntity<Object> consultarUsuarios(Model modelo) {
		String urlservicebd = "http://localhost:8081/buscarUsuarios";
		HttpHeaders headers = new HttpHeaders();
		headers.setContentType(MediaType.APPLICATION_JSON);
		Usuario[] response = template.postForObject(urlservicebd, headers, Usuario[].class);
		List<Usuario> usuarios = (response != null) ? Arrays.asList(response) : Collections.emptyList();
		if (modelo != null) {
			modelo.addAttribute("listaUsuarios", usuarios);
		}
		return new ResponseEntity<Object>(usuarios, HttpStatus.OK);
	}

	@PostMapping("/obtenerTareas")
	public ResponseEntity<List<Tareas>> obtenerTareas(@RequestBody Tareas idProyecto, Model modelo) {
		String urlservicebd = "http://localhost:8081/getTarea";
		HttpHeaders headers = new HttpHeaders();
		headers.setContentType(MediaType.APPLICATION_JSON);
		Tareas[] response = template.postForObject(urlservicebd, idProyecto, Tareas[].class);
		List<Tareas> listaTareas = (response != null) ? Arrays.asList(response) : Collections.emptyList();
		if (modelo != null) {
			modelo.addAttribute("tareas", listaTareas);
		}
		return ResponseEntity.ok(listaTareas);
	}

	@PostMapping("/obtenerComentarios")
	public ResponseEntity<List<Comentarios>> obtenerComentarios(@RequestBody Comentarios idComentario, Model modelo) {
		String urlservicebd = "http://localhost:8081/getComentario";
		HttpHeaders headers = new HttpHeaders();
		headers.setContentType(MediaType.APPLICATION_JSON);
		Comentarios[] response = template.postForObject(urlservicebd, idComentario, Comentarios[].class);
		List<Comentarios> listaComentarios = (response != null) ? Arrays.asList(response) : Collections.emptyList();
		if (modelo != null) {
			modelo.addAttribute("comentarios", listaComentarios);
		}
		return ResponseEntity.ok(listaComentarios);
	}

	@GetMapping("/proyecto")
	public String getProyecto(Model modelo) {
		Tareas tarea = new Tareas();
		modelo.addAttribute("tarea", tarea);

		Comentarios comentario = new Comentarios();
		modelo.addAttribute("comentarios", comentario);

		HttpHeaders headers = new HttpHeaders();
		headers.setContentType(MediaType.APPLICATION_JSON);

		String urlservicebdUsuario = "http://localhost:8081/buscarUsuarios";
		Usuario[] responseUsuario = template.postForObject(urlservicebdUsuario, headers, Usuario[].class);
		List<Usuario> usuarios = (responseUsuario != null) ? Arrays.asList(responseUsuario) : Collections.emptyList();
		modelo.addAttribute("listaUsuarios", usuarios);

		return "proyecto";
	}

	@GetMapping("/tarea")
	public String getTarea(Model modelo) {
		Tareas tareas = new Tareas();
		modelo.addAttribute("tareas", tareas);
		return "proyecto";
	}

	@PostMapping("/saveTarea")
	public ModelAndView setSaveTareas(@ModelAttribute Tareas tareas,
			@RequestParam(value = "id_proyecto", required = false) Integer idProyectoParam) {
		if (tareas.getId_proyecto() == null && idProyectoParam != null) {
			tareas.setId_proyecto(idProyectoParam);
		}
		if (tareas.getFecha_vencimiento() != null && tareas.getFecha_vencimiento().trim().isEmpty()) {
			tareas.setFecha_vencimiento(null);
		}
		String urlservicebd = "http://localhost:8081/addTareas";
		template.postForEntity(urlservicebd, tareas, Integer.class);

		ModelAndView mav = new ModelAndView("redirect:/proyecto");
		if (tareas.getId_proyecto() != null) {
			mav.addObject("id_proyecto", tareas.getId_proyecto());
		}
		return mav;
	}

	@PostMapping("/saveComentario")
	@org.springframework.web.bind.annotation.ResponseBody
	public ResponseEntity<Integer> setSaveComentario(@RequestBody Comentarios comentarios) {
		try {
			String urlservicebd = "http://localhost:8081/addComentario";
			ResponseEntity<Integer> response = template.postForEntity(urlservicebd, comentarios, Integer.class);
			return response;
		} catch (Exception e) {
			System.err.println("Error controlado al guardar comentario en cliente: " + e.getMessage());
			return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).build();
		}
	}

	@PostMapping("/actualizarTarea")
	public ResponseEntity<Object> updateTarea(@RequestBody Tareas tarea) {
		Map<String, Integer> params = new HashMap<String, Integer>();
		params.put("id_tarea", tarea.getId_tarea());
		String urlservicebd = "http://localhost:8081/buscarTarea?id_tarea={id_tarea}";
		Tareas tareas = template.getForObject(urlservicebd, Tareas.class, params);
		return new ResponseEntity<Object>(tareas, HttpStatus.OK);
	}

	@PostMapping("/eliminarTarea")
	public ModelAndView eliminarTarea(@RequestBody Tareas tareas) {
		String urlservicebd = "http://localhost:8081/deleteTareas";
		HttpEntity<Tareas> request = new HttpEntity<Tareas>(tareas);
		template.exchange(urlservicebd, HttpMethod.DELETE, request, Integer.class);
		return new ModelAndView("redirect:/proyecto");
	}

	@PostMapping("/verificarTarea")
	public ResponseEntity<Integer> verificarTarea(@RequestBody Tareas paramName) {
		String urlservicebd = "http://localhost:8081/addTareas";
		ResponseEntity<Integer> response = template.postForEntity(urlservicebd, paramName, Integer.class);
		return response;
	}

	@PostMapping("/cambiosTareas")
	public ResponseEntity<Integer> cambiosTareas(@RequestBody Tareas paramDatos) {
		String urlservicebd = "http://localhost:8081/addTareas";
		ResponseEntity<Integer> response = template.postForEntity(urlservicebd, paramDatos, Integer.class);
		return response;
	}

	@PostMapping("/consultarTareas")
	public ResponseEntity<Object> consultarTareas() {
		String urlservicebd = "http://localhost:8081/buscarTareas";
		HttpHeaders headers = new HttpHeaders();
		headers.setContentType(MediaType.APPLICATION_JSON);
		Tareas[] response = template.postForObject(urlservicebd, headers, Tareas[].class);
		List<Tareas> tareas = (response != null) ? Arrays.asList(response) : Collections.emptyList();
		return new ResponseEntity<Object>(tareas, HttpStatus.OK);
	}

	@PostMapping("/consultarComentarios")
	public ResponseEntity<Object> consultarComentarios() {
		String urlservicebd = "http://localhost:8081/buscarComentarios";
		HttpHeaders headers = new HttpHeaders();
		headers.setContentType(MediaType.APPLICATION_JSON);
		Comentarios[] response = template.postForObject(urlservicebd, headers, Comentarios[].class);
		List<Comentarios> comentarios = (response != null) ? Arrays.asList(response) : Collections.emptyList();
		return new ResponseEntity<Object>(comentarios, HttpStatus.OK);
	}
}
