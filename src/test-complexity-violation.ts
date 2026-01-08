// 测试复杂度违规文件
export const testFunction = () => {
    let a = 1;
    let b = 2;
    let c = 3;
    let d = 4;
    let e = 5;
    let f = 6;
    let g = 7;
    let h = 8;
    let i = 9;
    let j = 10;
    if (a === 1) {
        if (b === 2) {
            if (c === 3) {
                if (d === 4) {
                    if (e === 5) {
                        return f;
                    }
                }
            }
        }
    }
    return 0;
};
