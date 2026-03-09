import { useEffect, useState } from 'react';
import { useNavigate, useSearchParams } from 'react-router-dom';
import { useAuth } from '../context/AuthContext';

/**
 * OAuth2 Callback Page
 * Handles the redirect from Keycloak after user authentication
 * Extracts the authorization code and exchanges it for tokens
 */
const CallbackPage = () => {
  const [searchParams] = useSearchParams();
  const navigate = useNavigate();
  const { handleOAuth2Callback } = useAuth();
  const [error, setError] = useState(null);
  const [processing, setProcessing] = useState(true);

  useEffect(() => {
    const processCallback = async () => {
      try {
        // Check for OAuth2 error response
        const errorParam = searchParams.get('error');
        const errorDescription = searchParams.get('error_description');

        if (errorParam) {
          setError(errorDescription || `Authentication error: ${errorParam}`);
          setProcessing(false);
          return;
        }

        // Get authorization code
        const code = searchParams.get('code');

        if (!code) {
          setError('No authorization code received from authentication server');
          setProcessing(false);
          return;
        }

        // Exchange code for tokens
        await handleOAuth2Callback(code);

        // Redirect to dashboard on success
        navigate('/dashboard', { replace: true });
      } catch (err) {
        console.error('OAuth2 callback error:', err);
        setError(err.message || 'Authentication failed');
        setProcessing(false);
      }
    };

    processCallback();
  }, [searchParams, handleOAuth2Callback, navigate]);

  if (error) {
    return (
      <div className="container" style={{ maxWidth: '500px', marginTop: '50px' }}>
        <div className="card">
          <h2 style={{ color: '#ff6b6b', marginBottom: '16px' }}>
            Authentication Error
          </h2>
          <div
            className="alert alert-error"
            style={{ marginBottom: '24px' }}
          >
            {error}
          </div>
          <button
            onClick={() => navigate('/login')}
            className="btn btn-primary"
            style={{ width: '100%' }}
          >
            Back to Login
          </button>
        </div>
      </div>
    );
  }

  return (
    <div className="container" style={{ maxWidth: '500px', marginTop: '50px' }}>
      <div className="card" style={{ textAlign: 'center' }}>
        <h2 style={{ marginBottom: '24px' }}>Processing Authentication</h2>
        <div
          className="loading"
          style={{
            display: 'inline-block',
            width: '40px',
            height: '40px',
            marginBottom: '16px',
          }}
        ></div>
        <p style={{ color: '#888' }}>
          Please wait while we complete your login...
        </p>
        <p style={{ fontSize: '14px', color: '#666', marginTop: '16px' }}>
          Exchanging authorization code for access tokens
        </p>
      </div>
    </div>
  );
};

export default CallbackPage;
