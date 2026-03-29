# 接口文档 (API Documentation)

> **基础说明**
> 所有接口默认返回统一的 JSON 结构，如下所示：
> ```json
> {
>   "code": 200,           // 状态码，200 为成功，其他为失败
>   "message": "success",  // 提示信息
>   "data": ...            // 实际数据内容
> }
> ```
> 日期时间类型默认返回标准 ISO 或时间戳格式。

---

## 1. 用户模块 (`/user`)

### 1.1 用户注册
- **URL**: `/user/register`
- **Method**: `POST`
- **请求 Body**:
  ```json
  {
    "username": "test_user",
    "password": "password123",
    "gender": "男",
    "phone": "13800138000",
    "role": "user"
  }
  ```
- **响应 Body**:
  ```json
  {
    "code": 200,
    "message": "success",
    "data": "注册成功"
  }
  ```

### 1.2 用户登录
- **URL**: `/user/login`
- **Method**: `POST`
- **请求 Body**:
  ```json
  {
    "username": "test_user",
    "password": "password123"
  }
  ```
- **响应 Body**:
  ```json
  {
    "code": 200,
    "message": "success",
    "data": {
      "token": "eyJhbGciOiJIUzI1NiJ9...",
      "user": {
        "id": 1,
        "username": "test_user",
        "gender": "男",
        "phone": "13800138000",
        "role": "user",
        "createdAt": "2026-03-28T10:00:00.000+00:00",
        "updatedAt": "2026-03-28T10:00:00.000+00:00"
      }
    }
  }
  ```

### 1.3 获取当前用户信息
- **URL**: `/user/info`
- **Method**: `GET`
- **响应 Body**:
  ```json
  {
    "code": 200,
    "message": "success",
    "data": {
      "id": 1,
      "username": "test_user",
      "gender": "男",
      "phone": "13800138000",
      "role": "user",
      "createdAt": "2026-03-28T10:00:00.000+00:00",
      "updatedAt": "2026-03-28T10:00:00.000+00:00"
    }
  }
  ```

### 1.4 获取所有用户列表 (仅限管理员)
- **URL**: `/user/list`
- **Method**: `GET`
- **响应 Body**:
  ```json
  {
    "code": 200,
    "message": "success",
    "data": [
      {
        "id": 1,
        "username": "test_user",
        "gender": "男",
        "phone": "13800138000",
        "role": "user",
        "createdAt": "2026-03-28T10:00:00.000+00:00",
        "updatedAt": "2026-03-28T10:00:00.000+00:00"
      }
    ]
  }
  ```

### 1.5 更新用户信息
- **URL**: `/user/update`
- **Method**: `PUT`
- **请求 Body**:
  ```json
  {
    "id": 1,
    "username": "test_user_new",
    "gender": "女",
    "phone": "13900139000"
  }
  ```
- **响应 Body**:
  ```json
  {
    "code": 200,
    "message": "success",
    "data": "更新成功"
  }
  ```

### 1.6 删除用户 (仅限管理员)
- **URL**: `/user/{id}`
- **Method**: `DELETE`
- **响应 Body**:
  ```json
  {
    "code": 200,
    "message": "success",
    "data": "删除成功"
  }
  ```

---

## 2. 图书模块 (`/book`)

### 2.1 添加图书 (仅限管理员)
- **URL**: `/book/add`
- **Method**: `POST`
- **请求 Body**:
  ```json
  {
    "isbn": "978-3-16-148410-0",
    "title": "Java编程思想",
    "author": "Bruce Eckel",
    "publisher": "机械工业出版社",
    "publishDate": "2007-06-01T00:00:00.000+00:00",
    "stock": 10,
    "borrowedCount": 0,
    "reservedCount": 0,
    "location": "A区-01架"
  }
  ```
- **响应 Body**:
  ```json
  {
    "code": 200,
    "message": "success",
    "data": "添加成功"
  }
  ```

### 2.2 删除图书 (仅限管理员)
- **URL**: `/book/{id}`
- **Method**: `DELETE`
- **响应 Body**:
  ```json
  {
    "code": 200,
    "message": "success",
    "data": "删除成功"
  }
  ```

