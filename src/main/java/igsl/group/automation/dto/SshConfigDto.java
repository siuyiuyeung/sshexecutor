package igsl.group.automation.dto;

import igsl.group.automation.entity.SshConfig;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class SshConfigDto {
    private String name;
    private String host;
    private Integer port;
    private String username;
    private SshConfig.AuthType authType;
    private String password;
    private String privateKey;
    private String passphrase;
    private String command;
    private String script;
    private String description;
    private boolean legacyMode = false;
}
