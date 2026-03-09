import { useEffect, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { useAuth } from '../context/AuthContext';
import apiService from '../services/apiService';

const HomePage = () => {
  const navigate = useNavigate();
  const { isAuthenticated } = useAuth();
  const [publicMessage, setPublicMessage] = useState(null);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    loadPublicData();
  }, []);

  const loadPublicData = async () => {
    try {
      const data = await apiService.getPublicHello();
      setPublicMessage(data);
    } catch (err) {
      console.error('Failed to load public data:', err);
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="container" style={{ marginTop: '50px' }}>
      <div className="card" style={{ textAlign: 'center' }}>
        <h1 style={{ fontSize: '36px', marginBottom: '16px', color: '#646cff' }}>
          🔐 oidcplugplay Demo
        </h1>
        <p style={{ fontSize: '18px', marginBottom: '32px', color: '#888' }}>
          Enterprise-level OAuth2/OIDC Authentication with Keycloak
        </p>

        {!loading && publicMessage && (
          <div
            style={{
              background: '#2a2a2a',
              padding: '24px',
              borderRadius: '8px',
              marginBottom: '32px',
            }}
          >
            <h3 style={{ marginBottom: '12px' }}>Public API Test</h3>
            <p style={{ fontSize: '16px', color: '#646cff', marginBottom: '8px' }}>
              {publicMessage.message}
            </p>
            <p style={{ fontSize: '14px', color: '#888' }}>
              {publicMessage.description}
            </p>
          </div>
        )}

        {!isAuthenticated ? (
          <div>
            <button
              onClick={() => navigate('/login')}
              className="btn btn-primary"
              style={{ fontSize: '16px', padding: '12px 32px' }}
            >
              Login to Continue
            </button>
          </div>
        ) : (
          <div>
            <p style={{ fontSize: '16px', marginBottom: '24px', color: '#28a745' }}>
              ✓ You are authenticated!
            </p>
            <div style={{ display: 'flex', gap: '16px', justifyContent: 'center' }}>
              <button
                onClick={() => navigate('/dashboard')}
                className="btn btn-primary"
              >
                Go to Dashboard
              </button>
              <button
                onClick={() => navigate('/profile')}
                className="btn btn-secondary"
              >
                View Profile
              </button>
            </div>
          </div>
        )}
      </div>

      <div className="card">
        <h3 style={{ marginBottom: '16px' }}>Features</h3>
        <ul style={{ listStyle: 'none', padding: 0 }}>
          {[
            '✅ JWT-based authentication with Keycloak',
            '✅ Automatic token refresh on expiration',
            '✅ Protected routes with React Router',
            '✅ User profile and dashboard',
            '✅ Role-based access control ready',
            '✅ Enterprise-grade security',
          ].map((feature, index) => (
            <li
              key={index}
              style={{
                padding: '12px',
                background: '#2a2a2a',
                marginBottom: '8px',
                borderRadius: '6px',
                fontSize: '15px',
              }}
            >
              {feature}
            </li>
          ))}
        </ul>
      </div>

      <div className="card">
        <h3 style={{ marginBottom: '16px' }}>Technology Stack</h3>
        <div style={{ display: 'flex', flexWrap: 'wrap', gap: '12px' }}>
          {[
            'React 18',
            'Vite',
            'React Router',
            'Axios',
            'Spring Boot 3.3.12',
            'oidcplugplay 1.0.0',
            'Keycloak 23',
            'Docker',
          ].map((tech, index) => (
            <span
              key={index}
              style={{
                padding: '8px 16px',
                background: '#646cff',
                borderRadius: '20px',
                fontSize: '14px',
                fontWeight: '500',
              }}
            >
              {tech}
            </span>
          ))}
        </div>
      </div>

      <div
        style={{
          marginTop: '32px',
          padding: '24px',
          background: '#2a2a2a',
          borderRadius: '8px',
          textAlign: 'center',
        }}
      >
        <p style={{ fontSize: '14px', color: '#888', margin: 0 }}>
          This is a sample application demonstrating the{' '}
          <strong style={{ color: '#646cff' }}>oidcplugplay</strong> OAuth2/OIDC
          Spring Boot starter library with Keycloak as the identity provider.
        </p>
      </div>
    </div>
  );
};

export default HomePage;
