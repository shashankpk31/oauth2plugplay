# Sample Frontend - React Vite with oidcplugplay

This is a sample React application demonstrating enterprise-level OAuth2/OIDC authentication using the **oidcplugplay** Spring Boot starter with Keycloak.

## Features

- ✅ React 18 with Vite for fast development
- ✅ JWT-based authentication with automatic token refresh
- ✅ Protected routes using React Router v6
- ✅ Context API for global authentication state
- ✅ Axios interceptors for automatic token injection
- ✅ Modern, responsive UI
- ✅ Login, Dashboard, and Profile pages
- ✅ Real-time user information display

## Technology Stack

- React 18
- Vite (Build tool)
- React Router v6
- Axios
- Context API
- Modern CSS

## Project Structure

```
sample-frontend/
├── src/
│   ├── components/
│   │   ├── Navbar.jsx               # Navigation bar component
│   │   └── PrivateRoute.jsx         # Protected route wrapper
│   ├── context/
│   │   └── AuthContext.jsx          # Authentication context provider
│   ├── pages/
│   │   ├── HomePage.jsx             # Landing page
│   │   ├── LoginPage.jsx            # Login page
│   │   ├── DashboardPage.jsx        # User dashboard (protected)
│   │   └── ProfilePage.jsx          # User profile (protected)
│   ├── services/
│   │   ├── authService.js           # Authentication API calls
│   │   └── apiService.js            # Application API calls
│   ├── App.jsx                      # Main app component
│   ├── main.jsx                     # Entry point
│   └── index.css                    # Global styles
├── index.html
├── vite.config.js
└── package.json
```

## Prerequisites

Before running this application, ensure you have:

1. **Node.js** (v18 or higher) installed
2. **npm** or **yarn** package manager
3. **Keycloak** running on Docker (see backend README)
4. **Backend application** (sample-backend) running on port 8081

## Installation

1. Install dependencies:

```bash
cd sample-frontend
npm install
```

## Running the Application

### Development Mode

```bash
npm run dev
```

The application will start on: http://localhost:5173

### Production Build

```bash
npm run build
npm run preview
```

## Configuration

The API base URL is configured in `src/services/authService.js`:

```javascript
const API_BASE_URL = 'http://localhost:8081';
```

Vite proxy is configured in `vite.config.js` to forward `/api` and `/oauth2` requests to the backend.

## Application Flow

### 1. Authentication Flow

```
1. User enters credentials on Login page
2. Frontend calls POST /oauth2/login
3. Backend validates with Keycloak
4. Keycloak returns JWT tokens
5. Frontend stores tokens in localStorage
6. User is redirected to Dashboard
```

### 2. Accessing Protected Routes

```
1. User navigates to protected route (Dashboard/Profile)
2. PrivateRoute checks if user is authenticated
3. If not authenticated, redirect to Login
4. If authenticated, render the protected component
```

### 3. Making Authenticated API Calls

```
1. Axios interceptor adds JWT token to request headers
2. Backend validates JWT token
3. If token is valid, process request
4. If token expired, interceptor refreshes token automatically
5. Original request is retried with new token
```

### 4. Automatic Token Refresh

The Axios response interceptor in `authService.js` handles token refresh:

```javascript
// When 401 error occurs
1. Extract refresh token from localStorage
2. Call POST /oauth2/refresh
3. Update access token in localStorage
4. Retry original failed request
5. If refresh fails, logout user and redirect to login
```

## Pages Overview

### Home Page (`/`)

- Public landing page
- Displays features and technology stack
- Shows public API test
- Login button for unauthenticated users
- Quick links for authenticated users

### Login Page (`/login`)

- Username and password inputs
- Login button with loading state
- Error message display
- Demo credentials helper

### Dashboard Page (`/dashboard`) - Protected

- Welcome message
- User information table
- Recent activities
- Token information (issued at, expires at)

### Profile Page (`/profile`) - Protected

- Complete user profile information
- User roles display
- JWT claims viewer (toggleable)
- Email verification status

## Authentication Context

The `AuthContext` provides global authentication state:

```javascript
const { user, login, logout, loading, error, isAuthenticated, refreshUserInfo } = useAuth();
```

### Available Methods

- `login(username, password)` - Authenticate user
- `logout()` - Logout user and clear tokens
- `refreshUserInfo()` - Refresh user info from token
- `isAuthenticated` - Boolean indicating auth status
- `user` - Current user object
- `error` - Authentication error message
- `loading` - Loading state

## API Integration

### Auth Service (`authService.js`)

```javascript
import authService from './services/authService';

// Login
await authService.login('username', 'password');

// Logout
await authService.logout();

// Get user info
const userInfo = await authService.getUserInfo();

// Validate token
const claims = await authService.validateToken();

// Refresh token
await authService.refreshToken(refreshToken);

// Check if authenticated
const isAuth = authService.isAuthenticated();
```

### API Service (`apiService.js`)

```javascript
import apiService from './services/apiService';

// Get user profile
const profile = await apiService.getUserProfile();

// Get dashboard data
const dashboard = await apiService.getDashboard();

// Get JWT claims
const claims = await apiService.getClaims();

// Public endpoints
const hello = await apiService.getPublicHello();
const health = await apiService.healthCheck();
```

