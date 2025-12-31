package com.todoapp.grpc.service;

import com.todoapp.entity.Todo;
import com.todoapp.entity.User;
import com.todoapp.grpc.*;
import com.todoapp.repository.TodoRepository;
import com.todoapp.repository.UserRepository;
import com.todoapp.util.JwtUtil;
import io.grpc.stub.StreamObserver;
import net.devh.boot.grpc.server.service.GrpcService;
import org.springframework.beans.factory.annotation.Autowired;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.stream.Collectors;

@GrpcService
public class TodoServiceImpl extends TodoServiceGrpc.TodoServiceImplBase {
    
    @Autowired
    private TodoRepository todoRepository;
    
    @Autowired
    private UserRepository userRepository;
    
    @Autowired
    private JwtUtil jwtUtil;
    
    private DateTimeFormatter formatter = DateTimeFormatter.ISO_LOCAL_DATE_TIME;
    
    private User getUserFromToken(String token) {
        if (!jwtUtil.validateToken(token)) {
            return null;
        }
        Long userId = jwtUtil.getUserIdFromToken(token);
        return userRepository.findById(userId).orElse(null);
    }
    
    private com.todoapp.grpc.Todo convertToProto(Todo todo) {
        return com.todoapp.grpc.Todo.newBuilder()
                .setId(todo.getId())
                .setUserId(todo.getUser().getId())
                .setTitle(todo.getTitle())
                .setDescription(todo.getDescription() != null ? todo.getDescription() : "")
                .setCompleted(todo.getCompleted())
                .setCreatedAt(todo.getCreatedAt().format(formatter))
                .setUpdatedAt(todo.getUpdatedAt().format(formatter))
                .build();
    }
    
    @Override
    public void createTodo(CreateTodoRequest request, StreamObserver<TodoResponse> responseObserver) {
        try {
            User user = getUserFromToken(request.getToken());
            if (user == null) {
                responseObserver.onNext(TodoResponse.newBuilder()
                        .setSuccess(false)
                        .setMessage("Invalid or expired token")
                        .build());
                responseObserver.onCompleted();
                return;
            }
            
            Todo todo = new Todo();
            todo.setUser(user);
            todo.setTitle(request.getTitle());
            todo.setDescription(request.getDescription());
            todo.setCompleted(false);
            
            todo = todoRepository.save(todo);
            
            responseObserver.onNext(TodoResponse.newBuilder()
                    .setSuccess(true)
                    .setMessage("Todo created successfully")
                    .setTodo(convertToProto(todo))
                    .build());
            responseObserver.onCompleted();
            
        } catch (Exception e) {
            responseObserver.onNext(TodoResponse.newBuilder()
                    .setSuccess(false)
                    .setMessage("Failed to create todo: " + e.getMessage())
                    .build());
            responseObserver.onCompleted();
        }
    }
    
    @Override
    public void getTodos(GetTodosRequest request, StreamObserver<TodoListResponse> responseObserver) {
        try {
            User user = getUserFromToken(request.getToken());
            if (user == null) {
                responseObserver.onNext(TodoListResponse.newBuilder()
                        .setSuccess(false)
                        .setMessage("Invalid or expired token")
                        .build());
                responseObserver.onCompleted();
                return;
            }
            
            List<Todo> todos;
            if (request.hasCompleted()) {
                todos = todoRepository.findByUserAndCompleted(user, request.getCompleted());
            } else {
                todos = todoRepository.findByUser(user);
            }
            
            List<com.todoapp.grpc.Todo> protoTodos = todos.stream()
                    .map(this::convertToProto)
                    .collect(Collectors.toList());
            
            responseObserver.onNext(TodoListResponse.newBuilder()
                    .setSuccess(true)
                    .setMessage("Todos retrieved successfully")
                    .addAllTodos(protoTodos)
                    .build());
            responseObserver.onCompleted();
            
        } catch (Exception e) {
            responseObserver.onNext(TodoListResponse.newBuilder()
                    .setSuccess(false)
                    .setMessage("Failed to retrieve todos: " + e.getMessage())
                    .build());
            responseObserver.onCompleted();
        }
    }
    
    @Override
    public void getTodoById(GetTodoByIdRequest request, StreamObserver<TodoResponse> responseObserver) {
        try {
            User user = getUserFromToken(request.getToken());
            if (user == null) {
                responseObserver.onNext(TodoResponse.newBuilder()
                        .setSuccess(false)
                        .setMessage("Invalid or expired token")
                        .build());
                responseObserver.onCompleted();
                return;
            }
            
            Todo todo = todoRepository.findByIdAndUser(request.getTodoId(), user)
                    .orElse(null);
            
            if (todo == null) {
                responseObserver.onNext(TodoResponse.newBuilder()
                        .setSuccess(false)
                        .setMessage("Todo not found")
                        .build());
                responseObserver.onCompleted();
                return;
            }
            
            responseObserver.onNext(TodoResponse.newBuilder()
                    .setSuccess(true)
                    .setMessage("Todo retrieved successfully")
                    .setTodo(convertToProto(todo))
                    .build());
            responseObserver.onCompleted();
            
        } catch (Exception e) {
            responseObserver.onNext(TodoResponse.newBuilder()
                    .setSuccess(false)
                    .setMessage("Failed to retrieve todo: " + e.getMessage())
                    .build());
            responseObserver.onCompleted();
        }
    }
    
