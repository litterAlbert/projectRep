# 智慧图书管理系统 API 接口文档

所有接口统一返回以下格式结构：
```json
{
  "code": 200,      // 状态码：200为成功，500为失败，401为未登录，403为无权限
  "message": "描述", // 提示信息
  "data": {}        // 具体返回的数据，类型视接口而定
}
```

请求需在 Header 中添加认证：
`Authorization: Bearer <token>` （注册和登录接口除外）

---

## 1. 用户模块

### 1.1 用户注册
- **URL**: `/user/register`
- **Method**: `POST`
- **Body**:
  ```json
  {
    "username": "testuser",
    "password": "123",
    "gender": "男",
    "phone": "13800000000",
    "role": "user"
  }
  ```
- **Response**:
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
- **Body**:
  ```json
  {
    "username": "admin",
    "password": "123"
  }
  ```
- **Response**: 
  ```json
  {
    "code": 200,
    "message": "success",
    "data": {
      "token": "eyJhbGciOiJIUzI1NiJ9...",
      "user": {
        "id": 1,
        "username": "admin",
        "gender": "男",
        "phone": "13800000000",
        "role": "admin",
        "createdAt": "2026-03-28T10:00:00.000+00:00",
        "updatedAt": "2026-03-28T10:00:00.000+00:00"
      }
    }
  }
  ```

### 1.3 获取当前用户信息
- **URL**: `/user/info`
- **Method**: `GET`
- **Response**:
  ```json
  {
    "code": 200,
    "message": "success",
    "data": {
      "id": 1,
      "username": "admin",
      "gender": "男",
      "phone": "13800000000",
      "role": "admin",
      "createdAt": "2026-03-28T10:00:00.000+00:00",
      "updatedAt": "2026-03-28T10:00:00.000+00:00"
    }
  }
  ```

