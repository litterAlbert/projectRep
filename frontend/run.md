# 智慧图书管理系统 - 前端运行说明

## 1. 环境准备

确保您的本地环境已经安装了以下软件：
- **Node.js**: 建议版本 v20.x 或以上 (项目 `engines` 要求 `^20.19.0 || >=22.12.0`)
- **npm**: 随 Node.js 安装

## 2. 依赖安装

进入 `frontend` 目录下，在终端中执行以下命令安装项目所需的依赖：

```bash
npm install
```

此命令会读取 `package.json` 中的依赖配置，下载并安装 Vue 3、Vite、Pinia、Vue Router、Element Plus、ECharts 以及 Axios 等核心包。

## 3. 启动开发服务器

依赖安装完成后，您可以启动本地开发服务器进行调试：

```bash
npm run dev
```

启动成功后，终端会显示本地访问地址，通常为：`http://localhost:5173`。

## 4. 打包构建

当项目开发完成需要部署到生产环境时，可执行以下命令进行打包：

```bash
npm run build
```

打包完成后，项目根目录会生成一个 `dist` 文件夹，您可以将该文件夹中的静态资源部署到 Nginx、Nginx 或任何静态服务器上。

## 5. 预览打包结果

如果在打包后想要在本地预览生产环境的构建结果，可以执行：

```bash
npm run preview
```

## 6. 后端接口配置说明

项目通过 Vite 代理解决了跨域问题。请在 `vite.config.js` 文件中找到 `server.proxy` 的配置：

```javascript
server: {
  proxy: {
    '/api': {
      target: 'http://localhost:8080', // 修改为您的后端实际运行地址
      changeOrigin: true,
      rewrite: (path) => path.replace(/^\/api/, '')
    }
  }
}
```

前端的统一请求前缀已配置在 `src/utils/request.js` 中：

```javascript
const request = axios.create({
  baseURL: '/api', // 使用代理解决跨域
  timeout: 10000
})
```
