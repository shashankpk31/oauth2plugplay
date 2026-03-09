import { useAuth } from '../context/AuthContext';
import { useNavigate } from 'react-router-dom';

const Navbar = () => {
  const { user, isAuthenticated, logout } = useAuth();
  const navigate = useNavigate();

  const handleLogout = async () => {
    await logout();
    navigate('/login');
  };

  return (
    <nav className="navbar">
      <div className="navbar-content">
        <h1>🔐 oidcplugplay Demo</h1>
        <div className="navbar-user">
          {isAuthenticated ? (
            <>
              <span>Welcome, {user?.name || user?.preferred_username || 'User'}!</span>
              <button onClick={() => navigate('/dashboard')} className="btn btn-secondary">
                Dashboard
              </button>
              <button onClick={() => navigate('/profile')} className="btn btn-secondary">
                Profile
              </button>
              <button onClick={handleLogout} className="btn btn-danger">
                Logout
              </button>
            </>
          ) : (
            <button onClick={() => navigate('/login')} className="btn btn-primary">
              Login
            </button>
          )}
        </div>
      </div>
    </nav>
  );
};

export default Navbar;
