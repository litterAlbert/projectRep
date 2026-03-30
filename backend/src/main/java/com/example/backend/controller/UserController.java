package com.example.backend.controller;

import com.example.backend.pojo.Result;
import com.example.backend.pojo.User;
import com.example.backend.service.UserService;
import com.example.backend.tools.JwtTool;
import com.example.backend.tools.UserContext;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 用户控制器
 * 时间: 2026-03-28
 */
@RestController
@RequestMapping("/user")
public class UserController {

    @Autowired
    private UserService userService;

    @Autowired
    private JwtTool jwtTool;

    /**
     * 用户注册
     *
     * @param user 用户信息
     * @return 注册结果
     */
    @PostMapping("/register")
    public Result<String> register(@RequestBody User user) {
        if (user.getUsername() == null || user.getPassword() == null) {
            return Result.error("用户名或密码不能为空");
        }
        QueryWrapper<User> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("username", user.getUsername());
        if (userService.getOne(queryWrapper) != null) {
            return Result.error("用户名已存在");
        }
        if (user.getRole() == null) {
            user.setRole("user");
        }
        userService.save(user);
        return Result.success("注册成功");
    }

    /**
     * 用户登录
     *
     * @param user 用户信息
     * @return 登录结果和token
     */
    @PostMapping("/login")
    public Result<Map<String, Object>> login(@RequestBody User user) {
        if (user.getUsername() == null || user.getPassword() == null) {
            return Result.error("用户名或密码不能为空");
        }
        QueryWrapper<User> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("username", user.getUsername())
                    .eq("password", user.getPassword());
        User dbUser = userService.getOne(queryWrapper);
        if (dbUser == null) {
            return Result.error("用户名或密码错误");
        }

        String token = jwtTool.generateToken(dbUser.getId(), dbUser.getUsername(), dbUser.getRole());
        Map<String, Object> data = new HashMap<>();
        data.put("token", token);
        data.put("user", dbUser);
        return Result.success(data);
    }

    /**
     * 获取当前用户信息
     *
     * @return 当前用户信息
     */
    @GetMapping("/info")
    public Result<User> getInfo() {
        UserContext.UserContextInfo info = UserContext.get();
        if (info == null) {
            return Result.error(401, "未登录");
        }
        User user = userService.getById(info.getUserId());
        user.setPassword(null);
        return Result.success(user);
    }

    /**
     * 获取所有用户列表 (管理员)
     *
     * @return 用户列表
     */
    @GetMapping("/list")
    public Result<List<User>> listUsers() {
        UserContext.UserContextInfo info = UserContext.get();
        if (info == null || !"admin".equals(info.getRole())) {
            return Result.error(403, "无权限");
        }
        List<User> list = userService.list();
        for (User u : list) {
            u.setPassword(null);
        }
        return Result.success(list);
    }

    /**
     * 更新用户信息
     * 增加时间：2026-03-30
     *
     * @param user 用户信息
     * @return 更新结果
     */
    @PutMapping("/update")
    public Result<String> update(@RequestBody User user) {
        UserContext.UserContextInfo info = UserContext.get();
        if (info == null) {
            return Result.error(401, "未登录");
        }
        // 如果不是管理员且修改的不是自己的信息，则拒绝
        if (!"admin".equals(info.getRole()) && !info.getUserId().equals(user.getId())) {
            return Result.error(403, "无权限");
        }

        // 仅管理员可修改角色
        if (!"admin".equals(info.getRole())) {
            user.setRole(null);
        }

        // 处理密码，如果为空字符串则设置为null，避免更新为空密码
        if (user.getPassword() != null && user.getPassword().trim().isEmpty()) {
            user.setPassword(null);
        }

        // 检查用户名是否已存在
        if (user.getUsername() != null && !user.getUsername().trim().isEmpty()) {
            QueryWrapper<User> queryWrapper = new QueryWrapper<>();
            queryWrapper.eq("username", user.getUsername()).ne("id", user.getId());
            if (userService.getOne(queryWrapper) != null) {
                return Result.error("用户名已存在");
            }
        }

        userService.updateById(user);
        return Result.success("更新成功");
    }

    /**
     * 删除用户 (管理员)
     *
     * @param id 用户ID
     * @return 删除结果
     */
    @DeleteMapping("/{id}")
    public Result<String> delete(@PathVariable Long id) {
        UserContext.UserContextInfo info = UserContext.get();
        if (info == null || !"admin".equals(info.getRole())) {
            return Result.error(403, "无权限");
        }
        userService.removeById(id);
        return Result.success("删除成功");
    }
}
