一旦我所属的文件夹有所变化，请更新我。
本目录存放 keystore 相关内容。
用于组织本模块代码与资源。

## 文件清单

| 文件 | 地位 | 功能 |
| --- | --- | --- |
| `README.md` | 说明文档 | 本目录说明 |
| `jwt_private.pem` | 配置 | JWT 私钥 (开发环境默认值) |
| `jwt_public.pem` | 配置 | JWT 公钥 (开发环境默认值) |
| `truststore.p12` | 配置 | 电子签章信任库 |

## 默认配置说明

本目录下的 `jwt_private.pem` 和 `jwt_public.pem` 是**开发环境的默认值**，在 `application.yml` 中配置为：

```yaml
jwt:
  public-key-location: ${JWT_PUBLIC_KEY_LOCATION:classpath:keystore/jwt_public.pem}
  private-key-location: ${JWT_PRIVATE_KEY_LOCATION:classpath:keystore/jwt_private.pem}
```

**生产环境** 必须通过环境变量覆盖：
- `JWT_PUBLIC_KEY_LOCATION` - 生产环境 JWT 公钥路径
- `JWT_PRIVATE_KEY_LOCATION` - 生产环境 JWT 私钥路径
