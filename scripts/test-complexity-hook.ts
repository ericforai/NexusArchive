// Test file for pre-commit complexity hook
// This file has some intentional complexity to test the hook

export function testComplexFunction(x: number, y: number, z: number, a: number, b: number, c: number, d: number, e: number, f: number, g: number, h: number): number {
    // This function has 11 parameters to test max-params rule (warns at 10)
    if (x > 0) {
        if (y > 0) {
            if (z > 0) {
                if (a > 0) {
                    if (b > 0) {
                        // 5 levels of nesting to test max-depth rule (warns at 4)
                        return x + y + z + a + b + c + d + e + f + g + h;
                    }
                }
            }
        }
    }
    return 0;
}
