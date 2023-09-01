package com.kibernumacademy.api.gateway.config;

import com.kibernumacademy.api.gateway.dto.TokenDto;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.factory.AbstractGatewayFilterFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

@Component
public class AuthFilter extends AbstractGatewayFilterFactory<AuthFilter.Config> {

  private WebClient.Builder webClient; // un cliente Http no bloqueante
  @Autowired
  public AuthFilter(WebClient.Builder webClient) {
    super(Config.class);
    this.webClient = webClient;
  }


  @Override
  public GatewayFilter apply(Config config) {
    // El método apply se invoca por cada solicitud que pasa por el Gateway
    // exchange: te da acceso a toda la información sobre la solicitud entrante y la respuesta saliente.
    // chain: Representa la cadena de filtros que se ejecutarán.
    return (((exchange, chain) -> {
      // Comprueba si la solicitud tiene un encabezado de autorización.
      if(!exchange.getRequest().getHeaders().containsKey(HttpHeaders.AUTHORIZATION)) {
        return onError(exchange, HttpStatus.BAD_REQUEST);
      }

      // Si viene el token dentro de Headers con el authorization, lo vamos a extraer
      String tokenHeader = exchange.getRequest().getHeaders().get(HttpHeaders.AUTHORIZATION).get(0); // bearer T3ufjfiajf93jfjfajk3311
      String[] chunks = tokenHeader.split(" "); // [Bearer, token]

      if(chunks.length !=2 || !chunks[0].equals("Bearer")) {
        return onError(exchange, HttpStatus.BAD_REQUEST);
      }

      // WebClient
      return webClient.build()
              .post()
              .uri("http://auth-service/auth/validate?token=" + chunks[1])
              .retrieve()
              .bodyToMono(TokenDto.class)
              .map(t -> {
                //log
                t.getToken();
                return exchange; // Mono<ServerWebExchange>
              }).flatMap(chain::filter); //.flatMap(exchange -> chain.filter(exchange))
    }));
  }

  // Nos sirve para manejar los errores y enviar una respuesta Http con el estado
  public Mono<Void> onError(ServerWebExchange exchange, HttpStatus httpStatus) {
    ServerHttpResponse response = exchange.getResponse();
    response.setStatusCode(httpStatus);
    return response.setComplete();
  }
  public static class Config {
    // Sirve para configurar aspectos específicos de filtros
  }


}