### 1.4 获取所有用户列表（管理员）
- **URL**: `/user/list`
- **Method**: `GET`
- **Response**:
  ```json
  {
    "code": 200,
    "message": "success",
    "data": [
      {
        "id": 1,
        "username": "admin",
        "gender": "男",
        "phone": "13800000000",
        "role": "admin",
        "createdAt": "2026-03-28T10:00:00.000+00:00",
        "updatedAt": "2026-03-28T10:00:00.000+00:00"
      },
      {
        "id": 2,
        "username": "user1",
        "gender": "女",
        "phone": "13800000001",
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
- **Body**:
  ```json
  {
    "id": 2,
    "phone": "13911112222",
    "gender": "女"
  }
  ```
- **Response**:
  ```json
  {
    "code": 200,
    "message": "success",
    "data": "更新成功"
  }
  ```

### 1.6 删除用户（管理员）
- **URL**: `/user/{id}`
- **Method**: `DELETE`
- **Response**:
  ```json
  {
    "code": 200,
    "message": "success",
    "data": "删除成功"
  }
  ```

---

## 2. 图书模块

### 2.1 添加图书（管理员）
- **URL**: `/book/add`
- **Method**: `POST`
- **Body**:
  ```json
  {
    "isbn": "9781234567890",
    "title": "测试图书",
    "author": "张三",
    "publisher": "某某出版社",
    "publishDate": "2026-01-01T00:00:00.000+00:00",
    "stock": 10,
    "location": "A区1架"
  }
  ```
- **Response**:
  ```json
  {
    "code": 200,
    "message": "success",
    "data": "添加成功"
  }
  ```

### 2.2 更新图书信息（管理员）
- **URL**: `/book/update`
- **Method**: `PUT`
- **Body**:
  ```json
  {
    "id": 1,
    "stock": 15,
    "location": "A区2架"
  }
  ```
- **Response**:
  ```json
  {
    "code": 200,
    "message": "success",
    "data": "更新成功"
  }
  ```

### 2.3 删除图书（管理员）
- **URL**: `/book/{id}`
- **Method**: `DELETE`
- **Response**:
  ```json
  {
    "code": 200,
    "message": "success",
    "data": "删除成功"
  }
  ```

### 2.4 检索图书
- **URL**: `/book/search?keyword=xxx`
- **Method**: `GET`
- **Response**:
  ```json
  {
    "code": 200,
    "message": "success",
    "data": [
      {
        "id": 1,
        "isbn": "9787111128069",
        "title": "C程序设计语言",
        "author": "Brian W.Kernighan",
        "publisher": "机械工业出版社",
        "publishDate": "2004-01-01T00:00:00.000+00:00",
        "stock": 5,
        "borrowedCount": 2,
        "reservedCount": 0,
        "location": "A区1架1层",
        "createdAt": "2026-03-28T10:00:00.000+00:00",
        "updatedAt": "2026-03-28T10:00:00.000+00:00"
      }
    ]
  }
  ```

### 2.5 获取图书详情
- **URL**: `/book/{id}`
- **Method**: `GET`
- **Response**:
  ```json
  {
    "code": 200,
    "message": "success",
    "data": {
      "id": 1,
      "isbn": "9787111128069",
      "title": "C程序设计语言",
      "author": "Brian W.Kernighan",
      "publisher": "机械工业出版社",
      "publishDate": "2004-01-01T00:00:00.000+00:00",
      "stock": 5,
      "borrowedCount": 2,
      "reservedCount": 0,
      "location": "A区1架1层",
      "createdAt": "2026-03-28T10:00:00.000+00:00",
      "updatedAt": "2026-03-28T10:00:00.000+00:00"
    }
  }
  ```

---

## 3. 借阅与预约模块

### 3.1 借书
- **URL**: `/action/borrow/{bookId}`
- **Method**: `POST`
- **Response**:
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
- **Response**:
  ```json
  {
    "code": 200,
    "message": "success",
    "data": "归还成功"
  }
  ```

### 3.3 预约
- **URL**: `/action/reserve/{bookId}`
- **Method**: `POST`
- **Response**:
  ```json
  {
    "code": 200,
    "message": "success",
    "data": "预约成功"
  }
  ```

### 3.4 获取个人借阅记录
- **URL**: `/action/borrow/list`
- **Method**: `GET`
- **Response**:
  ```json
  {
    "code": 200,
    "message": "success",
    "data": [
      {
        "id": 1,
        "userId": 2,
        "bookId": 1,
        "borrowDate": "2026-02-01T02:00:00.000+00:00",
        "returnDate": "2026-02-15T02:00:00.000+00:00",
        "status": "RETURNED",
        "createdAt": "2026-03-28T10:00:00.000+00:00",
        "updatedAt": "2026-03-28T10:00:00.000+00:00"
      }
    ]
  }
  ```

---

## 4. AI 助手模块

### 4.1 上传文档进行向量化（RAG）
- **URL**: `/ai/upload`
- **Method**: `POST` (multipart/form-data)
- **Form Data**: 
  - `file`: (文件)
- **Response**:
  ```json
  {
    "code": 200,
    "message": "success",
    "data": "https://bucket-name.oss-cn-hangzhou.aliyuncs.com/xxx.pdf"
  }
  ```

### 4.2 新建会话
- **URL**: `/ai/session`
- **Method**: `POST`
- **Body**:
  ```json
  {
    "title": "关于机器学习的讨论"
  }
  ```
- **Response**:
  ```json
  {
    "code": 200,
    "message": "success",
    "data": {
      "id": "550e8400-e29b-41d4-a716-446655440000",
      "userId": 1,
      "title": "关于机器学习的讨论",
      "createdAt": "2026-03-28T10:00:00.000+00:00",
      "updatedAt": "2026-03-28T10:00:00.000+00:00"
    }
  }
  ```

### 4.3 获取会话列表
- **URL**: `/ai/session/list`
- **Method**: `GET`
- **Response**:
  ```json
  {
    "code": 200,
    "message": "success",
    "data": [
      {
        "id": "550e8400-e29b-41d4-a716-446655440000",
        "userId": 1,
        "title": "关于机器学习的讨论",
        "createdAt": "2026-03-28T10:00:00.000+00:00",
        "updatedAt": "2026-03-28T10:00:00.000+00:00"
      }
    ]
  }
  ```

### 4.4 会话聊天（RAG问答）
- **URL**: `/ai/chat/{sessionId}`
- **Method**: `POST`
- **Body**:
  ```json
  {
    "message": "文档中提到的核心算法是什么？"
  }
  ```
- **Response**:
  ```json
  {
    "code": 200,
    "message": "success",
    "data": "文档中提到的核心算法包括支持向量机(SVM)、随机森林以及神经网络..."
  }
  ```

### 4.5 获取会话历史记录
- **URL**: `/ai/chat/{sessionId}/history`
- **Method**: `GET`
- **Response**:
  ```json
  {
    "code": 200,
    "message": "success",
    "data": [
      {
        "id": 1,
        "sessionId": "550e8400-e29b-41d4-a716-446655440000",
        "role": "user",
        "content": "文档中提到的核心算法是什么？",
        "source": null,
        "createdAt": "2026-03-28T10:01:00.000+00:00"
      },
      {
        "id": 2,
        "sessionId": "550e8400-e29b-41d4-a716-446655440000",
        "role": "ai",
        "content": "文档中提到的核心算法包括支持向量机(SVM)...",
        "source": null,
        "createdAt": "2026-03-28T10:01:05.000+00:00"
      }
    ]
  }
  ```

### 4.6 数据分析与图表生成
- **URL**: `/ai/analysis`
- **Method**: `POST`
- **Body**:
  ```json
  {
    "query": "查询借书数量前十的用户"
  }
  ```
- **Response**:
  ```json
  {
    "code": 200,
    "message": "success",
    "data": {
      "chartType": "bar",
      "data": [
        {
          "username": "user1",
          "borrow_count": 5
        },
        {
          "username": "user2",
          "borrow_count": 3
        }
      ]
    }
  }
  ```
