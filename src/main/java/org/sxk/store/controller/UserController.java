package org.sxk.store.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.sxk.store.common.Result;
import org.sxk.store.entity.User;
import org.sxk.store.service.UserService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/users")
@Tag(name = "用户管理", description = "用户的增删改查操作")
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping
    @Operation(summary = "获取所有用户", description = "返回系统中所有用户列表")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "查询成功")
    })
    public ResponseEntity<Result<List<User>>> getAllUsers() {
        log.info("GET /api/users - 获取所有用户");
        return ResponseEntity.ok(Result.success(userService.getAllUsers()));
    }

    @GetMapping("/{id}")
    @Operation(summary = "根据ID获取用户", description = "通过用户ID查询单个用户信息")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "查询成功"),
            @ApiResponse(responseCode = "404", description = "用户不存在")
    })
    public ResponseEntity<Result<User>> getUserById(
            @Parameter(description = "用户ID", required = true)
            @PathVariable Long id) {
        log.info("GET /api/users/{} - 根据ID获取用户", id);
        User user = userService.getUserById(id);
        if (user != null) {
            return ResponseEntity.ok(Result.success(user));
        }
        return ResponseEntity.ok(Result.notFound("用户不存在"));
    }

    @PostMapping
    @Operation(summary = "创建用户", description = "创建新用户，用户名、密码为必填项")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "创建成功"),
            @ApiResponse(responseCode = "500", description = "创建失败")
    })
    public ResponseEntity<Result<String>> createUser(
            @Parameter(description = "用户对象", required = true)
            @RequestBody User user) {
        log.info("POST /api/users - 创建用户: {}", user.getUsername());
        int result = userService.createUser(user);
        if (result > 0) {
            return ResponseEntity.ok(Result.success("用户创建成功"));
        }
        return ResponseEntity.ok(Result.error("用户创建失败"));
    }

    @PutMapping("/{id}")
    @Operation(summary = "更新用户", description = "根据ID更新用户信息")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "更新成功"),
            @ApiResponse(responseCode = "500", description = "更新失败")
    })
    public ResponseEntity<Result<String>> updateUser(
            @Parameter(description = "用户ID", required = true)
            @PathVariable Long id,
            @Parameter(description = "用户对象", required = true)
            @RequestBody User user) {
        log.info("PUT /api/users/{} - 更新用户", id);
        user.setId(id);
        int result = userService.updateUser(user);
        if (result > 0) {
            return ResponseEntity.ok(Result.success("用户更新成功"));
        }
        return ResponseEntity.ok(Result.error("用户更新失败"));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "删除用户", description = "根据ID删除用户")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "删除成功"),
            @ApiResponse(responseCode = "500", description = "删除失败")
    })
    public ResponseEntity<Result<String>> deleteUser(
            @Parameter(description = "用户ID", required = true)
            @PathVariable Long id) {
        log.info("DELETE /api/users/{} - 删除用户", id);
        int result = userService.deleteUser(id);
        if (result > 0) {
            return ResponseEntity.ok(Result.success("用户删除成功"));
        }
        return ResponseEntity.ok(Result.error("用户删除失败"));
    }
}