package com.example.backend.tools;

public class UserContext {
    private static final ThreadLocal<UserContextInfo> USER_THREAD_LOCAL = new ThreadLocal<>();

    public static void set(UserContextInfo info) {
        USER_THREAD_LOCAL.set(info);
    }

    public static UserContextInfo get() {
        return USER_THREAD_LOCAL.get();
    }

    public static void remove() {
        USER_THREAD_LOCAL.remove();
    }

    public static class UserContextInfo {
        private Long userId;
        private String username;
        private String role;

        public UserContextInfo() {}

        public UserContextInfo(Long userId, String username, String role) {
            this.userId = userId;
            this.username = username;
            this.role = role;
        }

        public Long getUserId() { return userId; }
        public void setUserId(Long userId) { this.userId = userId; }
        public String getUsername() { return username; }
        public void setUsername(String username) { this.username = username; }
        public String getRole() { return role; }
        public void setRole(String role) { this.role = role; }
    }
}