### 2.3 更新图书信息 (仅限管理员)
- **URL**: `/book/update`
- **Method**: `PUT`
- **请求 Body**:
  ```json
  {
    "id": 1,
    "isbn": "978-3-16-148410-0",
    "title": "Java编程思想 (第4版)",
    "author": "Bruce Eckel",
    "publisher": "机械工业出版社",
    "publishDate": "2007-06-01T00:00:00.000+00:00",
    "stock": 15,
    "borrowedCount": 5,
    "reservedCount": 1,
    "location": "A区-01架"
  }
  ```
- **响应 Body**:
  ```json
  {
    "code": 200,
    "message": "success",
    "data": "更新成功"
  }
  ```

### 2.4 检索图书 (支持书名、作者、ISBN模糊匹配)
- **URL**: `/book/search?keyword={keyword}`
- **Method**: `GET`
- **响应 Body**:
  ```json
  {
    "code": 200,
    "message": "success",
    "data": [
      {
        "id": 1,
        "isbn": "978-3-16-148410-0",
        "title": "Java编程思想",
        "author": "Bruce Eckel",
        "publisher": "机械工业出版社",
        "publishDate": "2007-06-01T00:00:00.000+00:00",
        "stock": 10,
        "borrowedCount": 2,
        "reservedCount": 0,
        "location": "A区-01架",
        "createdAt": "2026-03-28T10:00:00.000+00:00",
        "updatedAt": "2026-03-28T10:00:00.000+00:00"
      }
    ]
  }
  ```

### 2.5 获取图书详情
- **URL**: `/book/{id}`
- **Method**: `GET`
- **响应 Body**:
  ```json
  {
    "code": 200,
    "message": "success",
    "data": {
      "id": 1,
      "isbn": "978-3-16-148410-0",
      "title": "Java编程思想",
      "author": "Bruce Eckel",
      "publisher": "机械工业出版社",
      "publishDate": "2007-06-01T00:00:00.000+00:00",
      "stock": 10,
      "borrowedCount": 2,
      "reservedCount": 0,
      "location": "A区-01架",
      "createdAt": "2026-03-28T10:00:00.000+00:00",
      "updatedAt": "2026-03-28T10:00:00.000+00:00"
    }
  }
  ```

---

## 3. 借阅/预约操作模块 (`/action`)

### 3.1 借书
- **URL**: `/action/borrow/{bookId}`
- **Method**: `POST`
- **响应 Body**:
  ```json
  {
    "code": 200,
    "message": "success",
    "data": "借阅成功"
  }
  ```

### 3.2 还书
- **URL**: `/action/return/{recordId}`
- **Method**: `POST`
- **响应 Body**:
  ```json
  {
    "code": 200,
    "message": "success",
    "data": "归还成功"
  }
  ```

### 3.3 预约图书
- **URL**: `/action/reserve/{bookId}`
- **Method**: `POST`
- **响应 Body**:
  ```json
  {
    "code": 200,
    "message": "success",
    "data": "预约成功"
  }
  ```

### 3.4 获取个人借阅记录列表
- **URL**: `/action/borrow/list`
- **Method**: `GET`
- **响应 Body**:
  ```json
  {
    "code": 200,
    "message": "success",
    "data": [
      {
        "id": 1,
        "userId": 1,
        "bookId": 1,
        "borrowDate": "2026-03-28T10:00:00.000+00:00",
        "returnDate": null,
        "status": "BORROWED",
        "createdAt": "2026-03-28T10:00:00.000+00:00",
        "updatedAt": "2026-03-28T10:00:00.000+00:00"
      }
    ]
  }
  ```

---

## 4. AI 助手模块 (`/ai`)

### 4.1 上传文档进行向量化
- **URL**: `/ai/upload`
- **Method**: `POST`
- **Content-Type**: `multipart/form-data`
- **请求参数**: `file` (文件类型)
- **响应 Body**:
  ```json
  {
    "code": 200,
    "message": "success",
    "data": "https://oss-url-to-uploaded-file.com/doc.pdf"
  }
  ```

