# Vmq-App - 免 Root 收款监听助手

## 📱 项目简介

这是一款基于V 免签开发的 Android 收款监听应用，**无需 Root 权限和框架**即可实现支付宝和微信收款消息的自动监听与回调。

### ✨ 核心功能

- 🎯 **双平台监听**：支持支付宝和微信收款通知监听
- 🔔 **智能回调**：匹配服务端订单金额后自动触发回调
- 📊 **日志面板**：实时查看监听日志和回调记录
- 👥 **店员管理**：支持店员监听功能
- 🔋 **持久运行**：电池白名单保护，后台稳定运行
- ⚡ **性能优化**：精简代码结构，启动速度提升

### 🛠️ 版本改进

相比原版的主要改进：
- ✅ 修复支付宝和微信不回调的 BUG
- ✅ 优化代码结构，提升执行效率
- ✅ 删除冗余代码，加快启动速度
- ✅ 增加电池白名单权限，防止被系统杀掉进程

#### 有任何建议欢迎致信我，如果可能的话可增加功能，如果本项目对您有帮助请给我一个免费的Star⭐

## 🏗️ 技术架构

- **开发语言**：Java + XML
- **目标平台**：Android 5.0+ (API 21)
- **编译 SDK**：Android 36
- **构建工具**：Gradle 8.12.0
- **开发环境**：Android Studio / AIDE


## 📥 安装使用

### 方式一：直接安装（推荐）

从 [Releases](https://github.com/shinian-a/Vmq-App/releases) 下载最新 APK 安装包，完成以下操作：

1. 安装 APK 到 Android 设备
2. 授予必要的权限（通知读取、自启动等）
3. 配置服务端信息
4. 保持应用在前台或后台运行

### 方式二：源码构建

#### IntelliJ IDEA 构建
```bash
# 克隆项目到本地
git clone https://github.com/shinian-a/Vmq-App.git

# 使用 IntelliJ IDEA 打开项目(请提前安装Android插件)
# 等待 Gradle 同步完成
# 点击 Build → Build Bundle(s) / APK(s) → Build APK(s)
```

#### AIDE构建（可能无效）
将项目源码导入 `/sdcard/AppProjects` 目录，使用AIDE 进行构建编译。

## 💾 下载镜像

| 镜像源 | 链接 | 备注 |
|--------|------|------|
| GitHub Releases | [下载地址](https://github.com/shinian-a/Vmq-App/releases) | 官方最新版 |
| 蓝奏云 | [下载地址](https://shinianacn.lanzouy.com/b027kqata) | 密码：vmq |
| Gitee | [下载地址](https://gitee.com/shinian-a/Vmq-App/releases) | 国内镜像 |

## ⚙️ 使用说明

### 必要配置

1. **微信配置**
   - 关注公众号"微信支付"和"微信收款助手"
   - 开启微信收款通知权限

2. **支付宝配置**
   - 开启支付宝收款通知权限

3. **系统权限**（强烈推荐）
   - ✅ 开启**电池白名单**权限
   - ✅ 允许**自启动**和**后台运行**
   - ✅ 授予**通知读取**权限
   - 📱 具体方法请百度您手机品牌的设置教程

### 运行模式

应用支持以下运行方式（需满足上述权限）：
- 📱 前台运行
- 🔄 后台运行
- 🌙 息屏后台运行

### 注意事项

- **分辨率适配**：推荐分辨率 2400×1080，部分低分辨率设备可能显示异常（后续版本会优化）
- **云手机兼容性**：如遇到监听权限异常，请及时反馈
- **更新建议**：Android 生态更新频繁，请保持使用最新版本以确保业务稳定

### 故障排查

如遇问题，请按以下步骤检查：
1. 确认已授予所有必要权限
2. 检查电池白名单是否开启
3. 确认应用处于运行状态（前台/后台）
4. 查看日志面板确认回调状态
5. 升级到最新版本

## 📬 联系与支持

### 联系方式
- 📧 邮箱：[shiniana@qq.com](mailto:shiniana@qq.com)
- 💡 如有任何建议或遇到问题，欢迎致信交流

### 致谢
本项目基于 [V免签](https://github.com/szvone/Vmq) 开发，感谢原作者的贡献。

---

<div align="center">

**如果这个项目对您有帮助，请给一个免费的 Star⭐！**

[![Star History Chart](https://api.star-history.com/svg?repos=shinian-a/Vmq-App&type=Date)](https://star-history.com/#shinian-a/Vmq-App&Date)

</div>
