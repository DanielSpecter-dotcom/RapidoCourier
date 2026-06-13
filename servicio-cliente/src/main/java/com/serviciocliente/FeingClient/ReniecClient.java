package com.serviciocliente.FeingClient;

import com.serviciocliente.dto.response.ReniecResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;

@FeignClient(name = "reniec-api", url = "${reniec.api.url:https://api.decolecta.com/v1/reniec/dni}")
public interface ReniecClient {

    @GetMapping
    ReniecResponse consultarDni(
            @RequestParam("numero") String dni,
            @RequestHeader("Authorization") String token,
            @RequestHeader("User-Agent") String userAgent
    );
}