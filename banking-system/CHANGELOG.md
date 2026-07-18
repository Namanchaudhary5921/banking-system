# Changelog

## [1.0.0] - 2026-07-15
### Added
- Initial release: 3-tier banking system (presentation/business/data)
- Customer onboarding, account management, deposits/withdrawals/transfers
- Atomic transfer logic with row-level locking
- Rule-based fraud detection (large amount, velocity, high % withdrawal)
- Audit log for all state-changing actions
- Role-based auth (ADMIN/TELLER) via Spring Security
- PostgreSQL production schema with stored procedures, triggers, and views
- Static HTML/CSS/JS frontend calling the REST API
- Unit tests for core transaction/account logic
- SDLC docs: requirements, design, test plan
