// Input: Login credentials type definition
// Output: LoginCredentials interface
// Pos: src/api/auth.types.ts
// 一旦我被更新,务必更新我的开头注释,以及所属的文件夹的 md。

/**
 * Login credentials interface
 */
export interface LoginCredentials {
    username: string;
    password: string;
    mfaCode?: string; // Optional MFA verification code
    rememberMe?: boolean; // Optional remember me flag
}
