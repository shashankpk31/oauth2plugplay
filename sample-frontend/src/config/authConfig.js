// Authentication Configuration
// This configuration is fetched dynamically from the backend
// Frontend automatically adapts to backend's oauth2.custom-login-enabled setting

const authConfig = {
  // API Base URL
  apiBaseUrl: import.meta.env.VITE_API_BASE_URL || 'http://localhost:8081',

  // OAuth2 Redirect URI (must match Keycloak client configuration)
  redirectUri: import.meta.env.VITE_REDIRECT_URI || `${window.location.origin}/callback`,

  // Runtime configuration (fetched from backend)
  customLoginEnabled: null, // null = not yet fetched
  provider: null,
  clientId: null,

  // Fetch configuration from backend
  async fetchConfig() {
    try {
      const response = await fetch(`${this.apiBaseUrl}/oauth2/config`);
      const data = await response.json();

      if (data.success && data.data) {
        this.customLoginEnabled = data.data.customLoginEnabled;
        this.provider = data.data.provider;
        this.clientId = data.data.clientId;

        console.log('✅ Auth config loaded from backend:', {
          customLoginEnabled: this.customLoginEnabled,
          provider: this.provider,
        });

        return this;
      } else {
        throw new Error('Failed to load auth config');
      }
    } catch (error) {
      console.error('❌ Failed to fetch auth config from backend:', error);
      // Fallback to default (OAuth2 redirect)
      this.customLoginEnabled = false;
      throw error;
    }
  },

  // Check if config has been loaded
  isConfigLoaded() {
    return this.customLoginEnabled !== null;
  },

  // Get current auth mode
  useCustomLogin() {
    if (!this.isConfigLoaded()) {
      console.warn('Auth config not loaded yet. Call fetchConfig() first.');
      return false;
    }
    return this.customLoginEnabled;
  },
};

export default authConfig;
