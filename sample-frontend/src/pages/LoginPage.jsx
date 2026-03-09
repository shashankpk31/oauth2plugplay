import { useState } from "react";
import { useNavigate } from "react-router-dom";
import { useAuth } from "../context/AuthContext";

/**
 * Dynamic Login Page
 * Automatically adapts to backend configuration:
 * - Shows username/password form if backend has oauth2.custom-login-enabled=true
 * - Shows "Login with Provider" button if backend has oauth2.custom-login-enabled=false
 *
 * No manual configuration needed - frontend syncs with backend automatically!
 */
const LoginPage = () => {
  const [username, setUsername] = useState("");
  const [password, setPassword] = useState("");
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState(null);
  const navigate = useNavigate();
  const { login, loginWithOAuth2, authConfig, configLoaded } = useAuth();

  // Custom login with username/password
  const handleCustomLogin = async (e) => {
    e.preventDefault();
    setError(null);
    setLoading(true);

    try {
      await login(username, password);
      navigate("/dashboard");
    } catch (err) {
      setError(err.message);
    } finally {
      setLoading(false);
    }
  };

  // OAuth2 redirect login
  const handleOAuth2Login = async () => {
    try {
      setLoading(true);
      setError(null);
      // This will redirect to Keycloak
      await loginWithOAuth2();
    } catch (err) {
      setError(err.message);
      setLoading(false);
    }
  };

  // Show loading state while fetching config
  if (!configLoaded) {
    return (
      <div className="container" style={{ maxWidth: "500px", marginTop: "50px" }}>
        <div className="card" style={{ textAlign: "center" }}>
          <h2 style={{ marginBottom: "24px" }}>Loading...</h2>
          <div
            className="loading"
            style={{
              display: "inline-block",
              width: "40px",
              height: "40px",
              marginBottom: "16px",
            }}
          ></div>
          <p style={{ color: "#888" }}>
            Fetching authentication configuration from backend...
          </p>
        </div>
      </div>
    );
  }

  // Get provider display name
  const providerName = authConfig.provider
    ? authConfig.provider.charAt(0) + authConfig.provider.slice(1).toLowerCase()
    : 'Provider';

  return (
    <div className="container" style={{ maxWidth: "500px", marginTop: "50px" }}>
      <div className="card">
        <h2 style={{ marginBottom: "24px", textAlign: "center" }}>Login</h2>

        {error && <div className="alert alert-error">{error}</div>}

        {authConfig.useCustomLogin() ? (
          /* Custom Login Mode - Username/Password Form */
          <>
            <form onSubmit={handleCustomLogin}>
              <div className="form-group">
                <label htmlFor="username">Username</label>
                <input
                  type="text"
                  id="username"
                  value={username}
                  onChange={(e) => setUsername(e.target.value)}
                  placeholder="Enter your username"
                  required
                  disabled={loading}
                />
              </div>

              <div className="form-group">
                <label htmlFor="password">Password</label>
                <input
                  type="password"
                  id="password"
                  value={password}
                  onChange={(e) => setPassword(e.target.value)}
                  placeholder="Enter your password"
                  required
                  disabled={loading}
                />
              </div>

              <button
                type="submit"
                className="btn btn-primary"
                style={{ width: "100%", marginTop: "16px" }}
                disabled={loading}
              >
                {loading ? (
                  <>
                    <span className="loading" style={{ marginRight: "8px" }}></span>
                    Logging in...
                  </>
                ) : (
                  "Login"
                )}
              </button>
            </form>

            <div
              style={{
                marginTop: "24px",
                padding: "16px",
                background: "#2a2a2a",
                borderRadius: "6px",
              }}
            >
              <p style={{ fontSize: "14px", marginBottom: "8px" }}>
                <strong>Demo Credentials:</strong>
              </p>
              <p style={{ fontSize: "14px", margin: 0 }}>
                Username: <code>testuser</code>
                <br />
                Password: <code>testUser</code>
              </p>
            </div>
          </>
        ) : (
          /* OAuth2 Mode - Redirect to Provider */
          <>
            <button
              onClick={handleOAuth2Login}
              className="btn btn-primary"
              style={{ width: "100%", marginTop: "16px" }}
              disabled={loading}
            >
              {loading ? (
                <>
                  <span className="loading" style={{ marginRight: "8px" }}></span>
                  Redirecting to {providerName}...
                </>
              ) : (
                `Login with ${providerName}`
              )}
            </button>

            <div
              style={{
                marginTop: "24px",
                padding: "16px",
                background: "#2a2a2a",
                borderRadius: "6px",
              }}
            >
              <p style={{ fontSize: "14px", marginBottom: "8px" }}>
                <strong>Authentication Flow:</strong>
              </p>
              <p style={{ fontSize: "14px", margin: 0 }}>
                You will be redirected to {providerName}'s login page. After
                successful authentication, you'll be redirected back here.
              </p>
              <p style={{ fontSize: "14px", marginTop: "12px", color: "#888" }}>
                Demo: <code>testuser</code> / <code>testUser</code>
              </p>
            </div>
          </>
        )}

        <button
          className="btn btn-primary"
          style={{ width: "100%", marginTop: "16px" }}
          disabled={loading}
          onClick={() => {
            window.location.href =
              "http://localhost:8180/realms/sample-realm/protocol/openid-connect/registrations?client_id=sample-app&redirect_uri=http://localhost:5173";
          }}
        >
          Register New Account
        </button>

        <div
          style={{
            marginTop: "16px",
            fontSize: "14px",
            textAlign: "center",
            color: "#888",
          }}
        >
          <p>
            This demo uses <strong>oidcplugplay</strong> OAuth2 starter with
            Keycloak
          </p>
          <p style={{ fontSize: "12px", marginTop: "8px" }}>
            Mode: {authConfig.useCustomLogin() ? "✏️ Custom Login" : "🔐 OAuth2 Redirect"}
            {" • "}
            Provider: {providerName}
          </p>
        </div>
      </div>
    </div>
  );
};

export default LoginPage;
