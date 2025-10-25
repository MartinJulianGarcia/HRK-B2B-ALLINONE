package com.hrk.tienda_b2b;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Arrays;

@SpringBootApplication
@RestController
@Slf4j
@ComponentScan(basePackages = "com.hrk.tienda_b2b")
public class TiendaB2bHrkApplication {
	public static void main(String[] args) {
		SpringApplication.run(TiendaB2bHrkApplication.class, args);
	}
	
	@Bean
	public CommandLineRunner commandLineRunner(ApplicationContext ctx) {
		return args -> {
			log.info("🔍 ============== BEANS DETECTADOS ==============");
			String[] beanNames = ctx.getBeanDefinitionNames();
			Arrays.sort(beanNames);
			for (String beanName : beanNames) {
				if (beanName.toLowerCase().contains("controller") || 
				    beanName.toLowerCase().contains("auth") ||
				    beanName.toLowerCase().contains("password")) {
					log.info("🟢 Bean encontrado: {}", beanName);
				}
			}
			log.info("🔍 ============================================");
		};
	}
	
	@GetMapping("/test")
	public String test() {
		log.info("🟢 Endpoint /test llamado desde la clase principal - DEVOLVIENDO RESPUESTA");
		String respuesta = "Funcionando desde la clase principal - " + System.currentTimeMillis();
		log.info("🟢 Respuesta: {}", respuesta);
		return respuesta;
	}
	
	@GetMapping("/ping")
	public String ping() {
		log.info("🟢 Endpoint /ping llamado desde la clase principal - DEVOLVIENDO RESPUESTA");
		String respuesta = "PONG - Backend funcionando desde clase principal - " + System.currentTimeMillis();
		log.info("🟢 Respuesta: {}", respuesta);
		return respuesta;
	}
}