import { createContext, useState, useContext, useEffect } from 'react';
import authService from '../services/authService';
import authConfig from '../config/authConfig';

const AuthContext = createContext(null);

export const AuthProvider = ({ children }) => {
  const [user, setUser] = useState(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);
  const [configLoaded, setConfigLoaded] = useState(false);

  useEffect(() => {
    const initializeAuth = async () => {
      try {
        // First, fetch auth configuration from backend
        await authConfig.fetchConfig();
        setConfigLoaded(true);

        // Then, check if user is already authenticated
        if (authService.isAuthenticated()) {
          const storedUser = authService.getStoredUserInfo();
          setUser(storedUser);
        }
      } catch (err) {
        console.error('Failed to initialize auth:', err);
        // Continue anyway with default config
        setConfigLoaded(true);
      } finally {
        setLoading(false);
      }
    };

    initializeAuth();
  }, []);

  // Custom login with username/password
  const login = async (username, password) => {
    try {
      setError(null);
      const response = await authService.login(username, password);
      const userInfo = response.data.userInfo;
      setUser(userInfo);
      return response;
    } catch (err) {
      const errorMessage =
        err.response?.data?.message || 'Login failed. Please check your credentials.';
      setError(errorMessage);
      throw new Error(errorMessage);
    }
  };

  // OAuth2 Authorization Code Flow
  const loginWithOAuth2 = async () => {
    try {
      setError(null);
      // This will redirect to Keycloak
      await authService.loginWithOAuth2();
    } catch (err) {
      const errorMessage =
        err.response?.data?.message || 'OAuth2 login failed.';
      setError(errorMessage);
      throw new Error(errorMessage);
    }
  };

  // Handle OAuth2 callback after redirect from Keycloak
  const handleOAuth2Callback = async (code) => {
    try {
      setError(null);
      const response = await authService.exchangeCodeForToken(code);
      const userInfo = response.data.userInfo;
      setUser(userInfo);
      return response;
    } catch (err) {
      const errorMessage =
        err.response?.data?.message || 'Authentication failed.';
      setError(errorMessage);
      throw new Error(errorMessage);
    }
  };

  const logout = async () => {
    try {
      await authService.logout();
      setUser(null);
      setError(null);
    } catch (err) {
      console.error('Logout error:', err);
    }
  };

  const refreshUserInfo = async () => {
    try {
      const userInfo = await authService.getUserInfo();
      setUser(userInfo);
      localStorage.setItem('user_info', JSON.stringify(userInfo));
    } catch (err) {
      console.error('Failed to refresh user info:', err);
    }
  };

  const value = {
    user,
    login,
    loginWithOAuth2,
    handleOAuth2Callback,
    logout,
    loading,
    error,
    isAuthenticated: !!user,
    refreshUserInfo,
    configLoaded,
    authConfig, // Expose authConfig for components
  };

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>;
};

export const useAuth = () => {
  const context = useContext(AuthContext);
  if (!context) {
    throw new Error('useAuth must be used within an AuthProvider');
  }
  return context;
};

export default AuthContext;
