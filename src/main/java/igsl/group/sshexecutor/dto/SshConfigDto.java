package igsl.group.sshexecutor.dto;

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
    private String password;
    private String command;
    private String script;
    private String description;
}
