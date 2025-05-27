package igsl.group.sshexecutor.controller;

import com.jcraft.jsch.JSch;
import com.jcraft.jsch.KeyPair;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.io.ByteArrayOutputStream;
import java.util.HashMap;
import java.util.Map;


@RestController
@RequestMapping("/api/keys")
public class KeyGenerationController {
    @PostMapping("/generate")
    public ResponseEntity<Map<String, String>> generateKeyPair(
            @RequestParam(defaultValue = "RSA") String keyType,
            @RequestParam(defaultValue = "2048") int keySize) {

        try {
            JSch jsch = new JSch();

            // Determine key type
            int type;
            switch (keyType.toUpperCase()) {
                case "DSA":
                    type = KeyPair.DSA;
                    break;
                case "RSA":
                default:
                    type = KeyPair.RSA;
                    break;
            }

            // Generate key pair
            KeyPair keyPair = KeyPair.genKeyPair(jsch, type, keySize);

            // Get private key
            ByteArrayOutputStream privateKeyOut = new ByteArrayOutputStream();
            keyPair.writePrivateKey(privateKeyOut);
            String privateKey = privateKeyOut.toString();

            // Get public key
            ByteArrayOutputStream publicKeyOut = new ByteArrayOutputStream();
            keyPair.writePublicKey(publicKeyOut, "ssh-manager-generated-key");
            String publicKey = publicKeyOut.toString();

            // Dispose of key pair
            keyPair.dispose();

            Map<String, String> response = new HashMap<>();
            response.put("privateKey", privateKey);
            response.put("publicKey", publicKey);
            response.put("type", keyType);
            response.put("size", String.valueOf(keySize));

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            Map<String, String> error = new HashMap<>();
            error.put("error", "Failed to generate key pair: " + e.getMessage());
            return ResponseEntity.internalServerError().body(error);
        }
    }
}
