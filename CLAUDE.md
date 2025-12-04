# Claude Code 项目配置

## 工作规则

### 代码编写原则

**重要：如果没有我的特殊要求，请永远不要帮我进行代码的编写**

- 只在我明确要求时才编写、修改或生成代码
- 默认情况下，只提供：
  - 架构建议和分析
  - 技术方案讨论
  - 代码审查和问题诊断
  - 文档和说明
  - 回答技术问题

## 项目信息

- **项目名称**: VideoFeedDemo
- **项目类型**: Android 应用
- **架构模式**: MVVM (Model-View-ViewModel)
- **开发语言**: Kotlin
- **包名**: cn.mareep.videofeeddemo

### 技术栈

- **最低 SDK**: 24 (Android 7.0)
- **目标 SDK**: 36
- **编译 SDK**: 36
- **JVM 目标**: 11
- **构建工具**: Gradle (Kotlin DSL)

### 核心依赖

- **架构组件**: Jetpack ViewModel + LiveData
- **依赖注入**: Hilt
- **网络请求**: OkHttp
- **图片加载**: Coil
- **视频播放**: ExoPlayer
- **本地存储**: Room Database
- **导航**: Navigation Component
- **UI绑定**: ViewBinding
- **基础库**: AndroidX Core KTX, AppCompat, Material Components, ConstraintLayout

## 架构说明

### 目录结构

```
cn.mareep.videofeeddemo/
├── data/                    # 数据层
│   ├── local/              # 本地数据
│   │   ├── database/       # 数据库相关
│   │   └── entity/         # 数据实体
│   ├── network/            # 网络层
│   │   ├── api/            # API 接口定义
│   │   ├── dto/            # 数据传输对象
│   │   └── interceptor/    # 网络拦截器
│   └── repository/         # 数据仓库
├── di/                     # 依赖注入
├── ui/                     # UI 层
│   └── main/               # 主界面
└── utils/                  # 工具类
```

### MVVM 分层

1. **Model (数据层)**
   - `data/local`: 本地数据存储 (Room Database)
   - `data/network`: 网络请求 (OkHttp)
   - `data/repository`: 数据仓库，统一数据源

2. **View (视图层)**
   - `ui/`: Activity/Fragment (使用 ViewBinding)
   - 负责 UI 展示和用户交互
   - 使用 Navigation Component 进行页面导航
   - 图片加载使用 Coil
   - 视频播放使用 ExoPlayer

3. **ViewModel (视图模型层)**
   - 使用 Jetpack ViewModel
   - 通过 LiveData 向 View 暴露数据
   - 连接 View 和 Model
   - 处理业务逻辑

4. **其他组件**
   - `di/`: 依赖注入配置 (Hilt)
   - `utils/`: 通用工具类

---

*本文件用于指导 Claude Code 在本项目中的工作方式*
