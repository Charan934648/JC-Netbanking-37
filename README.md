# JC Bank NetBanking

A secure real-time JC Bank netbanking system built with Java and Spring Boot. It includes JWT + OTP authentication, account management, live balance and transaction notifications via Server-Sent Events, fund transfers (NEFT/IMPS/RTGS), bill payments, audit logging, and role-based access. Uses Spring Security, JPA/Hibernate, MySQL, and Docker for deployment.

Real-time stream endpoint:

```text
GET /api/realtime/stream
Authorization: Bearer <jwt>
```
