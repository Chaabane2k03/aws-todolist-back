package com.todoapp.grpc.service;

import com.todoapp.entity.User;
import com.todoapp.grpc.*;
import com.todoapp.repository.UserRepository;
import com.todoapp.util.JwtUtil;
import io.grpc.stub.StreamObserver;
import net.devh.boot.grpc.server.service.GrpcService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

@GrpcService
public class AuthServiceImpl extends AuthServiceGrpc.AuthServiceImplBase {
    
    @Autowired
    private UserRepository userRepository;
    
    @Autowired
    private JwtUtil jwtUtil;
    
    @Autowired
    private BCryptPasswordEncoder passwordEncoder;
    
    @Override
    public void register(RegisterRequest request, StreamObserver<AuthResponse> responseObserver) {
        try {
            if (userRepository.existsByUsername(request.getUsername())) {
                responseObserver.onNext(AuthResponse.newBuilder()
                        .setSuccess(false)
                        .setMessage("Username already exists")
                        .build());
                responseObserver.onCompleted();
                return;
            }
            
            if (userRepository.existsByEmail(request.getEmail())) {
                responseObserver.onNext(AuthResponse.newBuilder()
                        .setSuccess(false)
                        .setMessage("Email already exists")
                        .build());
                responseObserver.onCompleted();
                return;
            }
            
            User user = new User();
            user.setUsername(request.getUsername());
            user.setPassword(passwordEncoder.encode(request.getPassword()));
            user.setEmail(request.getEmail());
            
            user = userRepository.save(user);
            
            String token = jwtUtil.generateToken(user.getId(), user.getUsername());
            
            responseObserver.onNext(AuthResponse.newBuilder()
                    .setSuccess(true)
                    .setMessage("Registration successful")
                    .setToken(token)
                    .setUserId(user.getId())
                    .setUsername(user.getUsername())
                    .build());
            responseObserver.onCompleted();
            
        } catch (Exception e) {
            responseObserver.onNext(AuthResponse.newBuilder()
                    .setSuccess(false)
                    .setMessage("Registration failed: " + e.getMessage())
                    .build());
            responseObserver.onCompleted();
        }
    }
    
    @Override
    public void login(LoginRequest request, StreamObserver<AuthResponse> responseObserver) {
        try {
            User user = userRepository.findByUsername(request.getUsername())
                    .orElse(null);
            
            if (user == null || !passwordEncoder.matches(request.getPassword(), user.getPassword())) {
                responseObserver.onNext(AuthResponse.newBuilder()
                        .setSuccess(false)
                        .setMessage("Invalid username or password")
                        .build());
                responseObserver.onCompleted();
                return;
            }
            
            String token = jwtUtil.generateToken(user.getId(), user.getUsername());
            
            responseObserver.onNext(AuthResponse.newBuilder()
                    .setSuccess(true)
                    .setMessage("Login successful")
                    .setToken(token)
                    .setUserId(user.getId())
                    .setUsername(user.getUsername())
                    .build());
            responseObserver.onCompleted();
            
        } catch (Exception e) {
            responseObserver.onNext(AuthResponse.newBuilder()
                    .setSuccess(false)
                    .setMessage("Login failed: " + e.getMessage())
                    .build());
            responseObserver.onCompleted();
        }
    }
    
    @Override
    public void validateToken(ValidateTokenRequest request, StreamObserver<ValidateTokenResponse> responseObserver) {
        try {
            if (jwtUtil.validateToken(request.getToken())) {
                Long userId = jwtUtil.getUserIdFromToken(request.getToken());
                String username = jwtUtil.getUsernameFromToken(request.getToken());
                
                responseObserver.onNext(ValidateTokenResponse.newBuilder()
                        .setValid(true)
                        .setUserId(userId)
                        .setUsername(username)
                        .build());
            } else {
                responseObserver.onNext(ValidateTokenResponse.newBuilder()
                        .setValid(false)
                        .build());
            }
            responseObserver.onCompleted();
            
        } catch (Exception e) {
            responseObserver.onNext(ValidateTokenResponse.newBuilder()
                    .setValid(false)
                    .build());
            responseObserver.onCompleted();
        }
    }
}