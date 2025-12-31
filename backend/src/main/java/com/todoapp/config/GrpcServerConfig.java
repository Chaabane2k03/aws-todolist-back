package com.todoapp.config;

import io.grpc.Server;
import io.grpc.ServerBuilder;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class GrpcServerConfig {
    
    @Value("${grpc.server.port:9090}")
    private int grpcPort;
    
    @Bean
    public Server grpcServer(com.todoapp.grpc.service.AuthServiceImpl authService,
                            com.todoapp.grpc.service.TodoServiceImpl todoService) {
        return ServerBuilder.forPort(grpcPort)
                .addService(authService)
                .addService(todoService)
                .build();
    }
}