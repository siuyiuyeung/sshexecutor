package igsl.group.sshexecutor.service;

import com.jcraft.jsch.Channel;
import com.jcraft.jsch.ChannelExec;
import com.jcraft.jsch.JSch;
import com.jcraft.jsch.JSchException;
import com.jcraft.jsch.Session;
import igsl.group.sshexecutor.entity.ExecutionResult;
import igsl.group.sshexecutor.entity.SshConfig;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.Properties;

@Service
@Slf4j
public class SshService {

    private static final int SESSION_TIMEOUT = 30000; // 30 seconds
    private static final int CHANNEL_TIMEOUT = 30000; // 30 seconds

    public ExecutionResult executeCommand(SshConfig config) {
        long startTime = System.currentTimeMillis();
        Session session = null;
        Channel channel = null;

        log.info("Starting SSH execution for host: {} ({}@{}:{})",
                config.getName(), config.getUsername(), config.getHost(), config.getPort());

        try {
            // Create session
            JSch jsch = new JSch();

            // Configure authentication based on type
            if (config.getAuthType() == SshConfig.AuthType.PRIVATE_KEY) {
                // Add private key
                if (config.getPrivateKey() != null && !config.getPrivateKey().trim().isEmpty()) {
                    setPrivateKey(config, jsch);
                } else {
                    throw new Exception("Private key is required for key-based authentication");
                }
            }

            session = jsch.getSession(config.getUsername(), config.getHost(), config.getPort());

            // Set password for password authentication
            if (config.getAuthType() == SshConfig.AuthType.PASSWORD) {
                if (config.getPassword() == null || config.getPassword().trim().isEmpty()) {
                    throw new Exception("Password is required for password authentication");
                }
                session.setPassword(config.getPassword());
            }

            // Session configuration
            Properties sessionConfig = getProperties(config);
            session.setConfig(sessionConfig);

            // Connect
            session.connect(SESSION_TIMEOUT);

            // Determine command to execute
            String commandToExecute = config.getCommand();
            if (config.getScript() != null && !config.getScript().trim().isEmpty()) {
                commandToExecute = config.getScript();
            }

            // Execute command
            channel = session.openChannel("exec");
            ((ChannelExec) channel).setCommand(commandToExecute);

            // Get output streams
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            ByteArrayOutputStream errorStream = new ByteArrayOutputStream();

            channel.setOutputStream(outputStream);
            ((ChannelExec) channel).setErrStream(errorStream);

            // Connect and execute
            channel.connect(CHANNEL_TIMEOUT);

            // Wait for command to complete
            while (!channel.isClosed()) {
                Thread.sleep(100);
            }

            int exitCode = channel.getExitStatus();
            long executionTime = System.currentTimeMillis() - startTime;

            log.info("SSH execution completed for {} - Exit code: {}, Time: {}ms",
                    config.getName(), exitCode, executionTime);

            if (log.isDebugEnabled()) {
                log.debug("Command output for {}: {}", config.getName(),
                        outputStream.toString().substring(0, Math.min(outputStream.toString().length(), 200)));
            }

            return ExecutionResult.builder()
                    .sshConfig(config)
                    .command(commandToExecute)
                    .output(outputStream.toString())
                    .error(errorStream.toString())
                    .exitCode(exitCode)
                    .success(exitCode == 0)
                    .executionTime(executionTime)
                    .build();

        } catch (Exception e) {
            log.error("SSH execution failed for {} ({}@{}:{})",
                    config.getName(), config.getUsername(), config.getHost(), config.getPort(), e);
            long executionTime = System.currentTimeMillis() - startTime;

            return ExecutionResult.builder()
                    .sshConfig(config)
                    .command(config.getCommand())
                    .output("")
                    .error("Connection failed: " + e.getMessage())
                    .exitCode(-1)
                    .success(false)
                    .executionTime(executionTime)
                    .build();

        } finally {
            if (channel != null && channel.isConnected()) {
                channel.disconnect();
            }
            if (session != null && session.isConnected()) {
                session.disconnect();
            }
        }
    }

    private static Properties getProperties(SshConfig config) {
        Properties sessionConfig = new Properties();
        sessionConfig.put("StrictHostKeyChecking", "no");

        // Apply legacy mode settings if enabled
        if (config.isLegacyMode()) {
            sessionConfig.put("X11Forwarding", "no");
            sessionConfig.put("Compression", "no");
            sessionConfig.put("TerminalType", "vt100");
        }

        if (config.getAuthType() == SshConfig.AuthType.PRIVATE_KEY) {
            sessionConfig.put("PreferredAuthentications", "publickey");
        }
        return sessionConfig;
    }

    public boolean testConnection(SshConfig config) {
        Session session = null;
        try {
            JSch jsch = new JSch();

            // Configure authentication
            if (config.getAuthType() == SshConfig.AuthType.PRIVATE_KEY) {
                if (config.getPrivateKey() != null && !config.getPrivateKey().trim().isEmpty()) {
                    setPrivateKey(config, jsch);
                }
            }

            session = jsch.getSession(config.getUsername(), config.getHost(), config.getPort());

            if (config.getAuthType() == SshConfig.AuthType.PASSWORD) {
                session.setPassword(config.getPassword());
            }

            java.util.Properties sessionConfig = new java.util.Properties();
            sessionConfig.put("StrictHostKeyChecking", "no");
            if (config.getAuthType() == SshConfig.AuthType.PRIVATE_KEY) {
                sessionConfig.put("PreferredAuthentications", "publickey");
            }
            session.setConfig(sessionConfig);

            session.connect(5000); // 5 second timeout for test
            return true;
        } catch (Exception e) {
            log.error("Connection test failed for host: " + config.getHost(), e);
            return false;
        } finally {
            if (session != null && session.isConnected()) {
                session.disconnect();
            }
        }
    }

    private void setPrivateKey(SshConfig config, JSch jsch) throws JSchException {
        if (config.getPassphrase() != null && !config.getPassphrase().trim().isEmpty()) {
            jsch.addIdentity("key",
                    config.getPrivateKey().getBytes(StandardCharsets.UTF_8),
                    null,
                    config.getPassphrase().getBytes(StandardCharsets.UTF_8));
        } else {
            jsch.addIdentity("key",
                    config.getPrivateKey().getBytes(StandardCharsets.UTF_8),
                    null,
                    null);
        }
    }
}