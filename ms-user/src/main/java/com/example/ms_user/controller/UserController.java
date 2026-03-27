package com.example.ms_user.controller;

import com.example.ms_user.dto.CreateUserRequest;
import com.example.ms_user.dto.UserDto;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

@RestController
@RequestMapping("/api/users")
@Tag(name = "User", description = "사용자 관리 API")
public class UserController {

    private final Map<Long, UserDto> store = new ConcurrentHashMap<>();
    private final AtomicLong sequence = new AtomicLong(1);

    @GetMapping
    @Operation(summary = "전체 사용자 목록 조회", operationId = "listUsers")
    public List<UserDto> listUsers() {
        return List.copyOf(store.values());
    }

    @GetMapping("/{id}")
    @Operation(summary = "사용자 상세 조회", operationId = "getUser")
    public ResponseEntity<UserDto> getUser(
            @Parameter(description = "사용자 ID") @PathVariable Long id) {
        UserDto user = store.get(id);
        return user != null ? ResponseEntity.ok(user) : ResponseEntity.notFound().build();
    }

    @PostMapping
    @Operation(summary = "사용자 생성", operationId = "createUser")
    public UserDto createUser(@RequestBody CreateUserRequest request) {
        long id = sequence.getAndIncrement();
        UserDto user = new UserDto(id, request.name(), request.email(), request.phone());
        store.put(id, user);
        return user;
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "사용자 삭제", operationId = "deleteUser")
    public ResponseEntity<Void> deleteUser(
            @Parameter(description = "사용자 ID") @PathVariable Long id) {
        return store.remove(id) != null
                ? ResponseEntity.noContent().build()
                : ResponseEntity.notFound().build();
    }
}
