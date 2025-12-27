# Fix Build

Diagnose and fix build failures for backend (Maven) and frontend (Vite/npm).

## Diagnostic Steps

### Backend (Maven)

1. **Check compilation errors**
   ```bash
   cd nexusarchive-java && mvn clean compile 2>&1 | tail -50
   ```

2. **Check test failures**
   ```bash
   cd nexusarchive-java && mvn test 2>&1 | grep -A 10 "FAILURE\|ERROR"
   ```

3. **Check dependency issues**
   ```bash
   cd nexusarchive-java && mvn dependency:tree | grep -i conflict
   ```

4. **Common fixes**
   - Missing Lombok: Ensure IDE annotation processing enabled
   - Version conflicts: Check `<dependencyManagement>` in pom.xml
   - Resource not found: Check `src/main/resources/` paths

### Frontend (npm/Vite)

1. **Check TypeScript errors**
   ```bash
   npx tsc --noEmit 2>&1 | head -50
   ```

2. **Check build errors**
   ```bash
   npm run build 2>&1 | tail -50
   ```

3. **Check test failures**
   ```bash
   npm run test:run 2>&1 | grep -A 5 "FAIL\|Error"
   ```

4. **Common fixes**
   - Clear cache: `rm -rf node_modules/.vite && npm run dev`
   - Reinstall: `rm -rf node_modules && npm install`
   - Type errors: Check import paths and type definitions

## Usage

Run `/fix-build` when build fails. I will diagnose and propose fixes.
