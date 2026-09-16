package api.models;

public record AuthRequest(String username, String password) {
    @Override
    public String toString() { return "AuthRequest[credentials redacted]"; }
}

