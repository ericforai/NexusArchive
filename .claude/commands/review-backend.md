# Review Backend

Review Spring Boot backend code for issues and improvements.

## Checklist

1. **Controller Layer**
   - REST endpoint naming follows conventions (`/api/v1/...`)
   - Proper HTTP methods (GET/POST/PUT/DELETE)
   - Request validation with `@Valid`
   - Appropriate response status codes

2. **Service Layer**
   - Business logic in service, not controller
   - Transaction boundaries (`@Transactional`) correct
   - Exception handling appropriate

3. **Mapper/Repository Layer**
   - MyBatis-Plus annotations correct
   - No N+1 query issues
   - Proper use of `@Select`, `@Update` for custom queries

4. **Security**
   - Endpoints properly secured
   - No sensitive data in logs
   - Input sanitization for user data

5. **General**
   - Lombok annotations used consistently
   - No hardcoded values (use config)
   - Proper null handling

## Usage

Provide the file path or describe the code area to review.
