package com.pokedex.api.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.data.mongodb.core.convert.DefaultMongoTypeMapper;
import org.springframework.data.mongodb.core.convert.MappingMongoConverter;

/**
 * Grava os documentos sem o campo {@code _class} do Spring Data: cada coleção guarda um único tipo
 * de documento, então os dados no MongoDB ficam iguais ao formato definido na atividade.
 */
@Configuration(proxyBeanMethods = false)
public class MongoConfig {

    public MongoConfig(MappingMongoConverter converter) {
        converter.setTypeMapper(new DefaultMongoTypeMapper(null));
    }
}
