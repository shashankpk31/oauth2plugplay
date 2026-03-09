import { useState, useEffect } from 'react';
import apiService from '../services/apiService';
import { useAuth } from '../context/AuthContext';

const DashboardPage = () => {
  const [dashboardData, setDashboardData] = useState(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);
  const { user } = useAuth();

  useEffect(() => {
    loadDashboard();
  }, []);

  const loadDashboard = async () => {
    try {
      setLoading(true);
      const data = await apiService.getDashboard();
      setDashboardData(data);
      setError(null);
    } catch (err) {
      setError(err.response?.data?.message || 'Failed to load dashboard');
    } finally {
      setLoading(false);
    }
  };

  if (loading) {
    return (
      <div className="container" style={{ textAlign: 'center', padding: '50px' }}>
        <div className="loading"></div>
        <p style={{ marginTop: '20px' }}>Loading dashboard...</p>
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
      <h2 style={{ marginBottom: '24px' }}>Dashboard</h2>

      <div className="card">
        <h3 style={{ marginBottom: '16px' }}>Welcome Message</h3>
        <p style={{ fontSize: '18px', color: '#646cff' }}>
          {dashboardData?.welcomeMessage}
        </p>
      </div>

      <div className="card">
        <h3 style={{ marginBottom: '16px' }}>User Information</h3>
        <table style={{ width: '100%', borderCollapse: 'collapse' }}>
          <tbody>
            <tr style={{ borderBottom: '1px solid #444' }}>
              <td style={{ padding: '12px 0', fontWeight: '500' }}>Username:</td>
              <td style={{ padding: '12px 0' }}>{user?.preferred_username || 'N/A'}</td>
            </tr>
            <tr style={{ borderBottom: '1px solid #444' }}>
              <td style={{ padding: '12px 0', fontWeight: '500' }}>Email:</td>
              <td style={{ padding: '12px 0' }}>{user?.email || 'N/A'}</td>
            </tr>
            <tr style={{ borderBottom: '1px solid #444' }}>
              <td style={{ padding: '12px 0', fontWeight: '500' }}>Name:</td>
              <td style={{ padding: '12px 0' }}>{user?.name || 'N/A'}</td>
            </tr>
            <tr>
              <td style={{ padding: '12px 0', fontWeight: '500' }}>Email Verified:</td>
              <td style={{ padding: '12px 0' }}>
                {user?.email_verified ? (
                  <span style={{ color: '#28a745' }}>✓ Yes</span>
                ) : (
                  <span style={{ color: '#dc3545' }}>✗ No</span>
                )}
              </td>
            </tr>
          </tbody>
        </table>
      </div>

      {dashboardData?.recentActivities && (
        <div className="card">
          <h3 style={{ marginBottom: '16px' }}>Recent Activities</h3>
          <ul style={{ listStyle: 'none', padding: 0 }}>
            {dashboardData.recentActivities.map((activity, index) => (
              <li
                key={index}
                style={{
                  padding: '12px',
                  background: '#2a2a2a',
                  marginBottom: '8px',
                  borderRadius: '6px',
                  borderLeft: '4px solid #646cff',
                }}
              >
                {activity}
              </li>
            ))}
          </ul>
        </div>
      )}

      {dashboardData?.lastLogin && (
        <div className="card">
          <h3 style={{ marginBottom: '16px' }}>Token Information</h3>
          <table style={{ width: '100%', borderCollapse: 'collapse' }}>
            <tbody>
              <tr style={{ borderBottom: '1px solid #444' }}>
                <td style={{ padding: '12px 0', fontWeight: '500' }}>Token Issued At:</td>
                <td style={{ padding: '12px 0' }}>
                  {new Date(dashboardData.lastLogin * 1000).toLocaleString()}
                </td>
              </tr>
              <tr>
                <td style={{ padding: '12px 0', fontWeight: '500' }}>Token Expires At:</td>
                <td style={{ padding: '12px 0' }}>
                  {new Date(dashboardData.tokenExpiry * 1000).toLocaleString()}
                </td>
              </tr>
            </tbody>
          </table>
        </div>
      )}
    </div>
  );
};

export default DashboardPage;
