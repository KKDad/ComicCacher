# Security Policy

## Supported Versions

| Version | Supported |
| ------- | --------- |
| Latest on `master` | Yes |
| Older versions | No |

Only the most recent release receives security updates.

## Reporting a Vulnerability

**Please do not open public issues for security vulnerabilities.**

Use GitHub's private vulnerability reporting to submit a report:

1. Go to the [Security tab](../../security) of this repository
2. Click **"Report a vulnerability"**
3. Provide a detailed description of the vulnerability, including steps to reproduce

## Response Process

- **Acknowledgment:** We will acknowledge receipt of your report within **48 hours**.
- **Resolution:** We aim to develop and release a fix within **90 days** of the initial report.
- **Disclosure:** We follow coordinated disclosure. Please allow us to release a fix before making any details public.

We will keep you informed of our progress throughout the process.

## Scope

### In Scope

- Authentication and authorization (JWT, session handling, password storage)
- API security (REST and GraphQL endpoints)
- Server-side proxy and token handling
- Image upload and processing validation
- Cross-site scripting (XSS), injection, and CSRF
- Information disclosure via error messages or logs

### Out of Scope

- Vulnerabilities in upstream/third-party dependencies (please report these directly to the dependency maintainer)
- Denial of service attacks against self-hosted instances
- Social engineering

## Security Architecture Overview

For context when investigating potential vulnerabilities:

- **Authentication:** JWT-based with httpOnly cookies (tokens are not accessible to client-side JavaScript)
- **Password Storage:** BCrypt hashing
- **API Proxy:** Server-side GraphQL proxy with automatic token refresh
- **Input Handling:** Sanitized error messages with truncation to prevent information leakage
- **Framework:** Spring Security with stateless session management
