# Frontend Setup Instructions

## Configuration Steps

### 1. Choose Authentication Mode

Open `src/config/authConfig.js` and set `useCustomLogin`:

```javascript
const authConfig = {
  // Set to true if backend has oauth2.custom-login-enabled=true
  // Set to false if backend has oauth2.custom-login-enabled=false
  useCustomLogin: false,  // ← Change this based on backend config

  // Other settings...
};
```

**Important:** This setting MUST match your backend configuration!

| Backend Setting | Frontend Setting |
|----------------|------------------|
| `oauth2.custom-login-enabled=true` | `useCustomLogin: true` |
| `oauth2.custom-login-enabled=false` | `useCustomLogin: false` |

### 2. Environment Variables (Optional)

Copy `.env.example` to `.env`:

```bash
cp .env.example .env
```

Modify values if needed:
```env
VITE_API_BASE_URL=http://localhost:8081
VITE_REDIRECT_URI=http://localhost:5173/callback
```

### 3. Update LoginPage (Optional)

If you want to use the dual-mode login page:

1. Backup current LoginPage:
   ```bash
   cp src/pages/LoginPage.jsx src/pages/LoginPage_Original.jsx
   ```

2. Replace with updated version:
   ```bash
   cp src/pages/LoginPage_Updated.jsx src/pages/LoginPage.jsx
   ```

Or manually copy the content from `LoginPage_Updated.jsx`.

### 4. Configure Keycloak (For OAuth2 Mode Only)

If using OAuth2 mode (`useCustomLogin: false`):

1. Open Keycloak Admin Console: http://localhost:8180
2. Login: `admin` / `admin`
3. Select realm: `sample-realm`
4. Go to **Clients** → `sample-app`
5. Ensure these settings:
   - **Valid Redirect URIs**: `http://localhost:5173/callback`
   - **Web Origins**: `http://localhost:5173`
   - **Access Type**: `confidential`
6. Click **Save**

### 5. Install Dependencies & Run

```bash
npm install
npm run dev
```

The app will run on http://localhost:5173

---

## Testing Different Modes

### Custom Login Mode (useCustomLogin: true)

**Backend:**
```properties
oauth2.custom-login-enabled=true
```

**Frontend:**
```javascript
useCustomLogin: true
```

**Behavior:**
- Shows username/password form on login page
- Submits directly to backend `/oauth2/login`
- No redirect to Keycloak

**Test:**
1. Go to http://localhost:5173/login
2. Enter: `testuser` / `testUser`
3. Should login directly

---

### OAuth2 Mode (useCustomLogin: false)

**Backend:**
```properties
oauth2.custom-login-enabled=false
```

**Frontend:**
```javascript
useCustomLogin: false
```

**Behavior:**
- Shows "Login with Keycloak" button
- Redirects to Keycloak login page
- Returns to `/callback` after authentication

**Test:**
1. Go to http://localhost:5173/login
2. Click "Login with Keycloak"
3. Redirected to Keycloak: http://localhost:8180/realms/sample-realm/...
4. Enter: `testuser` / `testUser`
5. Redirected to: http://localhost:5173/callback?code=...
6. Automatically exchanges code for token
7. Redirected to: http://localhost:5173/dashboard

---

## Troubleshooting

### Error: "Invalid redirect_uri"
**Solution:** Add `http://localhost:5173/callback` to Keycloak client's Valid Redirect URIs

### Error: "Custom login is not enabled"
**Solution:**
- Backend has `oauth2.custom-login-enabled=false`
- Frontend has `useCustomLogin: true`
- These must match! Set frontend to `false`

### Error: "No authorization code received"
**Solution:** Check browser URL for error parameters. Common causes:
- User cancelled login at Keycloak
- Invalid client configuration
- Incorrect redirect URI

### Stuck on Callback page
**Solution:** Check:
1. Browser console for errors
2. Network tab for failed `/oauth2/token` request
3. Backend logs for detailed error messages

### Callback page shows "Authentication Error"
**Solution:** Check URL parameters:
```
http://localhost:5173/callback?error=...&error_description=...
```
The error_description will tell you what went wrong.

---

## Files Modified

✅ `src/config/authConfig.js` - Configuration (NEW)
✅ `src/services/authService.js` - Added OAuth2 methods
✅ `src/context/AuthContext.jsx` - Added OAuth2 context
✅ `src/pages/CallbackPage.jsx` - OAuth2 callback handler (NEW)
✅ `src/pages/LoginPage_Updated.jsx` - Dual-mode login (NEW)
✅ `src/App.jsx` - Added /callback route

---

## Quick Start Checklist

- [ ] Set `useCustomLogin` in `src/config/authConfig.js`
- [ ] Ensure backend `oauth2.custom-login-enabled` matches
- [ ] (OAuth2 mode only) Configure Keycloak redirect URI
- [ ] Run `npm install`
- [ ] Run `npm run dev`
- [ ] Test login flow
- [ ] Verify tokens are stored in localStorage
- [ ] Test protected routes (/dashboard, /profile)

---

## Next Steps

After successful setup, you can:
1. Customize the login page UI
2. Add additional OAuth2 providers (Google, GitHub, etc.)
3. Implement remember me functionality
4. Add multi-factor authentication
5. Customize user profile page

For more details, see:
- `FRONTEND_MIGRATION_GUIDE.md` - Detailed migration guide
- `SECURITY.md` - Spring Security concepts
- Backend `README.md` - Backend configuration

---

**Need Help?**

- Check browser console for errors
- Enable debug logging in backend: `logging.level.org.springframework.security=DEBUG`
- Review network requests in DevTools
- Check Keycloak logs: `docker logs keycloak`
