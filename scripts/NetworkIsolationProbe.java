import java.net.Socket;

public class NetworkIsolationProbe {
    public static void main(String[] args) throws Exception {
        try (Socket socket = new Socket("198.51.100.1", 443)) {
            throw new IllegalStateException("外部 socket 未受阻擋，離線隔離無效");
        } catch (java.security.AccessControlException expected) {
            System.out.println("PASS: non-loopback socket is denied by the test policy");
        }
    }
}
