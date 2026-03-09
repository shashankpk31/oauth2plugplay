import { api } from './authService';

const apiService = {
  /**
   * Get user profile from backend
   */
  async getUserProfile() {
    const response = await api.get('/api/user/profile');
    return response.data.data;
  },

  /**
   * Get user dashboard data
   */
  async getDashboard() {
    const response = await api.get('/api/user/dashboard');
    return response.data.data;
  },

  /**
   * Get all JWT claims
   */
  async getClaims() {
    const response = await api.get('/api/user/claims');
    return response.data.data;
  },

  /**
   * Get public hello message
   */
  async getPublicHello() {
    const response = await api.get('/api/public/hello');
    return response.data.data;
  },

  /**
   * Health check
   */
  async healthCheck() {
    const response = await api.get('/api/public/health');
    return response.data.data;
  },
};

export default apiService;
