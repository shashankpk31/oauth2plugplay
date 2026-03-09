import axios from 'axios';
import authConfig from '../config/authConfig';

const API_BASE_URL = authConfig.apiBaseUrl;

// Create axios instance with default config
const api = axios.create({
  baseURL: API_BASE_URL,
  headers: {
    'Content-Type': 'application/json',
  },
});

// Request interceptor to add JWT token
api.interceptors.request.use(
  (config) => {
    const token = localStorage.getItem('access_token');
    if (token) {
      config.headers.Authorization = `Bearer ${token}`;
    }
    return config;
  },
  (error) => {
    return Promise.reject(error);
  }
);

// Response interceptor to handle token expiration
api.interceptors.response.use(
  (response) => response,
  async (error) => {
    const originalRequest = error.config;

    // If 401 and we haven't tried to refresh yet
    if (error.response?.status === 401 && !originalRequest._retry) {
      originalRequest._retry = true;

      const refreshToken = localStorage.getItem('refresh_token');
      if (refreshToken) {
        try {
          const response = await authService.refreshToken(refreshToken);
          localStorage.setItem('access_token', response.data.access_token);
          if (response.data.refresh_token) {
            localStorage.setItem('refresh_token', response.data.refresh_token);
          }

          // Retry original request with new token
          originalRequest.headers.Authorization = `Bearer ${response.data.access_token}`;
          return api(originalRequest);
        } catch (refreshError) {
          // Refresh failed, logout user
          authService.logout();
          window.location.href = '/login';
          return Promise.reject(refreshError);
        }
      }
    }

    return Promise.reject(error);
  }
);

const authService = {
  /**
   * Login with username and password
   */
  async login(username, password) {
    const response = await axios.post(`${API_BASE_URL}/oauth2/login`, {
      username,
      password,
    });

    if (response.data.success && response.data.data) {
      const { access_token, refresh_token, userInfo } = response.data.data;

      localStorage.setItem('access_token', access_token);
      if (refresh_token) {
        localStorage.setItem('refresh_token', refresh_token);
      }
      if (userInfo) {
        localStorage.setItem('user_info', JSON.stringify(userInfo));
      }

      return response.data;
    }

    throw new Error('Login failed');
  },

  /**
   * Refresh access token
   */
  async refreshToken(refreshToken) {
    const response = await axios.post(`${API_BASE_URL}/oauth2/refresh`, {
      refreshToken,
    });
    return response.data.data;
  },

  /**
   * Logout
   */
  async logout() {
    try {
      const token = localStorage.getItem('access_token');
      if (token) {
        await axios.post(
          `${API_BASE_URL}/oauth2/logout`,
          {},
          {
            headers: {
              Authorization: `Bearer ${token}`,
            },
          }
        );
      }
    } catch (error) {
      console.error('Logout error:', error);
    } finally {
      localStorage.removeItem('access_token');
      localStorage.removeItem('refresh_token');
      localStorage.removeItem('user_info');
    }
  },

  /**
   * Get user info from token
   */
  async getUserInfo() {
    const response = await api.get('/oauth2/userinfo');
    return response.data.data;
  },

  /**
   * Validate token
   */
  async validateToken() {
    const response = await api.get('/oauth2/validate');
    return response.data.data;
  },

  /**
   * Check if user is authenticated
   */
  isAuthenticated() {
    return !!localStorage.getItem('access_token');
  },

  /**
   * Get stored user info
   */
  getStoredUserInfo() {
    const userInfo = localStorage.getItem('user_info');
    return userInfo ? JSON.parse(userInfo) : null;
  },

  /**
   * Get access token
   */
  getAccessToken() {
    return localStorage.getItem('access_token');
  },

  // ============================================
  // OAuth2 Authorization Code Flow Methods
  // ============================================

  /**
   * Get OAuth2 authorization URL
   * This will be used to redirect user to Keycloak login page
   */
  async getAuthorizationUrl() {
    const response = await axios.get(`${API_BASE_URL}/oauth2/authorize`, {
      params: {
        redirectUri: authConfig.redirectUri,
      },
    });

    if (response.data.success && response.data.data) {
      return response.data.data.authorizationUrl;
    }

    throw new Error('Failed to get authorization URL');
  },

  /**
   * Exchange authorization code for tokens
   * Called after redirect from Keycloak with authorization code
   */
  async exchangeCodeForToken(code) {
    const response = await axios.post(`${API_BASE_URL}/oauth2/token`, {
      code: code,
      redirectUri: authConfig.redirectUri,
    });

    if (response.data.success && response.data.data) {
      const { access_token, refresh_token, userInfo } = response.data.data;

      localStorage.setItem('access_token', access_token);
      if (refresh_token) {
        localStorage.setItem('refresh_token', refresh_token);
      }
      if (userInfo) {
        localStorage.setItem('user_info', JSON.stringify(userInfo));
      }

      return response.data;
    }

    throw new Error('Token exchange failed');
  },

  /**
   * Start OAuth2 login flow
   * Redirects to Keycloak login page
   */
  async loginWithOAuth2() {
    const authUrl = await this.getAuthorizationUrl();
    // This will redirect the browser to Keycloak
    window.location.href = authUrl;
  },
};

export default authService;
export { api };
