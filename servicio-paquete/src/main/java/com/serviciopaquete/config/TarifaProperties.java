package com.serviciopaquete.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.context.config.annotation.RefreshScope;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

@Getter
@Setter
@RefreshScope
@Component
public class TarifaProperties {

    @Value("${tarifa.precio-base:10.0}")
    private BigDecimal precioBase;

    @Value("${tarifa.factor-peso:5.0}")
    private BigDecimal factorPeso;
}
