// 测试文件：故意违反复杂度规则以验证严格模式
export const badFunction = () => {
    // 这个函数会违反 max-lines-per-function (50行)
    let x = 1;
    let y = 2;
    let z = 3;
    // ... 重复代码直到超过50行
    for (let i = 0; i < 100; i++) {
        x += i;
        y += i * 2;
        z += i * 3;
        console.log(x, y, z);
    }
    for (let i = 0; i < 100; i++) {
        x -= i;
        y -= i * 2;
        z -= i * 3;
        console.log(x, y, z);
    }
    for (let i = 0; i < 50; i++) {
        x = x + 1;
        y = y + 1;
        z = z + 1;
    }
    return x + y + z;
};

// 高复杂度函数 (complexity > 10)
export const complexFunction = (n: number) => {
    if (n > 0) {
        if (n > 10) {
            if (n > 20) {
                if (n > 30) {
                    if (n > 40) {
                        if (n > 50) {
                            if (n > 60) {
                                if (n > 70) {
                                    if (n > 80) {
                                        if (n > 90) {
                                            return n * 2;
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
    return n;
};
