package com.service.pedidos;

import org.springframework.amqp.rabbit.annotation.EnableRabbit;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Bean;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import java.io.IOException;
import java.util.List;

@EnableRabbit
@EnableCaching
@SpringBootApplication
public class PedidosApplication {

	public static void main(String[] args) throws IOException {
		SpringApplication.run(PedidosApplication.class, args);
	}
}