    @Override
    public void updateTodo(UpdateTodoRequest request, StreamObserver<TodoResponse> responseObserver) {
        try {
            User user = getUserFromToken(request.getToken());
            if (user == null) {
                responseObserver.onNext(TodoResponse.newBuilder()
                        .setSuccess(false)
                        .setMessage("Invalid or expired token")
                        .build());
                responseObserver.onCompleted();
                return;
            }
            
            Todo todo = todoRepository.findByIdAndUser(request.getTodoId(), user)
                    .orElse(null);
            
            if (todo == null) {
                responseObserver.onNext(TodoResponse.newBuilder()
                        .setSuccess(false)
                        .setMessage("Todo not found")
                        .build());
                responseObserver.onCompleted();
                return;
            }
            
            todo.setTitle(request.getTitle());
            todo.setDescription(request.getDescription());
            todo.setCompleted(request.getCompleted());
            
            todo = todoRepository.save(todo);
            
            responseObserver.onNext(TodoResponse.newBuilder()
                    .setSuccess(true)
                    .setMessage("Todo updated successfully")
                    .setTodo(convertToProto(todo))
                    .build());
            responseObserver.onCompleted();
            
        } catch (Exception e) {
            responseObserver.onNext(TodoResponse.newBuilder()
                    .setSuccess(false)
                    .setMessage("Failed to update todo: " + e.getMessage())
                    .build());
            responseObserver.onCompleted();
        }
    }
    
    @Override
    public void deleteTodo(DeleteTodoRequest request, StreamObserver<DeleteTodoResponse> responseObserver) {
        try {
            User user = getUserFromToken(request.getToken());
            if (user == null) {
                responseObserver.onNext(DeleteTodoResponse.newBuilder()
                        .setSuccess(false)
                        .setMessage("Invalid or expired token")
                        .build());
                responseObserver.onCompleted();
                return;
            }
            
            Todo todo = todoRepository.findByIdAndUser(request.getTodoId(), user)
                    .orElse(null);
            
            if (todo == null) {
                responseObserver.onNext(DeleteTodoResponse.newBuilder()
                        .setSuccess(false)
                        .setMessage("Todo not found")
                        .build());
                responseObserver.onCompleted();
                return;
            }
            
            todoRepository.delete(todo);
            
            responseObserver.onNext(DeleteTodoResponse.newBuilder()
                    .setSuccess(true)
                    .setMessage("Todo deleted successfully")
                    .build());
            responseObserver.onCompleted();
            
        } catch (Exception e) {
            responseObserver.onNext(DeleteTodoResponse.newBuilder()
                    .setSuccess(false)
                    .setMessage("Failed to delete todo: " + e.getMessage())
                    .build());
            responseObserver.onCompleted();
        }
    }
    
    @Override
    public void toggleTodoComplete(ToggleTodoCompleteRequest request, StreamObserver<TodoResponse> responseObserver) {
        try {
            User user = getUserFromToken(request.getToken());
            if (user == null) {
                responseObserver.onNext(TodoResponse.newBuilder()
                        .setSuccess(false)
                        .setMessage("Invalid or expired token")
                        .build());
                responseObserver.onCompleted();
                return;
            }
            
            Todo todo = todoRepository.findByIdAndUser(request.getTodoId(), user)
                    .orElse(null);
            
            if (todo == null) {
                responseObserver.onNext(TodoResponse.newBuilder()
                        .setSuccess(false)
                        .setMessage("Todo not found")
                        .build());
                responseObserver.onCompleted();
                return;
            }
            
            todo.setCompleted(!todo.getCompleted());
            todo = todoRepository.save(todo);
            
            responseObserver.onNext(TodoResponse.newBuilder()
                    .setSuccess(true)
                    .setMessage("Todo status toggled successfully")
                    .setTodo(convertToProto(todo))
                    .build());
            responseObserver.onCompleted();
            
        } catch (Exception e) {
            responseObserver.onNext(TodoResponse.newBuilder()
                    .setSuccess(false)
                    .setMessage("Failed to toggle todo: " + e.getMessage())
                    .build());
            responseObserver.onCompleted();
        }
    }
}