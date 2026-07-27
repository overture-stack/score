# Vault Integration

[HashiCorp's Vault](https://www.vaultproject.io/) provides secure secret management and can be integrated with Score for enhanced security. This section covers how to configure Score to work with Vault.

:::info Optional Integration
Most deployments do not require Vault integration. The `optional:` prefix means your application will still start even if Vault is unavailable. If you're not explicitly using HashiCorp's Vault, you can skip this configuration.
:::

## Configuration Options

### Application YAML Configuration

The primary way to configure Vault is in your `application.yml`:

```yaml title="./score-server/src/main/resources/application.yml"
spring:
  config:
    import: optional:vault://  # Required for Vault integration
  cloud:
    vault:
      # Basic Configuration (Optional)
      # host: localhost
      # port: 8200
      # scheme: https
      
      # Authentication (Choose ONE method)
      
      # Method 1: Token Authentication
      # authentication: TOKEN
      # token: your-vault-token
      
      # Method 2: AppRole Authentication
      # authentication: APPROLE
      # app-role:
      #   role-id: your-role-id
      #   secret-id: your-secret-id
      #   app-auth-path: approle
      
      # Connection Timeouts (Optional)
      # connection-timeout: 5000
      # read-timeout: 15000
```

### Environment Variables

For Docker deployments, you can configure Vault through environment variables:

```bash 
# ============================
# Vault Configuration
# ============================

# Required - enables Vault integration and configuration import
SPRING_CONFIG_IMPORT=optional:vault://

# Basic Configuration (Optional)
SPRING_CLOUD_VAULT_HOST=localhost
SPRING_CLOUD_VAULT_PORT=8200
SPRING_CLOUD_VAULT_SCHEME=https  # Use 'https' for production

# Authentication (Choose ONE method)

# Method 1: Token Authentication
SPRING_CLOUD_VAULT_AUTHENTICATION=TOKEN
SPRING_CLOUD_VAULT_TOKEN=your-vault-token

# Method 2: AppRole Authentication
# SPRING_CLOUD_VAULT_AUTHENTICATION=APPROLE
# SPRING_CLOUD_VAULT_APP_ROLE_ROLE_ID=your-role-id
# SPRING_CLOUD_VAULT_APP_ROLE_SECRET_ID=your-secret-id
# SPRING_CLOUD_VAULT_APP_ROLE_APP_AUTH_PATH=approle

# Connection Timeouts (Optional)
SPRING_CLOUD_VAULT_CONNECTION_TIMEOUT=5000
SPRING_CLOUD_VAULT_READ_TIMEOUT=15000
```

## Authentication Methods

There are two authentication methods with Vault:

1. **Token Authentication** (simplest, but less secure for production):
    ```yaml
    authentication: TOKEN
    token: your-vault-token
    ```

2. **AppRole Authentication** (recommended for services):
    ```yaml
    authentication: APPROLE
    app-role:
      role-id: your-role-id
      secret-id: your-secret-id
      app-auth-path: approle
    ```

    :::info AppRole Authentication
    AppRole is Vault's recommended authentication method for applications and services. It uses two components:

    - `role-id`: A permanent identifier (like a username)
    - `secret-id`: A rotatable credential (like a password)

    AppRole provides enhanced security features like credential rotation, fine-grained access control, and IP restrictions. Contact your Vault administrator to set up AppRole authentication. xwFor details, see [Vault's AppRole documentation](https://developer.hashicorp.com/vault/docs/auth/approle).
    :::

    :::warning Security Notice
    Always follow HashiCorp's security guidelines when deploying Vault in production. Never commit sensitive values to version control - use environment variables or secure secret management.
    :::

For more information about Vault configuration, refer to:
- [Official Vault Documentation](https://www.vaultproject.io/docs)
- [Spring Cloud Vault Documentation](https://docs.spring.io/spring-cloud-vault/docs/current/reference/html/)