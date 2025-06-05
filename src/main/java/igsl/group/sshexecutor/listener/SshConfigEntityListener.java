package igsl.group.sshexecutor.listener;

import igsl.group.sshexecutor.entity.SshConfig;
import igsl.group.sshexecutor.jasypt.JasyptUtils;
import jakarta.persistence.PostLoad;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import org.springframework.stereotype.Component;

@Component
public class SshConfigEntityListener {
    @PrePersist
    @PreUpdate
    public void encryptSensitiveData(SshConfig config) {
        // Encrypt password if not already encrypted
        if (config.getPassword() != null && !config.getPassword().isEmpty() && !config.isPasswordEncrypted() && !JasyptUtils.isEncrypted(config.getPassword())) {
            config.setPassword(JasyptUtils.encrypt(config.getPassword()));
            config.setPasswordEncrypted(true);
        }

        // Encrypt passphrase if not already encrypted
        if (config.getPassphrase() != null && !config.getPassphrase().isEmpty() && !config.isPassphraseEncrypted() && !JasyptUtils.isEncrypted(config.getPassphrase())) {
            config.setPassphrase(JasyptUtils.encrypt(config.getPassphrase()));
            config.setPassphraseEncrypted(true);
        }
    }

    @PostLoad
    public void decryptSensitiveData(SshConfig config) {
        // Decrypt password
        if (config.getPassword() != null && !config.getPassword().isEmpty() && JasyptUtils.isEncrypted(config.getPassword())) {
            config.setPassword(JasyptUtils.decrypt(config.getPassword()));
            config.setPasswordEncrypted(true);
        }

        // Decrypt passphrase
        if (config.getPassphrase() != null && !config.getPassphrase().isEmpty() && JasyptUtils.isEncrypted(config.getPassphrase())) {
            config.setPassphrase(JasyptUtils.decrypt(config.getPassphrase()));
            config.setPassphraseEncrypted(true);
        }
    }
}
