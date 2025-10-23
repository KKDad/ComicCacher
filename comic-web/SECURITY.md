# Security Notes

## Known Dependencies with Advisories

### Development Dependencies

#### vite 6.0.0 - 6.4.0 (Moderate Severity)
- **Issue**: server.fs.deny bypass via backslash on Windows
- **Advisory**: https://github.com/advisories/GHSA-93m4-6634-74q7
- **Affected**: @angular-devkit/build-angular via @angular/build dependency
- **Impact**: Development-only dependency, Windows-specific vulnerability
- **Status**: Accepted risk
- **Rationale**:
  - Only affects development builds, not production deployments
  - Vulnerability is Windows-specific; macOS and Linux are not affected
  - Severity is moderate, not critical
  - Waiting for Angular team to update the dependency in future releases

## Security Best Practices

When working with this project:

1. **Never commit sensitive data** like API keys or credentials
2. **Keep dependencies updated** - Run `npm audit` regularly
3. **Use `.npmrc`** - The project uses public npm registry only
4. **Review updates** - Always review dependency updates before applying

## Reporting Security Issues

If you discover a security vulnerability in this project, please report it to the project maintainers privately rather than opening a public issue.

## Update History

- **2025-10-23**: Upgraded to Angular 19.2, Node 22 LTS, TypeScript 5.8
  - 3 moderate severity vulnerabilities identified in dev dependencies (vite)
  - All production dependencies have no known vulnerabilities