### 4.2 新建会话
- **URL**: `/ai/session`
- **Method**: `POST`
- **请求 Body**:
  ```json
  {
    "title": "关于Java编程的问题"
  }
  ```
- **响应 Body**:
  ```json
  {
    "code": 200,
    "message": "success",
    "data": {
      "id": "550e8400-e29b-41d4-a716-446655440000",
      "userId": 1,
      "title": "关于Java编程的问题",
      "createdAt": "2026-03-28T10:00:00.000+00:00",
      "updatedAt": "2026-03-28T10:00:00.000+00:00"
    }
  }
  ```

### 4.3 获取会话列表
- **URL**: `/ai/session/list`
- **Method**: `GET`
- **响应 Body**:
  ```json
  {
    "code": 200,
    "message": "success",
    "data": [
      {
        "id": "550e8400-e29b-41d4-a716-446655440000",
        "userId": 1,
        "title": "关于Java编程的问题",
        "createdAt": "2026-03-28T10:00:00.000+00:00",
        "updatedAt": "2026-03-28T10:00:00.000+00:00"
      }
    ]
  }
  ```

### 4.4 删除会话
- **URL**: `/ai/session/{sessionId}`
- **Method**: `DELETE`
- **响应 Body**:
  ```json
  {
    "code": 200,
    "message": "success",
    "data": "删除成功"
  }
  ```

### 4.5 重命名会话
- **URL**: `/ai/session/{sessionId}`
- **Method**: `PUT`
- **请求 Body**:
  ```json
  {
    "title": "新的会话名称"
  }
  ```
- **响应 Body**:
  ```json
  {
    "code": 200,
    "message": "success",
    "data": "重命名成功"
  }
  ```

### 4.6 会话聊天
- **URL**: `/ai/chat/{sessionId}`
- **Method**: `POST`
- **请求 Body**:
  ```json
  {
    "message": "你好，请帮我找一本Java相关的书"
  }
  ```
- **响应 Body**:
  根据用户请求的内容不同，AI 可能会返回三种不同的响应结构（`type` 字段区分）：

  **情况一：普通聊天响应 (`type: "text"`)**
  ```json
  {
    "code": 200,
    "message": "success",
    "data": {
      "type": "text",
      "content": "为您推荐《Java编程思想》..."
    }
  }
  ```

  **情况二：带有图表需求响应 (`type: "chart"`)**
  当用户请求中包含“折线图”、“柱状图”、“饼图”等关键词时，AI会调用数据分析工具返回特定格式。
  ```json
  {
    "code": 200,
    "message": "success",
    "data": {
      "type": "chart",
      "chartType": "line",  // line, bar, pie 等
      "data": [
        {"name": "Java编程思想", "value": 10},
        {"name": "Effective Java", "value": 15}
      ]
    }
  }
  ```

  **情况三：文档问答响应 (`type: "doc"`)**
  如果AI的回答基于检索到的上传文档（RAG 机制），通常会附带 `source_nodes`。
  *(注: 此处以文档检索返回时的标准扩展为例，需前端按约定的结构解析)*
  ```json
  {
    "code": 200,
    "message": "success",
    "data": {
      "type": "doc",
      "content": "根据文档内容，Java基础非常重要...",
      "source_nodes": [
        "Java基础教程.pdf",
        "Spring实战.docx"
      ]
    }
  }
  ```

### 4.7 获取会话历史记录
- **URL**: `/ai/chat/{sessionId}/history`
- **Method**: `GET`
- **响应 Body**:
  ```json
  {
    "code": 200,
    "message": "success",
    "data": [
      {
        "id": 1,
        "sessionId": "550e8400-e29b-41d4-a716-446655440000",
        "role": "user",
        "content": "你好，请帮我找一本Java相关的书",
        "source": null,
        "createdAt": "2026-03-28T10:00:00.000+00:00"
      },
      {
        "id": 2,
        "sessionId": "550e8400-e29b-41d4-a716-446655440000",
        "role": "ai",
        "content": "{\"reply\":\"为您推荐《Java编程思想》...\"}",
        "source": null,
        "createdAt": "2026-03-28T10:00:05.000+00:00"
      }
    ]
  }
  ```