## Testing the Application

### 1. Start Backend and Keycloak

```bash
# From root directory
docker-compose up -d
cd sample-backend
mvn spring-boot:run
```

### 2. Start Frontend

```bash
cd sample-frontend
npm run dev
```

### 3. Test Login

1. Navigate to http://localhost:5173
2. Click "Login" button
3. Enter credentials:
   - Username: `testuser`
   - Password: `password123`
4. Click "Login"
5. You should be redirected to Dashboard

### 4. Test Protected Routes

- Try accessing `/dashboard` without logging in (should redirect to login)
- Login and access `/dashboard` (should work)
- Access `/profile` to see user information
- Check JWT claims in Profile page

### 5. Test Token Refresh

1. Login to the application
2. Wait for token to expire (or modify token validity in backend)
3. Make an API call (navigate to Dashboard)
4. Token should refresh automatically without logout

### 6. Test Logout

1. Click "Logout" button in navbar
2. You should be redirected to login page
3. Try accessing `/dashboard` (should redirect to login)

## Local Storage

The application stores the following in localStorage:

- `access_token` - JWT access token
- `refresh_token` - JWT refresh token (if provided)
- `user_info` - User information object

You can inspect these in Chrome DevTools:
1. Open DevTools (F12)
2. Go to Application tab
3. Select Local Storage → http://localhost:5173

## Troubleshooting

### Cannot connect to backend

**Issue**: Network errors when calling API

**Solution**:
- Ensure backend is running on port 8081
- Check `API_BASE_URL` in `authService.js`
- Verify CORS configuration in backend

### Token expired error

**Issue**: Receiving 401 errors immediately after login

**Solution**:
- Check system time (JWT tokens are time-sensitive)
- Verify Keycloak issuer URI matches backend configuration
- Check token validity in backend application.properties

### Login fails

**Issue**: Invalid username or password

**Solution**:
- Verify user exists in Keycloak
- Check username and password are correct
- Verify "Direct Access Grants" is enabled in Keycloak client
- Check backend logs for detailed error

### Blank page after login

**Issue**: Redirected but page is blank

**Solution**:
- Check browser console for errors
- Verify user info is stored in localStorage
- Check if backend `/api/user/profile` endpoint is accessible
- Verify JWT token is valid

### Automatic refresh not working

**Issue**: Token expires and user is logged out

**Solution**:
- Check if refresh token is stored in localStorage
- Verify refresh token endpoint is working
- Check axios interceptor configuration
- Ensure backend refresh endpoint returns new tokens

## Browser Compatibility

This application works on all modern browsers:

- Chrome/Edge (Recommended)
- Firefox
- Safari
- Opera

Requires ES6+ support.

## Development Tips

### Hot Reload

Vite provides instant hot module replacement (HMR). Changes to React components will reflect immediately without full page reload.

### Debugging

1. **Redux DevTools**: Not applicable (using Context API)
2. **React DevTools**: Install browser extension for React debugging
3. **Network Tab**: Monitor API calls in browser DevTools
4. **Console Logs**: authService and apiService log important events

### Code Organization

- Keep components small and focused
- Use custom hooks for reusable logic
- Centralize API calls in service files
- Use Context for global state (authentication)
- Keep styling modular

## Production Deployment

### Build for Production

```bash
npm run build
```

This creates optimized production files in the `dist/` directory.

### Environment Variables

Create `.env.production` file:

```env
VITE_API_BASE_URL=https://your-api-domain.com
```

Update `authService.js`:

```javascript
const API_BASE_URL = import.meta.env.VITE_API_BASE_URL || 'http://localhost:8081';
```

### Deployment Options

1. **Static Hosting** (Netlify, Vercel, GitHub Pages)
2. **Docker** (serve with nginx)
3. **S3 + CloudFront** (AWS)
4. **Firebase Hosting**

### Security Considerations

1. **HTTPS**: Always use HTTPS in production
2. **Environment Variables**: Never commit sensitive data
3. **Token Storage**: Consider using httpOnly cookies instead of localStorage for enhanced security
4. **CORS**: Configure specific origins, not wildcard
5. **Content Security Policy**: Add CSP headers
6. **XSS Protection**: React escapes content by default, but be careful with `dangerouslySetInnerHTML`

## Future Enhancements

Potential improvements for this sample app:

- [ ] Remember me functionality
- [ ] Social login integration (Google, GitHub)
- [ ] Role-based component rendering
- [ ] User profile editing
- [ ] Password reset flow
- [ ] Email verification flow
- [ ] Multi-factor authentication
- [ ] Session management
- [ ] Activity logs
- [ ] Dark/Light theme toggle

## References

- [React Documentation](https://react.dev/)
- [Vite Documentation](https://vitejs.dev/)
- [React Router Documentation](https://reactrouter.com/)
- [Axios Documentation](https://axios-http.com/)
- [oidcplugplay Documentation](../oidcplugplay/README.md)
- [Backend Documentation](../sample-backend/README.md)

## License

MIT License

## Support

For issues or questions:
1. Check backend logs for API errors
2. Check browser console for frontend errors
3. Verify Keycloak configuration
4. Review oidcplugplay documentation

---

**Happy Coding! 🚀**
