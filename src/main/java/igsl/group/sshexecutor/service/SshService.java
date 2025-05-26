package igsl.group.sshexecutor.service;

import com.jcraft.jsch.Channel;
import com.jcraft.jsch.ChannelExec;
import com.jcraft.jsch.JSch;
import com.jcraft.jsch.Session;
import igsl.group.sshexecutor.entity.ExecutionResult;
import igsl.group.sshexecutor.entity.SshConfig;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;

@Service
@Slf4j
public class SshService {

    private static final int SESSION_TIMEOUT = 30000; // 30 seconds
    private static final int CHANNEL_TIMEOUT = 30000; // 30 seconds

    public ExecutionResult executeCommand(SshConfig config) {
        long startTime = System.currentTimeMillis();
        Session session = null;
        Channel channel = null;

        try {
            // Create session
            JSch jsch = new JSch();
            session = jsch.getSession(config.getUsername(), config.getHost(), config.getPort());
            session.setPassword(config.getPassword());

            // Disable host key checking (for demo purposes - in production, use known_hosts)
            session.setConfig("StrictHostKeyChecking", "no");

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
            log.error("SSH execution failed for host: {}",  config.getHost(), e);
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

    public boolean testConnection(SshConfig config) {
        Session session = null;
        try {
            JSch jsch = new JSch();
            session = jsch.getSession(config.getUsername(), config.getHost(), config.getPort());
            session.setPassword(config.getPassword());
            session.setConfig("StrictHostKeyChecking", "no");
            session.connect(5000); // 5 second timeout for test
            return true;
        } catch (Exception e) {
            log.error("Connection test failed for host: {}", config.getHost(), e);
            return false;
        } finally {
            if (session != null && session.isConnected()) {
                session.disconnect();
            }
        }
    }
}