import { useState, useEffect } from 'react';
import apiService from '../services/apiService';

const ProfilePage = () => {
  const [profile, setProfile] = useState(null);
  const [claims, setClaims] = useState(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);
  const [showClaims, setShowClaims] = useState(false);

  useEffect(() => {
    loadProfile();
  }, []);

  const loadProfile = async () => {
    try {
      setLoading(true);
      const [profileData, claimsData] = await Promise.all([
        apiService.getUserProfile(),
        apiService.getClaims(),
      ]);
      setProfile(profileData);
      setClaims(claimsData);
      setError(null);
    } catch (err) {
      setError(err.response?.data?.message || 'Failed to load profile');
    } finally {
      setLoading(false);
    }
  };

  if (loading) {
    return (
      <div className="container" style={{ textAlign: 'center', padding: '50px' }}>
        <div className="loading"></div>
        <p style={{ marginTop: '20px' }}>Loading profile...</p>
      </div>
    );
  }

  if (error) {
    return (
      <div className="container" style={{ marginTop: '50px' }}>
        <div className="alert alert-error">{error}</div>
      </div>
    );
  }

  return (
    <div className="container" style={{ marginTop: '50px' }}>
      <h2 style={{ marginBottom: '24px' }}>User Profile</h2>

      <div className="card">
        <h3 style={{ marginBottom: '16px' }}>Profile Information</h3>
        <table style={{ width: '100%', borderCollapse: 'collapse' }}>
          <tbody>
            <tr style={{ borderBottom: '1px solid #444' }}>
              <td style={{ padding: '12px 0', fontWeight: '500', width: '200px' }}>Username:</td>
              <td style={{ padding: '12px 0' }}>{profile?.username || 'N/A'}</td>
            </tr>
            <tr style={{ borderBottom: '1px solid #444' }}>
              <td style={{ padding: '12px 0', fontWeight: '500' }}>Email:</td>
              <td style={{ padding: '12px 0' }}>{profile?.email || 'N/A'}</td>
            </tr>
            <tr style={{ borderBottom: '1px solid #444' }}>
              <td style={{ padding: '12px 0', fontWeight: '500' }}>First Name:</td>
              <td style={{ padding: '12px 0' }}>{profile?.firstName || 'N/A'}</td>
            </tr>
            <tr style={{ borderBottom: '1px solid #444' }}>
              <td style={{ padding: '12px 0', fontWeight: '500' }}>Last Name:</td>
              <td style={{ padding: '12px 0' }}>{profile?.lastName || 'N/A'}</td>
            </tr>
            <tr style={{ borderBottom: '1px solid #444' }}>
              <td style={{ padding: '12px 0', fontWeight: '500' }}>Full Name:</td>
              <td style={{ padding: '12px 0' }}>{profile?.fullName || 'N/A'}</td>
            </tr>
            <tr>
              <td style={{ padding: '12px 0', fontWeight: '500' }}>Email Verified:</td>
              <td style={{ padding: '12px 0' }}>
                {profile?.emailVerified ? (
                  <span style={{ color: '#28a745', fontWeight: '500' }}>✓ Verified</span>
                ) : (
                  <span style={{ color: '#dc3545', fontWeight: '500' }}>✗ Not Verified</span>
                )}
              </td>
            </tr>
          </tbody>
        </table>
      </div>

      {profile?.roles && profile.roles.length > 0 && (
        <div className="card">
          <h3 style={{ marginBottom: '16px' }}>Roles</h3>
          <div style={{ display: 'flex', flexWrap: 'wrap', gap: '8px' }}>
            {profile.roles.map((role, index) => (
              <span
                key={index}
                style={{
                  padding: '6px 12px',
                  background: '#646cff',
                  borderRadius: '16px',
                  fontSize: '14px',
                  fontWeight: '500',
                }}
              >
                {role}
              </span>
            ))}
          </div>
        </div>
      )}

      <div className="card">
        <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '16px' }}>
          <h3 style={{ margin: 0 }}>JWT Claims</h3>
          <button
            onClick={() => setShowClaims(!showClaims)}
            className="btn btn-secondary"
          >
            {showClaims ? 'Hide Claims' : 'Show Claims'}
          </button>
        </div>

        {showClaims && claims && (
          <pre
            style={{
              background: '#2a2a2a',
              padding: '16px',
              borderRadius: '6px',
              overflow: 'auto',
              maxHeight: '400px',
              fontSize: '13px',
              lineHeight: '1.5',
            }}
          >
            {JSON.stringify(claims, null, 2)}
          </pre>
        )}
      </div>

      <div style={{ marginTop: '24px', padding: '16px', background: '#2a2a2a', borderRadius: '6px' }}>
        <p style={{ fontSize: '14px', margin: 0, color: '#888' }}>
          This profile information is extracted from the JWT token provided by Keycloak
          and validated by the <strong>oidcplugplay</strong> OAuth2 starter.
        </p>
      </div>
    </div>
  );
};

export default ProfilePage;
