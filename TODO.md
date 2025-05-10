# ComicCacher TODO List

* Add support for weekly comics and flag Foxtrot as a weekly comics
* See what's going on with Committed &amp; Griswells
* Add support for indexed comics (Xkcd, etc)
* Document add/remove

# User Management & Authentication Implementation Plan

## Phase 1: Core Backend Setup

### Step 1: Add Dependencies
- Add Spring Security dependencies to `build.gradle`
- Add JWT library (jjwt) for token management
- Add BCrypt for password hashing

### Step 2: Create User Data Models
- Create the following entities in `org.stapledon.dto`:
  - `User.java`: Core user entity with authentication data
  - `UserPreference.java`: User comic preferences
  - `UserConfig.java`: Container for storage
  - `AuthRequest.java`: Login request DTO
  - `AuthResponse.java`: Login response with JWT
  - `UserRegistrationDto.java`: Registration data
  - `JwtTokenDto.java`: Token data

### Step 3: JSON Storage Implementation
- Create `UserConfigWriter.java` in `org.stapledon.config`
- Implement methods to:
  - Save new users
  - Update user data
  - Verify credentials
  - Manage preferences

## Phase 2: Security Configuration

### Step 1: Core Security Setup
- Create `SecurityConfig.java` with JWT configuration
- Implement `JwtTokenFilter.java` for request authentication
- Create `JwtTokenUtil.java` for token generation/validation
- Add CORS configuration

### Step 2: Authentication Services
- Implement `UserService.java` interface and implementation
- Create `AuthenticationService.java` for login/registration
- Implement `UserDetailsService` for Spring Security

### Step 3: Exception Handling
- Add authentication exceptions to `exceptions` package
- Update `GlobalExceptionHandler.java` for auth errors

## Phase 3: API Endpoints

### Step 1: Authentication Controller
- Create `AuthController.java` with endpoints:
  - POST `/api/v1/auth/register`
  - POST `/api/v1/auth/login`
  - POST `/api/v1/auth/refresh-token`
  - POST `/api/v1/auth/logout`

### Step 2: User Management
- Create `UserController.java` with endpoints:
  - GET `/api/v1/users/profile`
  - PUT `/api/v1/users/profile`
  - PUT `/api/v1/users/password`

### Step 3: Preference Management
- Create `PreferenceController.java` with endpoints:
  - GET `/api/v1/preferences`
  - POST `/api/v1/preferences/comics/{id}/favorite`
  - DELETE `/api/v1/preferences/comics/{id}/favorite`
  - POST `/api/v1/preferences/comics/{id}/lastread`

### Step 4: Secure Existing Endpoints
- Add authentication requirements to existing endpoints
- Allow public access to comic viewing endpoints
- Restrict update operations to authenticated users

## Phase 4: Frontend Implementation

### Step 1: Core Authentication
- Create `auth.service.ts` with authentication logic
- Implement token storage and refresh handling
- Add HTTP interceptor for JWT inclusion
- Create authentication state management

### Step 2: Authentication Components
- Create `login.component.ts` and template
- Implement `register.component.ts` and template
- Add `profile.component.ts` for user settings
- Create route guards for protected routes

### Step 3: UI Integration
- Update app header with login/register buttons
- Add user profile dropdown menu
- Implement user preference toggles in comic view
- Create favorites view with bookmarked comics

### Step 4: Comic Reading Integration
- Update comic service to track reading position
- Implement "Add to favorites" functionality
- Save last read position per comic

## Phase 5: Testing & Security Hardening

### Step 1: Unit Testing
- Add tests for authentication services
- Create tests for JWT validation
- Test user preference functionality

### Step 2: Integration Testing
- Implement end-to-end authentication flow tests
- Test secure vs. public endpoint access
- Validate user preference persistence

### Step 3: Security Hardening
- Implement rate limiting for auth endpoints
- Add CSRF protection mechanisms
- Review and update CORS settings
- Implement password strength validation
- Add security event logging

## Phase 6: Documentation & Deployment

### Step 1: API Documentation
- Update Swagger documentation for new endpoints
- Add authentication requirements to API docs
- Document user preferences API

### Step 2: User Documentation
- Update README with authentication information
- Add user profile documentation
- Document favorite comics functionality

### Step 3: Deployment
- Update Docker images for new dependencies
- Configure JWT secret management for production
- Update deployment scripts

## Implementation Timeline

| Phase | Description | Estimated Time |
|-------|-------------|----------------|
| 1     | Core Backend Setup | 2-3 days |
| 2     | Security Configuration | 2 days |
| 3     | API Endpoints | 3 days |
| 4     | Frontend Implementation | 4-5 days |
| 5     | Testing & Security | 3 days |
| 6     | Documentation & Deployment | 1-2 days |

**Total Estimated Time**: 15-18 days