package igsl.group.automation.entity;

import igsl.group.automation.listener.SshConfigEntityListener;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "ssh_configs")
@EntityListeners(SshConfigEntityListener.class)
public class SshConfig {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank(message = "Name is required")
    @Column(unique = true)
    private String name;

    @NotBlank(message = "Host is required")
    private String host;

    @NotNull(message = "Port is required")
    private Integer port = 22;

    @NotBlank(message = "Username is required")
    private String username;

    @Enumerated(EnumType.STRING)
    @Column(name = "auth_type")
    private AuthType authType = AuthType.PASSWORD;

    private String password;

    @Column(name = "private_key", columnDefinition = "TEXT")
    private String privateKey;

    @Column(name = "passphrase")
    private String passphrase;

    @Column(columnDefinition = "TEXT")
    private String command;

    @Column(columnDefinition = "TEXT")
    private String script;

    private String description;

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @Transient
    private boolean passwordEncrypted = false;

    @Transient
    private boolean passphraseEncrypted = false;

    @Column(name = "legacy_mode")
    private boolean legacyMode = false;

    public enum AuthType {
        PASSWORD,
        PRIVATE_KEY
    }
}